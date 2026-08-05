#!/usr/bin/env bash
# STOP - sunucuda calisan deploy scripti.
#
# deploy.yml bu dosyayi base64'leyip SSM ile /opt/STOP/deploy.sh olarak yazar ve
# ARKA PLANDA (setsid nohup) baslatir; sonra kisa SSM komutlariyla durum dosyasini
# yoklar. Isin kendisi SSM document worker'inin ICINDE calismaz -- eskiden oyleydi
# ve deploy 10+ dakika surdugunde SSM su hatayla dusuyordu:
#   "document process failed unexpectedly: ipc messaging received timeout signal"
#
# Dosya olarak repoda durdugu icin `bash -n` / shellcheck ile denetlenebiliyor;
# eskiden deploy.yml icinde bir heredoc'un icine gomuluydu.
set -euo pipefail

APP_DIR=/opt/STOP
REGION=eu-north-1
GHCR_REGISTRY=ghcr.io/mehmettguzell

INFRA="broker redis identity-db match-db communication-db notification-db"
SERVICES="identity-service match-service communication-service notification-service api-gateway"

# Tek bir servisin saglikli olmasi icin taninan sure. Olculen deger: prod imaji
# bos bir makinede saglik ucunu UP dondurene kadar ~50sn aliyor (JPA/Hibernate +
# Kafka consumer'lari + springdoc taramasi). Sunucuda es zamanli yuk altinda bu
# rahatlikla katlaniyor, o yuzden genis tutuldu.
HEALTH_BUDGET=240

COMPOSE="docker compose --env-file $APP_DIR/.env -f $APP_DIR/docker-compose.yml"

# Cikis kodunu durum dosyasina yaz: Actions tarafi bu dosyayi yokluyor.
trap 'rc=$?; echo "$rc" > "$APP_DIR/deploy.status"; exit $rc' EXIT

command -v jq >/dev/null 2>&1 || { apt-get update -y && apt-get install -y jq; }

get_secret() {
  aws secretsmanager get-secret-value --region "$REGION" --secret-id "$1" \
    --query SecretString --output text
}

# Sunucu hem GHCR'a hem Secrets Manager'a kendi IAM instance rolu ile
# kimlik dogruluyor; bu Actions kosusundan hicbir uzun omurlu AWS kimligi
# gecmiyor. GHCR pull token'i GitHub'da IAM benzeri kisa omurlu bir karsiligi
# olmadigi icin zorunlu bir uzun omurlu sir; o da Secrets Manager'da duruyor.
GHCR_PAT=$(get_secret stop/prod/ghcr-pat | jq -r .token)
echo "$GHCR_PAT" | docker login ghcr.io --username mehmettguzell --password-stdin

IDENTITY_DB_PASSWORD=$(get_secret stop/prod/identity-db | jq -r .password)
MATCH_DB_PASSWORD=$(get_secret stop/prod/match-db | jq -r .password)
COMMUNICATION_DB_PASSWORD=$(get_secret stop/prod/communication-db | jq -r .password)
NOTIFICATION_DB_PASSWORD=$(get_secret stop/prod/notification-db | jq -r .password)
JWT_PUBLIC_KEY=$(get_secret stop/prod/jwt-keys | jq -r .public)
JWT_PRIVATE_KEY=$(get_secret stop/prod/jwt-keys | jq -r .private)

cat > "$APP_DIR/.env" <<ENVEOF
SPRING_PROFILES_ACTIVE=prod
ALLOWED_ORIGIN=https://stophalisaha.duckdns.org
GHCR_REGISTRY=$GHCR_REGISTRY
IMAGE_TAG=latest

COMMUNICATION_DB_NAME=communication_db
COMMUNICATION_DB_USER=communication_user
COMMUNICATION_DB_PASSWORD=$COMMUNICATION_DB_PASSWORD
COMMUNICATION_DB_URL=jdbc:postgresql://communication-db:5432/communication_db

IDENTITY_DB_NAME=identity_db
IDENTITY_DB_USER=identity_user
IDENTITY_DB_PASSWORD=$IDENTITY_DB_PASSWORD
IDENTITY_DB_URL=jdbc:postgresql://identity-db:5432/identity_db

MATCH_DB_NAME=match_db
MATCH_DB_USER=match_user
MATCH_DB_PASSWORD=$MATCH_DB_PASSWORD
MATCH_DB_URL=jdbc:postgresql://match-db:5432/match_db

NOTIFICATION_DB_NAME=notification_db
NOTIFICATION_DB_USER=notification_user
NOTIFICATION_DB_PASSWORD=$NOTIFICATION_DB_PASSWORD
NOTIFICATION_DB_URL=jdbc:postgresql://notification-db:5432/notification_db

KAFKA_BOOTSTRAP_SERVERS=broker:9092
REDIS_URL=redis://redis:6379
MATCH_SERVICE_URL=http://match-service:8082/api/v1

GATEWAY_IDENTITY_URI=http://identity-service:8081
GATEWAY_MODERATION_URI=http://identity-service:8081
GATEWAY_MATCH_URI=http://match-service:8082
GATEWAY_NOTIFICATION_URI=http://notification-service:8083
GATEWAY_COMMUNICATION_URI=http://communication-service:8084
GATEWAY_COMMUNICATION_WS_URI=ws://communication-service:8084

JWT_PUBLIC_KEY="$JWT_PUBLIC_KEY"
JWT_PRIVATE_KEY="$JWT_PRIVATE_KEY"
ENVEOF
chmod 600 "$APP_DIR/.env"

# Su an calisan `latest`i yerel `previous` etiketiyle isaretle -- saf yerel
# retag (ag yok), yani bu kosuda yeniden derlenmemis bir servis icin de
# calisiyor. Rollback hedefi bu.
for svc in $SERVICES; do
  docker tag "$GHCR_REGISTRY/stop/$svc:latest" "$GHCR_REGISTRY/stop/$svc:previous" 2>/dev/null || true
done

wait_one() {
  svc=$1
  budget=$2
  deadline=$(( SECONDS + budget ))
  status=none
  while [ "$SECONDS" -lt "$deadline" ]; do
    status=$(docker inspect -f '{{.State.Health.Status}}' "$svc" 2>/dev/null || echo none)
    if [ "$status" = "healthy" ]; then
      return 0
    fi
    if [ "$(docker inspect -f '{{.State.Status}}' "$svc" 2>/dev/null || echo none)" = "exited" ]; then
      echo "HATA: $svc oldu. Son loglar:"
      docker logs --tail 40 "$svc" 2>&1 || true
      return 1
    fi
    sleep 5
  done
  echo "HATA: $svc ${budget}sn icinde saglikli olmadi (son durum: $status). Son loglar:"
  docker logs --tail 40 "$svc" 2>&1 || true
  return 1
}

echo "=== imajlar cekiliyor ==="
$COMPOSE pull

echo "=== altyapi aciliyor ==="
$COMPOSE up -d --remove-orphans $INFRA

# Servisler TEK TEK aciliyor. Eskiden tek bir `up -d` hepsini ayni anda
# baslatiyordu: bes JVM es zamanli acilis yapinca kucuk bir EC2'da CPU ve
# bellek tavan yapiyor, bu da hem acilislari yavaslatiyor hem de SSM agent'ini
# ac birakip yukarida anlatilan IPC timeout'una zemin hazirliyordu.
# Sirali acilis toplam sureyi uzatir ama tepe yuku ciddi sekilde dusurur ve
# hangi servisin patladigini net gosterir.
failed=""
for svc in $SERVICES; do
  echo "=== $svc aciliyor ==="
  $COMPOSE up -d --no-deps "$svc"
  if ! wait_one "$svc" "$HEALTH_BUDGET"; then
    failed=$svc
    break
  fi
  echo "=== $svc saglikli ==="
done

if [ -n "$failed" ]; then
  echo "=== rollback: $failed acilmadi, previous imajlara donuluyor ==="
  IMAGE_TAG=previous $COMPOSE up -d --remove-orphans
  for svc in $SERVICES; do
    wait_one "$svc" 120 || echo "UYARI: rollback sonrasi $svc de saglikli degil"
  done
  exit 1
fi

docker image prune -f
echo "=== deploy basarili ==="

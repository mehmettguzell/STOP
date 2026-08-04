#!/bin/bash
# EC2 User Data - Ubuntu 22.04/24.04
# Sadece bootstrap: instance ilk acilista sunucuyu Docker calistirmaya
# hazir hale getirir. Uygulamanin kendisini deploy etmez (bu CI/CD'nin isi).
set -euxo pipefail

# ---- Sistemi guncelle ----
apt-get update -y
apt-get upgrade -y

# ---- Docker'in resmi apt reposunu ekle ----
apt-get install -y ca-certificates curl gnupg git unzip
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip -q awscliv2.zip
./aws/install
rm -rf awscliv2.zip aws/

# ---- Docker Engine + Compose plugin kur ----
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker

# ubuntu kullanicisini docker grubuna ekle (sudo'suz docker komutu icin)
usermod -aG docker ubuntu

import React, {
  useCallback,
  useEffect,
  useState,
  useLayoutEffect,
} from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  RefreshControl,
  TouchableOpacity,
  Modal,
  TextInput,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { matchApi } from "../../api/match.api";
import { participationApi } from "../../api/participant.api";
import { userApi } from "../../api/user.api";
import { invitationApi } from "../../api/invitation.api";
import { friendshipApi } from "../../api/friendship.api";
import {
  MatchResponse,
  MatchParticipantResponse,
  ParticipationRequestResponse,
  MatchStatus,
} from "../../types/match.types";
import { UserSelfResponse } from "../../types/user.types";
import { MatchDetailScreenProps } from "../../navigation/types";
import { useFocusEffect } from "@react-navigation/native";
import { useAuthStore } from "../../store/auth.store";
import Button from "../../components/ui/Button";
import Card from "../../components/ui/Card";
import Badge from "../../components/ui/Badge";
import Avatar from "../../components/ui/Avatar";
import FieldVisualization from "../../components/ui/FieldVisualization";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { getErrorMessage } from "../../utils/error";

const STATUS_LABELS: Record<MatchStatus, string> = {
  CREATED: "Oluşturuldu",
  OPEN: "Açık",
  FULL: "Dolu",
  STARTED: "Başladı",
  COMPLETED: "Tamamlandı",
  CANCELLED: "İptal Edildi",
  RATINGS_CLOSED: "Sona Erdi",
};

const STATUS_COLORS: Record<MatchStatus, string> = {
  CREATED: Colors.info,
  OPEN: Colors.primary,
  FULL: Colors.warning,
  STARTED: Colors.accent,
  COMPLETED: "#34D399",
  CANCELLED: Colors.error,
  RATINGS_CLOSED: Colors.amber,
};

export default function MatchDetailScreen({
  route,
  navigation,
}: MatchDetailScreenProps) {
  const { matchId, rated } = route.params;
  const currentUserId = useAuthStore((s) => s.user?.id);

  const [match, setMatch] = useState<MatchResponse | null>(null);
  const [participants, setParticipants] = useState<MatchParticipantResponse[]>(
    [],
  );
  const [pendingRequests, setPendingRequests] = useState<
    ParticipationRequestResponse[]
  >([]);
  const [myRequest, setMyRequest] =
    useState<ParticipationRequestResponse | null>(null);
  const [displayNames, setDisplayNames] = useState<Record<string, string>>({});
  const [avatarUrls, setAvatarUrls] = useState<Record<string, string | null>>({});
  const [loading, setLoading] = useState(true);
  const [hasRated, setHasRated] = useState(rated ?? false);
  const [refreshing, setRefreshing] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [transferModalVisible, setTransferModalVisible] = useState(false);
  const [scoreModalVisible, setScoreModalVisible] = useState(false);
  const [inviteModalVisible, setInviteModalVisible] = useState(false);
  const [inviteSearch, setInviteSearch] = useState("");
  const [inviteResults, setInviteResults] = useState<UserSelfResponse[]>([]);
  const [inviteSearchLoading, setInviteSearchLoading] = useState(false);
  const [sentInvites, setSentInvites] = useState<Set<string>>(new Set());
  const [friends, setFriends] = useState<
    { id: string; displayName: string; avatarUrl: string | null }[]
  >([]);
  const [friendsLoading, setFriendsLoading] = useState(false);
  const [homeScore, setHomeScore] = useState("0");
  const [awayScore, setAwayScore] = useState("0");
  const [teamModalVisible, setTeamModalVisible] = useState(false);
  const [teamDraft, setTeamDraft] = useState<Record<string, "HOME" | "AWAY">>(
    {},
  );

  // 1. Temel Durumlar
  const isOrganizer = match?.organizerId === currentUserId;
  const isParticipant = participants.some((p) => p.userId === currentUserId);
  const isMatchActive =
    match &&
    match.status !== "COMPLETED" &&
    match.status !== "CANCELLED" &&
    match.status !== "STARTED" &&
    match.status !== "RATINGS_CLOSED";

  // 2. İstek Durumları (canJoin'den önce tanımlanmalı)
  const hasPendingRequest = myRequest?.status === "PENDING";
  const isRejected = myRequest?.status === "REJECTED";
  const isCancelledRequest = myRequest?.status === "CANCELLED";

  // 3. Kullanıcı ayrılmış mı kontrolü (myRequestBlocksJoin yerine daha temiz çözüm)
  // Onaylanmış bir isteği olup da katılımcı listesinde değilse ayrılmış demektir.
  const hasLeftMatch = myRequest?.status === "APPROVED" && !isParticipant;

  // 4. Katılma Butonu Mantığı
  const canJoin =
    match &&
    !isParticipant &&
    !isOrganizer &&
    !hasPendingRequest &&
    (myRequest === null || isRejected || isCancelledRequest || hasLeftMatch) &&
    (match.status === "OPEN" || match.status === "CREATED") &&
    match.participantCount < match.capacity;

  // 5. Ayrılma Butonu Mantığı
  const canLeave =
    isParticipant &&
    !isOrganizer &&
    match &&
    (match.status === "CREATED" ||
      match.status === "OPEN" ||
      match.status === "FULL");

  const fetchDisplayNames = useCallback(async (userIds: string[]) => {
    const uniqueIds = Array.from(new Set(userIds));
    const names: Record<string, string> = {};
    const avatars: Record<string, string | null> = {};
    try {
      const users = await userApi.getUsersByIds(uniqueIds);
      const userMap = new Map(users.map((u) => [u.id, u]));
      uniqueIds.forEach((uid) => {
        const user = userMap.get(uid);
        names[uid] = user?.displayName ?? uid.substring(0, 8) + "...";
        avatars[uid] = user?.avatarUrl ?? null;
      });
    } catch {
      uniqueIds.forEach((uid) => {
        names[uid] = uid.substring(0, 8) + "...";
      });
    }
    setDisplayNames((prev) => ({ ...prev, ...names }));
    setAvatarUrls((prev) => ({ ...prev, ...avatars }));
  }, []);

  const fetchData = useCallback(async () => {
    try {
      setError("");
      const [matchData, participantData] = await Promise.all([
        matchApi.getById(matchId),
        participationApi.getParticipants(matchId),
      ]);
      setMatch(matchData);
      setParticipants(participantData);

      const userIds = participantData.map((p) => p.userId);

      if (matchData.organizerId === currentUserId) {
        // Organizer: fetch pending requests for private matches
        if (
          matchData.visibility === "PRIVATE" &&
          matchData.status !== "COMPLETED" &&
          matchData.status !== "CANCELLED"
        ) {
          try {
            const requests = await participationApi.getMatchRequests(matchId);
            setPendingRequests(requests);
            const requestUserIds = requests
              .map((r) => r.userID)
              .filter((id) => !userIds.includes(id));
            if (requestUserIds.length > 0) userIds.push(...requestUserIds);
          } catch {
            setPendingRequests([]);
          }
        } else {
          setPendingRequests([]);
        }
      } else {
        // Non-organizer: check own request status
        setPendingRequests([]);
        try {
          const req = await participationApi.getMyRequest(matchId);
          setMyRequest(req || null);
        } catch {
          setMyRequest(null);
        }
      }

      if (userIds.length > 0) {
        await fetchDisplayNames(userIds);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [matchId, currentUserId, fetchDisplayNames]);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      fetchData().finally(() => setLoading(false));
    }, [fetchData])
  );

  useEffect(() => {
    if (rated) setHasRated(true);
  }, [rated]);

  useLayoutEffect(() => {
    if (loading || !match) {
      navigation.setOptions({ headerRight: undefined });
      return;
    }

    const showChat = isParticipant || isOrganizer;
    const showEdit =
      isOrganizer &&
      (match.status === "CREATED" ||
        match.status === "OPEN" ||
        match.status === "FULL");

    navigation.setOptions({
      headerRight: () => (
        <View
          style={{
            flexDirection: "row",
            alignItems: "center",
            gap: 15,
            marginRight: 5,
          }}
        >
          {showEdit && (
            <TouchableOpacity
              onPress={() =>
                navigation.navigate("EditMatch", { matchId: match.id })
              }
              activeOpacity={0.7}
            >
              <Text style={{ fontSize: 22 }}>✏️</Text>
            </TouchableOpacity>
          )}

          {showChat && (
            <TouchableOpacity
              onPress={() =>
                navigation.navigate("Chat", {
                  chatId: matchId,
                  title: match.title,
                  chatType: "MATCH",
                })
              }
              activeOpacity={0.7}
            >
              <Text style={{ fontSize: 24 }}>💬</Text>
            </TouchableOpacity>
          )}
        </View>
      ),
    });
  }, [navigation, match, isParticipant, isOrganizer, loading]);

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchData();
    setRefreshing(false);
  };

  const handleJoin = async () => {
    if (!match) return;
    setActionLoading(true);
    try {
      const req = await participationApi.sendRequest(match.id);
      if (match.visibility === "PUBLIC") {
        Alert.alert("Katıldın!", "Maça başarıyla katıldın.");
      } else {
        Alert.alert(
          "İstek Gönderildi",
          "Organizatör onayladığında katılımın onaylanacak.",
        );
        setMyRequest(req);
      }
      await fetchData();
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleWithdraw = async () => {
    if (!myRequest) return;
    Alert.alert(
      "İsteği Geri Al",
      "Katılım isteğini geri almak istediğine emin misin?",
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Geri Al",
          style: "destructive",
          onPress: async () => {
            setActionLoading(true);
            try {
              await participationApi.withdraw(myRequest.id);
              Alert.alert("Tamam", "İsteğin geri alındı.");
              setMyRequest(null);
              await fetchData();
            } catch (err) {
              Alert.alert("Hata", getErrorMessage(err));
            } finally {
              setActionLoading(false);
            }
          },
        },
      ],
    );
  };

  const handleLeave = async () => {
    if (!match) return;
    Alert.alert("Maçtan Ayrıl", "Bu maçtan ayrılmak istediğine emin misin?", [
      { text: "Vazgeç", style: "cancel" },
      {
        text: "Ayrıl",
        style: "destructive",
        onPress: async () => {
          setActionLoading(true);
          try {
            await participationApi.leave(match.id);
            Alert.alert("Oldu", "Maçtan ayrıldın.");
            await fetchData();
          } catch (err) {
            Alert.alert("Hata", getErrorMessage(err));
          } finally {
            setActionLoading(false);
          }
        },
      },
    ]);
  };

  const handleCancel = () => {
    if (!match) return;
    Alert.alert(
      "Maçı İptal Et",
      "Bu maçı iptal etmek istediğine emin misin? Bu işlem geri alınamaz.",
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "İptal Et",
          style: "destructive",
          onPress: async () => {
            setActionLoading(true);
            try {
              await matchApi.cancel(match.id);
              Alert.alert("İptal Edildi", "Maç silindi.", [
                {
                  text: "Tamam",
                  onPress: () => navigation.replace("MatchList"),
                },
              ]);
            } catch (err) {
              setActionLoading(false);
              Alert.alert("Hata", getErrorMessage(err));
            }
          },
        },
      ],
    );
  };

  const handleStart = async () => {
    if (!match) return;
    setActionLoading(true);
    try {
      await matchApi.start(match.id);
      Alert.alert("Maç Başladı!", "İyi maçlar!");
      await fetchData();
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const openTeamModal = () => {
    if (!match) return;
    // Mevcut takım atamalarını draft'a yükle, atanmamışları HOME yap (otomatik dağıtım için)
    const draft: Record<string, "HOME" | "AWAY"> = {};
    participants.forEach((p, i) => {
      draft[p.userId] = p.team ?? (i % 2 === 0 ? "HOME" : "AWAY");
    });
    setTeamDraft(draft);
    setTeamModalVisible(true);
  };

  const toggleTeam = (userId: string) => {
    setTeamDraft((prev) => ({
      ...prev,
      [userId]: prev[userId] === "HOME" ? "AWAY" : "HOME",
    }));
  };

  const handleAssignTeams = async () => {
    if (!match) return;
    const assignments = participants.map((p) => ({
      userId: p.userId,
      team: teamDraft[p.userId] ?? "HOME",
    }));
    setTeamModalVisible(false);
    setActionLoading(true);
    try {
      await participationApi.assignTeams(match.id, { assignments });
      Alert.alert("Kaydedildi", "Takımlar güncellendi.");
      await fetchData();
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleComplete = async () => {
    if (!match) return;
    setScoreModalVisible(true);
  };

  const handleCompleteConfirm = async () => {
    if (!match) return;
    setScoreModalVisible(false);
    setActionLoading(true);
    try {
      await matchApi.complete(
        match.id,
        parseInt(homeScore) || 0,
        parseInt(awayScore) || 0,
      );
      Alert.alert("Tamamlandı!", "Maç bitti.");
      await fetchData();
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemovePlayer = (userId: string) => {
    if (!match) return;
    const name = displayNames[userId] || "Bu oyuncu";
    Alert.alert(
      "Oyuncuyu Çıkar",
      `${name} oyuncuyu maçtan çıkarmak istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Çıkar",
          style: "destructive",
          onPress: async () => {
            try {
              await participationApi.removePlayer(match.id, userId);
              Alert.alert("Tamam", "Oyuncu maçtan çıkarıldı.");
              await fetchData();
            } catch (err) {
              Alert.alert("Hata", getErrorMessage(err));
            }
          },
        },
      ],
    );
  };

  const handleApproveRequest = async (requestId: string) => {
    setActionLoading(true);
    try {
      await participationApi.approve(requestId);
      Alert.alert("Onaylandı", "Katılım isteği onaylandı.");
      await fetchData();
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectRequest = (requestId: string, userName: string) => {
    Alert.alert(
      "İsteği Reddet",
      `${userName} adlı kişinin isteğini reddetmek istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Reddet",
          style: "destructive",
          onPress: async () => {
            setActionLoading(true);
            try {
              await participationApi.reject(requestId);
              Alert.alert("Reddedildi", "Katılım isteği reddedildi.");
              await fetchData();
            } catch (err) {
              Alert.alert("Hata", getErrorMessage(err));
            } finally {
              setActionLoading(false);
            }
          },
        },
      ],
    );
  };

  const loadFriends = useCallback(async () => {
    setFriendsLoading(true);
    try {
      const friendships = await friendshipApi.getFriends();
      const participantIds = new Set(participants.map((p) => p.userId));
      participantIds.add(currentUserId ?? "");

      const candidateIds = Array.from(
        new Set(
          friendships
            .map((f) => (f.requesterId === currentUserId ? f.receiverId : f.requesterId))
            .filter((id) => !participantIds.has(id)),
        ),
      );
      const users = candidateIds.length ? await userApi.getUsersByIds(candidateIds) : [];
      setFriends(
        users.map((u) => ({ id: u.id, displayName: u.displayName, avatarUrl: u.avatarUrl })),
      );
    } catch {
      setFriends([]);
    } finally {
      setFriendsLoading(false);
    }
  }, [participants, currentUserId]);

  const handleInviteSearch = async (text: string) => {
    setInviteSearch(text);
    if (text.trim().length < 2) {
      setInviteResults([]);
      return;
    }
    setInviteSearchLoading(true);
    try {
      const res = await userApi.getAllUsers(0, 10, text.trim());
      const participantIds = new Set(participants.map((p) => p.userId));
      participantIds.add(currentUserId ?? "");
      const filtered = res.content.filter(
        (u) => !participantIds.has(u.id)
      );
      setInviteResults(filtered);
    } catch {
      setInviteResults([]);
    } finally {
      setInviteSearchLoading(false);
    }
  };

  const handleSendInvite = async (receiverId: string) => {
    if (!match) return;
    try {
      await invitationApi.create({ matchId: match.id, receiverId });
      setSentInvites((prev) => new Set(prev).add(receiverId));
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    }
  };

  const handleTransferOrganizer = async (newOrganizerId: string) => {
    if (!match) return;
    const name = displayNames[newOrganizerId] || "Bu oyuncu";
    Alert.alert(
      "Organizatörlüğü Devret",
      `${name} adlı oyuncuya organizatörlüğü devretmek istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Devret",
          onPress: async () => {
            setTransferModalVisible(false);
            setActionLoading(true);
            try {
              await matchApi.transferOrganizer(match.id, { newOrganizerId });
              Alert.alert("Devredildi", "Organizatörlük devredildi.");
              await fetchData();
            } catch (err) {
              Alert.alert("Hata", getErrorMessage(err));
            } finally {
              setActionLoading(false);
            }
          },
        },
      ],
    );
  };

  const navigateToProfile = (userId: string) => {
    navigation.navigate("PlayerProfile", { userId, matchId });
  };

  const formatDateTime = (iso: string) => {
    const d = new Date(iso);
    const day = d.getDate().toString().padStart(2, "0");
    const month = (d.getMonth() + 1).toString().padStart(2, "0");
    const year = d.getFullYear();
    const hours = d.getHours().toString().padStart(2, "0");
    const mins = d.getMinutes().toString().padStart(2, "0");
    return `${day}.${month}.${year}  ${hours}:${mins}`;
  };

  const getParticipantName = (userId: string): string => {
    if (userId === currentUserId) return "Sen";
    return displayNames[userId] || "...";
  };

  const otherParticipants = participants.filter(
    (p) => p.userId !== currentUserId,
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centered}>
          <ActivityIndicator color={Colors.primary} size="large" />
        </View>
      </SafeAreaView>
    );
  }

  if (error || !match) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centered}>
          <Text style={styles.errorText}>{error || "Maç bulunamadı"}</Text>
          <Button
            title="Geri Dön"
            onPress={() => navigation.goBack()}
            variant="outline"
            size="small"
            style={{ marginTop: 16 }}
          />
        </View>
      </SafeAreaView>
    );
  }

  const statusColor = STATUS_COLORS[match.status];

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={Colors.primary}
            colors={[Colors.primary]}
          />
        }
      >
        {/* Hero Card */}
        <View style={styles.heroCard}>
          <View style={[styles.heroAccent, { backgroundColor: statusColor }]} />
          <View style={styles.heroBadgeRow}>
            <Badge
              label={STATUS_LABELS[match.status]}
              color={statusColor}
              variant="dot"
              size="md"
            />
            {match.visibility === "PRIVATE" && (
              <Badge
                label="Ozel"
                color={Colors.amber}
                variant="soft"
                size="sm"
                icon="🔒"
              />
            )}
          </View>
          <Text style={styles.matchTitle}>{match.title}</Text>
          {match.description ? (
            <Text style={styles.matchDesc}>{match.description}</Text>
          ) : null}
        </View>

        {/* Info Card */}
        <Card style={{ marginTop: 20 }}>
          {(() => {
            const parts = match.location.split(" / ");
            const hasThreeParts = parts.length === 3;
            return hasThreeParts ? (
              <>
                <InfoRow icon="🏙" label="Il" value={parts[0].trim()} />
                <InfoRow icon="📌" label="Ilce" value={parts[1].trim()} />
                <InfoRow icon="🏟" label="Saha" value={parts[2].trim()} />
              </>
            ) : (
              <InfoRow icon="📍" label="Konum" value={match.location} />
            );
          })()}
          <InfoRow
            icon="📅"
            label="Tarih"
            value={formatDateTime(match.startTime)}
          />
          <InfoRow
            icon="👥"
            label="Kapasite"
            value={`${match.participantCount} / ${match.capacity}`}
          />
          <InfoRow
            icon="🌐"
            label="Gorunurluk"
            value={match.visibility === "PUBLIC" ? "Herkese Acik" : "Ozel"}
            isLast
          />
        </Card>

        {/* My Request Status (non-organizer, non-participant) */}
        {!isOrganizer &&
          !isParticipant &&
          myRequest &&
          myRequest.status !== "PENDING" && (
            <View
              style={[
                styles.requestStatusBanner,
                {
                  backgroundColor:
                    myRequest.status === "REJECTED"
                      ? Colors.error + "15"
                      : Colors.textMuted + "15",
                },
              ]}
            >
              <Text
                style={[
                  styles.requestStatusText,
                  {
                    color:
                      myRequest.status === "REJECTED"
                        ? Colors.error
                        : Colors.textMuted,
                  },
                ]}
              >
                {myRequest.status === "REJECTED"
                  ? "❌ Katılım isteğin reddedildi"
                  : myRequest.status === "CANCELLED"
                    ? "↩️ İsteğin geri alındı"
                    : myRequest.status === "EXPIRED"
                      ? "⏰ İsteğin süresi doldu"
                      : ""}
              </Text>
            </View>
          )}

        {/* Pending Requests Section (organizer only, private match) */}
        {isOrganizer &&
          match.visibility === "PRIVATE" &&
          pendingRequests.length > 0 && (
            <>
              <Text style={styles.sectionTitle}>
                Bekleyen İstekler ({pendingRequests.length})
              </Text>
              <Card>
                {pendingRequests.map((req, i) => {
                  const name =
                    displayNames[req.userID] ||
                    req.userID.substring(0, 8) + "...";
                  return (
                    <View
                      key={req.id}
                      style={[
                        styles.requestRow,
                        i < pendingRequests.length - 1 &&
                          styles.participantBorder,
                      ]}
                    >
                      <TouchableOpacity
                        style={styles.requestUser}
                        onPress={() => navigateToProfile(req.userID)}
                        activeOpacity={0.7}
                      >
                        <Avatar
                          uri={avatarUrls[req.userID]}
                          name={name}
                          size={42}
                          style={styles.participantAvatar}
                          backgroundColor={Colors.surfaceElevated}
                          borderWidth={0}
                          textColor={Colors.text}
                        />
                        <Text style={styles.participantName}>{name}</Text>
                      </TouchableOpacity>
                      <View style={styles.requestActions}>
                        <Button
                          title="Onayla"
                          onPress={() => handleApproveRequest(req.id)}
                          size="small"
                          loading={actionLoading}
                          style={{ marginRight: 8 }}
                        />
                        <Button
                          title="Reddet"
                          onPress={() => handleRejectRequest(req.id, name)}
                          variant="danger"
                          size="small"
                        />
                      </View>
                    </View>
                  );
                })}
              </Card>
            </>
          )}

        {isOrganizer &&
          match.visibility === "PRIVATE" &&
          pendingRequests.length === 0 &&
          isMatchActive && (
            <View style={styles.emptyRequestsBanner}>
              <Text style={styles.emptyRequestsText}>Bekleyen istek yok</Text>
            </View>
          )}

        {/* Field Visualization */}
        {participants.length > 0 && (
          <FieldVisualization
            matchId={match.id}
            participants={participants}
            displayNames={displayNames}
            isOrganizer={isOrganizer}
            onPositionsUpdated={(updated) => setParticipants(updated)}
          />
        )}

        {/* Participants */}
        <Text style={styles.sectionTitle}>
          Katılımcılar ({participants.length})
        </Text>
        <Card>
          {participants.length === 0 ? (
            <Text style={styles.noParticipants}>Henüz kimse katılmamış</Text>
          ) : (
            participants.map((p, i) => {
              const name = getParticipantName(p.userId);
              const isMe = p.userId === currentUserId;
              const isMatchOrganizer = p.userId === match.organizerId;

              return (
                <TouchableOpacity
                  key={p.participantId}
                  style={[
                    styles.participantRow,
                    i < participants.length - 1 && styles.participantBorder,
                  ]}
                  onPress={() => navigateToProfile(p.userId)}
                  activeOpacity={0.7}
                >
                  {isMatchOrganizer ? (
                    <View style={[styles.participantAvatar, styles.organizerAvatar]}>
                      <Text style={styles.participantAvatarText}>👑</Text>
                    </View>
                  ) : (
                    <Avatar
                      uri={avatarUrls[p.userId]}
                      name={name}
                      size={42}
                      style={styles.participantAvatar}
                      backgroundColor={Colors.surfaceElevated}
                      borderWidth={0}
                      textColor={Colors.text}
                    />
                  )}
                  <View style={styles.participantInfo}>
                    <Text style={styles.participantName}>
                      {name}
                      {isMe ? " (Sen)" : ""}
                    </Text>
                    {isMatchOrganizer && (
                      <Text style={styles.organizerTag}>Organizatör</Text>
                    )}
                  </View>
                  {p.team && (
                    <View
                      style={[
                        styles.teamBadge,
                        {
                          backgroundColor:
                            p.team === "HOME"
                              ? Colors.primaryGlow
                              : Colors.accentGlow,
                          borderColor:
                            p.team === "HOME"
                              ? Colors.primaryBorder
                              : Colors.accent + "40",
                        },
                      ]}
                    >
                      <Text
                        style={[
                          styles.teamBadgeText,
                          {
                            color:
                              p.team === "HOME"
                                ? Colors.primary
                                : Colors.accent,
                          },
                        ]}
                      >
                        {p.team === "HOME" ? "Ev" : "Dep"}
                        {match?.whiteTeam
                          ? ` · ${p.team === match.whiteTeam ? "Beyaz" : "Siyah"}`
                          : ""}
                      </Text>
                    </View>
                  )}
                  {isOrganizer &&
                    !isMe &&
                    match.status !== "STARTED" &&
                    match.status !== "COMPLETED" &&
                    match.status !== "CANCELLED" &&
                    match.status !== "RATINGS_CLOSED" && (
                      <Button
                        title="Çıkar"
                        onPress={() => handleRemovePlayer(p.userId)}
                        variant="danger"
                        size="small"
                      />
                    )}
                  {(!isOrganizer || isMe) && (
                    <Text style={styles.chevron}>›</Text>
                  )}
                </TouchableOpacity>
              );
            })
          )}
        </Card>

        {/* Rate Match */}
        {(isParticipant || isOrganizer) && match.status === "COMPLETED" && !hasRated && (
          <Button
            title="⭐  Değerlendir"
            onPress={() =>
              navigation.navigate("RateMatch", {
                matchId: match.id,
                matchTitle: match.title,
              })
            }
            variant="outline"
            style={{ marginTop: 12 }}
          />
        )}

        {/* Actions */}
        <View style={styles.actions}>
          {canJoin && (
            <Button
              title={
                match.visibility === "PRIVATE"
                  ? "Katılım İsteği Gönder"
                  : "Maça Katıl"
              }
              onPress={handleJoin}
              loading={actionLoading}
              icon={match.visibility === "PRIVATE" ? "📩" : "⚽"}
            />
          )}

          {hasPendingRequest && (
            <Button
              title="İsteği Geri Al"
              onPress={handleWithdraw}
              loading={actionLoading}
              variant="outline"
              icon="↩️"
            />
          )}

          {canLeave && (
            <Button
              title="Maçtan Ayrıl"
              onPress={handleLeave}
              loading={actionLoading}
              variant="outline"
            />
          )}

          {(isOrganizer || isParticipant) &&
            (match.status === "CREATED" || match.status === "OPEN") && (
              <Button
                title="Oyuncu Davet Et"
                onPress={() => {
                  setInviteSearch("");
                  setInviteResults([]);
                  setSentInvites(new Set());
                  setFriends([]);
                  setInviteModalVisible(true);
                  loadFriends();
                }}
                variant="outline"
                icon="📨"
              />
            )}

          {isOrganizer && (
            <>

              {(match.status === "CREATED" ||
                match.status === "OPEN" ||
                match.status === "FULL") &&
                participants.length >= 2 && (
                  <Button
                    title="Takımları Düzenle"
                    onPress={openTeamModal}
                    variant="outline"
                    icon="⚔️"
                  />
                )}

              {(match.status === "CREATED" || match.status === "OPEN" || match.status === "FULL") && (
                <Button
                  title="Maçı Başlat"
                  onPress={handleStart}
                  loading={actionLoading}
                  icon="💣"
                />
              )}

              {match.status === "STARTED" && (
                <Button
                  title="Maçı Tamamla"
                  onPress={handleComplete}
                  loading={actionLoading}
                  icon="✅"
                />
              )}

              {otherParticipants.length > 0 &&
                match.status !== "COMPLETED" &&
                match.status !== "CANCELLED" &&
                match.status !== "RATINGS_CLOSED" && (
                  <Button
                    title="Organizatörlüğü Devret"
                    onPress={() => setTransferModalVisible(true)}
                    variant="outline"
                    icon="🕵🏽"
                  />
                )}

              {match.status !== "COMPLETED" &&
                match.status !== "CANCELLED" &&
                match.status !== "RATINGS_CLOSED" && (
                  <Button
                    title="Maçı İptal Et"
                    onPress={handleCancel}
                    loading={actionLoading}
                    variant="danger"
                    icon="❌"
                  />
                )}
            </>
          )}
        </View>
      </ScrollView>

      {/* Transfer Organizer Modal */}
      <Modal
        visible={transferModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setTransferModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <Text style={styles.modalTitle}>Organizatörlüğü Devret</Text>
            <Text style={styles.modalSubtitle}>
              Hangi oyuncuya devretmek istiyorsun?
            </Text>
            <ScrollView
              style={styles.modalList}
              showsVerticalScrollIndicator={false}
            >
              {otherParticipants.map((p, i) => {
                const name = getParticipantName(p.userId);
                return (
                  <TouchableOpacity
                    key={p.participantId}
                    style={[
                      styles.modalParticipantRow,
                      i < otherParticipants.length - 1 &&
                        styles.participantBorder,
                    ]}
                    onPress={() => handleTransferOrganizer(p.userId)}
                    activeOpacity={0.7}
                  >
                    <Avatar
                      uri={avatarUrls[p.userId]}
                      name={name}
                      size={42}
                      style={styles.participantAvatar}
                      backgroundColor={Colors.surfaceElevated}
                      borderWidth={0}
                      textColor={Colors.text}
                    />
                    <Text style={styles.participantName}>{name}</Text>
                    <Text style={styles.chevron}>›</Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
            <Button
              title="Vazgeç"
              onPress={() => setTransferModalVisible(false)}
              variant="outline"
              style={{ marginTop: 12 }}
            />
          </View>
        </View>
      </Modal>

      {/* Score Modal */}
      <Modal
        visible={scoreModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setScoreModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View
            style={[
              styles.modalContainer,
              { borderRadius: 20, marginHorizontal: 24 },
            ]}
          >
            <Text style={styles.modalTitle}>Maç Skoru</Text>
            <Text style={styles.modalSubtitle}>Maçın final skorunu girin</Text>
            <View style={styles.scoreRow}>
              <View style={styles.scoreInputGroup}>
                <Text style={styles.scoreLabel}>Ev Sahibi</Text>
                <TextInput
                  style={styles.scoreInput}
                  value={homeScore}
                  onChangeText={setHomeScore}
                  keyboardType="number-pad"
                  maxLength={2}
                  selectTextOnFocus
                />
              </View>
              <Text style={styles.scoreSeparator}>-</Text>
              <View style={styles.scoreInputGroup}>
                <Text style={styles.scoreLabel}>Deplasman</Text>
                <TextInput
                  style={styles.scoreInput}
                  value={awayScore}
                  onChangeText={setAwayScore}
                  keyboardType="number-pad"
                  maxLength={2}
                  selectTextOnFocus
                />
              </View>
            </View>
            <View style={styles.scoreActions}>
              <Button
                title="Vazgeç"
                onPress={() => setScoreModalVisible(false)}
                variant="outline"
                style={{ flex: 1, marginRight: 8 }}
              />
              <Button
                title="Onayla"
                onPress={handleCompleteConfirm}
                style={{ flex: 1 }}
              />
            </View>
          </View>
        </View>
      </Modal>

      {/* ── Invite Player Modal ─────────────────────────── */}
      <Modal
        visible={inviteModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setInviteModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContainer, { maxHeight: "82%" }]}>
            <Text style={styles.modalTitle}>Oyuncu Davet Et</Text>

            <TextInput
              style={[styles.scoreInput, {
                width: "100%",
                height: 44,
                fontSize: 14,
                textAlign: "left",
                paddingHorizontal: 12,
                marginBottom: 12,
              }]}
              placeholder="🔍  İsim yazarak ara..."
              placeholderTextColor={Colors.textMuted}
              value={inviteSearch}
              onChangeText={handleInviteSearch}
              autoCapitalize="none"
            />

            {(inviteSearchLoading || friendsLoading) && (
              <ActivityIndicator color={Colors.primary} style={{ marginBottom: 8 }} />
            )}

            <ScrollView style={{ maxHeight: 380 }} showsVerticalScrollIndicator={false}>
              {inviteSearch.trim().length < 2 ? (
                /* ── Arkadaşlar listesi ── */
                <>
                  <Text style={styles.inviteSectionLabel}>👥 Arkadaşlar</Text>
                  {!friendsLoading && friends.length === 0 ? (
                    <Text style={{ color: Colors.textMuted, textAlign: "center", paddingVertical: 12, fontSize: 13 }}>
                      Tüm arkadaşların zaten bu maçta veya arkadaşın yok
                    </Text>
                  ) : (
                    friends.map((friend, i) => {
                      const alreadySent = sentInvites.has(friend.id);
                      return (
                        <View
                          key={friend.id}
                          style={[
                            styles.modalParticipantRow,
                            i < friends.length - 1 && styles.participantBorder,
                          ]}
                        >
                          <Avatar
                            uri={friend.avatarUrl}
                            name={friend.displayName}
                            size={42}
                            style={styles.participantAvatar}
                            backgroundColor={Colors.surfaceElevated}
                            borderWidth={0}
                            textColor={Colors.text}
                          />
                          <View style={{ flex: 1 }}>
                            <Text style={styles.participantName}>{friend.displayName}</Text>
                          </View>
                          <Button
                            title={alreadySent ? "Gönderildi ✓" : "Davet Et"}
                            onPress={() => handleSendInvite(friend.id)}
                            variant={alreadySent ? "outline" : "primary"}
                            size="small"
                            disabled={alreadySent}
                          />
                        </View>
                      );
                    })
                  )}
                  <Text style={[styles.inviteSectionLabel, { marginTop: 20 }]}>
                    🔍 Tüm Kullanıcılarda Ara
                  </Text>
                  <Text style={{ color: Colors.textMuted, fontSize: 12, textAlign: "center", paddingBottom: 8 }}>
                    Aramak için en az 2 karakter gir
                  </Text>
                </>
              ) : (
                /* ── Arama sonuçları ── */
                <>
                  {!inviteSearchLoading && inviteResults.length === 0 && (
                    <Text style={{ color: Colors.textMuted, textAlign: "center", paddingVertical: 16 }}>
                      Kullanıcı bulunamadı
                    </Text>
                  )}
                  {inviteResults.map((user, i) => {
                    const alreadySent = sentInvites.has(user.id);
                    return (
                      <View
                        key={user.id}
                        style={[
                          styles.modalParticipantRow,
                          i < inviteResults.length - 1 && styles.participantBorder,
                        ]}
                      >
                        <Avatar
                          uri={user.avatarUrl}
                          name={user.displayName}
                          size={42}
                          style={styles.participantAvatar}
                          backgroundColor={Colors.surfaceElevated}
                          borderWidth={0}
                          textColor={Colors.text}
                        />
                        <View style={{ flex: 1 }}>
                          <Text style={styles.participantName}>{user.displayName}</Text>
                        </View>
                        <Button
                          title={alreadySent ? "Gönderildi ✓" : "Davet Et"}
                          onPress={() => handleSendInvite(user.id)}
                          variant={alreadySent ? "outline" : "primary"}
                          size="small"
                          disabled={alreadySent}
                        />
                      </View>
                    );
                  })}
                </>
              )}
            </ScrollView>

            <Button
              title="Kapat"
              onPress={() => setInviteModalVisible(false)}
              variant="outline"
              style={{ marginTop: 16 }}
            />
          </View>
        </View>
      </Modal>

      <Modal
        visible={teamModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setTeamModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContainer, { maxHeight: "92%" }]}>
            <Text style={styles.modalTitle}>Takımları Düzenle</Text>
            <Text style={styles.modalSubtitle}>
              Oyuncuya dokunarak takımını değiştirebilirsin
            </Text>

            {/* Auto-shuffle button */}
            <TouchableOpacity
              style={styles.shuffleBtn}
              onPress={() => {
                const shuffled = [...participants].sort(
                  () => Math.random() - 0.5,
                );
                const draft: Record<string, "HOME" | "AWAY"> = {};
                shuffled.forEach((p, i) => {
                  draft[p.userId] = i % 2 === 0 ? "HOME" : "AWAY";
                });
                setTeamDraft(draft);
              }}
              activeOpacity={0.85}
            >
              <Text style={styles.shuffleIcon}>⚖️</Text>
              <Text style={styles.shuffleText}>Karıştır</Text>
            </TouchableOpacity>

            <View style={styles.teamColumns}>
              <TeamColumn
                title="Ev Sahibi"
                color={Colors.primary}
                glowColor={Colors.primaryGlow}
                borderColor={Colors.primaryBorder}
                players={participants.filter(
                  (p) => (teamDraft[p.userId] ?? "HOME") === "HOME",
                )}
                onPlayerPress={(userId) => toggleTeam(userId)}
                getName={getParticipantName}
                getAvatarUrl={(userId) => avatarUrls[userId]}
              />
              <View style={styles.vsBetween}>
                <Text style={styles.vsBetweenText}>VS</Text>
              </View>
              <TeamColumn
                title="Deplasman"
                color={Colors.accent}
                glowColor={Colors.accentGlow}
                borderColor={Colors.accent + "40"}
                players={participants.filter(
                  (p) => teamDraft[p.userId] === "AWAY",
                )}
                onPlayerPress={(userId) => toggleTeam(userId)}
                getName={getParticipantName}
                getAvatarUrl={(userId) => avatarUrls[userId]}
              />
            </View>

            <Text style={styles.teamHint}>
              💡 Oyuncuya dokun, takımı değişsin
            </Text>

            <View style={styles.scoreActions}>
              <Button
                title="Vazgeç"
                onPress={() => setTeamModalVisible(false)}
                variant="outline"
                style={{ flex: 1, marginRight: 8 }}
              />
              <Button
                title="Kaydet"
                onPress={handleAssignTeams}
                loading={actionLoading}
                style={{ flex: 1 }}
              />
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function TeamColumn({
  title,
  color,
  glowColor,
  borderColor,
  players,
  onPlayerPress,
  getName,
  getAvatarUrl,
}: {
  title: string;
  color: string;
  glowColor: string;
  borderColor: string;
  players: MatchParticipantResponse[];
  onPlayerPress: (userId: string) => void;
  getName: (userId: string) => string;
  getAvatarUrl: (userId: string) => string | null | undefined;
}) {
  return (
    <View
      style={[
        teamColStyles.column,
        { backgroundColor: glowColor, borderColor },
      ]}
    >
      <View style={[teamColStyles.header, { borderBottomColor: borderColor }]}>
        <Text style={[teamColStyles.title, { color }]}>{title}</Text>
        <View style={[teamColStyles.countPill, { backgroundColor: color }]}>
          <Text style={teamColStyles.countText}>{players.length}</Text>
        </View>
      </View>

      <ScrollView
        style={teamColStyles.list}
        showsVerticalScrollIndicator={false}
      >
        {players.length === 0 ? (
          <View style={teamColStyles.emptySlot}>
            <Text style={[teamColStyles.emptyText, { color }]}>Boş</Text>
          </View>
        ) : (
          players.map((p) => {
            const name = getName(p.userId);
            return (
              <TouchableOpacity
                key={p.participantId}
                style={[teamColStyles.playerCard, { borderColor }]}
                onPress={() => onPlayerPress(p.userId)} // Tüm kart takımı değiştirir
                activeOpacity={0.7}
              >
                <Avatar
                  uri={getAvatarUrl(p.userId)}
                  name={name}
                  size={32}
                  style={teamColStyles.avatar}
                  backgroundColor={color}
                  borderWidth={0}
                  textColor={Colors.textInverse}
                  textStyle={{ fontSize: 13 }}
                />
                <Text style={teamColStyles.name} numberOfLines={1}>
                  {name}
                </Text>
                {/* İsteğe bağlı: Küçük bir ikon ekleyerek tıklanabilir olduğunu belirtebilirsin */}
                <Text style={{ fontSize: 12, color: Colors.textMuted }}>
                  🔄
                </Text>
              </TouchableOpacity>
            );
          })
        )}
      </ScrollView>
    </View>
  );
}

function InfoRow({
  icon,
  label,
  value,
  isLast = false,
}: {
  icon: string;
  label: string;
  value: string;
  isLast?: boolean;
}) {
  return (
    <View style={[infoStyles.row, !isLast && infoStyles.border]}>
      <Text style={infoStyles.icon}>{icon}</Text>
      <Text style={infoStyles.label}>{label}</Text>
      <Text style={infoStyles.value}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  centered: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  content: { paddingHorizontal: 20, paddingBottom: 40 },
  errorText: { color: Colors.error, fontSize: 15, textAlign: "center" },

  heroCard: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.xl,
    padding: 20,
    marginTop: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: "hidden",
    ...Shadows.md,
  },
  heroAccent: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    height: 3,
  },
  heroBadgeRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginBottom: 12,
  },
  matchTitle: {
    fontSize: 24,
    fontWeight: "800",
    color: Colors.text,
    letterSpacing: -0.4,
  },
  matchDesc: {
    fontSize: 14,
    color: Colors.textSecondary,
    marginTop: 8,
    lineHeight: 20,
    fontWeight: "500",
  },

  requestStatusBanner: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 12,
    marginTop: 16,
  },
  requestStatusText: {
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
  },

  emptyRequestsBanner: {
    paddingVertical: 10,
    marginTop: 16,
    alignItems: "center",
  },
  emptyRequestsText: {
    fontSize: 13,
    color: Colors.textMuted,
  },

  sectionTitle: {
    fontSize: 16,
    fontWeight: "700",
    color: Colors.text,
    marginTop: 28,
    marginBottom: 12,
    letterSpacing: -0.2,
  },

  noParticipants: {
    color: Colors.textMuted,
    fontSize: 14,
    textAlign: "center",
    paddingVertical: 8,
  },

  requestRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
  },
  requestUser: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },
  requestActions: {
    flexDirection: "row",
    alignItems: "center",
  },

  participantRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
  },
  participantBorder: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  participantAvatar: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: Colors.surfaceElevated,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  organizerAvatar: {
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1.5,
    borderColor: Colors.primary,
  },
  participantAvatarText: { fontSize: 18 },
  participantInfo: { flex: 1 },
  participantName: { fontSize: 14, fontWeight: "600", color: Colors.text },
  organizerTag: {
    fontSize: 11,
    color: Colors.primary,
    fontWeight: "700",
    marginTop: 2,
  },
  chevron: {
    fontSize: 22,
    color: Colors.textMuted,
    fontWeight: "300",
    marginLeft: 8,
  },

  actions: {
    marginTop: 24,
    gap: 12,
  },

  // Modal styles
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  modalContainer: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    maxHeight: "70%",
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: "800",
    color: Colors.text,
    marginBottom: 6,
  },
  modalSubtitle: {
    fontSize: 14,
    color: Colors.textSecondary,
    marginBottom: 16,
  },
  modalList: {
    maxHeight: 300,
  },
  modalParticipantRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 14,
  },

  scoreRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    marginVertical: 20,
    gap: 12,
  },
  scoreInputGroup: {
    alignItems: "center",
    gap: 6,
  },
  scoreLabel: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: "600",
  },
  scoreInput: {
    width: 72,
    height: 56,
    borderRadius: 12,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1.5,
    borderColor: Colors.border,
    color: Colors.text,
    fontSize: 28,
    fontWeight: "800",
    textAlign: "center",
  },
  scoreSeparator: {
    fontSize: 28,
    fontWeight: "700",
    color: Colors.textMuted,
    marginTop: 20,
  },
  scoreActions: {
    flexDirection: "row",
    marginTop: 8,
  },

  teamSummary: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 14,
    marginTop: 8,
    marginBottom: 16,
  },
  teamSummaryBox: {
    flex: 1,
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: "center",
  },
  teamSummaryLabel: {
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
  teamSummaryCount: {
    fontSize: 28,
    fontWeight: "800",
    marginTop: 4,
    letterSpacing: -0.5,
  },
  vsDot: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surfaceHigh,
    alignItems: "center",
    justifyContent: "center",
  },
  vsText: {
    fontSize: 11,
    fontWeight: "800",
    color: Colors.textMuted,
    letterSpacing: 0.5,
  },

  teamRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
    paddingHorizontal: 4,
    gap: 12,
  },
  teamAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  teamAvatarText: {
    fontSize: 16,
    fontWeight: "800",
  },
  teamPlayerName: {
    flex: 1,
    fontSize: 15,
    fontWeight: "600",
    color: Colors.text,
  },
  teamPill: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 12,
    minWidth: 84,
    alignItems: "center",
  },
  teamPillText: {
    fontSize: 12,
    fontWeight: "800",
    color: Colors.textInverse,
    letterSpacing: 0.3,
  },

  teamBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    borderWidth: 1,
    marginRight: 8,
  },
  teamBadgeText: {
    fontSize: 10,
    fontWeight: "800",
    letterSpacing: 0.3,
    textTransform: "uppercase",
  },

  inviteSectionLabel: {
    fontSize: 12,
    fontWeight: "700",
    color: Colors.textSecondary,
    marginBottom: 8,
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },

  shuffleBtn: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: Colors.surfaceHigh,
    borderWidth: 1,
    borderColor: Colors.borderLight,
    gap: 8,
    marginTop: 12,
    marginBottom: 16,
  },
  shuffleIcon: {
    fontSize: 18,
    color: Colors.primary,
    fontWeight: "800",
  },
  shuffleText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: 0.2,
  },

  teamColumns: {
    flexDirection: "row",
    alignItems: "stretch",
    gap: 8,
    minHeight: 280,
    maxHeight: 360,
  },
  vsBetween: {
    width: 32,
    alignItems: "center",
    justifyContent: "center",
  },
  vsBetweenText: {
    fontSize: 12,
    fontWeight: "900",
    color: Colors.textDim,
    letterSpacing: 1,
  },
  teamHint: {
    fontSize: 12,
    color: Colors.textMuted,
    textAlign: "center",
    marginTop: 14,
    marginBottom: 12,
    fontWeight: "500",
  },
});

const teamColStyles = StyleSheet.create({
  column: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 16,
    overflow: "hidden",
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderBottomWidth: 1,
  },
  title: {
    fontSize: 12,
    fontWeight: "800",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
  countPill: {
    minWidth: 24,
    height: 22,
    paddingHorizontal: 7,
    borderRadius: 11,
    alignItems: "center",
    justifyContent: "center",
  },
  countText: {
    fontSize: 12,
    fontWeight: "800",
    color: Colors.textInverse,
  },
  list: {
    padding: 8,
  },
  emptySlot: {
    paddingVertical: 36,
    alignItems: "center",
    justifyContent: "center",
  },
  emptyText: {
    fontSize: 11,
    fontWeight: "700",
    opacity: 0.6,
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
  playerCard: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 10,
    backgroundColor: Colors.surface,
    marginBottom: 6,
    gap: 8,
    borderWidth: 1,
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
  },
  name: {
    flex: 1,
    fontSize: 13,
    fontWeight: "600",
    color: Colors.text,
  },
});

const infoStyles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
  },
  border: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  icon: { fontSize: 16, marginRight: 10, width: 24 },
  label: { fontSize: 14, color: Colors.textSecondary, flex: 1 },
  value: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: "600",
    textAlign: "right",
  },
});

import React, { useCallback, useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { notificationApi } from "../../api/notification.api";
import { invitationApi } from "../../api/invitation.api";
import { participationApi } from "../../api/participant.api";
import {
  NotificationResponse,
  NotificationType,
} from "../../types/notification.types";
import { extractErrorMessage } from "../../types/api.types";
import { Colors } from "../../theme/colors";

const TYPE_META: Record<NotificationType, { icon: string; color: string }> = {
  MATCH_STARTED: { icon: "▶️", color: Colors.primary },
  MATCH_UPDATED: { icon: "✏️", color: Colors.accent },
  MATCH_CANCELLED: { icon: "❌", color: Colors.error },
  MATCH_COMPLETED: { icon: "🏁", color: Colors.success },
  INVITATION_RECEIVED: { icon: "📨", color: Colors.warning },
  PARTICIPANT_JOINED: { icon: "✅", color: Colors.primary },
  PARTICIPANT_LEFT: { icon: "🚪", color: Colors.textMuted },
  FRIEND_REQUEST_RECEIVED: { icon: "👋", color: Colors.accent },
  FRIEND_REQUEST_ACCEPTED: { icon: "🤝", color: Colors.primary },
  WAITLIST_PROMOTED: { icon: "🎉", color: Colors.success },
};

function timeAgo(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Az önce";
  if (mins < 60) return `${mins}dk önce`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}sa önce`;
  return `${Math.floor(hrs / 24)}g önce`;
}

export default function NotificationsScreen({ navigation }: any) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>(
    [],
  );
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [markingAll, setMarkingAll] = useState(false);

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    setError(null);
    try {
      const data = await notificationApi.getAll();
      setNotifications(data);
    } catch (e) {
      setError(extractErrorMessage(e));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const navigateForNotification = (item: NotificationResponse) => {
    if (!item.referenceId || !navigation) return;
    const matchTypes: NotificationType[] = [
      "MATCH_STARTED",
      "MATCH_UPDATED",
      "MATCH_CANCELLED",
      "MATCH_COMPLETED",
      "PARTICIPANT_JOINED",
      "PARTICIPANT_LEFT",
      "WAITLIST_PROMOTED",
    ];
    const friendTypes: NotificationType[] = [
      "FRIEND_REQUEST_RECEIVED",
      "FRIEND_REQUEST_ACCEPTED",
    ];

    try {
      const tabNav = (navigation as any).getParent?.()?.getParent?.() ?? (navigation as any).getParent?.() ?? navigation;

      if (matchTypes.includes(item.type)) {
        tabNav.navigate("MatchTab", {
          screen: "MatchDetail",
          params: { matchId: item.referenceId },
        });
      } else if (friendTypes.includes(item.type)) {
        tabNav.navigate("FriendsTab", {
          screen: "PlayerProfile",
          params: { userId: item.referenceId },
        });
      }
    } catch {}
  };

  const handleMarkOne = async (item: NotificationResponse) => {
    try {
      const updated = await notificationApi.markAsRead(item.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === item.id ? updated : n)),
      );
    } catch {}
    navigateForNotification(item);
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (e) {
      setError(extractErrorMessage(e));
    } finally {
      setMarkingAll(false);
    }
  };

  const handleDeleteAll = async () => {
    if (notifications.length === 0) {
      return;
    }
    try {
      await notificationApi.deleteAll();
      setNotifications([]);
    } catch (e) {
      setError(extractErrorMessage(e));
    }
  };

  // referenceId = matchId for INVITATION_RECEIVED
  const handleAcceptInvitation = async (item: NotificationResponse) => {
    if (!item.referenceId) return;
    try {
      await invitationApi.accept(item.referenceId);
      await participationApi.sendRequest(item.referenceId);
      await notificationApi.markAsRead(item.id);
      Alert.alert("Kabul Edildi", "Daveti kabul ettin ve maça katıldın! 🎉");
      load(true);
    } catch (e) {
      Alert.alert("Hata", extractErrorMessage(e));
    }
  };

  const handleRejectInvitation = async (item: NotificationResponse) => {
    if (!item.referenceId) return;
    Alert.alert(
      "Daveti Reddet",
      "Bu daveti reddetmek istediğine emin misin?",
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Reddet",
          style: "destructive",
          onPress: async () => {
            try {
              await invitationApi.reject(item.referenceId!);
              await notificationApi.markAsRead(item.id);
              load(true);
            } catch (e) {
              Alert.alert("Hata", extractErrorMessage(e));
            }
          },
        },
      ]
    );
  };

  const renderItem = ({ item }: { item: NotificationResponse }) => {
    const meta = TYPE_META[item.type] ?? {
      icon: "🔔",
      color: Colors.textMuted,
    };
    const isInvitation = item.type === "INVITATION_RECEIVED" && !item.isRead;

    return (
      <View style={[styles.card, !item.isRead && styles.cardUnread]}>
        <TouchableOpacity
          style={styles.cardTouchable}
          onPress={() => handleMarkOne(item)}
          activeOpacity={0.75}
        >
          <View
            style={[styles.iconCircle, { backgroundColor: meta.color + "22" }]}
          >
            <Text style={styles.iconText}>{meta.icon}</Text>
          </View>
          <View style={styles.cardBody}>
            <Text style={styles.cardTitle}>{item.title}</Text>
            <Text style={styles.cardMessage} numberOfLines={2}>
              {item.message}
            </Text>
            <Text style={styles.cardTime}>{timeAgo(item.createdAt)}</Text>
          </View>
          {!item.isRead && <View style={styles.unreadDot} />}
        </TouchableOpacity>

        {isInvitation && (
          <View style={styles.inviteActions}>
            <TouchableOpacity
              style={[styles.inviteBtn, styles.inviteAccept]}
              onPress={() => handleAcceptInvitation(item)}
              activeOpacity={0.8}
            >
              <Text style={styles.inviteAcceptText}>✓ Kabul Et</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.inviteBtn, styles.inviteReject]}
              onPress={() => handleRejectInvitation(item)}
              activeOpacity={0.8}
            >
              <Text style={styles.inviteRejectText}>✕ Reddet</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safe} edges={["bottom"]}>
      <View style={styles.header}>
        <View>
          {unreadCount > 0 && (
            <Text style={styles.headerSub}>{unreadCount} okunmamış</Text>
          )}
        </View>

        <View style={styles.headerActions}>
          {notifications.length > 0 && (
            <TouchableOpacity
              style={styles.deleteAllBtn}
              activeOpacity={0.7}
              onPress={handleDeleteAll}
            >
              <Text style={styles.deleteAllText}>Tümünü sil</Text>
            </TouchableOpacity>
          )}
          {unreadCount > 0 && (
            <TouchableOpacity
              style={styles.markAllBtn}
              onPress={handleMarkAll}
              disabled={markingAll}
            >
              {markingAll ? (
                <ActivityIndicator size="small" color={Colors.primary} />
              ) : (
                <Text style={styles.markAllText}>Tümünü oku</Text>
              )}
            </TouchableOpacity>
          )}
        </View>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>{error}</Text>
          <TouchableOpacity style={styles.retryBtn} onPress={() => load()}>
            <Text style={styles.retryText}>Tekrar Dene</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={notifications}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={
            notifications.length === 0
              ? styles.emptyContainer
              : styles.listContent
          }
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => {
                setRefreshing(true);
                load(true);
              }}
              tintColor={Colors.primary}
            />
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyIcon}>🔔</Text>
              <Text style={styles.emptyTitle}>Bildirim yok</Text>
              <Text style={styles.emptyText}>
                Henüz hiç bildirimin yok.
              </Text>
            </View>
          }
          ItemSeparatorComponent={() => <View style={styles.separator} />}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },

  headerActions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },

  headerSub: { fontSize: 12, color: Colors.textMuted, marginTop: 2 },
  markAllBtn: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    backgroundColor: Colors.primaryGlow,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.primary,
    minWidth: 48,
    alignItems: "center",
  },
  markAllText: { fontSize: 13, color: Colors.primary, fontWeight: "700" },

  listContent: { paddingVertical: 8 },
  emptyContainer: { flex: 1 },

  card: {
    backgroundColor: Colors.background,
    paddingBottom: 4,
  },
  cardTouchable: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingVertical: 14,
  },

  inviteActions: {
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 20,
    paddingBottom: 12,
    paddingTop: 0,
  },
  inviteBtn: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: 10,
    alignItems: "center",
    borderWidth: 1,
  },
  inviteAccept: {
    backgroundColor: Colors.primaryGlow,
    borderColor: Colors.primary,
  },
  inviteReject: {
    backgroundColor: Colors.error + "15",
    borderColor: Colors.error,
  },
  inviteAcceptText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.primary,
  },
  inviteRejectText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.error,
  },

  deleteAllBtn: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    backgroundColor: Colors.error + "15",
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.error,
    minWidth: 48,
    alignItems: "center",
  },

  cardUnread: { backgroundColor: Colors.surfaceElevated + "AA" },
  iconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 14,
    flexShrink: 0,
  },
  iconText: { fontSize: 20 },
  cardBody: { flex: 1 },
  cardTitle: {
    fontSize: 14,
    fontWeight: "700",
    color: Colors.text,
    marginBottom: 3,
  },
  cardMessage: { fontSize: 13, color: Colors.textSecondary, lineHeight: 18 },
  cardTime: { fontSize: 11, color: Colors.textMuted, marginTop: 5 },
  unreadDot: {
    width: 9,
    height: 9,
    borderRadius: 5,
    backgroundColor: Colors.primary,
    marginLeft: 10,
    flexShrink: 0,
  },

  separator: {
    height: 1,
    backgroundColor: Colors.border,
    marginHorizontal: 20,
  },

  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
  errorText: { color: Colors.error, textAlign: "center", marginBottom: 16 },
  retryBtn: {
    paddingHorizontal: 24,
    paddingVertical: 10,
    backgroundColor: Colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  retryText: { color: Colors.text, fontWeight: "600" },

  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: {
    fontSize: 18,
    fontWeight: "700",
    color: Colors.text,
    marginBottom: 6,
  },

  deleteAllText: {
    fontSize: 13,
    color: Colors.error,
    fontWeight: "700",
  },

  emptyText: { fontSize: 14, color: Colors.textMuted, textAlign: "center" },
});

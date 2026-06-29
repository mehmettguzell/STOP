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
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { friendshipApi } from "../../api/friendship.api";
import { userApi } from "../../api/user.api";
import { FriendshipResponse } from "../../types/friendship.types";
import { useAuthStore } from "../../store/auth.store";
import { extractErrorMessage } from "../../types/api.types";
import { FriendsStackParamList } from "../../navigation/types";
import EmptyState from "../../components/ui/EmptyState";
import { Colors, Radius, Shadows } from "../../theme/colors";

type Tab = "friends" | "incoming" | "outgoing";

type FriendEntry = {
  friendshipId: string;
  otherUserId: string;
  displayName: string;
};

type Nav = NativeStackNavigationProp<FriendsStackParamList>;

export default function FriendsScreen() {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const navigation = useNavigation<Nav>();

  const [activeTab, setActiveTab] = useState<Tab>("friends");
  const [friends, setFriends] = useState<FriendEntry[]>([]);
  const [incoming, setIncoming] = useState<FriendEntry[]>([]);
  const [outgoing, setOutgoing] = useState<FriendEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);

  const resolveEntries = useCallback(
    async (
      list: FriendshipResponse[],
      iAmRequester: boolean,
    ): Promise<FriendEntry[]> => {
      return Promise.all(
        list.map(async (f) => {
          const otherUserId = iAmRequester ? f.receiverId : f.requesterId;
          let displayName = otherUserId.slice(0, 8);
          try {
            const user = await userApi.getUser(otherUserId);
            displayName = user.displayName;
          } catch {}
          return { friendshipId: f.id, otherUserId, displayName };
        }),
      );
    },
    [],
  );

  const load = useCallback(
    async (silent = false) => {
      if (!silent) setLoading(true);
      try {
        const [friendsRaw, incomingRaw, outgoingRaw] = await Promise.all([
          friendshipApi.getFriends(),
          friendshipApi.getIncoming(),
          friendshipApi.getOutgoing(),
        ]);

        const [friendsRes, incomingRes, outgoingRes] = await Promise.all([
          Promise.all(
            friendsRaw.map(async (f) => {
              const otherUserId =
                f.requesterId === currentUserId ? f.receiverId : f.requesterId;
              let displayName = otherUserId.slice(0, 8);
              try {
                const user = await userApi.getUser(otherUserId);
                displayName = user.displayName;
              } catch {}
              return { friendshipId: f.id, otherUserId, displayName };
            }),
          ),
          resolveEntries(incomingRaw, false),
          resolveEntries(outgoingRaw, true),
        ]);

        setFriends(friendsRes);
        setIncoming(incomingRes);
        setOutgoing(outgoingRes);
      } catch (e) {
        Alert.alert("Hata", extractErrorMessage(e));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [currentUserId, resolveEntries],
  );

  useEffect(() => {
    load();
  }, [load]);

  // Reload on focus (when returning from search)
  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      load(true);
    });
    return unsubscribe;
  }, [navigation, load]);

  const handleAccept = async (friendshipId: string) => {
    setActionId(friendshipId);
    try {
      await friendshipApi.accept(friendshipId);
      await load(true);
    } catch (e) {
      Alert.alert("Hata", extractErrorMessage(e));
    } finally {
      setActionId(null);
    }
  };

  const handleReject = (friendshipId: string, name: string) => {
    Alert.alert(
      "İsteği Reddet",
      `${name} adlı kişinin arkadaşlık isteğini reddetmek istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Reddet",
          style: "destructive",
          onPress: async () => {
            setActionId(friendshipId);
            try {
              await friendshipApi.reject(friendshipId);
              await load(true);
            } catch (e) {
              Alert.alert("Hata", extractErrorMessage(e));
            } finally {
              setActionId(null);
            }
          },
        },
      ],
    );
  };

  const handleCancel = (friendshipId: string, name: string) => {
    Alert.alert(
      "İsteği Geri Al",
      `${name} adlı kişiye gönderilen isteği geri almak istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Geri Al",
          style: "destructive",
          onPress: async () => {
            setActionId(friendshipId);
            try {
              await friendshipApi.cancelRequest(friendshipId);
              await load(true);
            } catch (e) {
              Alert.alert("Hata", extractErrorMessage(e));
            } finally {
              setActionId(null);
            }
          },
        },
      ],
    );
  };

  const handleRemove = (friendshipId: string, name: string) => {
    Alert.alert(
      "Arkadaşlıktan Çıkar",
      `${name} kişisini arkadaş listenden çıkarmak istiyor musun?`,
      [
        { text: "Vazgeç", style: "cancel" },
        {
          text: "Çıkar",
          style: "destructive",
          onPress: async () => {
            setActionId(friendshipId);
            try {
              await friendshipApi.removeFriend(friendshipId);
              await load(true);
            } catch (e) {
              Alert.alert("Hata", extractErrorMessage(e));
            } finally {
              setActionId(null);
            }
          },
        },
      ],
    );
  };

  const handleOpenProfile = (userId: string) => {
    navigation.navigate("PlayerProfile", { userId });
  };

  const tabs: { key: Tab; label: string; count: number }[] = [
    { key: "friends", label: "Arkadaşlar", count: friends.length },
    { key: "incoming", label: "Gelen", count: incoming.length },
    { key: "outgoing", label: "Gönderilenler", count: outgoing.length },
  ];

  const currentData =
    activeTab === "friends"
      ? friends
      : activeTab === "incoming"
        ? incoming
        : outgoing;

  const renderItem = ({ item }: { item: FriendEntry }) => {
    const initial = item.displayName.charAt(0).toUpperCase();
    const busy = actionId === item.friendshipId;

    return (
      <View style={styles.row}>
        <TouchableOpacity
          style={styles.userPart}
          onPress={() => handleOpenProfile(item.otherUserId)}
          activeOpacity={0.7}
        >
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{initial}</Text>
          </View>
          <Text style={styles.name} numberOfLines={1}>
            {item.displayName}
          </Text>
        </TouchableOpacity>

        {activeTab === "incoming" && (
          <View style={styles.actions}>
            <TouchableOpacity
              style={[styles.actionBtn, styles.acceptBtn]}
              onPress={() => handleAccept(item.friendshipId)}
              disabled={busy}
            >
              {busy ? (
                <ActivityIndicator size="small" color={Colors.textInverse} />
              ) : (
                <Text style={styles.acceptText}>Kabul</Text>
              )}
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionBtn, styles.rejectBtn]}
              onPress={() => handleReject(item.friendshipId, item.displayName)}
              disabled={busy}
            >
              <Text style={styles.rejectText}>Reddet</Text>
            </TouchableOpacity>
          </View>
        )}

        {activeTab === "outgoing" && (
          <TouchableOpacity
            style={[styles.actionBtn, styles.cancelBtn]}
            onPress={() => handleCancel(item.friendshipId, item.displayName)}
            disabled={busy}
          >
            {busy ? (
              <ActivityIndicator size="small" color={Colors.textMuted} />
            ) : (
              <Text style={styles.cancelText}>Geri Al</Text>
            )}
          </TouchableOpacity>
        )}

        {activeTab === "friends" && (
          <TouchableOpacity
            style={styles.removeBtn}
            onPress={() => handleRemove(item.friendshipId, item.displayName)}
            disabled={busy}
          >
            {busy ? (
              <ActivityIndicator size="small" color={Colors.error} />
            ) : (
              <Text style={styles.removeText}>✕</Text>
            )}
          </TouchableOpacity>
        )}
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.safe} edges={["bottom"]}>
      {/* Add Friend Action */}
      <View style={styles.addFriendWrapper}>
        <TouchableOpacity
          style={styles.addFriendCard}
          onPress={() => navigation.navigate("Search")}
          activeOpacity={0.85}
        >
          <View style={styles.addFriendIcon}>
            <Text style={styles.addFriendIconText}>+</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.addFriendTitle}>Arkadaş Ekle</Text>
            <Text style={styles.addFriendSubtitle}>
              Oyuncu bul, arkadaşlık isteği gönder
            </Text>
          </View>
          <Text style={styles.addFriendChevron}>›</Text>
        </TouchableOpacity>
      </View>

      {/* Segmented Tabs */}
      <View style={styles.segmented}>
        {tabs.map((t) => (
          <TouchableOpacity
            key={t.key}
            style={[
              styles.segment,
              activeTab === t.key && styles.segmentActive,
            ]}
            onPress={() => setActiveTab(t.key)}
            activeOpacity={0.85}
          >
            <Text
              style={[
                styles.segmentText,
                activeTab === t.key && styles.segmentTextActive,
              ]}
            >
              {t.label}
            </Text>
            {t.count > 0 && (
              <View
                style={[
                  styles.segmentBadge,
                  activeTab === t.key && styles.segmentBadgeActive,
                ]}
              >
                <Text
                  style={[
                    styles.segmentBadgeText,
                    activeTab === t.key && styles.segmentBadgeTextActive,
                  ]}
                >
                  {t.count}
                </Text>
              </View>
            )}
          </TouchableOpacity>
        ))}
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={currentData}
          keyExtractor={(item) => item.friendshipId}
          renderItem={renderItem}
          contentContainerStyle={
            currentData.length === 0 ? styles.emptyContainer : styles.list
          }
          ItemSeparatorComponent={() => <View style={styles.sep} />}
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
            <EmptyState
              icon={
                activeTab === "friends"
                  ? "◎"
                  : activeTab === "incoming"
                    ? "✉"
                    : "↗"
              }
              title={
                activeTab === "friends"
                  ? "Henüz arkadaşın yok"
                  : activeTab === "incoming"
                    ? "Bekleyen istek yok"
                    : "Gönderilen istek yok"
              }
              description={
                activeTab === "friends"
                  ? 'Yukarıdan oyuncu ara ve arkadaşlık isteği gönder.'
                  : undefined
              }
              actionLabel={activeTab === "friends" ? "Arkadas Ara" : undefined}
              onAction={
                activeTab === "friends"
                  ? () => navigation.navigate("Search")
                  : undefined
              }
            />
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  addFriendWrapper: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  addFriendCard: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: Colors.primaryGlow,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    padding: 14,
    gap: 14,
    ...Shadows.sm,
  },
  addFriendIcon: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
    ...Shadows.glow(Colors.primary),
  },
  addFriendIconText: {
    fontSize: 26,
    fontWeight: "300",
    color: Colors.textInverse,
    marginTop: -3,
  },
  addFriendTitle: {
    fontSize: 15,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: -0.2,
  },
  addFriendSubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
    fontWeight: "500",
  },
  addFriendChevron: {
    fontSize: 26,
    color: Colors.primary,
    fontWeight: "300",
  },

  segmented: {
    flexDirection: "row",
    marginHorizontal: 20,
    marginTop: 8,
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 4,
  },
  segment: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 9,
    borderRadius: Radius.sm,
    gap: 6,
  },
  segmentActive: {
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
  },
  segmentText: {
    fontSize: 12,
    fontWeight: "700",
    color: Colors.textMuted,
    letterSpacing: 0.2,
  },
  segmentTextActive: {
    color: Colors.primary,
  },
  segmentBadge: {
    backgroundColor: Colors.surfaceHigh,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    paddingHorizontal: 5,
    alignItems: "center",
    justifyContent: "center",
  },
  segmentBadgeActive: {
    backgroundColor: Colors.primary,
  },
  segmentBadgeText: {
    fontSize: 10,
    color: Colors.textMuted,
    fontWeight: "800",
  },
  segmentBadgeTextActive: {
    color: Colors.textInverse,
  },

  list: { paddingVertical: 12, paddingHorizontal: 20 },
  emptyContainer: { flex: 1, paddingHorizontal: 20 },

  row: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 14,
    paddingVertical: 12,
    gap: 10,
  },
  userPart: {
    flexDirection: "row",
    alignItems: "center",
    flex: 1,
    gap: 12,
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontSize: 17, fontWeight: "800", color: Colors.primary },
  name: {
    flex: 1,
    fontSize: 15,
    fontWeight: "600",
    color: Colors.text,
    letterSpacing: -0.1,
  },

  actions: { flexDirection: "row", gap: 6 },
  actionBtn: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: Radius.sm,
    minWidth: 56,
    alignItems: "center",
    justifyContent: "center",
  },
  acceptBtn: {
    backgroundColor: Colors.primary,
    ...Shadows.glow(Colors.primary),
  },
  acceptText: { fontSize: 12, fontWeight: "700", color: Colors.textInverse },
  rejectBtn: {
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  rejectText: { fontSize: 12, fontWeight: "600", color: Colors.textSecondary },
  cancelBtn: {
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 14,
  },
  cancelText: { fontSize: 12, fontWeight: "600", color: Colors.textMuted },
  removeBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.errorGlow,
    alignItems: "center",
    justifyContent: "center",
  },
  removeText: { fontSize: 13, color: Colors.error, fontWeight: "700" },

  sep: { height: 8 },

  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
});


import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  TouchableOpacity,
  RefreshControl,
  TextInput,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { matchApi } from "../../api/match.api";
import { MatchResponse, MatchStatus } from "../../types/match.types";
import { MatchListScreenProps } from "../../navigation/types";
import CityPicker from "../../components/ui/CityPicker";
import EmptyState from "../../components/ui/EmptyState";
import Badge from "../../components/ui/Badge";
import NotificationBell from "../../components/ui/NotificationBell";
import HeaderAvatar from "../../components/ui/HeaderAvatar";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { getErrorMessage } from "../../utils/error";

const STATUS_LABELS: Record<MatchStatus, string> = {
  CREATED: "Yakında",
  OPEN: "Açık",
  FULL: "Dolu",
  STARTED: "Devam Ediyor",
  COMPLETED: "Tamamlandı",
  CANCELLED: "İptal",
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

type Mode = "all" | "mine";
type MineFilter = "all" | "active" | "past";

const MINE_FILTERS: { key: MineFilter; label: string }[] = [
  { key: "all", label: "Tümü" },
  { key: "active", label: "Aktif" },
  { key: "past", label: "Geçmiş" },
];

export default function MatchListScreen({ navigation }: MatchListScreenProps) {
  const [mode, setMode] = useState<Mode>("mine");

  const [matches, setMatches] = useState<MatchResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const [myMatches, setMyMatches] = useState<MatchResponse[]>([]);
  const [mineFilter, setMineFilter] = useState<MineFilter>("active");

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [error, setError] = useState("");
  const titleTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const fetchAll = useCallback(
    async (pageNum: number, isRefresh = false, titleOverride?: string) => {
      try {
        setError("");
        const result = await matchApi.search({
          title: (titleOverride ?? title).trim() || undefined,
          location: location.trim() || undefined,
          excludeJoined: true,
          page: pageNum,
          size: 15,
          sort: "startTime,asc",
        });
        const DISCOVERY_STATUSES: MatchStatus[] = ["CREATED", "OPEN", "FULL"];
        const filtered = result.content.filter((m) =>
          DISCOVERY_STATUSES.includes(m.status),
        );
        if (isRefresh || pageNum === 0) {
          setMatches(filtered);
        } else {
          setMatches((prev) => [...prev, ...filtered]);
        }
        setHasMore(!result.last);
        setPage(pageNum);
      } catch (err) {
        setError(getErrorMessage(err));
      }
    },
    [location, title],
  );

  const fetchMine = useCallback(
    async (titleOverride?: string) => {
      try {
        setError("");
        const result = await matchApi.getMyMatches(
          (titleOverride ?? title).trim() || undefined,
        );
        setMyMatches(result);
      } catch (err) {
        setError(getErrorMessage(err));
      }
    },
    [title],
  );

  useEffect(() => {
    setLoading(true);
    if (mode === "all") {
      fetchAll(0).finally(() => setLoading(false));
    } else {
      fetchMine().finally(() => setLoading(false));
    }
  }, [mode]);

  const onTitleChange = (text: string) => {
    setTitle(text);
    if (titleTimer.current) clearTimeout(titleTimer.current);
    titleTimer.current = setTimeout(() => {
      if (mode === "all") fetchAll(0, true, text);
      else fetchMine(text);
    }, 400);
  };

  const onRefresh = async () => {
    setRefreshing(true);
    if (mode === "all") await fetchAll(0, true);
    else await fetchMine();
    setRefreshing(false);
  };

  const onLoadMore = async () => {
    if (mode !== "all" || !hasMore || loadingMore) return;
    setLoadingMore(true);
    await fetchAll(page + 1);
    setLoadingMore(false);
  };

  const formatDate = (iso: string) => {
    const d = new Date(iso);
    return {
      day: d.getDate().toString().padStart(2, "0"),
      month: d.toLocaleString("tr-TR", { month: "short" }),
      time: `${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`,
    };
  };

  const filteredMine = myMatches
    .filter((m) => {
      if (mineFilter === "active")
        return ["OPEN", "FULL", "CREATED", "STARTED"].includes(m.status);
      if (mineFilter === "past")
        return ["COMPLETED", "CANCELLED", "RATINGS_CLOSED"].includes(m.status);
      return true;
    })
    .slice(0, mineFilter === "past" ? 20 : undefined);

  const displayed = mode === "all" ? matches : filteredMine;

  return (
    <SafeAreaView style={styles.safeArea} edges={["top"]}>
      <View style={styles.header}>
        <View style={{ flex: 1 }}>
          <Text style={styles.titleHero}>Maçlar</Text>
          <Text style={styles.subtitleHero}>
            {mode === "all"
              ? "Yakınındaki maçları keşfet"
              : "Katıldığın ve oluşturduğun maçlar"}
          </Text>
        </View>
        <NotificationBell />
        <HeaderAvatar />
        <TouchableOpacity
          style={styles.createBtn}
          onPress={() => navigation.navigate("CreateMatch")}
          activeOpacity={0.85}
        >
          <Text style={styles.createBtnIcon}>+</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.segmented}>
        <TouchableOpacity
          style={[styles.segment, mode === "mine" && styles.segmentActive]}
          onPress={() => setMode("mine")}
          activeOpacity={0.85}
        >
          <Text
            style={[
              styles.segmentText,
              mode === "mine" && styles.segmentTextActive,
            ]}
          >
            Maçlarım
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.segment, mode === "all" && styles.segmentActive]}
          onPress={() => setMode("all")}
          activeOpacity={0.85}
        >
          <Text
            style={[
              styles.segmentText,
              mode === "all" && styles.segmentTextActive,
            ]}
          >
            Keşfet
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.searchBar}>
        <Text style={styles.searchIcon}>⌕</Text>
        <TextInput
          style={styles.searchInput}
          placeholder={mode === "all" ? "Maç adı ara..." : "Maçlarımda ara..."}
          placeholderTextColor={Colors.textDim}
          value={title}
          onChangeText={onTitleChange}
          returnKeyType="search"
        />
        {title.length > 0 && (
          <TouchableOpacity
            style={styles.clearBtn}
            onPress={() => onTitleChange("")}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Text style={styles.clearBtnText}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* City filter (only in discover) */}
      {mode === "all" && (
        <View style={styles.cityFilterRow}>
          <View style={{ flex: 1 }}>
            <CityPicker value={location} onSelect={setLocation} />
          </View>
        </View>
      )}

      {/* Mine sub-filter */}
      {mode === "mine" && (
        <View style={styles.mineFilterRow}>
          {MINE_FILTERS.map((f) => (
            <TouchableOpacity
              key={f.key}
              style={[
                styles.filterPill,
                mineFilter === f.key && styles.filterPillActive,
              ]}
              onPress={() => setMineFilter(f.key)}
              activeOpacity={0.85}
            >
              <Text
                style={[
                  styles.filterPillText,
                  mineFilter === f.key && styles.filterPillTextActive,
                ]}
              >
                {f.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Error */}
      {error ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      ) : null}

      {/* List */}
      {loading && !refreshing ? (
        <View style={styles.centered}>
          <ActivityIndicator color={Colors.primary} size="large" />
        </View>
      ) : (
        <FlatList
          data={displayed}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              tintColor={Colors.primary}
              colors={[Colors.primary]}
            />
          }
          onEndReached={onLoadMore}
          onEndReachedThreshold={0.3}
          ItemSeparatorComponent={() => <View style={{ height: 12 }} />}
          ListEmptyComponent={
            mode === "all" ? (
              <EmptyState
                icon="ᯓ⚽️"
                title="Sonuç bulunamadı"
                description="Aradığın kriterde maç bulunamadı."
                actionLabel="Maç Oluştur"
                onAction={() => navigation.navigate("CreateMatch")}
              />
            ) : (
              <EmptyState
                icon="★"
                title="Henüz maçın yok"
                description="Keşfet sekmesinden maç bulup katılabilirsin."
                actionLabel="Maçları Keşfet"
                onAction={() => setMode("all")}
              />
            )
          }
          ListFooterComponent={
            loadingMore ? (
              <ActivityIndicator
                color={Colors.primary}
                style={{ paddingVertical: 16 }}
              />
            ) : null
          }
          renderItem={({ item }) => (
            <MatchCard
              match={item}
              onPress={() =>
                navigation.navigate("MatchDetail", { matchId: item.id })
              }
              formatDate={formatDate}
            />
          )}
        />
      )}
    </SafeAreaView>
  );
}

function MatchCard({
  match,
  onPress,
  formatDate,
}: {
  match: MatchResponse;
  onPress: () => void;
  formatDate: (iso: string) => { day: string; month: string; time: string };
}) {
  const statusColor = STATUS_COLORS[match.status];
  const date = formatDate(match.startTime);
  const fillRatio = match.participantCount / match.capacity;

  return (
    <TouchableOpacity
      style={cardStyles.card}
      onPress={onPress}
      activeOpacity={0.85}
    >
      <View style={cardStyles.row}>
        <View style={cardStyles.dateBox}>
          <Text style={cardStyles.dateDay}>{date.day}</Text>
          <Text style={cardStyles.dateMonth}>{date.month}</Text>
          <View style={cardStyles.timeWrapper}>
            <Text style={cardStyles.dateTime}>{date.time}</Text>
          </View>
        </View>

        <View style={cardStyles.body}>
          <View style={cardStyles.topRow}>
            <Text style={cardStyles.title} numberOfLines={1}>
              {match.title}
            </Text>
            {match.visibility === "PRIVATE" && (
              <Text style={cardStyles.lockIcon}>🔒</Text>
            )}
          </View>

          <View style={cardStyles.locationRow}>
            <Text style={cardStyles.locationIcon}>◉</Text>
            <Text style={cardStyles.locationText} numberOfLines={1}>
              {match.location}
            </Text>
          </View>

          <View style={cardStyles.bottomRow}>
            <Badge
              label={STATUS_LABELS[match.status]}
              color={statusColor}
              variant="dot"
              size="sm"
            />
            <View style={cardStyles.capacityWrapper}>
              <View style={cardStyles.capacityBar}>
                <View
                  style={[
                    cardStyles.capacityBarFill,
                    {
                      width: `${Math.min(fillRatio * 100, 100)}%`,
                      backgroundColor:
                        fillRatio >= 1 ? Colors.warning : Colors.primary,
                    },
                  ]}
                />
              </View>
              <Text style={cardStyles.capacityText}>
                <Text style={{ color: Colors.text, fontWeight: "800" }}>
                  {match.participantCount}
                </Text>
                <Text style={{ color: Colors.textDim }}>/{match.capacity}</Text>
              </Text>
            </View>
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 4,
    gap: 10,
  },
  titleHero: {
    fontSize: 30,
    fontWeight: "800",
    color: Colors.text,
    letterSpacing: -0.5,
  },
  subtitleHero: {
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 2,
    fontWeight: "500",
  },
  createBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
    ...Shadows.glow(Colors.primary),
  },
  createBtnIcon: {
    fontSize: 24,
    fontWeight: "300",
    color: Colors.textInverse,
    marginTop: -2,
  },

  segmented: {
    flexDirection: "row",
    marginHorizontal: 20,
    marginTop: 14,
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 4,
  },
  segment: {
    flex: 1,
    paddingVertical: 10,
    alignItems: "center",
    borderRadius: Radius.sm,
  },
  segmentActive: {
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
  },
  segmentText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.textMuted,
    letterSpacing: 0.2,
  },
  segmentTextActive: {
    color: Colors.primary,
  },

  searchBar: {
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 20,
    marginTop: 12,
    paddingHorizontal: 16,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    height: 46,
    gap: 10,
  },
  searchIcon: {
    fontSize: 18,
    color: Colors.textMuted,
    fontWeight: "700",
  },
  searchInput: {
    flex: 1,
    color: Colors.text,
    fontSize: 14,
    fontWeight: "500",
  },
  clearBtn: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: Colors.surfaceHigh,
    alignItems: "center",
    justifyContent: "center",
  },
  clearBtnText: {
    color: Colors.textMuted,
    fontSize: 11,
    fontWeight: "700",
  },

  cityFilterRow: {
    flexDirection: "row",
    paddingHorizontal: 20,
    marginTop: 10,
    marginBottom: 4,
  },

  mineFilterRow: {
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 20,
    marginTop: 12,
    marginBottom: 4,
  },
  filterPill: {
    paddingVertical: 7,
    paddingHorizontal: 14,
    borderRadius: Radius.pill,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  filterPillActive: {
    backgroundColor: Colors.primaryGlow,
    borderColor: Colors.primaryBorder,
  },
  filterPillText: {
    fontSize: 12,
    fontWeight: "600",
    color: Colors.textMuted,
  },
  filterPillTextActive: {
    color: Colors.primary,
    fontWeight: "700",
  },

  errorBox: {
    marginHorizontal: 20,
    marginTop: 8,
    backgroundColor: Colors.errorGlow,
    borderRadius: Radius.md,
    padding: 12,
    borderWidth: 1,
    borderColor: Colors.error,
  },
  errorText: {
    color: Colors.error,
    fontSize: 13,
    textAlign: "center",
    fontWeight: "600",
  },

  centered: { flex: 1, justifyContent: "center", alignItems: "center" },
  list: {
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 32,
    flexGrow: 1,
  },
});

const cardStyles = StyleSheet.create({
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: "hidden",
  },
  row: {
    flexDirection: "row",
    padding: 14,
    gap: 14,
  },
  dateBox: {
    width: 64,
    backgroundColor: Colors.primaryGlow,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 10,
  },
  dateDay: {
    fontSize: 22,
    fontWeight: "900",
    color: Colors.primary,
    letterSpacing: -0.8,
  },
  dateMonth: {
    fontSize: 10,
    fontWeight: "700",
    color: Colors.primary,
    textTransform: "uppercase",
    letterSpacing: 0.5,
    marginTop: 1,
  },
  timeWrapper: {
    marginTop: 6,
    paddingTop: 6,
    borderTopWidth: 1,
    borderTopColor: Colors.primaryBorder,
    width: "70%",
    alignItems: "center",
  },
  dateTime: {
    fontSize: 11,
    fontWeight: "700",
    color: Colors.primary,
  },

  body: { flex: 1, justifyContent: "space-between" },
  topRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  title: {
    flex: 1,
    fontSize: 16,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: -0.2,
  },
  lockIcon: {
    fontSize: 12,
  },

  locationRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    gap: 5,
  },
  locationIcon: {
    fontSize: 11,
    color: Colors.textMuted,
  },
  locationText: {
    fontSize: 13,
    color: Colors.textSecondary,
    fontWeight: "500",
    flexShrink: 1,
  },

  bottomRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 10,
    gap: 10,
  },
  capacityWrapper: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  capacityBar: {
    width: 50,
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.surfaceHigh,
    overflow: "hidden",
  },
  capacityBarFill: {
    height: "100%",
    borderRadius: 2,
  },
  capacityText: {
    fontSize: 12,
  },
});

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
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { reportApi } from "../../api/report.api";
import { ReportResponse, ReportStatus } from "../../types/report.types";
import { extractErrorMessage } from "../../types/api.types";
import { AdminStackParamList } from "../../navigation/types";
import { Colors } from "../../theme/colors";

type Props = NativeStackScreenProps<AdminStackParamList, "AdminReports">;

type FilterTab = "ALL" | ReportStatus;

const FILTERS: { key: FilterTab; label: string }[] = [
  { key: "ALL", label: "Tümü" },
  { key: "OPEN", label: "Açık" },
  { key: "REVIEWING", label: "İnceleniyor" },
  { key: "RESOLVED", label: "Çözüldü" },
  { key: "REJECTED", label: "Reddedildi" },
];

const STATUS_COLOR: Record<ReportStatus, string> = {
  OPEN: Colors.primary,
  REVIEWING: Colors.warning,
  RESOLVED: Colors.success,
  REJECTED: Colors.error,
};

const STATUS_LABEL: Record<ReportStatus, string> = {
  OPEN: "Açık",
  REVIEWING: "İnceleniyor",
  RESOLVED: "Çözüldü",
  REJECTED: "Reddedildi",
};

function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Az önce";
  if (mins < 60) return `${mins}dk`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}sa`;
  return `${Math.floor(hrs / 24)}g`;
}

export default function AdminReportsScreen({ navigation }: Props) {
  const [filter, setFilter] = useState<FilterTab>("ALL");
  const [reports, setReports] = useState<ReportResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [page, setPage] = useState(0);

  const load = useCallback(
    async (pageNum: number, tab: FilterTab, silent = false) => {
      if (!silent) setLoading(true);
      try {
        const status = tab === "ALL" ? undefined : tab;
        const res = await reportApi.getAll(status, pageNum, 20);
        if (pageNum === 0) {
          setReports(res.content);
        } else {
          setReports((prev) => [...prev, ...res.content]);
        }
        setHasMore(res.hasNext);
        setPage(pageNum);
      } catch (e) {
        Alert.alert("Hata", extractErrorMessage(e));
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [],
  );

  useEffect(() => {
    load(0, filter);
  }, [filter]);

  const renderItem = ({ item }: { item: ReportResponse }) => {
    const color = STATUS_COLOR[item.status];
    return (
      <TouchableOpacity
        style={styles.card}
        onPress={() =>
          navigation.navigate("AdminReportDetail", { reportId: item.id })
        }
        activeOpacity={0.75}
      >
        <View style={styles.cardTop}>
          <View
            style={[
              styles.statusBadge,
              { backgroundColor: color + "22", borderColor: color + "55" },
            ]}
          >
            <View style={[styles.statusDot, { backgroundColor: color }]} />
            <Text style={[styles.statusText, { color }]}>
              {STATUS_LABEL[item.status]}
            </Text>
          </View>
          <Text style={styles.timeText}>{timeAgo(item.createdAt)}</Text>
        </View>
        <Text style={styles.reason} numberOfLines={2}>
          {item.reason}
        </Text>
        <Text style={styles.meta}>
          ID: {item.id.slice(0, 8)}… • Hedef: {item.targetUserId.slice(0, 8)}…
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.safe} edges={["bottom"]}>
      {/* Filter tabs */}
      <View style={styles.filters}>
        <FlatList
          data={FILTERS}
          horizontal
          showsHorizontalScrollIndicator={false}
          keyExtractor={(f) => f.key}
          contentContainerStyle={{ paddingHorizontal: 16, gap: 8 }}
          renderItem={({ item: f }) => (
            <TouchableOpacity
              style={[
                styles.filterChip,
                filter === f.key && styles.filterChipActive,
              ]}
              onPress={() => setFilter(f.key)}
            >
              <Text
                style={[
                  styles.filterText,
                  filter === f.key && styles.filterTextActive,
                ]}
              >
                {f.label}
              </Text>
            </TouchableOpacity>
          )}
        />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={reports}
          keyExtractor={(r) => r.id}
          renderItem={renderItem}
          contentContainerStyle={
            reports.length === 0 ? styles.emptyContainer : styles.list
          }
          ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => {
                setRefreshing(true);
                load(0, filter, true);
              }}
              tintColor={Colors.primary}
            />
          }
          onEndReached={() => hasMore && load(page + 1, filter, true)}
          onEndReachedThreshold={0.3}
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyIcon}>📋</Text>
              <Text style={styles.emptyText}>Şikayet bulunamadı.</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  filters: {
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  filterChip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  filterChipActive: {
    backgroundColor: Colors.primaryGlow,
    borderColor: Colors.primary,
  },
  filterText: { fontSize: 13, color: Colors.textMuted, fontWeight: "600" },
  filterTextActive: { color: Colors.primary },

  list: { padding: 16 },
  emptyContainer: { flex: 1 },

  card: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 16,
  },
  cardTop: { flexDirection: "row", alignItems: "center", marginBottom: 10 },
  statusBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 20,
    borderWidth: 1,
    gap: 5,
  },
  statusDot: { width: 7, height: 7, borderRadius: 4 },
  statusText: { fontSize: 12, fontWeight: "700" },
  timeText: { marginLeft: "auto", fontSize: 12, color: Colors.textMuted },

  reason: { fontSize: 14, color: Colors.text, lineHeight: 20, marginBottom: 8 },
  meta: { fontSize: 11, color: Colors.textMuted },

  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
  emptyIcon: { fontSize: 40, marginBottom: 12 },
  emptyText: { fontSize: 14, color: Colors.textMuted },
});

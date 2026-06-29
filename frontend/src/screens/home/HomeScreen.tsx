import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useNavigation } from "@react-navigation/native";
import { BottomTabNavigationProp } from "@react-navigation/bottom-tabs";
import { useAuthStore } from "../../store/auth.store";
import ScoreBadge from "../../components/ui/ScoreBadge";
import Badge from "../../components/ui/Badge";
import NotificationBell from "../../components/ui/NotificationBell";
import HeaderAvatar from "../../components/ui/HeaderAvatar";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { AppTabParamList } from "../../navigation/types";
import { matchApi } from "../../api/match.api";
import { MatchResponse } from "../../types/match.types";

type HomeNav = BottomTabNavigationProp<AppTabParamList, "HomeTab">;

export default function HomeScreen() {
  const user = useAuthStore((s) => s.user);
  const navigation = useNavigation<HomeNav>();
  const [upcomingMatches, setUpcomingMatches] = useState<MatchResponse[]>([]);

  useEffect(() => {
    matchApi
      .getMyMatches()
      .then((matches) => {
        const upcoming = matches
          .filter(
            (m) =>
              m.status === "OPEN" ||
              m.status === "FULL" ||
              m.status === "CREATED",
          )
          .sort(
            (a, b) =>
              new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
          )
          .slice(0, 3);
        setUpcomingMatches(upcoming);
      })
      .catch(() => {});
  }, []);

  if (!user) return null;

  const greeting = (() => {
    const h = new Date().getHours();
    if (h < 6) return "İyi geceler";
    if (h < 12) return "Günaydın";
    if (h < 18) return "İyi günler";
    return "İyi akşamlar";
  })();

  return (
    <SafeAreaView style={styles.safeArea} edges={["top"]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
      >
        {/* Header */}
        <View style={styles.header}>
          <View style={{ flex: 1 }}>
            <Text style={styles.greeting}>{greeting},</Text>
            <Text style={styles.displayName} numberOfLines={1}>
              {user.displayName}
            </Text>
          </View>
          <NotificationBell />
          <HeaderAvatar />
        </View>

        {/* Hero Stats Card */}
        <View style={styles.heroCard}>
          <View style={styles.heroGlow} />
          <View style={styles.heroHeader}>
            <Text style={styles.heroLabel}>Performansım</Text>
            <Badge
              label="Aktif"
              color={Colors.primary}
              variant="dot"
              size="sm"
            />
          </View>
          <View style={styles.scoresRow}>
            <ScoreBadge label="Güven" score={user.trustScore} icon="🛡" />
            <ScoreBadge label="Rank" score={user.rankScore} icon="⚡" />
          </View>
        </View>

        {/* Quick Actions */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Hızlı Erişim</Text>
        </View>
        <View style={styles.actionsGrid}>
          <QuickAction
            icon="🏟"
            label="Maçları Keşfet"
            description="Yakındaki maçlar"
            color={Colors.primary}
            onPress={() => navigation.navigate("MatchTab")}
          />
          <QuickAction
            icon="🏃🏻"
            label="Maç Oluştur"
            description="Yeni etkinlik"
            color={Colors.accent}
            onPress={() =>
              navigation.navigate("MatchTab", {
                screen: "CreateMatch",
                initial: false,
              } as any)
            }
          />
          <QuickAction
            icon="🏆"
            label="Maçlarım"
            description="Aktif maçlar"
            color={Colors.info}
            onPress={() =>
              navigation.navigate("MatchTab", { screen: "MatchList" } as any)
            }
          />
          <QuickAction
            icon="🧟‍♂️"
            label="Arkadaşlarım"
            description="Arkadaş bul"
            color={Colors.amber}
            onPress={() => navigation.navigate("FriendsTab")}
          />
        </View>

        {/* Upcoming Matches */}
        {upcomingMatches.length > 0 && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Yaklaşan Maçlarım</Text>
              <TouchableOpacity onPress={() => navigation.navigate("MatchTab")}>
                <Text style={styles.sectionLink}>Tümü →</Text>
              </TouchableOpacity>
            </View>
            <View style={styles.upcomingList}>
              {upcomingMatches.map((m) => (
                <UpcomingMatchCard
                  key={m.id}
                  match={m}
                  onPress={() =>
                    navigation.navigate("MatchTab", {
                      screen: "MatchDetail",
                      params: { matchId: m.id },
                      initial: false,
                    } as any)
                  }
                />
              ))}
            </View>
          </>
        )}

        <View style={{ height: 24 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

function QuickAction({
  icon,
  label,
  description,
  color,
  onPress,
}: {
  icon: string;
  label: string;
  description: string;
  color: string;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={qaStyles.card}
      onPress={onPress}
      activeOpacity={0.8}
    >
      <View
        style={[
          qaStyles.iconCircle,
          { backgroundColor: color + "1F", borderColor: color + "40" },
        ]}
      >
        <Text style={[qaStyles.icon, { color }]}>{icon}</Text>
      </View>
      <Text style={qaStyles.label}>{label}</Text>
      <Text style={qaStyles.description}>{description}</Text>
    </TouchableOpacity>
  );
}

function UpcomingMatchCard({
  match,
  onPress,
}: {
  match: MatchResponse;
  onPress: () => void;
}) {
  const d = new Date(match.startTime);
  const day = d.getDate().toString().padStart(2, "0");
  const month = d.toLocaleString("tr-TR", { month: "short" });
  const time = `${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;

  return (
    <TouchableOpacity
      style={upStyles.card}
      onPress={onPress}
      activeOpacity={0.85}
    >
      <View style={upStyles.dateBox}>
        <Text style={upStyles.dateDay}>{day}</Text>
        <Text style={upStyles.dateMonth}>{month}</Text>
      </View>
      <View style={upStyles.body}>
        <Text style={upStyles.title} numberOfLines={1}>
          {match.title}
        </Text>
        <View style={upStyles.metaRow}>
          <Text style={upStyles.metaIcon}>⏱</Text>
          <Text style={upStyles.metaText}>{time}</Text>
          <View style={upStyles.metaDot} />
          <Text style={upStyles.metaIcon}>◉</Text>
          <Text style={upStyles.metaText} numberOfLines={1}>
            {match.location}
          </Text>
        </View>
        <View style={upStyles.bottomRow}>
          <View style={upStyles.capacityChip}>
            <Text style={upStyles.capacityText}>
              {match.participantCount}/{match.capacity}
            </Text>
            <Text style={upStyles.capacityLabel}>oyuncu</Text>
          </View>
        </View>
      </View>
      <Text style={upStyles.chevron}>›</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  content: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 32 },

  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
    marginBottom: 4,
    gap: 10,
  },
  greeting: {
    fontSize: 14,
    color: Colors.textMuted,
    fontWeight: "500",
  },
  displayName: {
    fontSize: 26,
    fontWeight: "800",
    color: Colors.text,
    marginTop: 2,
    letterSpacing: -0.4,
  },
  heroCard: {
    marginTop: 16,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.xl,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: "hidden",
  },
  heroGlow: {
    position: "absolute",
    top: -60,
    right: -60,
    width: 200,
    height: 200,
    borderRadius: 100,
    backgroundColor: Colors.primaryGlow,
    opacity: 0.6,
  },
  heroHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 16,
  },
  heroLabel: {
    fontSize: 12,
    color: Colors.textMuted,
    fontWeight: "700",
    letterSpacing: 0.6,
    textTransform: "uppercase",
  },
  scoresRow: {
    flexDirection: "row",
    gap: 12,
  },

  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 28,
    marginBottom: 14,
  },
  sectionTitle: {
    fontSize: 17,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: -0.2,
  },
  sectionLink: {
    fontSize: 13,
    fontWeight: "600",
    color: Colors.primary,
  },

  actionsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
  },

  upcomingList: {
    gap: 10,
  },
});

const qaStyles = StyleSheet.create({
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 16,
    width: "47.7%",
  },
  iconCircle: {
    width: 44,
    height: 44,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 12,
    borderWidth: 1,
  },
  icon: {
    fontSize: 22,
    fontWeight: "700",
  },
  label: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: "700",
    letterSpacing: -0.1,
  },
  description: {
    fontSize: 12,
    color: Colors.textMuted,
    marginTop: 2,
    fontWeight: "500",
  },
});

const upStyles = StyleSheet.create({
  card: {
    flexDirection: "row",
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 14,
    alignItems: "center",
    gap: 14,
  },
  dateBox: {
    width: 56,
    height: 64,
    borderRadius: Radius.md,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
  },
  dateDay: {
    fontSize: 22,
    fontWeight: "800",
    color: Colors.primary,
    letterSpacing: -0.5,
  },
  dateMonth: {
    fontSize: 10,
    fontWeight: "700",
    color: Colors.primary,
    textTransform: "uppercase",
    letterSpacing: 0.5,
    marginTop: 2,
  },
  body: { flex: 1 },
  title: {
    fontSize: 15,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: -0.2,
  },
  metaRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    gap: 4,
  },
  metaIcon: { fontSize: 11, color: Colors.textMuted },
  metaText: {
    fontSize: 12,
    color: Colors.textMuted,
    fontWeight: "500",
    flexShrink: 1,
  },
  metaDot: {
    width: 3,
    height: 3,
    borderRadius: 1.5,
    backgroundColor: Colors.textDim,
    marginHorizontal: 6,
  },
  bottomRow: {
    flexDirection: "row",
    marginTop: 8,
  },
  capacityChip: {
    flexDirection: "row",
    alignItems: "baseline",
    gap: 4,
  },
  capacityText: {
    fontSize: 13,
    fontWeight: "800",
    color: Colors.primary,
  },
  capacityLabel: {
    fontSize: 11,
    color: Colors.textDim,
    fontWeight: "500",
  },
  chevron: {
    fontSize: 28,
    color: Colors.textDim,
    fontWeight: "300",
  },
});

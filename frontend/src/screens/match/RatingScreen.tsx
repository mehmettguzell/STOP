import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { participationApi } from '../../api/participant.api';
import { userApi } from '../../api/user.api';
import { ratingApi } from '../../api/rating.api';
import { MatchParticipantResponse } from '../../types/match.types';
import { useAuthStore } from '../../store/auth.store';
import { extractErrorMessage } from '../../types/api.types';
import { MatchStackParamList } from '../../navigation/types';
import Button from '../../components/ui/Button';
import { Colors } from '../../theme/colors';

type Props = NativeStackScreenProps<MatchStackParamList, 'RateMatch'>;

interface PlayerRating {
  skill: number;
  teamwork: number;
  sportsmanship: number;
}

interface RateEntry {
  userId: string;
  displayName: string;
}

const CRITERIA: { key: keyof PlayerRating; label: string; icon: string }[] = [
  { key: 'skill',         label: 'Beceri',        icon: '⚽' },
  { key: 'teamwork',      label: 'Takım Oyunu',   icon: '🤝' },
  { key: 'sportsmanship', label: 'Sportmenlik',   icon: '🏅' },
];

function StarRow({
  value,
  onChange,
}: {
  value: number;
  onChange: (v: number) => void;
}) {
  return (
    <View style={starStyles.row}>
      {[1, 2, 3, 4, 5].map((n) => (
        <TouchableOpacity key={n} onPress={() => onChange(n)} activeOpacity={0.7} style={starStyles.star}>
          <Text style={[starStyles.starText, n <= value && starStyles.starFilled]}>
            {n <= value ? '★' : '☆'}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

export default function RatingScreen({ route, navigation }: Props) {
  const { matchId, matchTitle } = route.params;
  const currentUserId = useAuthStore((s) => s.user?.id);

  const [players, setPlayers] = useState<RateEntry[]>([]);
  const [ratings, setRatings] = useState<Record<string, PlayerRating>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    try {
      const participants: MatchParticipantResponse[] = await participationApi.getParticipants(matchId);
      const others = participants.filter((p) => p.userId !== currentUserId);

      const entries = await Promise.all(
        others.map(async (p): Promise<RateEntry> => {
          let displayName = p.userId.slice(0, 8);
          try {
            const user = await userApi.getUser(p.userId);
            displayName = user.displayName;
          } catch {}
          return { userId: p.userId, displayName };
        }),
      );

      setPlayers(entries);
      // Default all ratings to 3
      const defaults: Record<string, PlayerRating> = {};
      entries.forEach((e) => { defaults[e.userId] = { skill: 3, teamwork: 3, sportsmanship: 3 }; });
      setRatings(defaults);
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [matchId, currentUserId]);

  useEffect(() => { load(); }, [load]);

  const setScore = (userId: string, key: keyof PlayerRating, value: number) => {
    setRatings((prev) => ({
      ...prev,
      [userId]: { ...prev[userId], [key]: value },
    }));
  };

  const handleSubmit = async () => {
    if (players.length === 0) return;
    setSubmitting(true);
    let successCount = 0;
    const errors: string[] = [];

    // Sequential gönder — backend isEveryoneRated() race condition'a girmesin
    for (const p of players) {
      const r = ratings[p.userId];
      if (!r) continue;
      try {
        await ratingApi.submit(matchId, {
          ratedId: p.userId,
          skill: r.skill,
          teamwork: r.teamwork,
          sportsmanship: r.sportsmanship,
        });
        successCount++;
      } catch (e) {
        errors.push(`${p.displayName}: ${extractErrorMessage(e)}`);
      }
    }

    setSubmitting(false);

    if (errors.length === 0) {
      Alert.alert(
        'Değerlendirmeler Gönderildi',
        `${successCount} oyuncu başarıyla değerlendirildi.`,
        [{ text: 'Tamam', onPress: () => navigation.navigate('MatchDetail', { matchId, rated: true }) }],
      );
    } else if (successCount > 0) {
      Alert.alert(
        'Kısmen Başarılı',
        `${successCount} oyuncu değerlendirildi, ${errors.length} başarısız:\n\n${errors.join('\n')}`,
        [{ text: 'Tamam', onPress: () => navigation.goBack() }],
      );
    } else {
      Alert.alert('Değerlendirme Gönderilemedi', errors.join('\n\n'));
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.safe}>
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      </SafeAreaView>
    );
  }

  if (players.length === 0) {
    return (
      <SafeAreaView style={styles.safe}>
        <View style={styles.center}>
          <Text style={styles.emptyIcon}>🏟️</Text>
          <Text style={styles.emptyTitle}>Değerlendirilecek oyuncu yok</Text>
          <Text style={styles.emptyText}>Bu maçta değerlendirilebilecek başka oyuncu bulunamadı.</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <Text style={styles.subtitle}>
          Her oyuncuyu üç kategoride 1–5 yıldız ile değerlendir.
        </Text>

        {players.map((player) => {
          const r = ratings[player.userId] ?? { skill: 3, teamwork: 3, sportsmanship: 3 };
          return (
            <View key={player.userId} style={styles.card}>
              {/* Player header */}
              <View style={styles.cardHeader}>
                <View style={styles.avatar}>
                  <Text style={styles.avatarText}>
                    {player.displayName.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <Text style={styles.playerName}>{player.displayName}</Text>
              </View>

              {/* Criteria */}
              {CRITERIA.map((c, i) => (
                <View
                  key={c.key}
                  style={[styles.criteriaRow, i < CRITERIA.length - 1 && styles.criteriaBorder]}
                >
                  <View style={styles.criteriaLabel}>
                    <Text style={styles.criteriaIcon}>{c.icon}</Text>
                    <Text style={styles.criteriaText}>{c.label}</Text>
                  </View>
                  <StarRow
                    value={r[c.key]}
                    onChange={(v) => setScore(player.userId, c.key, v)}
                  />
                </View>
              ))}
            </View>
          );
        })}

        <Button
          title={`Değerlendirmeleri Gönder (${players.length} oyuncu)`}
          onPress={handleSubmit}
          loading={submitting}
          style={styles.submitBtn}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 40 },
  emptyIcon: { fontSize: 48, marginBottom: 14 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: Colors.text, marginBottom: 8 },
  emptyText: { fontSize: 14, color: Colors.textMuted, textAlign: 'center' },

  content: { padding: 20, paddingBottom: 40 },
  subtitle: { fontSize: 14, color: Colors.textMuted, marginBottom: 20, lineHeight: 20 },

  card: {
    backgroundColor: Colors.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 16,
    overflow: 'hidden',
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
    backgroundColor: Colors.surfaceElevated,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1.5,
    borderColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  avatarText: { fontSize: 16, fontWeight: '700', color: Colors.primary },
  playerName: { fontSize: 16, fontWeight: '700', color: Colors.text },

  criteriaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  criteriaBorder: { borderBottomWidth: 1, borderBottomColor: Colors.border },
  criteriaLabel: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  criteriaIcon: { fontSize: 18 },
  criteriaText: { fontSize: 14, color: Colors.textSecondary, fontWeight: '600' },

  submitBtn: { marginTop: 8 },
});

const starStyles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 4 },
  star: { padding: 2 },
  starText: { fontSize: 26, color: Colors.border },
  starFilled: { color: Colors.warning },
});

import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  TouchableOpacity,
  Alert,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { profileApi } from '../../api/profile.api';
import { UserProfileResponse } from '../../types/user.types';
import { SearchScreenProps } from '../../navigation/types';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import CityPicker from '../../components/ui/CityPicker';
import EmptyState from '../../components/ui/EmptyState';
import Badge from '../../components/ui/Badge';
import { Colors, Radius } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

export default function SearchScreen({ navigation }: SearchScreenProps) {
  const [city, setCity] = useState('');
  const [position, setPosition] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [minTrustScore, setMinTrustScore] = useState('');
  const [maxTrustScore, setMaxTrustScore] = useState('');
  const [minRankScore, setMinRankScore] = useState('');
  const [maxRankScore, setMaxRankScore] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [results, setResults] = useState<UserProfileResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const parseScore = (val: string): number | undefined => {
    const n = parseInt(val, 10);
    return isNaN(n) ? undefined : n;
  };

  const handleSearch = async () => {
    setLoading(true);
    setSearched(true);
    try {
      const data = await profileApi.searchProfiles({
        city: city.trim() || undefined,
        position: position.trim() || undefined,
        displayName: displayName.trim() || undefined,
        minTrustScore: parseScore(minTrustScore),
        maxTrustScore: parseScore(maxTrustScore),
        minRankScore: parseScore(minRankScore),
        maxRankScore: parseScore(maxRankScore),
        size: 20,
      });
      setResults(data.content);
    } catch (err) {
      setResults([]);
      Alert.alert('Arama Başarısız', getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const hasAdvancedFilter =
    !!city || !!position || minTrustScore || maxTrustScore || minRankScore || maxRankScore;

  const ListHeader = (
    <>
      {/* Quick search bar */}
      <View style={styles.searchBar}>
        <Text style={styles.searchIcon}>⌕</Text>
        <TextInput
          style={styles.searchInput}
          placeholder="Kullanıcı adı yaz..."
          placeholderTextColor={Colors.textDim}
          value={displayName}
          onChangeText={setDisplayName}
          returnKeyType="search"
          onSubmitEditing={handleSearch}
        />
        {displayName.length > 0 && (
          <TouchableOpacity
            style={styles.clearBtn}
            onPress={() => setDisplayName('')}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Text style={styles.clearBtnText}>✕</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Advanced toggle */}
      <TouchableOpacity
        style={styles.advancedToggle}
        onPress={() => setShowAdvanced((v) => !v)}
        activeOpacity={0.7}
      >
        <Text style={styles.advancedIcon}>{showAdvanced ? '▴' : '▾'}</Text>
        <Text style={styles.advancedText}>Gelişmiş Filtreler</Text>
        {hasAdvancedFilter && !showAdvanced && (
          <View style={styles.activeDot} />
        )}
      </TouchableOpacity>

      {showAdvanced && (
        <View style={styles.advancedPanel}>
          <CityPicker value={city} onSelect={setCity} />
          <Input
            label="Pozisyon"
            placeholder="Forvet, Kaleci..."
            value={position}
            onChangeText={setPosition}
          />

          <Text style={styles.scoreGroupLabel}>Güven Skoru</Text>
          <View style={styles.scoreRow}>
            <Input
              placeholder="Min"
              value={minTrustScore}
              onChangeText={setMinTrustScore}
              keyboardType="numeric"
              containerStyle={styles.scoreInput}
            />
            <Text style={styles.scoreDash}>—</Text>
            <Input
              placeholder="Max"
              value={maxTrustScore}
              onChangeText={setMaxTrustScore}
              keyboardType="numeric"
              containerStyle={styles.scoreInput}
            />
          </View>

          <Text style={styles.scoreGroupLabel}>Rank Skoru</Text>
          <View style={styles.scoreRow}>
            <Input
              placeholder="Min"
              value={minRankScore}
              onChangeText={setMinRankScore}
              keyboardType="numeric"
              containerStyle={styles.scoreInput}
            />
            <Text style={styles.scoreDash}>—</Text>
            <Input
              placeholder="Max"
              value={maxRankScore}
              onChangeText={setMaxRankScore}
              keyboardType="numeric"
              containerStyle={styles.scoreInput}
            />
          </View>
        </View>
      )}

      <Button
        title="Ara"
        onPress={handleSearch}
        loading={loading}
        icon="⌕"
        size="md"
        fullWidth
        style={{ marginTop: 8, marginBottom: 16 }}
      />

      {searched && results.length > 0 && (
        <Text style={styles.resultCount}>
          {results.length} oyuncu bulundu
        </Text>
      )}
    </>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <FlatList
        data={loading ? [] : results}
        keyExtractor={(_, i) => String(i)}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={ListHeader}
        ItemSeparatorComponent={() => <View style={{ height: 10 }} />}
        ListEmptyComponent={
          loading ? (
            <View style={styles.centered}>
              <ActivityIndicator color={Colors.primary} size="large" />
            </View>
          ) : searched ? (
            <EmptyState
              icon="⌕"
              title="Oyuncu bulunamadı"
              description="Farklı filtrelerle tekrar dene."
            />
          ) : (
            <View style={styles.tipBox}>
              <Text style={styles.tipTitle}>💡 İpucu</Text>
              <Text style={styles.tipText}>
                Kullanıcı adı yazarak hızlıca oyuncu bulabilir ya da gelişmiş filtrelerle şehir, pozisyon ve skor bazlı arama yapabilirsin.
              </Text>
            </View>
          )
        }
        renderItem={({ item }) => (
          <PlayerCard
            profile={item}
            onPress={() => navigation.navigate('PlayerProfile', { userId: item.userId })}
          />
        )}
      />
    </SafeAreaView>
  );
}

function PlayerCard({ profile, onPress }: { profile: UserProfileResponse; onPress: () => void }) {
  const initial = (profile.displayName ?? '?').charAt(0).toUpperCase();
  return (
    <TouchableOpacity style={cardStyles.card} activeOpacity={0.85} onPress={onPress}>
      <View style={cardStyles.avatar}>
        <Text style={cardStyles.avatarText}>{initial}</Text>
      </View>
      <View style={cardStyles.info}>
        <Text style={cardStyles.name} numberOfLines={1}>{profile.displayName}</Text>
        {(profile.position || profile.city) && (
          <Text style={cardStyles.meta}>
            {[profile.position, profile.city].filter(Boolean).join(' · ')}
          </Text>
        )}
        <View style={cardStyles.badgeRow}>
          {profile.dominantFoot && (
            <Badge
              label={
                profile.dominantFoot === 'LEFT'
                  ? 'Sol Ayak'
                  : profile.dominantFoot === 'RIGHT'
                  ? 'Sağ Ayak'
                  : 'Her İkisi'
              }
              color={Colors.textSecondary}
              variant="soft"
              size="sm"
            />
          )}
          {profile.trustScore != null && (
            <Badge
              label={`Güven ${profile.trustScore}`}
              color={Colors.warning}
              variant="soft"
              size="sm"
            />
          )}
          {profile.rankScore != null && (
            <Badge
              label={`Rank ${profile.rankScore}`}
              color={Colors.accent}
              variant="soft"
              size="sm"
            />
          )}
        </View>
      </View>
      <Text style={cardStyles.chevron}>›</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  list: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 32, flexGrow: 1 },

  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    height: 48,
    gap: 10,
  },
  searchIcon: { fontSize: 18, color: Colors.textMuted, fontWeight: '700' },
  searchInput: {
    flex: 1,
    color: Colors.text,
    fontSize: 14,
    fontWeight: '500',
  },
  clearBtn: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: Colors.surfaceHigh,
    alignItems: 'center',
    justifyContent: 'center',
  },
  clearBtnText: { color: Colors.textMuted, fontSize: 11, fontWeight: '700' },

  advancedToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    gap: 8,
  },
  advancedIcon: {
    fontSize: 14,
    color: Colors.primary,
    fontWeight: '700',
  },
  advancedText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.primary,
  },
  activeDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: Colors.warning,
  },

  advancedPanel: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 16,
    marginBottom: 8,
  },

  scoreGroupLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textMuted,
    marginTop: 4,
    marginBottom: 8,
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  scoreRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 6 },
  scoreInput: { flex: 1, marginBottom: 12 },
  scoreDash: { fontSize: 16, color: Colors.textDim, marginHorizontal: 4, marginTop: 16 },

  resultCount: {
    fontSize: 12,
    color: Colors.textMuted,
    fontWeight: '700',
    letterSpacing: 0.5,
    textTransform: 'uppercase',
    marginBottom: 10,
  },

  centered: {
    paddingVertical: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },

  tipBox: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 18,
    marginTop: 4,
  },
  tipTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 6,
  },
  tipText: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 19,
    fontWeight: '500',
  },
});

const cardStyles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 14,
    gap: 12,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontSize: 19,
    fontWeight: '800',
    color: Colors.primary,
  },
  info: { flex: 1 },
  name: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.text,
    letterSpacing: -0.2,
  },
  meta: {
    fontSize: 12,
    color: Colors.textMuted,
    marginTop: 3,
    fontWeight: '500',
  },
  badgeRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 5,
    marginTop: 8,
  },
  chevron: {
    fontSize: 24,
    color: Colors.textDim,
    fontWeight: '300',
  },
});

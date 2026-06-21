import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { userApi } from '../../api/user.api';
import { profileApi } from '../../api/profile.api';
import { UserSelfResponse, UserProfileResponse } from '../../types/user.types';
import { AdminUserDetailScreenProps } from '../../navigation/types';
import Card from '../../components/ui/Card';
import ScoreBadge from '../../components/ui/ScoreBadge';
import Button from '../../components/ui/Button';
import { Colors } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

export default function AdminUserDetailScreen({
  route,
  navigation,
}: AdminUserDetailScreenProps) {
  const { userId } = route.params;
  const [user, setUser] = useState<UserSelfResponse | null>(null);
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const userInfo = (await userApi.getUser(userId)) as UserSelfResponse;
      setUser(userInfo);

      try {
        const profileInfo = await profileApi.getProfile(userId);
        setProfile(profileInfo);
      } catch {
        // No profile
      }
    } catch (err) {
      Alert.alert('Hata', getErrorMessage(err));
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [userId]);

  const handleSuspend = () => {
    Alert.alert(
      'Hesabı Askıya Al',
      `${user?.displayName} adlı kullanıcının hesabını askıya almak istiyor musun?`,
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Askıya Al',
          style: 'destructive',
          onPress: async () => {
            setActionLoading(true);
            try {
              const updated = await userApi.suspendUser(userId);
              setUser(updated);
              Alert.alert('Başarılı', 'Kullanıcı askıya alındı.');
            } catch (err) {
              Alert.alert('İşlem Başarısız', getErrorMessage(err));
            } finally {
              setActionLoading(false);
            }
          },
        },
      ],
    );
  };

  const handleUnsuspend = async () => {
    setActionLoading(true);
    try {
      const updated = await userApi.unsuspendUser(userId);
      setUser(updated);
      Alert.alert('Başarılı', 'Kullanıcı aktif edildi.');
    } catch (err) {
      Alert.alert('İşlem Başarısız', getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  if (!user) return null;

  const getStatusColor = () => {
    if (user.status === 'ACTIVE') return Colors.success;
    if (user.status === 'SUSPENDED') return Colors.error;
    return Colors.textMuted;
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={styles.hero}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarText}>
              {user.displayName.charAt(0).toUpperCase()}
            </Text>
          </View>
          <Text style={styles.displayName}>{user.displayName}</Text>
          <View
            style={[
              styles.statusBadge,
              { backgroundColor: getStatusColor() + '1A' },
            ]}
          >
            <View
              style={[styles.statusDot, { backgroundColor: getStatusColor() }]}
            />
            <Text style={[styles.statusText, { color: getStatusColor() }]}>
              {user.status}
            </Text>
          </View>
        </View>

        {/* Scores */}
        <Text style={styles.sectionTitle}>Skorlar</Text>
        <View style={styles.scoresRow}>
          <ScoreBadge label="Güven" score={user.trustScore} />
          <ScoreBadge label="Rank" score={user.rankScore} />
        </View>

        {/* Account Info */}
        <Text style={styles.sectionTitle}>Hesap Bilgileri</Text>
        <Card>
          <InfoRow label="ID" value={user.id} />
          <InfoRow label="E-posta" value={user.email} />
          <InfoRow label="Telefon" value={user.phoneNumber ?? '—'} />
          <InfoRow label="Rol" value={user.role} />
          <InfoRow
            label="E-posta Doğrulama"
            value={user.emailVerified ? 'Evet' : 'Hayır'}
          />
          <InfoRow
            label="Telefon Doğrulama"
            value={user.phoneVerified ? 'Evet' : 'Hayır'}
          />
          <InfoRow
            label="Kayıt Tarihi"
            value={new Date(user.createdAt).toLocaleDateString('tr-TR')}
            isLast
          />
        </Card>

        {/* Profile Info */}
        {profile && (
          <>
            <Text style={styles.sectionTitle}>Profil Bilgileri</Text>
            <Card>
              {profile.firstName && (
                <InfoRow
                  label="Ad Soyad"
                  value={`${profile.firstName} ${profile.lastName ?? ''}`}
                />
              )}
              {profile.position && (
                <InfoRow label="Pozisyon" value={profile.position} />
              )}
              {profile.city && <InfoRow label="Şehir" value={profile.city} />}
              {profile.dominantFoot && (
                <InfoRow label="Baskın Ayak" value={profile.dominantFoot} />
              )}
              {profile.heightCm && (
                <InfoRow label="Boy" value={`${profile.heightCm} cm`} />
              )}
              {profile.weightKg && (
                <InfoRow label="Kilo" value={`${profile.weightKg} kg`} isLast />
              )}
            </Card>
          </>
        )}

        {/* Admin Actions */}
        <Text style={styles.sectionTitle}>Admin İşlemleri</Text>
        <View style={styles.actions}>
          {user.status === 'ACTIVE' ? (
            <Button
              title="Hesabı Askıya Al"
              onPress={handleSuspend}
              variant="danger"
              loading={actionLoading}
            />
          ) : user.status === 'SUSPENDED' ? (
            <Button
              title="Hesabı Aktif Et"
              onPress={handleUnsuspend}
              variant="primary"
              loading={actionLoading}
            />
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function InfoRow({
  label,
  value,
  isLast = false,
}: {
  label: string;
  value: string;
  isLast?: boolean;
}) {
  return (
    <View style={[infoStyles.row, !isLast && infoStyles.border]}>
      <Text style={infoStyles.label}>{label}</Text>
      <Text style={infoStyles.value} numberOfLines={1}>
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
  },
  content: { paddingHorizontal: 20, paddingBottom: 32, paddingTop: 8 },
  hero: { alignItems: 'center', paddingVertical: 28 },
  avatarCircle: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
    borderWidth: 3,
    borderColor: Colors.primaryLight,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.4,
    shadowRadius: 20,
    elevation: 10,
  },
  avatarText: { fontSize: 34, fontWeight: '800', color: Colors.black },
  displayName: { fontSize: 24, fontWeight: '800', color: Colors.text },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    paddingHorizontal: 14,
    paddingVertical: 5,
    borderRadius: 20,
  },
  statusDot: { width: 8, height: 8, borderRadius: 4, marginRight: 6 },
  statusText: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase' },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 14,
    marginTop: 24,
    letterSpacing: 0.3,
  },
  scoresRow: { flexDirection: 'row', gap: 12 },
  actions: { gap: 10 },
});

const infoStyles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 12,
  },
  border: { borderBottomWidth: 1, borderBottomColor: Colors.border },
  label: { fontSize: 14, color: Colors.textSecondary, flex: 1 },
  value: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: '600',
    flex: 2,
    textAlign: 'right',
  },
});

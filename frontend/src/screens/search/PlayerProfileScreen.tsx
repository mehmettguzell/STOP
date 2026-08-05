import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { userApi } from '../../api/user.api';
import { profileApi } from '../../api/profile.api';
import { friendshipApi } from '../../api/friendship.api';
import { chatApi } from '../../api/chat.api';
import { PlayerProfileScreenProps } from '../../navigation/types';
import { UserPublicResponse, UserProfileResponse } from '../../types/user.types';
import { FriendRelation } from '../../types/friendship.types';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import ScoreBadge from '../../components/ui/ScoreBadge';
import AvatarPicker from '../../components/ui/AvatarPicker';
import { useAuthStore } from '../../store/auth.store';
import { Colors } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';
import { extractErrorMessage } from '../../types/api.types';

export default function PlayerProfileScreen({ route, navigation }: any) {
  const { userId, matchId } = route.params as { userId: string; matchId?: string };
  const currentUserId = useAuthStore((s) => s.user?.id);
  const isOwnProfile = currentUserId === userId;

  const [publicInfo, setPublicInfo] = useState<UserPublicResponse | null>(null);
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [relation, setRelation] = useState<FriendRelation>({ state: 'NONE' });
  const [loading, setLoading] = useState(true);
  const [friendLoading, setFriendLoading] = useState(false);
  const [msgLoading, setMsgLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const results = await Promise.allSettled([
          userApi.getUser(userId),
          profileApi.getProfile(userId),
          ...(!isOwnProfile ? [
            friendshipApi.getFriends(),
            friendshipApi.getIncoming(),
            friendshipApi.getOutgoing(),
          ] : []),
        ]);

        if (results[0].status === 'fulfilled') setPublicInfo(results[0].value as UserPublicResponse);
        if (results[1].status === 'fulfilled') setProfile(results[1].value);

        if (!isOwnProfile) {
          const friends  = results[2].status === 'fulfilled' ? (results[2].value as any[]) : [];
          const incoming = results[3].status === 'fulfilled' ? (results[3].value as any[]) : [];
          const outgoing = results[4].status === 'fulfilled' ? (results[4].value as any[]) : [];

          const friendEntry  = friends.find((f: any)  => f.requesterId === userId || f.receiverId === userId);
          const incomingEntry = incoming.find((f: any) => f.requesterId === userId);
          const outgoingEntry = outgoing.find((f: any) => f.receiverId === userId);

          if (friendEntry)       setRelation({ state: 'ACCEPTED',         friendshipId: friendEntry.id });
          else if (incomingEntry) setRelation({ state: 'PENDING_RECEIVED', friendshipId: incomingEntry.id });
          else if (outgoingEntry) setRelation({ state: 'PENDING_SENT',     friendshipId: outgoingEntry.id });
          else                    setRelation({ state: 'NONE' });
        }
      } catch (err) {
        Alert.alert('Hata', getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [userId, isOwnProfile]);

  const handleSendRequest = async () => {
    setFriendLoading(true);
    try {
      const res = await friendshipApi.sendRequest(userId);
      setRelation({ state: 'PENDING_SENT', friendshipId: res.id });
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setFriendLoading(false);
    }
  };

  const handleAccept = async () => {
    if (relation.state !== 'PENDING_RECEIVED') return;
    setFriendLoading(true);
    try {
      await friendshipApi.accept(relation.friendshipId);
      setRelation({ state: 'ACCEPTED', friendshipId: relation.friendshipId });
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setFriendLoading(false);
    }
  };

  const handleCancel = async () => {
    if (relation.state !== 'PENDING_SENT') return;
    setFriendLoading(true);
    try {
      await friendshipApi.cancelRequest(relation.friendshipId);
      setRelation({ state: 'NONE' });
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setFriendLoading(false);
    }
  };

  const handleSendMessage = async () => {
    setMsgLoading(true);
    try {
      const { chatId } = await chatApi.getOrCreateDirectChat(userId);
      const title = profile?.displayName ?? publicInfo?.displayName ?? 'Mesaj';
      // Bulunduğun stack içinde Chat'e git.
      // navigation.getParent() ile üst tab'e çıkmak yerine mevcut stack'te navigate ediyoruz —
      // FriendsTab, MatchTab ve MessagesTab'ın hepsi Chat route'una sahip.
      (navigation as any).navigate('Chat', { chatId, title, chatType: 'DIRECT' });
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setMsgLoading(false);
    }
  };

  const handleRemove = () => {
    if (relation.state !== 'ACCEPTED') return;
    const id = relation.friendshipId;
    Alert.alert('Arkadaşlıktan Çıkar', 'Bu kişiyi arkadaş listesinden çıkarmak istiyor musun?', [
      { text: 'Vazgeç', style: 'cancel' },
      {
        text: 'Çıkar', style: 'destructive',
        onPress: async () => {
          setFriendLoading(true);
          try {
            await friendshipApi.removeFriend(id);
            setRelation({ state: 'NONE' });
          } catch (e) {
            Alert.alert('Hata', extractErrorMessage(e));
          } finally {
            setFriendLoading(false);
          }
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  const displayName = profile?.displayName ?? publicInfo?.displayName ?? '?';

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Avatar + Name */}
        <View style={styles.hero}>
          {isOwnProfile ? (
            <View style={styles.avatarPickerWrapper}>
              <AvatarPicker
                avatarUrl={profile?.avatarUrl ?? null}
                displayName={displayName}
                size={96}
                onChange={(url) =>
                  setProfile((p) => (p ? { ...p, avatarUrl: url } : p))
                }
              />
            </View>
          ) : (
            <View style={styles.avatarCircle}>
              {profile?.avatarUrl ? (
                <Image
                  source={{ uri: profile.avatarUrl }}
                  style={styles.avatarImage}
                />
              ) : (
                <Text style={styles.avatarText}>
                  {displayName.charAt(0).toUpperCase()}
                </Text>
              )}
            </View>
          )}
          <Text style={styles.displayName}>{displayName}</Text>
          {profile?.firstName && profile?.lastName && (
            <Text style={styles.fullName}>
              {profile.firstName} {profile.lastName}
            </Text>
          )}
          {profile?.city && (
            <View style={styles.cityBadge}>
              <Text style={styles.cityText}>📍 {profile.city}</Text>
            </View>
          )}
        </View>

        {/* Report button — only available from match context */}
        {!isOwnProfile && matchId && (
          <View style={styles.friendRow}>
            <Button
              title="🚩  Şikayet Et"
              variant="danger"
              onPress={() =>
                navigation.navigate('Report', {
                  targetUserId: userId,
                  targetName: profile?.displayName ?? publicInfo?.displayName ?? userId.slice(0, 8),
                  matchId,
                })
              }
            />
          </View>
        )}

        {/* Mesaj Gönder */}
        {!isOwnProfile && (
          <View style={styles.friendRow}>
            <Button
              title="💬  Mesaj Gönder"
              onPress={handleSendMessage}
              loading={msgLoading}
              variant="outline"
            />
          </View>
        )}

        {/* Friendship action */}
        {!isOwnProfile && (
          <View style={styles.friendRow}>
            {relation.state === 'NONE' && (
              <Button title="👋  Arkadaş Ekle" onPress={handleSendRequest} loading={friendLoading} />
            )}
            {relation.state === 'PENDING_SENT' && (
              <Button title="⏳  İstek Gönderildi" onPress={handleCancel} loading={friendLoading} variant="outline" />
            )}
            {relation.state === 'PENDING_RECEIVED' && (
              <View style={styles.friendActions}>
                <Button title="✅  Kabul Et" onPress={handleAccept} loading={friendLoading} style={{ flex: 1 }} />
                <Button title="Reddet" onPress={() => relation.state === 'PENDING_RECEIVED' && friendshipApi.reject(relation.friendshipId).then(() => setRelation({ state: 'NONE' }))} variant="outline" style={{ flex: 1 }} />
              </View>
            )}
            {relation.state === 'ACCEPTED' && (
              <Button title="🤝  Arkadaşsın" onPress={handleRemove} loading={friendLoading} variant="outline" />
            )}
          </View>
        )}

        {/* Scores */}
        {publicInfo && (
          <>
            <Text style={styles.sectionTitle}>Skorlar</Text>
            <View style={styles.scoresRow}>
              <ScoreBadge label="Güven" score={publicInfo.trustScore} />
              <ScoreBadge label="Rank" score={publicInfo.rankScore} />
            </View>
          </>
        )}

        {/* Profile details */}
        {profile ? (
          <>
            <Text style={styles.sectionTitle}>Oyuncu Bilgileri</Text>
            <Card>
              {profile.position && (
                <ProfileRow label="Pozisyon" value={profile.position} />
              )}
              {profile.dominantFoot && (
                <ProfileRow
                  label="Baskın Ayak"
                  value={
                    profile.dominantFoot === 'LEFT'
                      ? 'Sol'
                      : profile.dominantFoot === 'RIGHT'
                        ? 'Sağ'
                        : 'Her ikisi'
                  }
                />
              )}
              {profile.heightCm && (
                <ProfileRow label="Boy" value={`${profile.heightCm} cm`} />
              )}
              {profile.weightKg && (
                <ProfileRow label="Kilo" value={`${profile.weightKg} kg`} />
              )}
              {profile.birthDate && (
                <ProfileRow
                  label="Doğum Tarihi"
                  value={profile.birthDate}
                  isLast={!profile.bio}
                />
              )}
              {profile.bio && (
                <ProfileRow label="Hakkında" value={profile.bio} isLast />
              )}
            </Card>
          </>
        ) : (
          <View style={styles.noProfile}>
            <View style={styles.noProfileIconCircle}>
              <Text style={styles.noProfileIcon}>👤</Text>
            </View>
            <Text style={styles.noProfileText}>
              Bu kullanıcı henüz profil oluşturmamış.
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function ProfileRow({
  label,
  value,
  isLast = false,
}: {
  label: string;
  value: string;
  isLast?: boolean;
}) {
  return (
    <View style={[rowStyles.row, !isLast && rowStyles.border]}>
      <Text style={rowStyles.label}>{label}</Text>
      <Text style={rowStyles.value}>{value}</Text>
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
    width: 96,
    height: 96,
    borderRadius: 48,
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
  avatarText: { fontSize: 40, fontWeight: '800', color: Colors.black },
  avatarImage: { width: 96, height: 96, borderRadius: 48 },
  avatarPickerWrapper: { marginBottom: 14 },
  displayName: { fontSize: 26, fontWeight: '800', color: Colors.text },
  fullName: { fontSize: 15, color: Colors.textSecondary, marginTop: 4 },
  cityBadge: {
    marginTop: 8,
    backgroundColor: Colors.surface,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cityText: { fontSize: 13, color: Colors.textMuted },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 14,
    marginTop: 24,
    letterSpacing: 0.3,
  },
  scoresRow: { flexDirection: 'row', gap: 12 },
  friendRow: { width: '100%', marginBottom: 8 },
  friendActions: { flexDirection: 'row', gap: 10, width: '100%' },
  noProfile: {
    alignItems: 'center',
    paddingVertical: 36,
    backgroundColor: Colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    marginTop: 24,
  },
  noProfileIconCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: Colors.surfaceGlass,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  noProfileIcon: { fontSize: 24 },
  noProfileText: { fontSize: 15, color: Colors.textMuted },
});

const rowStyles = StyleSheet.create({
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

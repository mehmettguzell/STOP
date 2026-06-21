import React, {
  useEffect,
  useState,
  useCallback,
  useLayoutEffect,
} from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { profileApi } from "../../api/profile.api";
import { useAuthStore } from "../../store/auth.store";
import { useProfileStore } from "../../store/profile.store";
import { ProfileViewScreenProps } from "../../navigation/types";
import { UserProfileResponse } from "../../types/user.types";
import Card from "../../components/ui/Card";
import ScoreBadge from "../../components/ui/ScoreBadge";
import Button from "../../components/ui/Button";
import Badge from "../../components/ui/Badge";
import NotificationBell from "../../components/ui/NotificationBell";
import HeaderAvatar from "../../components/ui/HeaderAvatar";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { getErrorCode, getErrorMessage } from "../../utils/error";
import { AxiosError } from "axios";

export default function ProfileViewScreen({
  navigation,
  route,
}: ProfileViewScreenProps) {
  const { user, logout } = useAuthStore();
  const targetUserId = route.params?.userId ?? user?.id ?? "";
  const isOwnProfile =
    !route.params?.userId || route.params.userId === user?.id;

  const {
    profile,
    hasProfile,
    loaded,
    needsRefresh,
    setProfile: storeSetProfile,
    setNoProfile,
  } = useProfileStore();

  const [loading, setLoading] = useState(!loaded);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const data = await profileApi.getProfile(targetUserId);
      storeSetProfile(data);
    } catch (err) {
      const code = getErrorCode(err);
      const is404 =
        code === "PROFILE_NOT_FOUND" ||
        code === "RESOURCE_NOT_FOUND" ||
        code === "NOT_FOUND" ||
        (err instanceof AxiosError && err.response?.status === 404) ||
        (typeof err === "object" &&
          err !== null &&
          "isAxiosError" in err &&
          (err as AxiosError).response?.status === 404);
      if (is404) {
        setNoProfile();
      } else {
        Alert.alert("Hata", getErrorMessage(err));
      }
    } finally {
      setLoading(false);
    }
  }, [targetUserId, storeSetProfile, setNoProfile]);

  useEffect(() => {
    if (!loaded && user) {
      loadProfile();
    }
  }, [loaded, loadProfile, user]);

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      if (needsRefresh && user) {
        loadProfile();
      }
    });
    return unsubscribe;
  }, [navigation, needsRefresh, loadProfile, user]);

  const handleLogout = () => {
    Alert.alert("Çıkış Yap", "Hesabından çıkmak istediğine emin misin?", [
      { text: "Vazgeç", style: "cancel" },
      { text: "Çıkış Yap", style: "destructive", onPress: logout },
    ]);
  };

  useLayoutEffect(() => {
    if (!isOwnProfile) return;
    navigation.setOptions({
      headerRight: () => (
        <View style={headerStyles.row}>
          {hasProfile && (
            <TouchableOpacity
              style={headerStyles.iconBtn}
              onPress={() => navigation.navigate("EditProfile")}
            >
              <Text style={headerStyles.iconText}>✏️</Text>
            </TouchableOpacity>
          )}
          <TouchableOpacity
            style={headerStyles.iconBtn}
            onPress={() => navigation.navigate("EditAccount")}
          >
            <Text style={headerStyles.iconText}>⚙️</Text>
          </TouchableOpacity>
          <NotificationBell />
        </View>
      ),
    });
  }, [navigation, isOwnProfile, hasProfile]);

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea} edges={["bottom"]}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Hero Card */}
        <View style={styles.hero}>
          <View style={styles.heroOrb} />
          <View style={styles.avatarRing}>
            <View style={styles.avatarCircle}>
              <Text style={styles.avatarText}>
                {(profile?.displayName ?? user?.displayName ?? "?")
                  .charAt(0)
                  .toUpperCase()}
              </Text>
            </View>
          </View>
          <Text style={styles.displayName}>
            {profile?.displayName ?? user?.displayName}
          </Text>
          {profile?.firstName && profile?.lastName && (
            <Text style={styles.fullName}>
              {profile.firstName} {profile.lastName}
            </Text>
          )}
          <View style={styles.badgeRow}>
            {profile?.city && (
              <Badge
                label={profile.city}
                color={Colors.primary}
                variant="soft"
                size="sm"
                icon="◉"
              />
            )}
            {isOwnProfile && user && (
              <Badge
                label={user.status === "ACTIVE" ? "Aktif" : user.status}
                color={user.status === "ACTIVE" ? Colors.success : Colors.error}
                variant="dot"
                size="sm"
              />
            )}
          </View>
        </View>

        {/* Scores */}
        {isOwnProfile && user && (
          <>
            <Text style={styles.sectionTitle}>Skorlarım</Text>
            <View style={styles.scoresRow}>
              <ScoreBadge label="Güven" score={user.trustScore} />
              <ScoreBadge label="Rank" score={user.rankScore} />
            </View>
          </>
        )}

        {/* Account Info */}
        {isOwnProfile && user && (
          <>
            <Text style={styles.sectionTitle}>Hesap Bilgileri</Text>
            <Card>
              <ProfileRow label="E-posta" value={user.email} />
              <ProfileRow label="Telefon" value={user.phoneNumber ?? "—"} />
              <ProfileRow label="Rol" value={user.role} />
              <ProfileRow
                label="E-posta Doğrulama"
                value={user.emailVerified ? "Doğrulandı" : "Doğrulanmadı"}
              />
              <ProfileRow
                label="Telefon Doğrulama"
                value={user.phoneVerified ? "Doğrulandı" : "Doğrulanmadı"}
                isLast
              />
            </Card>
          </>
        )}

        {/* Profile details */}
        {hasProfile && profile ? (
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
                    profile.dominantFoot === "LEFT"
                      ? "Sol"
                      : profile.dominantFoot === "RIGHT"
                        ? "Sağ"
                        : "Her ikisi"
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
          isOwnProfile && (
            <View style={styles.noProfile}>
              <View style={styles.noProfileIconCircle}>
                <Text style={styles.noProfileIcon}>📋</Text>
              </View>
              <Text style={styles.noProfileText}>Henüz profilin yok</Text>
              <Text style={styles.noProfileSub}>
                Profilini oluştur, diğer oyuncular seni tanısın.
              </Text>
              <Button
                title="Profil Oluştur"
                onPress={() => navigation.navigate("CreateProfile")}
                style={styles.createButton}
              />
            </View>
          )
        )}

        {isOwnProfile && (
          <View style={styles.actions}>
            <Button
              title="Çıkış Yap"
              onPress={handleLogout}
              variant="danger"
              icon="🚪"
              fullWidth
            />
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
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: Colors.background,
  },
  content: { paddingHorizontal: 20, paddingBottom: 32, paddingTop: 8 },
  hero: {
    alignItems: "center",
    paddingVertical: 28,
    overflow: "hidden",
  },
  heroOrb: {
    position: "absolute",
    top: -60,
    width: 240,
    height: 240,
    borderRadius: 120,
    backgroundColor: Colors.primaryGlow,
    opacity: 0.5,
  },
  avatarRing: {
    width: 108,
    height: 108,
    borderRadius: 54,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    padding: 6,
    marginBottom: 16,
    alignItems: "center",
    justifyContent: "center",
    ...Shadows.glow(Colors.primary),
  },
  avatarCircle: {
    width: 96,
    height: 96,
    borderRadius: 48,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontSize: 40, fontWeight: "800", color: Colors.textInverse },
  displayName: {
    fontSize: 26,
    fontWeight: "800",
    color: Colors.text,
    letterSpacing: -0.4,
  },
  fullName: {
    fontSize: 15,
    color: Colors.textSecondary,
    marginTop: 4,
    fontWeight: "500",
  },
  badgeRow: {
    flexDirection: "row",
    gap: 8,
    marginTop: 12,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: "700",
    color: Colors.text,
    marginBottom: 12,
    marginTop: 24,
    letterSpacing: -0.2,
  },
  scoresRow: { flexDirection: "row", gap: 12 },
  noProfile: {
    alignItems: "center",
    paddingVertical: 32,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.xl,
    borderWidth: 1,
    borderColor: Colors.border,
    marginTop: 20,
    paddingHorizontal: 24,
  },
  noProfileIconCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 16,
  },
  noProfileIcon: { fontSize: 30 },
  noProfileText: {
    fontSize: 17,
    fontWeight: "700",
    color: Colors.text,
    letterSpacing: -0.2,
  },
  noProfileSub: {
    fontSize: 14,
    color: Colors.textMuted,
    marginTop: 6,
    textAlign: "center",
    fontWeight: "500",
    lineHeight: 20,
  },
  createButton: { marginTop: 20, minWidth: 180 },
  actions: { marginTop: 28, gap: 10 },
});

const headerStyles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    marginRight: 4,
  },
  iconBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: "center",
    justifyContent: "center",
  },
  iconText: {
    fontSize: 16,
    color: Colors.text,
  },
});

const rowStyles = StyleSheet.create({
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingVertical: 12,
  },
  border: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  label: { fontSize: 13, color: Colors.textMuted, flex: 1, fontWeight: "500" },
  value: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: "600",
    flex: 2,
    textAlign: "right",
  },
});

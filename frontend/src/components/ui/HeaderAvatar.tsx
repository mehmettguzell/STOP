import React from "react";
import { TouchableOpacity, View, Text, Image, StyleSheet } from "react-native";
import { useNavigation } from "@react-navigation/native";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { useAuthStore } from "../../store/auth.store";
import { useProfileStore } from "../../store/profile.store";

export default function HeaderAvatar() {
  const navigation = useNavigation<any>();
  const user = useAuthStore((s) => s.user);
  const avatarUrl = useProfileStore((s) => s.profile?.avatarUrl);

  if (!user) return null;

  const statusColor =
    user.status === "ACTIVE"
      ? Colors.success
      : user.status === "SUSPENDED"
        ? Colors.error
        : Colors.textMuted;

  return (
    <TouchableOpacity
      style={styles.button}
      onPress={() => navigation.navigate("ProfileTab")}
      activeOpacity={0.85}
    >
      {avatarUrl ? (
        <Image source={{ uri: avatarUrl }} style={styles.image} />
      ) : (
        <Text style={styles.text}>
          {user.displayName.charAt(0).toUpperCase()}
        </Text>
      )}
      <View style={[styles.statusDot, { backgroundColor: statusColor }]} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
    ...Shadows.glow(Colors.primary),
  },
  text: {
    fontSize: 16,
    fontWeight: "800",
    color: Colors.textInverse,
  },
  image: {
    width: 40,
    height: 40,
    borderRadius: 20,
  },
  statusDot: {
    position: "absolute",
    bottom: 1,
    right: 1,
    width: 11,
    height: 11,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: Colors.backgroundElevated,
  },
});

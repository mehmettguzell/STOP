import React from "react";
import { View, Text, StyleSheet, ViewStyle } from "react-native";
import { Colors, Radius } from "../../theme/colors";
import Button from "./Button";

interface EmptyStateProps {
  icon: string;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  style?: ViewStyle;
}

export default function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  style,
}: EmptyStateProps) {
  return (
    <View style={[styles.container, style]}>
      <View style={styles.iconWrapper}>
        <View style={styles.iconRing} />
        <View style={styles.iconCircle}>
          <Text style={styles.icon}>{icon}</Text>
        </View>
      </View>
      <Text style={styles.title}>{title}</Text>
      {description && <Text style={styles.description}>{description}</Text>}
      {actionLabel && onAction && (
        <Button
          title={actionLabel}
          onPress={onAction}
          variant="subtle"
          size="md"
          style={{ marginTop: 20 }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
    paddingVertical: 56,
    paddingHorizontal: 32,
  },
  iconWrapper: {
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 20,
  },
  iconRing: {
    position: "absolute",
    width: 96,
    height: 96,
    borderRadius: 48,
    borderWidth: 1,
    borderColor: Colors.borderLight,
    opacity: 0.4,
  },
  iconCircle: {
    width: 76,
    height: 76,
    borderRadius: 38,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: "center",
    justifyContent: "center",
  },
  icon: {
    fontSize: 32,
  },
  title: {
    fontSize: 17,
    fontWeight: "700",
    color: Colors.text,
    textAlign: "center",
    letterSpacing: -0.2,
  },
  description: {
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: "center",
    marginTop: 6,
    lineHeight: 20,
  },
});

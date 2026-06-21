import React from "react";
import { View, Text, StyleSheet, ViewStyle } from "react-native";
import { Colors, Radius } from "../../theme/colors";

interface BadgeProps {
  label: string;
  color?: string;
  variant?: "filled" | "soft" | "dot";
  size?: "sm" | "md";
  icon?: string;
  style?: ViewStyle;
}

export default function Badge({
  label,
  color = Colors.primary,
  variant = "soft",
  size = "md",
  icon,
  style,
}: BadgeProps) {
  const isSm = size === "sm";

  const containerStyle = [
    styles.base,
    isSm ? styles.sm : styles.md,
    variant === "filled" && { backgroundColor: color },
    variant === "soft" && { backgroundColor: color + "1F", borderWidth: 1, borderColor: color + "33" },
    variant === "dot" && { backgroundColor: "transparent" },
    style,
  ];

  const textColor =
    variant === "filled"
      ? Colors.textInverse
      : color;

  return (
    <View style={containerStyle}>
      {variant === "dot" && (
        <View style={[styles.dot, { backgroundColor: color }]} />
      )}
      {icon && <Text style={[styles.icon, { color: textColor }]}>{icon}</Text>}
      <Text
        style={[
          isSm ? styles.textSm : styles.textMd,
          { color: textColor },
        ]}
      >
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: Radius.pill,
  },
  sm: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    gap: 4,
  },
  md: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    gap: 5,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  icon: {
    fontSize: 10,
  },
  textSm: {
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  textMd: {
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
});

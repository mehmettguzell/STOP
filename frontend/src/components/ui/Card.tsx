import React from "react";
import { View, StyleSheet, ViewStyle, TouchableOpacity } from "react-native";
import { Colors, Radius, Shadows } from "../../theme/colors";

interface CardProps {
  children: React.ReactNode;
  style?: ViewStyle;
  variant?: "default" | "elevated" | "glass" | "glow" | "outlined";
  onPress?: () => void;
  padding?: number;
}

export default function Card({
  children,
  style,
  variant = "default",
  onPress,
  padding = 18,
}: CardProps) {
  const containerStyle = [
    styles.card,
    { padding },
    variantStyles[variant],
    style,
  ];

  if (onPress) {
    return (
      <TouchableOpacity style={containerStyle} onPress={onPress} activeOpacity={0.85}>
        {children}
      </TouchableOpacity>
    );
  }

  return <View style={containerStyle}>{children}</View>;
}

const styles = StyleSheet.create({
  card: {
    borderRadius: Radius.lg,
  },
});

const variantStyles: Record<string, ViewStyle> = {
  default: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  elevated: {
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.md,
  },
  glass: {
    backgroundColor: Colors.surfaceGlass,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  glow: {
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    ...Shadows.glow(Colors.primary),
  },
  outlined: {
    backgroundColor: "transparent",
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
};

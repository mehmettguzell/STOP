import React from "react";
import { View, Text, StyleProp, ViewStyle, TextStyle } from "react-native";
import { Image } from "expo-image";
import { Colors } from "../../theme/colors";

type AvatarProps = {
  uri?: string | null;
  name: string;
  size: number;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
  backgroundColor?: string;
  borderColor?: string;
  borderWidth?: number;
  textColor?: string;
};

export default function Avatar({
  uri,
  name,
  size,
  style,
  textStyle,
  backgroundColor = Colors.primaryGlow,
  borderColor = Colors.primaryBorder,
  borderWidth = 1,
  textColor = Colors.primary,
}: AvatarProps) {
  const initial = (name ?? "").trim().charAt(0).toUpperCase() || "?";

  const circleStyle: ViewStyle = {
    width: size,
    height: size,
    borderRadius: size / 2,
    backgroundColor,
    borderWidth,
    borderColor,
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  };

  if (uri) {
    return (
      <Image
        source={{ uri }}
        style={[circleStyle, style] as any}
        cachePolicy="memory-disk"
        transition={150}
      />
    );
  }

  return (
    <View style={[circleStyle, style]}>
      <Text
        style={[
          { fontSize: size * 0.4, fontWeight: "800", color: textColor },
          textStyle,
        ]}
      >
        {initial}
      </Text>
    </View>
  );
}

import React from 'react';
import {
  TouchableOpacity,
  Text,
  ActivityIndicator,
  StyleSheet,
  ViewStyle,
  TextStyle,
  View,
} from 'react-native';
import { Colors, Radius, Shadows } from '../../theme/colors';

type ButtonSize = 'lg' | 'md' | 'sm' | 'default' | 'small';

interface ButtonProps {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'subtle';
  size?: ButtonSize;
  icon?: string;
  iconPosition?: 'left' | 'right';
  fullWidth?: boolean;
  style?: ViewStyle;
  textStyle?: TextStyle;
}

const SIZE_ALIAS: Record<ButtonSize, 'lg' | 'md' | 'sm'> = {
  lg: 'lg',
  md: 'md',
  sm: 'sm',
  default: 'md',
  small: 'sm',
};

export default function Button({
  title,
  onPress,
  loading = false,
  disabled = false,
  variant = 'primary',
  size = 'md',
  icon,
  iconPosition = 'left',
  fullWidth = false,
  style,
  textStyle,
}: ButtonProps) {
  const isDisabled = disabled || loading;

  const resolvedSize = SIZE_ALIAS[size] ?? 'md';
  const sizeStyle = sizeStyles[resolvedSize];
  const variantStyle = variantStyles[variant];
  const variantText = variantTextStyles[variant];

  return (
    <TouchableOpacity
      style={[
        styles.base,
        sizeStyle.container,
        variantStyle,
        fullWidth && styles.fullWidth,
        isDisabled && styles.disabled,
        style,
      ]}
      onPress={onPress}
      activeOpacity={0.85}
      disabled={isDisabled}
    >
      {loading ? (
        <ActivityIndicator
          color={variant === 'primary' || variant === 'danger' ? Colors.textInverse : Colors.primary}
          size="small"
        />
      ) : (
        <View style={styles.content}>
          {icon && iconPosition === 'left' && (
            <Text style={[sizeStyle.icon, variantText]}>{icon}</Text>
          )}
          <Text
            style={[
              sizeStyle.text,
              variantText,
              styles.textBase,
              textStyle,
            ]}
          >
            {title}
          </Text>
          {icon && iconPosition === 'right' && (
            <Text style={[sizeStyle.icon, variantText]}>{icon}</Text>
          )}
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: Radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
  },
  fullWidth: {
    alignSelf: 'stretch',
  },
  disabled: {
    opacity: 0.4,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  textBase: {
    fontWeight: '700',
    letterSpacing: 0.2,
  },
});

const sizeStyles = {
  lg: StyleSheet.create({
    container: { paddingVertical: 16, paddingHorizontal: 24, minHeight: 56 },
    text: { fontSize: 16 },
    icon: { fontSize: 18 },
  }),
  md: StyleSheet.create({
    container: { paddingVertical: 13, paddingHorizontal: 20, minHeight: 48 },
    text: { fontSize: 15 },
    icon: { fontSize: 16 },
  }),
  sm: StyleSheet.create({
    container: { paddingVertical: 9, paddingHorizontal: 14, minHeight: 36 },
    text: { fontSize: 13 },
    icon: { fontSize: 14 },
  }),
};

const variantStyles: Record<string, ViewStyle> = {
  primary: {
    backgroundColor: Colors.primary,
    ...Shadows.glow(Colors.primary),
  },
  secondary: {
    backgroundColor: Colors.surfaceHigh,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  outline: {
    backgroundColor: 'transparent',
    borderWidth: 1.5,
    borderColor: Colors.primaryBorder,
  },
  ghost: {
    backgroundColor: 'transparent',
  },
  subtle: {
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
  },
  danger: {
    backgroundColor: Colors.error,
    ...Shadows.glow(Colors.error),
  },
};

const variantTextStyles: Record<string, TextStyle> = {
  primary: { color: Colors.textInverse },
  secondary: { color: Colors.text },
  outline: { color: Colors.primary },
  ghost: { color: Colors.textSecondary },
  subtle: { color: Colors.primary },
  danger: { color: Colors.white },
};

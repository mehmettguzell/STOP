import React, { useState } from "react";
import {
  View,
  TextInput,
  Text,
  TouchableOpacity,
  StyleSheet,
  TextInputProps,
  ViewStyle,
} from "react-native";
import { Colors, Radius } from "../../theme/colors";

interface InputProps extends TextInputProps {
  label?: string;
  error?: string;
  hint?: string;
  containerStyle?: ViewStyle;
  isPassword?: boolean;
  leftIcon?: string;
}

export default function Input({
  label,
  error,
  hint,
  containerStyle,
  isPassword = false,
  leftIcon,
  ...props
}: InputProps) {
  const [showPassword, setShowPassword] = useState(false);
  const [focused, setFocused] = useState(false);

  return (
    <View style={[styles.container, containerStyle]}>
      {label && (
        <Text style={[styles.label, focused && styles.labelFocused, error && styles.labelError]}>
          {label}
        </Text>
      )}
      <View
        style={[
          styles.inputWrapper,
          focused && styles.inputFocused,
          error ? styles.inputError : null,
        ]}
      >
        {leftIcon && <Text style={styles.leftIcon}>{leftIcon}</Text>}
        <TextInput
          style={[styles.input, leftIcon ? { paddingLeft: 4 } : null]}
          placeholderTextColor={Colors.textDim}
          secureTextEntry={isPassword && !showPassword}
          autoCorrect={false}
          autoCapitalize="none"
          onFocus={(e) => {
            setFocused(true);
            props.onFocus?.(e);
          }}
          onBlur={(e) => {
            setFocused(false);
            props.onBlur?.(e);
          }}
          {...props}
        />
        {isPassword && (
          <TouchableOpacity
            onPress={() => setShowPassword((v) => !v)}
            style={styles.eyeButton}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Text style={styles.eyeIcon}>{showPassword ? "🙈" : "👁"}</Text>
          </TouchableOpacity>
        )}
      </View>
      {error ? (
        <Text style={styles.errorText}>{error}</Text>
      ) : hint ? (
        <Text style={styles.hintText}>{hint}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  label: {
    fontSize: 12,
    fontWeight: "700",
    color: Colors.textMuted,
    marginBottom: 8,
    letterSpacing: 0.6,
    textTransform: "uppercase",
  },
  labelFocused: {
    color: Colors.primary,
  },
  labelError: {
    color: Colors.error,
  },
  inputWrapper: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1.5,
    borderColor: Colors.border,
    paddingHorizontal: 16,
  },
  inputFocused: {
    borderColor: Colors.primary,
    backgroundColor: Colors.surfaceHigh,
  },
  inputError: {
    borderColor: Colors.error,
    backgroundColor: Colors.errorGlow,
  },
  leftIcon: {
    fontSize: 16,
    marginRight: 8,
    color: Colors.textMuted,
  },
  input: {
    flex: 1,
    paddingVertical: 14,
    fontSize: 15,
    color: Colors.text,
    fontWeight: "500",
  },
  eyeButton: {
    paddingHorizontal: 4,
    paddingVertical: 4,
  },
  eyeIcon: {
    fontSize: 16,
  },
  errorText: {
    fontSize: 12,
    color: Colors.error,
    marginTop: 6,
    marginLeft: 4,
    fontWeight: "600",
  },
  hintText: {
    fontSize: 12,
    color: Colors.textDim,
    marginTop: 6,
    marginLeft: 4,
    fontWeight: "500",
  },
});

import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuthStore } from "../../store/auth.store";
import { LoginScreenProps } from "../../navigation/types";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { getErrorMessage, getErrorCode } from "../../utils/error";

export default function LoginScreen({ navigation }: LoginScreenProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<{ email?: string; password?: string }>(
    {},
  );

  const { login, isLoading } = useAuthStore();

  const validate = (): boolean => {
    const newErrors: typeof errors = {};
    if (!email.trim()) newErrors.email = "E-posta adresini gir";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      newErrors.email = "Geçerli bir e-posta gir";
    if (!password) newErrors.password = "Parolanı gir";
    else if (password.length < 8) newErrors.password = "En az 8 karakter olmalı";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleLogin = async () => {
    if (!validate()) return;
    try {
      await login({ email: email.trim().toLowerCase(), password });
    } catch (err) {
      const code = getErrorCode(err);
      const titles: Record<string, string> = {
        INVALID_CREDENTIALS: "Giriş Yapılamadı",
        USER_SUSPENDED: "Hesap Askıya Alındı",
        USER_DELETED: "Hesap Silinmiş",
        NETWORK_ERROR: "Bağlantı Hatası",
      };
      Alert.alert(titles[code] ?? "Giriş Yapılamadı", getErrorMessage(err));
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={styles.flex}
      >
        {/* Decorative gradient orbs */}
        <View style={styles.bgOrb1} />
        <View style={styles.bgOrb2} />

        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.logoWrapper}>
              <View style={styles.logoOuter}>
                <View style={styles.logoInner}>
                  <Text style={styles.logoIcon}>⚽</Text>
                </View>
              </View>
            </View>
            <Text style={styles.brand}>STOP</Text>
            <Text style={styles.tagline}>Maçını bul. Sahaya çık.</Text>
          </View>

          {/* Form */}
          <View style={styles.formCard}>
            <Text style={styles.title}>Tekrar hoş geldin</Text>
            <Text style={styles.subtitle}>Hesabına giriş yap</Text>

            <View style={{ marginTop: 28 }}>
              <Input
                label="E-posta"
                placeholder="ornek@mail.com"
                keyboardType="email-address"
                value={email}
                onChangeText={setEmail}
                error={errors.email}
                autoComplete="email"
                leftIcon="📨"
              />

              <Input
                label="Parola"
                placeholder="••••••••"
                value={password}
                onChangeText={setPassword}
                error={errors.password}
                isPassword
                leftIcon="🔒"
              />

              <TouchableOpacity
                onPress={() => navigation.navigate("ForgotPassword")}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                style={styles.forgotWrapper}
              >
                <Text style={styles.forgotText}>Parolamı Unuttum</Text>
              </TouchableOpacity>

              <Button
                title="Giriş Yap"
                onPress={handleLogin}
                loading={isLoading}
                size="lg"
                fullWidth
                style={{ marginTop: 8 }}
              />
            </View>
          </View>

          {/* Footer */}
          <View style={styles.footer}>
            <Text style={styles.footerText}>Hesabın yok mu?</Text>
            <TouchableOpacity
              onPress={() => navigation.navigate("Register")}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.linkText}>Kayıt Ol</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  flex: { flex: 1 },
  scroll: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingTop: 32,
    paddingBottom: 32,
    justifyContent: "center",
  },
  bgOrb1: {
    position: "absolute",
    top: -100,
    right: -100,
    width: 280,
    height: 280,
    borderRadius: 140,
    backgroundColor: Colors.primaryGlow,
    opacity: 0.5,
  },
  bgOrb2: {
    position: "absolute",
    bottom: -120,
    left: -120,
    width: 320,
    height: 320,
    borderRadius: 160,
    backgroundColor: Colors.accentGlow,
    opacity: 0.4,
  },
  header: {
    alignItems: "center",
    marginBottom: 32,
  },
  logoWrapper: {
    marginBottom: 18,
  },
  logoOuter: {
    width: 88,
    height: 88,
    borderRadius: 28,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
    ...Shadows.glow(Colors.primary),
  },
  logoInner: {
    width: 64,
    height: 64,
    borderRadius: 20,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  logoIcon: {
    fontSize: 32,
  },
  brand: {
    fontSize: 38,
    fontWeight: "900",
    color: Colors.text,
    letterSpacing: 6,
  },
  tagline: {
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 6,
    letterSpacing: 0.4,
    fontWeight: "500",
  },
  formCard: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.xxl,
    padding: 28,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.lg,
  },
  title: {
    fontSize: 24,
    fontWeight: "800",
    color: Colors.text,
    letterSpacing: -0.4,
  },
  subtitle: {
    fontSize: 14,
    color: Colors.textMuted,
    marginTop: 4,
    fontWeight: "500",
  },
  footer: {
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    marginTop: 28,
    gap: 6,
  },
  footerText: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: "500",
  },
  linkText: {
    color: Colors.primary,
    fontSize: 14,
    fontWeight: "700",
  },
  forgotWrapper: {
    alignSelf: 'flex-end',
    marginTop: 6,
    marginBottom: 4,
  },
  forgotText: {
    color: Colors.textMuted,
    fontSize: 13,
    textDecorationLine: 'underline',
  },
});

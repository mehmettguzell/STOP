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
import { RegisterScreenProps } from "../../navigation/types";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { Colors, Radius, Shadows } from "../../theme/colors";
import { getErrorMessage } from "../../utils/error";

interface FormState {
  displayName: string;
  email: string;
  phoneNumber: string;
  password: string;
  confirmPassword: string;
}

interface FormErrors {
  displayName?: string;
  email?: string;
  phoneNumber?: string;
  password?: string;
  confirmPassword?: string;
}

const PHONE_PREFIX = "+90";

function getPasswordStrength(password: string): {
  level: 0 | 1 | 2 | 3;
  label: string;
  color: string;
} {
  if (!password) return { level: 0, label: "", color: "transparent" };
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (score <= 1) return { level: 1, label: "Zayıf", color: Colors.error };
  if (score === 2) return { level: 2, label: "Orta", color: Colors.warning };
  return { level: 3, label: "Güçlü", color: Colors.success };
}

export default function RegisterScreen({ navigation }: RegisterScreenProps) {
  const [form, setForm] = useState<FormState>({
    displayName: "",
    email: "",
    phoneNumber: PHONE_PREFIX,
    password: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});

  const { register, isLoading } = useAuthStore();

  const setField = (key: keyof FormState) => (value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    if (errors[key]) setErrors((prev) => ({ ...prev, [key]: undefined }));
  };

  const handlePhoneChange = (value: string) => {
    // +90 prefixini koruyalım, sadece arkasına rakam girilsin
    if (!value.startsWith(PHONE_PREFIX)) {
      const digits = value.replace(/\D/g, "");
      const safe = PHONE_PREFIX + digits.slice(2); // 90'ı çift almamak için
      setForm((prev) => ({ ...prev, phoneNumber: safe }));
    } else {
      const rest = value.slice(PHONE_PREFIX.length).replace(/\D/g, "");
      setForm((prev) => ({ ...prev, phoneNumber: PHONE_PREFIX + rest }));
    }
    if (errors.phoneNumber)
      setErrors((prev) => ({ ...prev, phoneNumber: undefined }));
  };

  const passwordStrength = getPasswordStrength(form.password);

  const validate = (): boolean => {
    const e: FormErrors = {};

    // Kullanıcı adı
    const name = form.displayName.trim();
    if (!name) e.displayName = "Kullanıcı adı gerekli";
    else if (name.length < 2 || name.length > 20)
      e.displayName = "2-20 karakter arasında olmalı";
    else if (!/^[a-zA-Z0-9_çğıöşüÇĞİÖŞÜ ]+$/.test(name))
      e.displayName = "Geçersiz karakter içeriyor";

    // E-posta
    if (!form.email.trim()) e.email = "E-posta adresini gir";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(form.email.trim()))
      e.email = "Geçerli bir e-posta gir";

    // Telefon — +90 + 10 rakam (Türk formatı)
    const phoneDigitsAfterPrefix = form.phoneNumber.slice(PHONE_PREFIX.length);
    if (phoneDigitsAfterPrefix.length === 0)
      e.phoneNumber = "Telefon numarası gerekli";
    else if (!/^5[0-9]{9}$/.test(phoneDigitsAfterPrefix))
      e.phoneNumber = "+90 ile başlayan geçerli bir numara gir";

    // Şifre
    if (!form.password) e.password = "Şifre gerekli";
    else if (form.password.length < 8) e.password = "En az 8 karakter olmalı";
    else if (form.password.length > 64)
      e.password = "En fazla 64 karakter olabilir";
    else if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).*$/.test(form.password))
      e.password = "Büyük-küçük harf ve rakam içermeli";

    // Şifre tekrar
    if (!form.confirmPassword) e.confirmPassword = "Şifreyi tekrar gir";
    else if (form.password !== form.confirmPassword)
      e.confirmPassword = "Şifreler eşleşmiyor";

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleRegister = async () => {
    if (!validate()) return;
    try {
      await register({
        displayName: form.displayName.trim(),
        email: form.email.trim().toLowerCase(),
        phoneNumber: form.phoneNumber,
        password: form.password,
      });
    } catch (err) {
      Alert.alert("Kayıt Yapılamadı", getErrorMessage(err));
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={styles.flex}
      >
        <View style={styles.bgOrb} />
        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* Header */}
          <View style={styles.header}>
            <View style={styles.logoOuter}>
              <View style={styles.logoInner}>
                <Text style={styles.logoIcon}>⚽</Text>
              </View>
            </View>
            <Text style={styles.brand}>STOP</Text>
            <Text style={styles.tagline}>
              Sahaya çıkmak için bir hesap oluştur
            </Text>
          </View>

          {/* Form Card */}
          <View style={styles.formCard}>
            <Text style={styles.title}>Hesap Oluştur</Text>
            <Text style={styles.subtitle}>Birkaç adımda tamamlanır</Text>

            <View style={{ marginTop: 24 }}>
              <Input
                label="Kullanıcı Adı"
                placeholder="Futbolcu27"
                value={form.displayName}
                onChangeText={setField("displayName")}
                error={errors.displayName}
                maxLength={20}
                leftIcon="👤"
              />

              <Input
                label="E-posta"
                placeholder="ornek@mail.com"
                keyboardType="email-address"
                value={form.email}
                onChangeText={setField("email")}
                error={errors.email}
                autoComplete="email"
                leftIcon="📨"
              />

              <Input
                label="Telefon Numarası"
                placeholder="+90 5XX XXX XX XX"
                keyboardType="phone-pad"
                value={form.phoneNumber}
                onChangeText={handlePhoneChange}
                error={errors.phoneNumber}
                leftIcon="📱"
              />

              <Input
                label="Şifre"
                placeholder="••••••••"
                value={form.password}
                onChangeText={setField("password")}
                error={errors.password}
                isPassword
                leftIcon="🔒"
              />
              {form.password.length > 0 && (
                <View style={styles.strengthContainer}>
                  <View style={styles.strengthBars}>
                    {[1, 2, 3].map((i) => (
                      <View
                        key={i}
                        style={[
                          styles.strengthBar,
                          {
                            backgroundColor:
                              passwordStrength.level >= i
                                ? passwordStrength.color
                                : Colors.border,
                          },
                        ]}
                      />
                    ))}
                  </View>
                  <Text
                    style={[
                      styles.strengthLabel,
                      { color: passwordStrength.color },
                    ]}
                  >
                    {passwordStrength.label}
                  </Text>
                </View>
              )}

              <Input
                label="Şifre (Tekrar)"
                placeholder="••••••••"
                value={form.confirmPassword}
                onChangeText={setField("confirmPassword")}
                error={errors.confirmPassword}
                isPassword
                leftIcon="🔒"
              />

              <Button
                title="Hesap Oluştur"
                onPress={handleRegister}
                loading={isLoading}
                size="lg"
                fullWidth
                style={{ marginTop: 8 }}
              />
            </View>
          </View>

          {/* Footer */}
          <View style={styles.footer}>
            <Text style={styles.footerText}>Zaten hesabın var mı?</Text>
            <TouchableOpacity
              onPress={() => navigation.goBack()}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.linkText}>Giriş Yap</Text>
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
  bgOrb: {
    position: "absolute",
    top: -120,
    right: -120,
    width: 320,
    height: 320,
    borderRadius: 160,
    backgroundColor: Colors.primaryGlow,
    opacity: 0.5,
  },
  scroll: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 32,
  },
  header: {
    alignItems: "center",
    marginBottom: 24,
  },
  logoOuter: {
    width: 76,
    height: 76,
    borderRadius: 24,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 14,
    ...Shadows.glow(Colors.primary),
  },
  logoInner: {
    width: 56,
    height: 56,
    borderRadius: 18,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  logoIcon: {
    fontSize: 26,
  },
  brand: {
    fontSize: 28,
    fontWeight: "900",
    color: Colors.text,
    letterSpacing: 5,
  },
  tagline: {
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 6,
    letterSpacing: 0.3,
    fontWeight: "500",
  },
  formCard: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.xxl,
    padding: 24,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.lg,
  },
  title: {
    fontSize: 22,
    fontWeight: "800",
    color: Colors.text,
    letterSpacing: -0.4,
  },
  subtitle: {
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 4,
    fontWeight: "500",
  },
  strengthContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: -8,
    marginBottom: 16,
    gap: 10,
  },
  strengthBars: {
    flexDirection: "row",
    gap: 5,
    flex: 1,
  },
  strengthBar: {
    flex: 1,
    height: 4,
    borderRadius: 2,
  },
  strengthLabel: {
    fontSize: 12,
    fontWeight: "700",
    width: 44,
    textAlign: "right",
  },
  footer: {
    flexDirection: "row",
    justifyContent: "center",
    marginTop: 24,
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
});

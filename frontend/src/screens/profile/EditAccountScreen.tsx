import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { userApi } from '../../api/user.api';
import { useAuthStore } from '../../store/auth.store';
import { EditAccountScreenProps } from '../../navigation/types';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import Card from '../../components/ui/Card';
import { Colors } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

const PHONE_PREFIX = '+90';

function getPasswordStrength(password: string): { level: 0 | 1 | 2 | 3; label: string; color: string } {
  if (!password) return { level: 0, label: '', color: 'transparent' };
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (score <= 1) return { level: 1, label: 'Zayıf', color: Colors.error };
  if (score === 2) return { level: 2, label: 'Orta', color: Colors.warning };
  return { level: 3, label: 'Güçlü', color: Colors.success };
}

export default function EditAccountScreen({ navigation }: EditAccountScreenProps) {
  const { user, refreshUser, deleteAccount } = useAuthStore();
  const [saving, setSaving] = useState(false);

  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [phoneNumber, setPhoneNumber] = useState(
    user?.phoneNumber?.startsWith('+') ? user.phoneNumber : PHONE_PREFIX + (user?.phoneNumber ?? ''),
  );
  const [password, setPassword] = useState('');
  const [rePassword, setRePassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const clearError = (key: string) =>
    setErrors((prev) => ({ ...prev, [key]: '' }));

  const passwordStrength = getPasswordStrength(password);

  const handlePhoneChange = (value: string) => {
    if (!value.startsWith(PHONE_PREFIX)) {
      const digits = value.replace(/\D/g, '');
      setPhoneNumber(PHONE_PREFIX + digits.slice(2));
    } else {
      const rest = value.slice(PHONE_PREFIX.length).replace(/\D/g, '');
      setPhoneNumber(PHONE_PREFIX + rest);
    }
    clearError('phoneNumber');
  };

  const validate = (): boolean => {
    const e: Record<string, string> = {};

    if (displayName && (displayName.length < 3 || displayName.length > 30))
      e.displayName = '3-30 karakter arasında olmalı';

    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim()))
      e.email = 'Geçerli bir e-posta adresi gir';

    const phoneDigits = phoneNumber.slice(PHONE_PREFIX.length);
    if (phoneDigits && !/^5[0-9]{9}$/.test(phoneDigits))
      e.phoneNumber = '+90 ile başlayan geçerli bir numara gir';

    if (password) {
      if (password.length < 8)
        e.password = 'En az 8 karakter olmalı';
      else if (password.length > 64)
        e.password = 'En fazla 64 karakter olabilir';
      else if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).*$/.test(password))
        e.password = 'Büyük-küçük harf ve rakam içermeli';

      if (!rePassword)
        e.rePassword = 'Şifreyi tekrar gir';
      else if (password !== rePassword)
        e.rePassword = 'Şifreler eşleşmiyor';
    }

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;

    const payload: Record<string, string> = {};
    if (displayName !== user?.displayName) payload.displayName = displayName;
    if (email !== user?.email) payload.email = email.trim().toLowerCase();
    if (phoneNumber !== user?.phoneNumber) payload.phoneNumber = phoneNumber;
    if (password) {
      payload.password = password;
      payload.rePassword = rePassword;
    }

    if (Object.keys(payload).length === 0) {
      Alert.alert('Değişiklik yok', 'Güncellenecek bir şey bulamadım.');
      return;
    }

    setSaving(true);
    try {
      await userApi.updateMe(payload);
      await refreshUser();
      Alert.alert('Güncellendi', 'Bilgilerin başarıyla kaydedildi.', [
        { text: 'Tamam', onPress: () => navigation.goBack() },
      ]);
    } catch (err) {
      Alert.alert('Güncellenemedi', getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAccount = () => {
    Alert.alert(
      'Hesabı Sil',
      'Bu işlem geri alınamaz. Hesabın ve tüm verileriniz kalıcı olarak silinecek.',
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Sil',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteAccount();
            } catch (err) {
              Alert.alert('Bir hata oluştu', getErrorMessage(err));
            }
          },
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.flex}
      >
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <Text style={styles.sectionTitle}>Bilgilerim</Text>

          <Input
            label="Kullanıcı Adı"
            value={displayName}
            onChangeText={(v) => { setDisplayName(v); clearError('displayName'); }}
            error={errors.displayName}
            maxLength={30}
          />
          <Input
            label="E-posta"
            value={email}
            onChangeText={(v) => { setEmail(v); clearError('email'); }}
            error={errors.email}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
          />
          <Input
            label="Telefon Numarası"
            value={phoneNumber}
            onChangeText={handlePhoneChange}
            error={errors.phoneNumber}
            keyboardType="phone-pad"
          />

          <Text style={styles.sectionTitle}>Şifre Değiştir</Text>
          <Text style={styles.hint}>Boş bırakırsan şifren değişmez.</Text>

          <Input
            label="Yeni Şifre"
            placeholder="••••••••"
            value={password}
            onChangeText={(v) => { setPassword(v); clearError('password'); }}
            error={errors.password}
            isPassword
          />
          {password.length > 0 && (
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
              <Text style={[styles.strengthLabel, { color: passwordStrength.color }]}>
                {passwordStrength.label}
              </Text>
            </View>
          )}
          <Input
            label="Şifre (Tekrar)"
            placeholder="••••••••"
            value={rePassword}
            onChangeText={(v) => { setRePassword(v); clearError('rePassword'); }}
            error={errors.rePassword}
            isPassword
          />

          <Button
            title="Kaydet"
            onPress={handleSave}
            loading={saving}
            style={styles.saveBtn}
          />

          <Text style={styles.dangerTitle}>Hesabı Kapat</Text>
          <Card style={styles.dangerCard}>
            <Text style={styles.dangerText}>
              Hesabını silersen tüm verilerine bir daha ulaşamazsın.
            </Text>
            <Button
              title="Hesabi Sil"
              onPress={handleDeleteAccount}
              variant="danger"
              style={styles.deleteBtn}
            />
          </Card>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  flex: { flex: 1 },
  content: { paddingHorizontal: 20, paddingBottom: 40, paddingTop: 8 },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 14,
    marginTop: 20,
    letterSpacing: 0.3,
  },
  hint: {
    fontSize: 12,
    color: Colors.textMuted,
    marginBottom: 12,
    marginTop: -8,
  },
  strengthContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: -8,
    marginBottom: 16,
    gap: 10,
  },
  strengthBars: {
    flexDirection: 'row',
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
    fontWeight: '700',
    width: 44,
    textAlign: 'right',
  },
  saveBtn: { marginTop: 8 },
  dangerTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.error,
    marginTop: 36,
    marginBottom: 14,
  },
  dangerCard: {
    borderColor: Colors.error + '44',
    backgroundColor: Colors.errorGlow,
  },
  dangerText: {
    fontSize: 14,
    color: Colors.textSecondary,
    marginBottom: 16,
    lineHeight: 20,
  },
  deleteBtn: {},
});

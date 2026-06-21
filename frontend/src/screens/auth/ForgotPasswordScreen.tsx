import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../navigation/types';
import { authApi } from '../../api/auth.api';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { Colors } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

type Props = NativeStackScreenProps<AuthStackParamList, 'ForgotPassword'>;

type Step = 'email' | 'reset';

export default function ForgotPasswordScreen({ navigation }: Props) {
  const [step, setStep]             = useState<Step>('email');
  const [email, setEmail]           = useState('');
  const [token, setToken]           = useState('');
  const [devToken, setDevToken]     = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm]       = useState('');
  const [loading, setLoading]       = useState(false);
  const [errors, setErrors]         = useState<Record<string, string>>({});

  // ── Adım 1: E-posta gönder ──────────────────────────────────────────────
  const handleRequestReset = async () => {
    if (!email.trim()) {
      setErrors({ email: 'E-posta adresini gir' });
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setErrors({ email: 'Geçerli bir e-posta gir' });
      return;
    }
    setErrors({});
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email.trim().toLowerCase());
      if (res.devToken) {
        // Dev modu: token ekranda gösterilir
        setDevToken(res.devToken);
        setToken(res.devToken);   // adım 2'yi otomatik doldur
        setStep('reset');
      } else {
        Alert.alert(
          'Kontrol Edin',
          'Eğer bu e-posta kayıtlıysa sıfırlama kodu gönderildi.\n\nKodu aldıktan sonra "Kodumu aldım" butonuna basın.',
          [{ text: 'Kodumu aldım', onPress: () => setStep('reset') }],
        );
      }
    } catch (err) {
      Alert.alert('Hata', getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  // ── Adım 2: Token + yeni şifre ─────────────────────────────────────────
  const handleReset = async () => {
    const e: Record<string, string> = {};
    if (!token.trim())         e.token = 'Sıfırlama kodunu gir';
    if (newPassword.length < 8) e.newPassword = 'Parola en az 8 karakter olmalı';
    if (newPassword !== confirm) e.confirm = 'Parolalar eşleşmiyor';
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setLoading(true);
    try {
      await authApi.resetPassword(token.trim(), newPassword);
      Alert.alert('Başarılı', 'Parolan güncellendi. Artık giriş yapabilirsin.', [
        { text: 'Giriş Yap', onPress: () => navigation.navigate('Login') },
      ]);
    } catch (err) {
      Alert.alert('Hata', getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {step === 'email' ? (
            <>
              <Text style={styles.icon}>🔑</Text>
              <Text style={styles.title}>Parolamı Unuttum</Text>
              <Text style={styles.subtitle}>
                Kayıtlı e-posta adresini gir. Sana bir sıfırlama kodu göndereceğiz.
              </Text>

              <Input
                label="E-posta"
                placeholder="ornek@email.com"
                value={email}
                onChangeText={(v) => { setEmail(v); setErrors({}); }}
                error={errors.email}
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
              />

              <View style={styles.actions}>
                <Button
                  title="Sıfırlama Kodu Gönder"
                  onPress={handleRequestReset}
                  loading={loading}
                  icon="📨"
                />
                <Button
                  title="Zaten kodum var"
                  onPress={() => setStep('reset')}
                  variant="ghost"
                />
              </View>
            </>
          ) : (
            <>
              <Text style={styles.icon}>🔐</Text>
              <Text style={styles.title}>Yeni Parola</Text>
              <Text style={styles.subtitle}>
                E-postana gelen kodu ve yeni parolanı gir.
              </Text>

              {!!devToken && (
                <TouchableOpacity
                  style={styles.devTokenBox}
                  onPress={() => Alert.alert('İpucu', 'Metni uzun basarak kopyalayabilirsiniz.')}
                  activeOpacity={0.7}
                >
                  <Text style={styles.devTokenLabel}>🛠️ Dev Modu — Sıfırlama Kodu (Kopyalamak için dokun)</Text>
                  <Text style={styles.devTokenValue} selectable>{devToken}</Text>
                </TouchableOpacity>
              )}

              <Input
                label="Sıfırlama Kodu"
                placeholder="E-postandan gelen kodu yapıştır"
                value={token}
                onChangeText={(v) => { setToken(v); setErrors({}); }}
                error={errors.token}
                autoCapitalize="none"
                autoCorrect={false}
              />

              <Input
                label="Yeni Parola"
                placeholder="En az 8 karakter"
                value={newPassword}
                onChangeText={(v) => { setNewPassword(v); setErrors({}); }}
                error={errors.newPassword}
                secureTextEntry
              />

              <Input
                label="Parola Tekrar"
                placeholder="Parolanı tekrar gir"
                value={confirm}
                onChangeText={(v) => { setConfirm(v); setErrors({}); }}
                error={errors.confirm}
                secureTextEntry
              />

              <View style={styles.actions}>
                <Button
                  title="Parolamı Güncelle"
                  onPress={handleReset}
                  loading={loading}
                  icon="✅"
                />
                <TouchableOpacity
                  onPress={() => setStep('email')}
                  style={styles.backBtn}
                  activeOpacity={0.7}
                >
                  <Text style={styles.backText}>← Geri dön, yeni kod iste</Text>
                </TouchableOpacity>
              </View>
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe:     { flex: 1, backgroundColor: Colors.background },
  content:  { padding: 24, paddingTop: 8, paddingBottom: 40 },

  icon:     { fontSize: 52, textAlign: 'center', marginBottom: 16, marginTop: 8 },
  title:    { fontSize: 26, fontWeight: '800', color: Colors.text, textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 14, color: Colors.textMuted, textAlign: 'center', lineHeight: 20, marginBottom: 28 },

  actions: { marginTop: 24, gap: 12 },
  backBtn: { alignItems: 'center', paddingVertical: 8 },
  backText: { fontSize: 13, color: Colors.primary },

  devTokenBox: {
    backgroundColor: '#1a1a2e',
    borderWidth: 1,
    borderColor: '#f59e0b',
    borderRadius: 10,
    padding: 14,
    marginBottom: 16,
  },
  devTokenLabel: {
    fontSize: 11,
    color: '#f59e0b',
    fontWeight: '700',
    marginBottom: 8,
  },
  devTokenValue: {
    fontSize: 12,
    color: '#fcd34d',
    fontFamily: Platform.OS === 'ios' ? 'Courier New' : 'monospace',
    lineHeight: 18,
  },
});

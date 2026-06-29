import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ActivityIndicator,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { profileApi } from '../../api/profile.api';
import { useAuthStore } from '../../store/auth.store';
import { useProfileStore } from '../../store/profile.store';
import { EditProfileScreenProps } from '../../navigation/types';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import CityPicker from '../../components/ui/CityPicker';
import DatePicker from '../../components/ui/DatePicker';
import { Colors } from '../../theme/colors';
import { DominantFoot } from '../../types/user.types';
import { getErrorMessage } from '../../utils/error';

type FootOption = { label: string; value: DominantFoot };
const FOOT_OPTIONS: FootOption[] = [
  { label: 'Sol', value: 'LEFT' },
  { label: 'Sağ', value: 'RIGHT' },
  { label: 'Her İkisi', value: 'BOTH' },
];

const POSITIONS = [
  'Kaleci', 'Stoper', 'Sol Bek', 'Sağ Bek', 'Defansif OO',
  'Merkez OO', 'Sol Kanat', 'Sağ Kanat', 'Forvet',
];

const NAME_REGEX = /^[\p{L}\p{M}\s'.-]+$/u;

export default function EditProfileScreen({ navigation }: EditProfileScreenProps) {
  const user = useAuthStore((s) => s.user);
  const { invalidate } = useProfileStore();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    birthDate: '',
    city: '',
    position: '',
    heightCm: '',
    weightKg: '',
    dominantFoot: '' as DominantFoot | '',
    bio: '',
    avatarUrl: '',
  });

  useEffect(() => {
    const load = async () => {
      if (!user) return;
      try {
        const profile = await profileApi.getProfile(user.id);
        setForm({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          birthDate: profile.birthDate ?? '',
          city: profile.city ?? '',
          position: profile.position ?? '',
          heightCm: profile.heightCm?.toString() ?? '',
          weightKg: profile.weightKg?.toString() ?? '',
          dominantFoot: (profile.dominantFoot as DominantFoot) ?? '',
          bio: profile.bio ?? '',
          avatarUrl: profile.avatarUrl ?? '',
        });
      } catch (err) {
        Alert.alert('Hata', getErrorMessage(err));
        navigation.goBack();
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user, navigation]);

  const setField = (key: string) => (value: string) => {
    setForm((p) => ({ ...p, [key]: value }));
    if (errors[key]) setErrors((p) => ({ ...p, [key]: '' }));
  };

  const validate = (): boolean => {
    const e: Record<string, string> = {};

    const firstName = form.firstName.trim();
    if (!firstName)
      e.firstName = 'Ad gerekli';
    else if (firstName.length < 2 || firstName.length > 50)
      e.firstName = '2-50 karakter arasında olmalı';
    else if (!NAME_REGEX.test(firstName))
      e.firstName = 'Geçersiz karakter içeriyor';

    const lastName = form.lastName.trim();
    if (!lastName)
      e.lastName = 'Soyad gerekli';
    else if (lastName.length < 2 || lastName.length > 50)
      e.lastName = '2-50 karakter arasında olmalı';
    else if (!NAME_REGEX.test(lastName))
      e.lastName = 'Geçersiz karakter içeriyor';

    if (!form.birthDate)
      e.birthDate = 'Doğum tarihi gerekli';
    else {
      const birth = new Date(form.birthDate);
      const today = new Date();
      const age = today.getFullYear() - birth.getFullYear();
      if (birth >= today)
        e.birthDate = 'Doğum tarihi geçmişte olmalı';
      else if (age < 5)
        e.birthDate = 'Geçersiz doğum tarihi';
      else if (age > 100)
        e.birthDate = 'Geçersiz doğum tarihi';
    }

    if (!form.city.trim())
      e.city = 'Şehir gerekli';

    if (!form.position.trim())
      e.position = 'Pozisyon seçin';

    if (!form.heightCm)
      e.heightCm = 'Boy gerekli';
    else if (isNaN(Number(form.heightCm)) || !Number.isInteger(Number(form.heightCm)))
      e.heightCm = 'Geçersiz değer';
    else if (Number(form.heightCm) < 50 || Number(form.heightCm) > 280)
      e.heightCm = '50-280 cm arasında olmalı';

    if (!form.weightKg)
      e.weightKg = 'Kilo gerekli';
    else if (isNaN(Number(form.weightKg)) || !Number.isInteger(Number(form.weightKg)))
      e.weightKg = 'Geçersiz değer';
    else if (Number(form.weightKg) < 15 || Number(form.weightKg) > 500)
      e.weightKg = '15-500 kg arasında olmalı';

    if (!form.dominantFoot)
      e.dominantFoot = 'Baskın ayak seçin';

    if (form.bio.trim().length > 2000)
      e.bio = 'En fazla 2000 karakter olabilir';

    if (form.avatarUrl.trim() && !/^https?:\/\/.+/.test(form.avatarUrl.trim()))
      e.avatarUrl = 'Geçerli bir http(s) adresi olmalı';

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    setSaving(true);
    try {
      await profileApi.updateProfile({
        firstName: form.firstName.trim() || undefined,
        lastName: form.lastName.trim() || undefined,
        birthDate: form.birthDate || undefined,
        city: form.city.trim() || undefined,
        position: form.position.trim() || undefined,
        heightCm: form.heightCm ? Number(form.heightCm) : undefined,
        weightKg: form.weightKg ? Number(form.weightKg) : undefined,
        dominantFoot: (form.dominantFoot as DominantFoot) || undefined,
        bio: form.bio.trim() || undefined,
        avatarUrl: form.avatarUrl.trim() || undefined,
      });
      invalidate();
      Alert.alert('Güncellendi', 'Profilin başarıyla kaydedildi.', [
        { text: 'Tamam', onPress: () => navigation.goBack() },
      ]);
    } catch (err) {
      Alert.alert('Güncelleme Başarısız', getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

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
          <Input
            label="Ad"
            placeholder="Mehmet"
            value={form.firstName}
            onChangeText={setField('firstName')}
            error={errors.firstName}
            maxLength={50}
          />
          <Input
            label="Soyad"
            placeholder="Güzel"
            value={form.lastName}
            onChangeText={setField('lastName')}
            error={errors.lastName}
            maxLength={50}
          />

          <DatePicker
            label="Doğum Tarihi"
            value={form.birthDate}
            onChange={setField('birthDate')}
            error={errors.birthDate}
            required
          />

          <CityPicker value={form.city} onSelect={setField('city')} error={errors.city} />

          {/* Pozisyon */}
          <Text style={styles.pickerLabel}>POZİSYON</Text>
          {errors.position ? <Text style={styles.errorText}>{errors.position}</Text> : null}
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.chipScroll}
            keyboardShouldPersistTaps="handled"
          >
            {POSITIONS.map((pos) => (
              <TouchableOpacity
                key={pos}
                style={[styles.chip, form.position === pos && styles.chipSelected]}
                onPress={() => setField('position')(pos)}
              >
                <Text style={[styles.chipText, form.position === pos && styles.chipTextSelected]}>
                  {pos}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          <View style={styles.row}>
            <View style={styles.halfInput}>
              <Input
                label="Boy (cm)"
                placeholder="178"
                keyboardType="numeric"
                value={form.heightCm}
                onChangeText={(v) => setField('heightCm')(v.replace(/[^0-9]/g, ''))}
                error={errors.heightCm}
                maxLength={3}
              />
            </View>
            <View style={styles.halfInput}>
              <Input
                label="Kilo (kg)"
                placeholder="72"
                keyboardType="numeric"
                value={form.weightKg}
                onChangeText={(v) => setField('weightKg')(v.replace(/[^0-9]/g, ''))}
                error={errors.weightKg}
                maxLength={3}
              />
            </View>
          </View>

          {/* Dominant ayak */}
          <Text style={styles.pickerLabel}>BASKIN AYAK</Text>
          {errors.dominantFoot ? <Text style={styles.errorText}>{errors.dominantFoot}</Text> : null}
          <View style={styles.footRow}>
            {FOOT_OPTIONS.map((opt) => (
              <TouchableOpacity
                key={opt.value}
                style={[styles.footBtn, form.dominantFoot === opt.value && styles.footBtnSelected]}
                onPress={() => setField('dominantFoot')(opt.value)}
              >
                <Text style={[styles.footBtnText, form.dominantFoot === opt.value && styles.footBtnTextSelected]}>
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <Input
            label="Hakkında (opsiyonel)"
            placeholder="Kendin hakkında bir şeyler yaz..."
            value={form.bio}
            onChangeText={setField('bio')}
            error={errors.bio}
            multiline
            numberOfLines={3}
            maxLength={2000}
          />
          <Input
            label="Avatar URL (opsiyonel)"
            placeholder="https://..."
            value={form.avatarUrl}
            onChangeText={setField('avatarUrl')}
            error={errors.avatarUrl}
            keyboardType="url"
            autoCapitalize="none"
          />

          <Button
            title="Kaydet"
            onPress={handleSave}
            loading={saving}
            style={styles.saveBtn}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  flex: { flex: 1 },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
  },
  content: { paddingHorizontal: 20, paddingBottom: 40, paddingTop: 8 },
  pickerLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textMuted,
    marginBottom: 8,
    letterSpacing: 1,
  },
  errorText: {
    fontSize: 12,
    color: Colors.error,
    marginTop: -4,
    marginBottom: 6,
    marginLeft: 2,
    fontWeight: '500',
  },
  chipScroll: { marginBottom: 16 },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 22,
    borderWidth: 1.5,
    borderColor: Colors.border,
    marginRight: 8,
    backgroundColor: Colors.surface,
  },
  chipSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryGlow,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 3,
  },
  chipText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '600' },
  chipTextSelected: { color: Colors.primary, fontWeight: '700' },
  row: { flexDirection: 'row', gap: 12 },
  halfInput: { flex: 1 },
  footRow: { flexDirection: 'row', gap: 10, marginBottom: 16 },
  footBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: Colors.border,
    alignItems: 'center',
    backgroundColor: Colors.surface,
  },
  footBtnSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryGlow,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 3,
  },
  footBtnText: { fontSize: 14, color: Colors.textSecondary, fontWeight: '600' },
  footBtnTextSelected: { color: Colors.primary, fontWeight: '700' },
  saveBtn: { marginTop: 8 },
});

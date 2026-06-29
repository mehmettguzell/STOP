import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { matchApi } from '../../api/match.api';
import { MatchVisibility } from '../../types/match.types';
import { CreateMatchScreenProps } from '../../navigation/types';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import CityPicker from '../../components/ui/CityPicker';
import DatePicker from '../../components/ui/DatePicker';
import TimePicker from '../../components/ui/TimePicker';
import Card from '../../components/ui/Card';
import { Colors } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

const CURRENT_YEAR = new Date().getFullYear();

export default function CreateMatchScreen({ navigation }: CreateMatchScreenProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [city, setCity] = useState('');
  const [district, setDistrict] = useState('');
  const [venue, setVenue] = useState('');
  const [date, setDate] = useState('');   // YYYY-MM-DD
  const [time, setTime] = useState('');   // HH:MM
  const [capacity, setCapacity] = useState('');
  const [visibility, setVisibility] = useState<MatchVisibility>('PUBLIC');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const clearError = (key: string) =>
    setErrors((prev) => ({ ...prev, [key]: '' }));

  const validate = (): boolean => {
    const e: Record<string, string> = {};

    const trimmedTitle = title.trim();
    if (!trimmedTitle)
      e.title = 'Başlık zorunludur';
    else if (trimmedTitle.length < 3)
      e.title = 'Başlık en az 3 karakter olmalıdır';
    else if (trimmedTitle.length > 150)
      e.title = 'Başlık en fazla 150 karakter olabilir';

    if (description.trim().length > 500)
      e.description = 'Açıklama en fazla 500 karakter olabilir';

    if (!city)
      e.city = 'İl seçimi zorunludur';
    if (!district.trim())
      e.district = 'İlçe zorunludur';
    if (!venue.trim())
      e.venue = 'Saha adı zorunludur';

    if (!date)
      e.date = 'Tarih zorunludur';

    if (!time)
      e.time = 'Saat zorunludur';

    const cap = parseInt(capacity, 10);
    if (!capacity.trim() || isNaN(cap))
      e.capacity = 'Geçerli bir kapasite gir';
    else if (cap < 2)
      e.capacity = 'Kapasite en az 2 olmalıdır';
    else if (cap > 100)
      e.capacity = 'Kapasite en fazla 100 olabilir';

    if (!e.date && !e.time && date && time) {
      const startTime = buildStartTime(date, time);
      if (startTime && new Date(startTime) <= new Date())
        e.date = 'Maç tarihi gelecekte olmalıdır';
    }

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const buildStartTime = (d: string, t: string): string | null => {
    if (!d || !t) return null;
    const [year, month, day] = d.split('-');
    const [hours, minutes] = t.split(':');
    if (!year || !month || !day || !hours || !minutes) return null;
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  };

  const handleCreate = async () => {
    if (!validate()) return;

    const startTime = buildStartTime(date, time);
    if (!startTime) {
      Alert.alert('Hata', 'Tarih veya saat geçersiz.');
      return;
    }

    setLoading(true);
    try {
      const match = await matchApi.create({
        title: title.trim(),
        description: description.trim() || undefined,
        location: `${city} / ${district.trim()} / ${venue.trim()}`,
        startTime,
        visibility,
        capacity: parseInt(capacity, 10),
      });
      Alert.alert('Maç Oluşturuldu', 'Maçın başarıyla oluşturuldu!', [
        {
          text: 'Tamam',
          onPress: () => navigation.replace('MatchDetail', { matchId: match.id }),
        },
      ]);
    } catch (err) {
      Alert.alert('Maç Oluşturulamadı', getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={styles.pageTitle}>Yeni Maç</Text>
        <Text style={styles.subtitle}>Maç detaylarını gir</Text>

        <Card style={{ marginTop: 20 }}>
          <Input
            label="Başlık"
            placeholder="ör. Çarşamba Akşam Maçı"
            value={title}
            onChangeText={(v) => { setTitle(v); clearError('title'); }}
            error={errors.title}
            maxLength={150}
          />

          <Input
            label="Açıklama (Opsiyonel)"
            placeholder="Maç hakkında bilgi..."
            value={description}
            onChangeText={(v) => { setDescription(v); clearError('description'); }}
            error={errors.description}
            multiline
            numberOfLines={3}
            style={{ minHeight: 80, textAlignVertical: 'top' }}
            maxLength={500}
          />

          <CityPicker
            value={city}
            onSelect={(v) => { setCity(v); clearError('city'); }}
            error={errors.city}
          />

          <Input
            label="İlçe"
            placeholder="ör. Kadıköy"
            value={district}
            onChangeText={(v) => { setDistrict(v); clearError('district'); }}
            error={errors.district}
            maxLength={100}
          />

          <Input
            label="Saha"
            placeholder="ör. Fenerbahçe Spor Kompleksi"
            value={venue}
            onChangeText={(v) => { setVenue(v); clearError('venue'); }}
            error={errors.venue}
            maxLength={150}
          />

          <View style={styles.dateRow}>
            <View style={{ flex: 1 }}>
              <DatePicker
                label="Tarih"
                value={date}
                onChange={(v) => { setDate(v); clearError('date'); }}
                error={errors.date}
                required
                yearMin={CURRENT_YEAR}
                yearMax={CURRENT_YEAR + 5}
              />
            </View>
            <View style={{ flex: 1 }}>
              <TimePicker
                label="Saat"
                value={time}
                onChange={(v) => { setTime(v); clearError('time'); }}
                error={errors.time}
                required
              />
            </View>
          </View>

          <Input
            label="Kapasite"
            placeholder="ör. 14"
            value={capacity}
            onChangeText={(v) => { setCapacity(v.replace(/[^0-9]/g, '')); clearError('capacity'); }}
            error={errors.capacity}
            keyboardType="number-pad"
            maxLength={3}
          />

          <Text style={styles.inputLabel}>GÖRÜNÜRLÜK</Text>
          <View style={styles.toggleRow}>
            <TouchableOpacity
              style={[styles.toggleBtn, visibility === 'PUBLIC' && styles.toggleActive]}
              onPress={() => setVisibility('PUBLIC')}
              activeOpacity={0.7}
            >
              <Text style={styles.toggleIcon}>🌍</Text>
              <Text style={[styles.toggleText, visibility === 'PUBLIC' && styles.toggleTextActive]}>
                Herkese Açık
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.toggleBtn, visibility === 'PRIVATE' && styles.toggleActive]}
              onPress={() => setVisibility('PRIVATE')}
              activeOpacity={0.7}
            >
              <Text style={styles.toggleIcon}>🔒</Text>
              <Text style={[styles.toggleText, visibility === 'PRIVATE' && styles.toggleTextActive]}>
                Özel
              </Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.toggleHint}>
            {visibility === 'PUBLIC'
              ? 'Herkes doğrudan katılabilir.'
              : 'Katılım için onayın gerekir.'}
          </Text>
        </Card>

        <View style={styles.actions}>
          <Button title="Maç Oluştur" onPress={handleCreate} loading={loading} icon="⚽" />
          <Button title="Vazgeç" onPress={() => navigation.goBack()} variant="ghost" />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  content: { paddingHorizontal: 20, paddingBottom: 40 },
  pageTitle: { fontSize: 28, fontWeight: '800', color: Colors.text, marginTop: 16 },
  subtitle: { fontSize: 14, color: Colors.textMuted, marginTop: 4 },
  inputLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textMuted,
    letterSpacing: 1,
    marginBottom: 6,
    marginTop: 8,
  },
  dateRow: { flexDirection: 'row', gap: 12 },
  toggleRow: { flexDirection: 'row', gap: 10, marginBottom: 4 },
  toggleBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    borderRadius: 14,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1.5,
    borderColor: Colors.border,
    gap: 8,
  },
  toggleActive: { borderColor: Colors.primary, backgroundColor: Colors.primaryGlow },
  toggleIcon: { fontSize: 18 },
  toggleText: { fontSize: 14, fontWeight: '600', color: Colors.textSecondary },
  toggleTextActive: { color: Colors.primary },
  toggleHint: { fontSize: 12, color: Colors.textMuted, marginTop: 4, marginBottom: 8 },
  actions: { marginTop: 24, gap: 12 },
});

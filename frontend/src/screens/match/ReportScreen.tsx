import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  Alert,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { reportApi } from '../../api/report.api';
import { extractErrorMessage } from '../../types/api.types';
import { MatchStackParamList } from '../../navigation/types';
import Button from '../../components/ui/Button';
import { Colors } from '../../theme/colors';

type Props = NativeStackScreenProps<MatchStackParamList, 'Report'>;

const REASON_MIN = 10;
const REASON_MAX = 500;

export default function ReportScreen({ route, navigation }: Props) {
  const { targetUserId, targetName, matchId } = route.params;
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  const isValid = reason.trim().length >= REASON_MIN;
  const charCount = reason.length;

  const handleSubmit = async () => {
    if (!isValid) return;
    setLoading(true);
    try {
      await reportApi.send(targetUserId, matchId, reason.trim());
      Alert.alert(
        'Şikayet İletildi',
        'Şikayetiniz moderatörlerimize iletildi. Teşekkür ederiz.',
        [{ text: 'Tamam', onPress: () => navigation.goBack() }],
      );
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {/* Target info */}
          <View style={styles.targetCard}>
            <Text style={styles.targetLabel}>Şikayet Edilen</Text>
            <Text style={styles.targetName}>{targetName}</Text>
          </View>

          {/* Reason input */}
          <Text style={styles.label}>Şikayet Nedeni</Text>
          <Text style={styles.hint}>
            Lütfen sorunu açık ve net şekilde açıklayın. (min {REASON_MIN} karakter)
          </Text>
          <View style={styles.inputWrapper}>
            <TextInput
              style={styles.textArea}
              value={reason}
              onChangeText={(t) => t.length <= REASON_MAX && setReason(t)}
              placeholder="Şikayet nedeninizi buraya yazın..."
              placeholderTextColor={Colors.textMuted}
              multiline
              numberOfLines={6}
              textAlignVertical="top"
            />
            <Text style={[styles.charCount, charCount > REASON_MAX * 0.9 && styles.charWarn]}>
              {charCount}/{REASON_MAX}
            </Text>
          </View>

          {/* Warning */}
          <View style={styles.warningBox}>
            <Text style={styles.warningText}>
              ⚠️ Asılsız şikayetler hesap puanınızı olumsuz etkileyebilir.
            </Text>
          </View>

          <Button
            title="Şikayeti Gönder"
            onPress={handleSubmit}
            loading={loading}
            style={[styles.submitBtn, !isValid && styles.submitDisabled]}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },
  content: { padding: 20, paddingBottom: 40 },

  targetCard: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 28,
  },
  targetLabel: { fontSize: 11, color: Colors.textMuted, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 4 },
  targetName: { fontSize: 18, fontWeight: '700', color: Colors.text },

  label: { fontSize: 15, fontWeight: '700', color: Colors.text, marginBottom: 6 },
  hint: { fontSize: 13, color: Colors.textMuted, marginBottom: 12, lineHeight: 18 },

  inputWrapper: { position: 'relative' },
  textArea: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    color: Colors.text,
    fontSize: 15,
    padding: 14,
    minHeight: 140,
    lineHeight: 22,
  },
  charCount: { position: 'absolute', bottom: 10, right: 14, fontSize: 11, color: Colors.textMuted },
  charWarn: { color: Colors.warning },

  warningBox: {
    backgroundColor: Colors.warning + '18',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.warning + '44',
    padding: 14,
    marginTop: 20,
    marginBottom: 24,
  },
  warningText: { fontSize: 13, color: Colors.warning, lineHeight: 18 },

  submitBtn: { marginTop: 4 },
  submitDisabled: { opacity: 0.5 },
});

import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Alert,
  TextInput,
  Modal,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { reportApi } from '../../api/report.api';
import { ReportDetailResponse, ReportStatus } from '../../types/report.types';
import { extractErrorMessage } from '../../types/api.types';
import { AdminStackParamList } from '../../navigation/types';
import Button from '../../components/ui/Button';
import { Colors } from '../../theme/colors';

type Props = NativeStackScreenProps<AdminStackParamList, 'AdminReportDetail'>;

const STATUS_COLOR: Record<ReportStatus, string> = {
  OPEN:      Colors.primary,
  REVIEWING: Colors.warning,
  RESOLVED:  Colors.success,
  REJECTED:  Colors.error,
};
const STATUS_LABEL: Record<ReportStatus, string> = {
  OPEN: 'Açık', REVIEWING: 'İnceleniyor', RESOLVED: 'Çözüldü', REJECTED: 'Reddedildi',
};

type ActionType = 'resolve' | 'reject';

export default function AdminReportDetailScreen({ route, navigation }: Props) {
  const { reportId } = route.params;
  const [report, setReport]     = useState<ReportDetailResponse | null>(null);
  const [loading, setLoading]   = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [modal, setModal]       = useState<ActionType | null>(null);
  const [note, setNote]         = useState('');

  useEffect(() => {
    reportApi.getById(reportId)
      .then(setReport)
      .catch((e) => Alert.alert('Hata', extractErrorMessage(e)))
      .finally(() => setLoading(false));
  }, [reportId]);

  const handleStartReview = async () => {
    if (!report) return;
    setActionLoading(true);
    try {
      const updated = await reportApi.startReview(report.id);
      setReport((prev) => prev ? { ...prev, status: updated.status } : prev);
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setActionLoading(false);
    }
  };

  const handleAction = async () => {
    if (!report || !modal) return;
    setActionLoading(true);
    try {
      if (modal === 'resolve') {
        await reportApi.resolve(report.id, note);
        setReport((prev) => prev ? { ...prev, status: 'RESOLVED', resolveNote: note } : prev);
      } else {
        await reportApi.reject(report.id, note);
        setReport((prev) => prev ? { ...prev, status: 'REJECTED', resolveNote: note } : prev);
      }
      setModal(null);
      setNote('');
    } catch (e) {
      Alert.alert('Hata', extractErrorMessage(e));
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (iso: string | null) => {
    if (!iso) return '—';
    const d = new Date(iso);
    return `${d.getDate().toString().padStart(2,'0')}.${(d.getMonth()+1).toString().padStart(2,'0')}.${d.getFullYear()}  ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.safe}>
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      </SafeAreaView>
    );
  }

  if (!report) return null;

  const color = STATUS_COLOR[report.status];
  const isClosed = report.status === 'RESOLVED' || report.status === 'REJECTED';

  return (
    <SafeAreaView style={styles.safe} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>

        {/* Status banner */}
        <View style={[styles.statusBanner, { backgroundColor: color + '18' }]}>
          <View style={[styles.statusDot, { backgroundColor: color }]} />
          <Text style={[styles.statusLabel, { color }]}>{STATUS_LABEL[report.status]}</Text>
          <Text style={styles.dateText}>{formatDate(report.createdAt)}</Text>
        </View>

        {/* Reason */}
        <Text style={styles.sectionTitle}>Şikayet Nedeni</Text>
        <View style={styles.reasonBox}>
          <Text style={styles.reasonText}>{report.reason}</Text>
        </View>

        {/* Details */}
        <Text style={styles.sectionTitle}>Detaylar</Text>
        <View style={styles.detailCard}>
          <DetailRow
            label="Şikayet Eden"
            value={report.reporterId.slice(0,8) + '…'}
            onPress={() => navigation.navigate('AdminUserDetail', { userId: report.reporterId })}
          />
          <DetailRow
            label="Şikayet Edilen"
            value={report.targetUserId.slice(0,8) + '…'}
            onPress={() => navigation.navigate('AdminUserDetail', { userId: report.targetUserId })}
          />
          <DetailRow
            label="Maç"
            value={report.matchId ? report.matchId.slice(0,8) + '…' : '—'}
            onPress={report.matchId ? () => navigation.navigate('AdminMatchDetail', { matchId: report.matchId! }) : undefined}
          />
          <DetailRow label="Oluşturulma" value={formatDate(report.createdAt)} isLast={!isClosed} />
          {isClosed && (
            <>
              <DetailRow label="İşlem Tarihi" value={formatDate(report.resolvedAt)} />
              <DetailRow label="Not" value={report.resolveNote ?? '—'} isLast />
            </>
          )}
        </View>

        {/* Actions */}
        {!isClosed && (
          <View style={styles.actions}>
            {report.status === 'OPEN' && (
              <Button
                title="🔍  İncelemeye Al"
                onPress={handleStartReview}
                loading={actionLoading}
                variant="outline"
              />
            )}
            <Button
              title="✅  Şikayeti Çöz"
              onPress={() => setModal('resolve')}
              loading={actionLoading}
            />
            <Button
              title="❌  Reddet"
              onPress={() => setModal('reject')}
              loading={actionLoading}
              variant="danger"
            />
          </View>
        )}
      </ScrollView>

      {/* Note Modal */}
      <Modal visible={modal !== null} transparent animationType="slide" onRequestClose={() => setModal(null)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>
              {modal === 'resolve' ? '✅ Şikayeti Çöz' : '❌ Şikayeti Reddet'}
            </Text>
            <Text style={styles.modalHint}>Not ekle (isteğe bağlı):</Text>
            <TextInput
              style={styles.noteInput}
              value={note}
              onChangeText={setNote}
              placeholder="Notunu buraya yaz..."
              placeholderTextColor={Colors.textMuted}
              multiline
              numberOfLines={4}
              textAlignVertical="top"
            />
            <View style={styles.modalActions}>
              <Button
                title="Vazgeç"
                onPress={() => { setModal(null); setNote(''); }}
                variant="outline"
                style={{ flex: 1 }}
              />
              <Button
                title="Onayla"
                onPress={handleAction}
                loading={actionLoading}
                style={{ flex: 1 }}
                variant={modal === 'reject' ? 'danger' : 'primary'}
              />
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function DetailRow({
  label,
  value,
  isLast = false,
  onPress,
}: {
  label: string;
  value: string;
  isLast?: boolean;
  onPress?: () => void;
}) {
  const inner = (
    <View style={[rowStyles.row, !isLast && rowStyles.border]}>
      <Text style={rowStyles.label}>{label}</Text>
      <View style={rowStyles.valueRow}>
        <Text style={[rowStyles.value, !!onPress && rowStyles.link]}>{value}</Text>
        {!!onPress && <Text style={rowStyles.chevron}>›</Text>}
      </View>
    </View>
  );
  if (onPress) {
    return (
      <TouchableOpacity onPress={onPress} activeOpacity={0.65}>
        {inner}
      </TouchableOpacity>
    );
  }
  return inner;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  content: { padding: 20, paddingBottom: 40 },

  statusBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 14,
    paddingHorizontal: 16,
    paddingVertical: 12,
    marginBottom: 24,
    gap: 8,
  },
  statusDot: { width: 9, height: 9, borderRadius: 5 },
  statusLabel: { fontSize: 14, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5 },
  dateText: { marginLeft: 'auto', fontSize: 12, color: Colors.textMuted },

  sectionTitle: { fontSize: 15, fontWeight: '700', color: Colors.text, marginBottom: 10, marginTop: 8 },

  reasonBox: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 16,
    marginBottom: 24,
  },
  reasonText: { fontSize: 15, color: Colors.text, lineHeight: 22 },

  detailCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 16,
    marginBottom: 28,
  },

  actions: { gap: 12 },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end' },
  modalBox: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
  },
  modalTitle: { fontSize: 18, fontWeight: '800', color: Colors.text, marginBottom: 16 },
  modalHint: { fontSize: 13, color: Colors.textMuted, marginBottom: 8 },
  noteInput: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    color: Colors.text,
    fontSize: 14,
    padding: 12,
    minHeight: 100,
    marginBottom: 20,
  },
  modalActions: { flexDirection: 'row', gap: 12 },
});

const rowStyles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 13 },
  border: { borderBottomWidth: 1, borderBottomColor: Colors.border },
  label: { fontSize: 13, color: Colors.textSecondary, flex: 1 },
  valueRow: { flex: 2, flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 4 },
  value: { fontSize: 13, color: Colors.text, fontWeight: '600', textAlign: 'right' },
  link: { color: Colors.primary },
  chevron: { fontSize: 18, color: Colors.primary, lineHeight: 20 },
});

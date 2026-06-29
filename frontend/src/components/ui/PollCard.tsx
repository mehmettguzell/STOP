import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Radius, Spacing, Typography } from '../../theme/colors';
import { PollResponse, PollOptionResponse } from '../../types/chat.types';
import { pollApi } from '../../api/poll.api';
import { userApi } from '../../api/user.api';

interface Props {
  poll: PollResponse;
  currentUserId: string;
  onPollUpdate: (updated: PollResponse) => void;
  isMine: boolean;
}

export default function PollCard({ poll, currentUserId, onPollUpdate, isMine }: Props) {
  const [loading, setLoading] = useState<string | null>(null);
  const [voterModal, setVoterModal] = useState<PollOptionResponse | null>(null);
  const [voterNames, setVoterNames] = useState<Record<string, string>>({});
  const [voterNamesLoading, setVoterNamesLoading] = useState(false);

  const myVotedOptionId = poll.options.find(o =>
    o.voterIds.includes(currentUserId)
  )?.id ?? null;

  const isClosed = poll.closesAt != null && new Date(poll.closesAt) < new Date();

  const handleVote = useCallback(async (optionId: string) => {
    if (isClosed) return;
    setLoading(optionId);
    try {
      const updated = await pollApi.vote(poll.id, optionId);
      onPollUpdate(updated);
    } catch {
      // ignore
    } finally {
      setLoading(null);
    }
  }, [poll.id, isClosed, onPollUpdate]);

  const totalVotes = poll.totalVotes || 1; // avoid div-by-zero

  return (
    <View style={styles.card}>
      {/* Header */}
      <View style={styles.header}>
        <Ionicons name="stats-chart" size={14} color={Colors.accent} />
        <Text style={styles.label}>ANKET</Text>
        {isClosed && <Text style={styles.closedBadge}>SONA ERDİ</Text>}
      </View>

      <Text style={styles.question}>{poll.question}</Text>

      {/* Options */}
      {poll.options.map(option => {
        const isMyVote = option.id === myVotedOptionId;
        const pct = poll.totalVotes > 0
          ? Math.round((option.voteCount / poll.totalVotes) * 100)
          : 0;
        const isLoadingThis = loading === option.id;

        return (
          <TouchableOpacity
            key={option.id}
            style={[styles.option, isMyVote && styles.optionSelected]}
            onPress={() => handleVote(option.id)}
            disabled={isClosed || loading != null}
            activeOpacity={0.75}
          >
            {/* fill bar */}
            <View style={[styles.fill, { width: `${pct}%` as any }, isMyVote && styles.fillSelected]} />

            <View style={styles.optionRow}>
              <View style={styles.optionLeft}>
                {isMyVote ? (
                  <Ionicons name="checkmark-circle" size={16} color={Colors.primary} />
                ) : (
                  <View style={styles.emptyCircle} />
                )}
                <Text style={[styles.optionText, isMyVote && styles.optionTextSelected]} numberOfLines={2}>
                  {option.text}
                </Text>
              </View>

              <View style={styles.optionRight}>
                {isLoadingThis ? (
                  <ActivityIndicator size="small" color={Colors.primary} />
                ) : (
                  <>
                    <Text style={styles.pct}>{pct}%</Text>
                    <TouchableOpacity
                      onPress={async () => {
                        setVoterModal(option);
                        const unknown = option.voterIds.filter(id => id !== currentUserId && !voterNames[id]);
                        if (unknown.length === 0) return;
                        setVoterNamesLoading(true);
                        try {
                          const results = await Promise.allSettled(unknown.map(id => userApi.getUser(id)));
                          const resolved: Record<string, string> = {};
                          results.forEach((r, i) => {
                            if (r.status === 'fulfilled') resolved[unknown[i]] = r.value.displayName ?? unknown[i].substring(0, 8);
                          });
                          setVoterNames(prev => ({ ...prev, ...resolved }));
                        } finally {
                          setVoterNamesLoading(false);
                        }
                      }}
                      hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                    >
                      <Text style={styles.voterCount}>{option.voteCount}</Text>
                    </TouchableOpacity>
                  </>
                )}
              </View>
            </View>
          </TouchableOpacity>
        );
      })}

      {/* Footer */}
      <Text style={styles.footer}>
        {poll.totalVotes} oy
        {poll.closesAt && !isClosed && (
          ` · ${formatDeadline(poll.closesAt)}`
        )}
      </Text>

      {/* Voter list modal */}
      <Modal
        visible={voterModal != null}
        transparent
        animationType="fade"
        onRequestClose={() => setVoterModal(null)}
      >
        <TouchableOpacity style={styles.backdrop} onPress={() => setVoterModal(null)} activeOpacity={1}>
          <View style={styles.modal}>
            <Text style={styles.modalTitle}>
              "{voterModal?.text}" — oylar
            </Text>
            {voterModal && voterModal.voterIds.length === 0 ? (
              <Text style={styles.noVotes}>Henüz oy yok</Text>
            ) : voterNamesLoading ? (
              <ActivityIndicator color={Colors.primary} style={{ paddingVertical: Spacing.lg }} />
            ) : (
              <FlatList
                data={voterModal?.voterIds ?? []}
                keyExtractor={id => id}
                renderItem={({ item }) => (
                  <View style={styles.voterRow}>
                    <Ionicons name="person-circle" size={20} color={Colors.textMuted} />
                    <Text style={styles.voterId} numberOfLines={1}>
                      {item === currentUserId ? 'Sen' : (voterNames[item] ?? item.substring(0, 8) + '…')}
                    </Text>
                  </View>
                )}
              />
            )}
            <TouchableOpacity style={styles.closeBtn} onPress={() => setVoterModal(null)}>
              <Text style={styles.closeBtnText}>Kapat</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

function formatDeadline(closesAt: string): string {
  const diff = new Date(closesAt).getTime() - Date.now();
  if (diff <= 0) return 'sona erdi';
  const h = Math.floor(diff / 3_600_000);
  const m = Math.floor((diff % 3_600_000) / 60_000);
  if (h > 0) return `${h}s ${m}dk kaldı`;
  return `${m}dk kaldı`;
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.accentGlow,
    padding: Spacing.md,
    gap: Spacing.xs,
    minWidth: 220,
    maxWidth: 300,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    marginBottom: 2,
  },
  label: {
    ...Typography.caption,
    color: Colors.accent,
    flex: 1,
  },
  closedBadge: {
    ...Typography.caption,
    color: Colors.error,
    fontSize: 9,
  },
  question: {
    ...Typography.bodySm,
    color: Colors.text,
    fontWeight: '700',
    marginBottom: Spacing.xs,
  },
  option: {
    borderRadius: Radius.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: 'hidden',
    marginBottom: 4,
    minHeight: 40,
    justifyContent: 'center',
  },
  optionSelected: {
    borderColor: Colors.primaryBorder,
  },
  fill: {
    position: 'absolute',
    top: 0,
    left: 0,
    bottom: 0,
    backgroundColor: Colors.primaryGlow,
    borderRadius: Radius.sm,
  },
  fillSelected: {
    backgroundColor: Colors.primaryGlowStrong,
  },
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
    zIndex: 1,
  },
  optionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    flex: 1,
  },
  emptyCircle: {
    width: 16,
    height: 16,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: Colors.borderStrong,
  },
  optionText: {
    ...Typography.bodySm,
    color: Colors.textSecondary,
    flex: 1,
  },
  optionTextSelected: {
    color: Colors.text,
    fontWeight: '700',
  },
  optionRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  pct: {
    ...Typography.caption,
    color: Colors.textMuted,
    minWidth: 30,
    textAlign: 'right',
  },
  voterCount: {
    ...Typography.caption,
    color: Colors.accent,
    minWidth: 18,
    textAlign: 'center',
    borderWidth: 1,
    borderColor: Colors.accentGlow,
    borderRadius: Radius.xs,
    paddingHorizontal: 4,
    paddingVertical: 1,
  },
  footer: {
    ...Typography.caption,
    color: Colors.textDim,
    marginTop: 2,
  },
  // Modal
  backdrop: {
    flex: 1,
    backgroundColor: Colors.overlay,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modal: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: Spacing.xl,
    width: '80%',
    maxHeight: '60%',
  },
  modalTitle: {
    ...Typography.bodySm,
    color: Colors.text,
    fontWeight: '700',
    marginBottom: Spacing.md,
  },
  noVotes: {
    ...Typography.body,
    color: Colors.textMuted,
    textAlign: 'center',
    paddingVertical: Spacing.lg,
  },
  voterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  voterId: {
    ...Typography.body,
    color: Colors.textSecondary,
  },
  closeBtn: {
    marginTop: Spacing.md,
    alignItems: 'center',
    paddingVertical: 10,
    backgroundColor: Colors.surfaceHigh,
    borderRadius: Radius.sm,
  },
  closeBtnText: {
    ...Typography.bodySm,
    color: Colors.text,
    fontWeight: '700',
  },
});

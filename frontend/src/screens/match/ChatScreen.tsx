import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { Client } from "@stomp/stompjs";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { chatApi } from "../../api/chat.api";
import { pollApi } from "../../api/poll.api";
import { userApi } from "../../api/user.api";
import { ChatMessageResponse, PollResponse } from "../../types/chat.types";
import { BASE_URL, STORAGE_KEYS } from "../../api/client";
import { useAuthStore } from "../../store/auth.store";
import { useChatStore } from "../../store/chat.store";
import { Colors, Radius, Spacing, Typography } from "../../theme/colors";
import PollCard from "../../components/ui/PollCard";

const WS_BASE = BASE_URL.replace("https://", "wss://").replace("http://", "ws://").replace(":8080/api/v1", ":8084/ws").replace("/api/v1", "/ws");
console.log("WS_BASE:", WS_BASE);

export interface ChatScreenParams {
  chatId: string;
  title: string;
  chatType?: "MATCH" | "DIRECT";
}

export default function ChatScreen({
  route,
}: {
  route: { params: ChatScreenParams };
}) {
  const { chatId, chatType } = route.params;
  const matchId = chatId;
  const currentUserId = useAuthStore((s) => s.user?.id) ?? "";
  const isMatchChat = !chatType || chatType === "MATCH";

  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [locked, setLocked] = useState(false);
  const [names, setNames] = useState<Record<string, string>>({});
  const [selectedMsgId, setSelectedMsgId] = useState<string | null>(null);

  // Poll creation modal state
  const [pollModal, setPollModal] = useState(false);
  const [pollQuestion, setPollQuestion] = useState("");
  const [pollOptions, setPollOptions] = useState(["", ""]);
  const [pollCreating, setPollCreating] = useState(false);

  const refreshUnread = useChatStore((s) => s.refreshUnread);
  const clientRef = useRef<Client | null>(null);
  const listRef = useRef<FlatList>(null);

  const resolveName = useCallback(
    async (senderId: string) => {
      if (names[senderId] || senderId === currentUserId) return;
      try {
        const user = await userApi.getUser(senderId);
        setNames((prev) => ({ ...prev, [senderId]: user.displayName }));
      } catch {
        setNames((prev) => ({ ...prev, [senderId]: senderId.slice(0, 8) }));
      }
    },
    [names, currentUserId],
  );

  useEffect(() => {
    if (isMatchChat) {
      chatApi.joinMatchChat(matchId).catch(() => {});
      chatApi
        .isChatLocked(matchId)
        .then(setLocked)
        .catch(() => {});
    }
    chatApi
      .markAsRead(matchId)
      .then(() => refreshUnread())
      .catch(() => {});
  }, [matchId]);

  useEffect(() => {
    chatApi
      .getHistory(matchId)
      .then((history) => {
        setMessages(history);
        const unknownIds = [...new Set(history.map((m) => m.senderId))].filter(
          (id) => id !== currentUserId,
        );
        unknownIds.forEach(resolveName);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [matchId]);

  useEffect(() => {
    let stompClient: Client;

    const connect = async () => {
      const token = await AsyncStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      if (!token) return;

      stompClient = new Client({
        webSocketFactory: () => new WebSocket(WS_BASE) as any,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        forceBinaryWSFrames: true,
        appendMissingNULLOnIncoming: true,
        onConnect: () => {
          setConnected(true);
          stompClient.subscribe(`/topic/chat/${matchId}`, (frame) => {
            try {
              const msg: ChatMessageResponse = JSON.parse(frame.body);

              // POLL_UPDATE → sadece mevcut anketi güncelle, listeye ekleme
              if (msg.type === "POLL_UPDATE") {
                setMessages((prev) =>
                  prev.map((m) =>
                    m.pollId === msg.pollId && m.type === "POLL"
                      ? { ...m, pollData: msg.pollData }
                      : m,
                  ),
                );
                return;
              }

              setMessages((prev) => {
                const exists = prev.findIndex((m) => m.id === msg.id);
                if (exists >= 0) {
                  const updated = [...prev];
                  updated[exists] = msg;
                  return updated;
                }
                return [...prev, msg];
              });
              if (msg.senderId !== currentUserId) resolveName(msg.senderId);
            } catch {}
          });
        },
        onDisconnect: () => setConnected(false),
        onStompError: () => setConnected(false),
        onWebSocketError: () => setConnected(false),
        onWebSocketClose: () => setConnected(false),
      });

      try {
        stompClient.activate();
        clientRef.current = stompClient;
      } catch {}
    };

    connect();
    return () => {
      stompClient?.deactivate();
    };
  }, [matchId]);

  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => listRef.current?.scrollToEnd({ animated: true }), 100);
    }
  }, [messages.length]);

  const sendMessage = () => {
    const text = input.trim();
    if (!text || locked) return;
    if (!clientRef.current?.connected) {
      Alert.alert("Bağlantı Yok", "Sunucuya bağlanılamıyor, lütfen bekleyin.");
      return;
    }
    clientRef.current.publish({
      destination: `/app/chat/${matchId}`,
      body: JSON.stringify({ content: text, chatType: chatType ?? "MATCH" }),
    });
    setInput("");
  };

  const handleDeleteMessage = (msgId: string) => {
    setSelectedMsgId(null);
    Alert.alert("Mesajı Sil", "Bu mesaj silinsin mi?", [
      { text: "Vazgeç", style: "cancel" },
      {
        text: "Sil",
        style: "destructive",
        onPress: async () => {
          try {
            await chatApi.deleteMessage(msgId);
            setMessages((prev) =>
              prev.map((m) =>
                m.id === msgId ? { ...m, deleted: true, content: null } : m,
              ),
            );
          } catch {
            Alert.alert("Hata", "Mesaj silinemedi.");
          }
        },
      },
    ]);
  };

  // ── Poll creation ────────────────────────────────────────────────────────

  const openPollModal = () => {
    setPollQuestion("");
    setPollOptions(["", ""]);
    setPollModal(true);
  };

  const addOption = () => {
    if (pollOptions.length >= 10) return;
    setPollOptions((prev) => [...prev, ""]);
  };

  const removeOption = (idx: number) => {
    if (pollOptions.length <= 2) return;
    setPollOptions((prev) => prev.filter((_, i) => i !== idx));
  };

  const updateOption = (idx: number, val: string) => {
    setPollOptions((prev) => prev.map((o, i) => (i === idx ? val : o)));
  };

  const submitPoll = async () => {
    const q = pollQuestion.trim();
    const opts = pollOptions.map((o) => o.trim()).filter(Boolean);
    if (!q) {
      Alert.alert("Hata", "Soru boş olamaz.");
      return;
    }
    if (opts.length < 2) {
      Alert.alert("Hata", "En az 2 seçenek girin.");
      return;
    }

    setPollCreating(true);
    try {
      await pollApi.createPoll(matchId, q, opts);
      setPollModal(false);
    } catch {
      Alert.alert("Hata", "Anket oluşturulamadı.");
    } finally {
      setPollCreating(false);
    }
  };

  // ── Poll vote callback ───────────────────────────────────────────────────

  const handlePollUpdate = useCallback(
    (msgId: string, updated: PollResponse) => {
      setMessages((prev) =>
        prev.map((m) => (m.id === msgId ? { ...m, pollData: updated } : m)),
      );
    },
    [],
  );

  // ── Render ───────────────────────────────────────────────────────────────

  const formatTime = (iso: string) => {
    const d = new Date(iso);
    return `${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;
  };

  const renderMessage = ({
    item,
    index,
  }: {
    item: ChatMessageResponse;
    index: number;
  }) => {
    const isMe = item.senderId === currentUserId;
    const resolvedName = names[item.senderId];
    const senderName = isMe ? "Sen" : (resolvedName ?? "Yükleniyor...");
    const initial = resolvedName ? resolvedName.charAt(0).toUpperCase() : "?";
    const prev = messages[index - 1];
    const isFirstInGroup = !prev || prev.senderId !== item.senderId;
    const isSelected = selectedMsgId === item.id;
    const isPoll = item.type === "POLL" && item.pollData != null;

    return (
      <TouchableWithoutFeedback
        onLongPress={() => {
          if (isMe && !item.deleted && !isPoll) setSelectedMsgId(item.id);
        }}
        onPress={() => {
          if (selectedMsgId) setSelectedMsgId(null);
        }}
      >
        <View
          style={[styles.msgRow, isMe ? styles.msgRowMe : styles.msgRowOther]}
        >
          {!isMe && (
            <View style={styles.avatarSlot}>
              {isFirstInGroup ? (
                <View style={styles.avatar}>
                  <Text style={styles.avatarText}>{initial}</Text>
                </View>
              ) : null}
            </View>
          )}
          <View style={[styles.msgContent, isPoll && styles.msgContentPoll]}>
            {!isMe && isFirstInGroup && (
              <Text style={styles.senderName}>{senderName}</Text>
            )}

            {isPoll ? (
              <View>
                <PollCard
                  poll={item.pollData!}
                  currentUserId={currentUserId}
                  onPollUpdate={(updated) => handlePollUpdate(item.id, updated)}
                  isMine={isMe}
                />
                <Text
                  style={[styles.timeText, { marginTop: 4, marginLeft: 4 }]}
                >
                  {formatTime(item.sentAt)}
                </Text>
              </View>
            ) : (
              <View
                style={[
                  styles.bubble,
                  isMe ? styles.bubbleMe : styles.bubbleOther,
                ]}
              >
                {item.deleted ? (
                  <Text style={styles.deletedText}>🚫 Bu mesaj silindi</Text>
                ) : (
                  <Text
                    style={[styles.bubbleText, isMe && styles.bubbleTextMe]}
                  >
                    {item.content}
                  </Text>
                )}
                <Text style={[styles.timeText, isMe && styles.timeTextMe]}>
                  {formatTime(item.sentAt)}
                </Text>
              </View>
            )}

            {isSelected && (
              <TouchableOpacity
                style={styles.deleteBtn}
                onPress={() => handleDeleteMessage(item.id)}
              >
                <Text style={styles.deleteBtnText}>Sil</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </TouchableWithoutFeedback>
    );
  };

  return (
    <SafeAreaView style={styles.safe} edges={["bottom"]}>
      {!connected && !loading && (
        <View style={styles.offlineBanner}>
          <ActivityIndicator
            size="small"
            color={Colors.warning}
            style={{ marginRight: 8 }}
          />
          <Text style={styles.offlineText}>Bağlanıyor...</Text>
        </View>
      )}

      {locked && (
        <View style={styles.lockedBanner}>
          <Text style={styles.lockedText}>
            🔒 Maç tamamlandı, chat kilitlendi
          </Text>
        </View>
      )}

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          ref={listRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderMessage}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyIcon}>💬</Text>
              <Text style={styles.emptyText}>
                Henüz mesaj yok. İlk mesajı sen gönder!
              </Text>
            </View>
          }
        />
      )}

      {!locked && (
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          keyboardVerticalOffset={90}
        >
          <View style={styles.inputBar}>
            <TouchableOpacity
              style={styles.pollBtn}
              onPress={openPollModal}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Ionicons name="stats-chart" size={20} color={Colors.accent} />
            </TouchableOpacity>
            <TextInput
              style={styles.input}
              value={input}
              onChangeText={setInput}
              placeholder="Mesaj yaz..."
              placeholderTextColor={Colors.textMuted}
              multiline
              maxLength={1000}
              onSubmitEditing={sendMessage}
            />
            <TouchableOpacity
              style={[styles.sendBtn, !input.trim() && styles.sendBtnDisabled]}
              onPress={sendMessage}
              disabled={!input.trim()}
              activeOpacity={0.7}
            >
              {!connected ? (
                <ActivityIndicator size="small" color={Colors.textMuted} />
              ) : (
                <Text
                  style={[
                    styles.sendIcon,
                    !input.trim() && styles.sendIconDisabled,
                  ]}
                >
                  ➤
                </Text>
              )}
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      )}

      {/* ── Poll Creation Modal ── */}
      <Modal
        visible={pollModal}
        transparent
        animationType="slide"
        onRequestClose={() => setPollModal(false)}
      >
        <KeyboardAvoidingView
          style={{ flex: 1 }}
          behavior={Platform.OS === "ios" ? "padding" : "height"}
        >
          <TouchableOpacity
            style={styles.modalBackdrop}
            onPress={() => setPollModal(false)}
            activeOpacity={1}
          >
            <TouchableOpacity activeOpacity={1} style={styles.modalSheet}>
              <View style={styles.modalHandle} />
              <Text style={styles.modalTitle}>Anket Oluştur</Text>

              <Text style={styles.fieldLabel}>Soru</Text>
              <TextInput
                style={styles.fieldInput}
                value={pollQuestion}
                onChangeText={setPollQuestion}
                placeholder="Anket sorusunu yaz..."
                placeholderTextColor={Colors.textMuted}
                maxLength={500}
                multiline
              />

              <Text style={[styles.fieldLabel, { marginTop: Spacing.md }]}>
                Seçenekler
              </Text>
              <ScrollView
                style={{ maxHeight: 220 }}
                keyboardShouldPersistTaps="handled"
              >
                {pollOptions.map((opt, idx) => (
                  <View key={idx} style={styles.optionRow}>
                    <TextInput
                      style={[styles.fieldInput, { flex: 1 }]}
                      value={opt}
                      onChangeText={(v) => updateOption(idx, v)}
                      placeholder={`Seçenek ${idx + 1}`}
                      placeholderTextColor={Colors.textMuted}
                      maxLength={200}
                    />
                    {pollOptions.length > 2 && (
                      <TouchableOpacity
                        onPress={() => removeOption(idx)}
                        style={styles.removeBtn}
                      >
                        <Ionicons
                          name="close-circle"
                          size={20}
                          color={Colors.error}
                        />
                      </TouchableOpacity>
                    )}
                  </View>
                ))}
              </ScrollView>

              {pollOptions.length < 10 && (
                <TouchableOpacity
                  style={styles.addOptionBtn}
                  onPress={addOption}
                >
                  <Ionicons
                    name="add-circle-outline"
                    size={18}
                    color={Colors.primary}
                  />
                  <Text style={styles.addOptionText}>Seçenek Ekle</Text>
                </TouchableOpacity>
              )}

              <View style={styles.modalActions}>
                <TouchableOpacity
                  style={styles.cancelBtn}
                  onPress={() => setPollModal(false)}
                >
                  <Text style={styles.cancelBtnText}>Vazgeç</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.createBtn, pollCreating && { opacity: 0.6 }]}
                  onPress={submitPoll}
                  disabled={pollCreating}
                >
                  {pollCreating ? (
                    <ActivityIndicator
                      size="small"
                      color={Colors.textInverse}
                    />
                  ) : (
                    <Text style={styles.createBtnText}>Oluştur</Text>
                  )}
                </TouchableOpacity>
              </View>
            </TouchableOpacity>
          </TouchableOpacity>
        </KeyboardAvoidingView>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  offlineBanner: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 8,
    backgroundColor: Colors.surfaceElevated,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  offlineText: { fontSize: 13, color: Colors.warning, fontWeight: "600" },

  lockedBanner: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    backgroundColor: Colors.surfaceElevated,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
    alignItems: "center",
  },
  lockedText: { fontSize: 13, color: Colors.textMuted, fontWeight: "600" },

  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
  emptyIcon: { fontSize: 40, marginBottom: 12 },
  emptyText: { color: Colors.textMuted, textAlign: "center", fontSize: 14 },

  listContent: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 8,
    flexGrow: 1,
  },

  msgRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    marginBottom: 6,
    gap: 6,
  },
  msgRowMe: { justifyContent: "flex-end" },
  msgRowOther: { justifyContent: "flex-start" },

  avatarSlot: { width: 30, alignItems: "center" },
  avatar: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontSize: 12, fontWeight: "800", color: Colors.primary },

  msgContent: { maxWidth: "78%" },
  msgContentPoll: { maxWidth: "90%" },

  senderName: {
    fontSize: 12,
    color: Colors.primary,
    marginBottom: 3,
    marginLeft: 4,
    fontWeight: "700",
    letterSpacing: -0.1,
  },

  bubble: {
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 9,
    flexDirection: "row",
    alignItems: "flex-end",
    flexWrap: "wrap",
    gap: 6,
  },
  bubbleOther: {
    backgroundColor: Colors.surfaceElevated,
    borderBottomLeftRadius: 4,
  },
  bubbleMe: { backgroundColor: Colors.primary, borderBottomRightRadius: 4 },

  bubbleText: { fontSize: 15, color: Colors.text, flexShrink: 1 },
  bubbleTextMe: { color: Colors.black },
  deletedText: { fontSize: 13, color: Colors.textMuted, fontStyle: "italic" },

  timeText: { fontSize: 10, color: Colors.textMuted, alignSelf: "flex-end" },
  timeTextMe: { color: "rgba(0,0,0,0.45)" },

  deleteBtn: {
    alignSelf: "flex-end",
    marginTop: 4,
    paddingHorizontal: 12,
    paddingVertical: 5,
    backgroundColor: Colors.error + "22",
    borderRadius: 10,
    borderWidth: 1,
    borderColor: Colors.error + "55",
  },
  deleteBtnText: { fontSize: 12, color: Colors.error, fontWeight: "700" },

  inputBar: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    backgroundColor: Colors.surface,
    gap: 8,
  },
  pollBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.accentGlow,
    alignItems: "center",
    justifyContent: "center",
    alignSelf: "flex-end",
    marginBottom: 4,
  },
  input: {
    flex: 1,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 22,
    paddingHorizontal: 16,
    paddingVertical: Platform.OS === "ios" ? 10 : 8,
    color: Colors.text,
    fontSize: 15,
    maxHeight: 120,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  sendBtnDisabled: { backgroundColor: Colors.surfaceHigh },
  sendIcon: { fontSize: 18, color: Colors.textInverse, marginLeft: 2 },
  sendIconDisabled: { color: Colors.textDim },

  // Poll modal
  modalBackdrop: {
    flex: 1,
    backgroundColor: Colors.overlay,
    justifyContent: "flex-end",
  },
  modalSheet: {
    backgroundColor: Colors.surfaceElevated,
    borderTopLeftRadius: Radius.xxl,
    borderTopRightRadius: Radius.xxl,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: Spacing.xl,
    paddingBottom: 36,
  },
  modalHandle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.borderStrong,
    alignSelf: "center",
    marginBottom: Spacing.lg,
  },
  modalTitle: {
    ...Typography.h3,
    color: Colors.text,
    marginBottom: Spacing.lg,
  },
  fieldLabel: {
    ...Typography.caption,
    color: Colors.textMuted,
    marginBottom: Spacing.xs,
  },
  fieldInput: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 14,
    paddingVertical: 10,
    color: Colors.text,
    fontSize: 14,
  },
  optionRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  removeBtn: {
    padding: 4,
  },
  addOptionBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: Spacing.xs,
    paddingVertical: Spacing.xs,
  },
  addOptionText: {
    ...Typography.bodySm,
    color: Colors.primary,
    fontWeight: "700",
  },
  modalActions: {
    flexDirection: "row",
    gap: Spacing.sm,
    marginTop: Spacing.xl,
  },
  cancelBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: Radius.sm,
    backgroundColor: Colors.surfaceHigh,
    alignItems: "center",
  },
  cancelBtnText: {
    ...Typography.bodySm,
    color: Colors.textSecondary,
    fontWeight: "700",
  },
  createBtn: {
    flex: 2,
    paddingVertical: 12,
    borderRadius: Radius.sm,
    backgroundColor: Colors.accent,
    alignItems: "center",
  },
  createBtnText: {
    ...Typography.bodySm,
    color: Colors.white,
    fontWeight: "700",
  },
});

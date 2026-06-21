import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Client } from '@stomp/stompjs';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { chatApi } from '../../api/chat.api';
import { userApi } from '../../api/user.api';
import { matchApi } from '../../api/match.api';
import { ChatMessageResponse, ConversationSummary } from '../../types/chat.types';
import { MessagesStackParamList } from '../../navigation/types';
import { BASE_URL, STORAGE_KEYS } from '../../api/client';
import { Colors } from '../../theme/colors';

const WS_BASE = BASE_URL.replace("https://", "wss://").replace("http://", "ws://").replace(":8080/api/v1", ":8084/ws").replace("/api/v1", "/ws");

type Props = NativeStackScreenProps<MessagesStackParamList, 'ChatList'>;

function timeAgo(iso: string | null): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Az önce';
  if (mins < 60) return `${mins}dk`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}sa`;
  return `${Math.floor(hrs / 24)}g`;
}

export default function ChatListScreen({ navigation }: Props) {
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [titles, setTitles] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectMode, setSelectMode] = useState(false);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const stompClientRef = useRef<Client | null>(null);
  const subscribedChatsRef = useRef<Set<string>>(new Set());

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const data = await chatApi.getConversations();
      setConversations(data);
      await resolveAllTitles(data);
    } catch {
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  const resolveAllTitles = async (convs: ConversationSummary[]) => {
    const resolved: Record<string, string> = {};
    await Promise.allSettled(
      convs.map(async (c) => {
        if (c.type === 'DIRECT') {
          try {
            const user = await userApi.getUser(c.referenceId);
            resolved[c.chatId] = user.displayName;
          } catch {
            resolved[c.chatId] = `Kullanıcı ${c.referenceId.slice(0, 6)}`;
          }
        } else {
          try {
            const match = await matchApi.getById(c.referenceId);
            resolved[c.chatId] = `⚽ ${match.title}`;
          } catch {
            resolved[c.chatId] = 'Maç Sohbeti';
          }
        }
      })
    );
    setTitles((prev) => ({ ...prev, ...resolved }));
  };

  // WebSocket bağlantısı — component mount/unmount
  useEffect(() => {
    let client: Client;

    const connect = async () => {
      const token = await AsyncStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      if (!token) return;

      // userId'yi token'dan oku (JWT sub alanı)
      let userId: string | null = null;
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        userId = payload.sub as string;
      } catch {}

      client = new Client({
        webSocketFactory: () => new WebSocket(WS_BASE) as any,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        forceBinaryWSFrames: true,
        appendMissingNULLOnIncoming: true,
        onConnect: () => {
          stompClientRef.current = client;

          // Gizlenmiş sohbet yeni mesajla geri gelince listeyi yenile
          if (userId) {
            client.subscribe(`/topic/user/${userId}/inbox-update`, () => {
              load(true);
            });
          }

          // Mevcut konuşmaların topic'lerine abone ol
          setConversations((current) => {
            subscribeToChats(client, current);
            return current;
          });
        },
        onDisconnect: () => { subscribedChatsRef.current.clear(); },
        onWebSocketClose: () => { subscribedChatsRef.current.clear(); },
      });

      client.activate();
    };

    connect();
    return () => { client?.deactivate(); stompClientRef.current = null; };
  }, [load]);

  // Yeni konuşmalar yüklenince henüz abone olunmayanları ekle
  useEffect(() => {
    const client = stompClientRef.current;
    if (!client?.connected) return;
    subscribeToChats(client, conversations);
  }, [conversations.length]);

  function subscribeToChats(client: Client, convs: ConversationSummary[]) {
    convs.forEach((conv) => {
      if (subscribedChatsRef.current.has(conv.chatId)) return;
      subscribedChatsRef.current.add(conv.chatId);

      client.subscribe(`/topic/chat/${conv.chatId}`, (frame) => {
        try {
          const msg: ChatMessageResponse = JSON.parse(frame.body);
          if (msg.deleted) return; // silinmiş mesaj son mesaj olarak gösterilmez
          setConversations((prev) =>
            prev.map((c) =>
              c.chatId === conv.chatId
                ? { ...c, lastMessage: msg.content ?? null, lastMessageAt: msg.sentAt }
                : c
            )
          );
        } catch {}
      });
    });
  }

  useFocusEffect(
    useCallback(() => {
      load();
      // Tab değişince seçim modunu sıfırla
      return () => {
        setSelectMode(false);
        setSelected(new Set());
      };
    }, [load])
  );

  // Header sağ buton
  useLayoutEffect(() => {
    navigation.setOptions({
      headerRight: () =>
        selectMode ? (
          <TouchableOpacity onPress={cancelSelect} style={styles.headerBtn}>
            <Text style={styles.headerBtnText}>Vazgeç</Text>
          </TouchableOpacity>
        ) : (
          <TouchableOpacity onPress={() => setSelectMode(true)} style={styles.headerBtn}>
            <Text style={styles.headerBtnText}>Seç</Text>
          </TouchableOpacity>
        ),
    });
  }, [navigation, selectMode]);

  const cancelSelect = () => {
    setSelectMode(false);
    setSelected(new Set());
  };

  const toggleSelect = (chatId: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(chatId) ? next.delete(chatId) : next.add(chatId);
      return next;
    });
  };

  const handleDeleteSelected = () => {
    if (selected.size === 0) return;
    Alert.alert(
      'Sohbetleri Sil',
      `${selected.size} sohbeti silmek istiyor musun?`,
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Sil',
          style: 'destructive',
          onPress: async () => {
            await Promise.allSettled(
              [...selected].map((id) => chatApi.hideConversation(id))
            );
            setConversations((prev) => prev.filter((c) => !selected.has(c.chatId)));
            cancelSelect();
          },
        },
      ]
    );
  };

  const handleNewMessage = () => {
    navigation.navigate('NewMessage');
  };

  const getTitle = (conv: ConversationSummary) =>
    titles[conv.chatId] ?? (conv.type === 'MATCH' ? 'Maç Sohbeti' : '...');

  const getInitial = (conv: ConversationSummary) => {
    const t = getTitle(conv);
    return t.replace(/[^a-zA-ZğüşıöçĞÜŞİÖÇ]/g, '').charAt(0).toUpperCase() || '?';
  };

  const handlePress = (conv: ConversationSummary) => {
    if (selectMode) {
      toggleSelect(conv.chatId);
      return;
    }
    navigation.navigate('Chat', {
      chatId: conv.chatId,
      title: getTitle(conv),
      chatType: conv.type,
    });
  };

  const handleLongPress = (conv: ConversationSummary) => {
    if (!selectMode) {
      setSelectMode(true);
      setSelected(new Set([conv.chatId]));
    }
  };

  const renderItem = ({ item }: { item: ConversationSummary }) => {
    const title = getTitle(item);
    const hasUnread = item.unreadCount > 0;
    const isMatch = item.type === 'MATCH';
    const isSelected = selected.has(item.chatId);

    return (
      <TouchableOpacity
        style={[styles.row, isSelected && styles.rowSelected]}
        onPress={() => handlePress(item)}
        onLongPress={() => handleLongPress(item)}
        activeOpacity={0.72}
      >
        {/* Seçim kutusu */}
        {selectMode && (
          <View style={[styles.checkbox, isSelected && styles.checkboxSelected]}>
            {isSelected && <Text style={styles.checkmark}>✓</Text>}
          </View>
        )}

        {/* Avatar */}
        <View style={[styles.avatar, isMatch && styles.avatarMatch]}>
          <Text style={[styles.avatarText, isMatch && styles.avatarTextMatch]}>
            {getInitial(item)}
          </Text>
        </View>

        {/* Body */}
        <View style={styles.body}>
          <View style={styles.topRow}>
            <Text style={[styles.title, hasUnread && styles.titleBold]} numberOfLines={1}>
              {title}
            </Text>
            <Text style={styles.time}>{timeAgo(item.lastMessageAt)}</Text>
          </View>
          <View style={styles.bottomRow}>
            <Text style={[styles.lastMsg, hasUnread && styles.lastMsgBold]} numberOfLines={1}>
              {(item.lastMessage && item.lastMessage.trim()) ? item.lastMessage : (item.lastMessageAt ? '📎 Medya' : 'Henüz mesaj yok')}
            </Text>
            {hasUnread && !selectMode && (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>
                  {item.unreadCount > 99 ? '99+' : item.unreadCount}
                </Text>
              </View>
            )}
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Mesajlar</Text>
        <TouchableOpacity onPress={handleNewMessage} style={styles.composeBtn} activeOpacity={0.7}>
          <Text style={styles.composeIcon}>✏️</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={conversations}
          keyExtractor={(item) => `${item.type}-${item.chatId}`}
          renderItem={renderItem}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => { setRefreshing(true); load(true); }}
              tintColor={Colors.primary}
            />
          }
          ItemSeparatorComponent={() => <View style={styles.separator} />}
          contentContainerStyle={
            conversations.length === 0 ? styles.emptyContainer : styles.listContent
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyIcon}>💬</Text>
              <Text style={styles.emptyTitle}>Henüz konuşma yok</Text>
              <Text style={styles.emptyText}>
                Bir oyuncunun profiline gidip{'\n'}
                <Text style={{ color: Colors.primary, fontWeight: '700' }}>"Mesaj Gönder"</Text>
                {' '}butonuna bas.{'\n\n'}
                Katıldığın maçların sohbetleri{'\n'}de burada görünür.
              </Text>
            </View>
          }
        />
      )}

      {/* Seçim modu alt toolbar */}
      {selectMode && (
        <View style={styles.selectionBar}>
          <Text style={styles.selectionCount}>
            {selected.size} seçildi
          </Text>
          <TouchableOpacity
            style={[styles.deleteBtn, selected.size === 0 && styles.deleteBtnDisabled]}
            onPress={handleDeleteSelected}
            disabled={selected.size === 0}
          >
            <Text style={styles.deleteBtnText}>Sil</Text>
          </TouchableOpacity>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  headerTitle: { fontSize: 26, fontWeight: '800', color: Colors.text },
  composeBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  composeIcon: { fontSize: 18 },
  headerBtn: { marginRight: 4, paddingHorizontal: 8, paddingVertical: 4 },
  headerBtnText: { fontSize: 15, color: Colors.primary, fontWeight: '600' },

  listContent: { paddingVertical: 4 },
  emptyContainer: { flex: 1 },

  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 14,
    gap: 12,
  },
  rowSelected: { backgroundColor: Colors.primaryGlow },

  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: Colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  checkboxSelected: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  checkmark: { fontSize: 12, color: Colors.black, fontWeight: '800' },

  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  avatarMatch: { backgroundColor: Colors.accentGlow, borderColor: Colors.accent },
  avatarText: { fontSize: 20, fontWeight: '800', color: Colors.primary },
  avatarTextMatch: { color: Colors.accent },

  body: { flex: 1, gap: 3 },
  topRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  title: { fontSize: 15, color: Colors.text, fontWeight: '500', flex: 1 },
  titleBold: { fontWeight: '800' },
  time: { fontSize: 12, color: Colors.textMuted, flexShrink: 0 },
  bottomRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  lastMsg: { fontSize: 13, color: Colors.textMuted, flex: 1 },
  lastMsgBold: { color: Colors.textSecondary, fontWeight: '600' },

  badge: {
    backgroundColor: Colors.primary,
    borderRadius: 12,
    minWidth: 22,
    height: 22,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 5,
    flexShrink: 0,
  },
  badgeText: { fontSize: 11, color: Colors.black, fontWeight: '800' },

  separator: { height: 1, backgroundColor: Colors.border, marginHorizontal: 16 },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 40 },
  emptyIcon: { fontSize: 52, marginBottom: 16 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: Colors.text, marginBottom: 12 },
  emptyText: { fontSize: 14, color: Colors.textMuted, textAlign: 'center', lineHeight: 22 },

  selectionBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  selectionCount: { fontSize: 15, color: Colors.textSecondary, fontWeight: '600' },
  deleteBtn: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    backgroundColor: Colors.error,
    borderRadius: 12,
  },
  deleteBtnDisabled: { backgroundColor: Colors.border },
  deleteBtnText: { fontSize: 14, color: Colors.white, fontWeight: '700' },
});

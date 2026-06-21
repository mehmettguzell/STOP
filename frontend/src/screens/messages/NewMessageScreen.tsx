import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { friendshipApi } from '../../api/friendship.api';
import { chatApi } from '../../api/chat.api';
import { userApi } from '../../api/user.api';
import { useAuthStore } from '../../store/auth.store';
import { MessagesStackParamList } from '../../navigation/types';
import { Colors, Radius } from '../../theme/colors';

type Props = NativeStackScreenProps<MessagesStackParamList, 'NewMessage'>;

type FriendEntry = { otherUserId: string; displayName: string };

export default function NewMessageScreen({ navigation }: Props) {
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [friends, setFriends] = useState<FriendEntry[]>([]);
  const [filtered, setFiltered] = useState<FriendEntry[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [openingId, setOpeningId] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const list = await friendshipApi.getFriends();
        const entries = await Promise.all(
          list.map(async (f) => {
            const otherUserId = f.requesterId === currentUserId ? f.receiverId : f.requesterId;
            let displayName = otherUserId.slice(0, 8);
            try {
              const user = await userApi.getUser(otherUserId);
              displayName = user.displayName;
            } catch {}
            return { otherUserId, displayName };
          })
        );
        setFriends(entries);
        setFiltered(entries);
      } catch {}
      finally { setLoading(false); }
    })();
  }, [currentUserId]);

  const handleQuery = useCallback((text: string) => {
    setQuery(text);
    const q = text.toLowerCase();
    setFiltered(friends.filter((f) => f.displayName.toLowerCase().includes(q)));
  }, [friends]);

  const handleOpen = async (friend: FriendEntry) => {
    if (openingId) return;
    setOpeningId(friend.otherUserId);
    try {
      const chat = await chatApi.getOrCreateDirectChat(friend.otherUserId);
      navigation.replace('Chat', {
        chatId: chat.chatId,
        title: friend.displayName,
        chatType: 'DIRECT',
      });
    } catch {
      setOpeningId(null);
    }
  };

  const initial = (name: string) => name.replace(/[^a-zA-ZğüşıöçĞÜŞİÖÇ]/g, '').charAt(0).toUpperCase() || '?';

  return (
    <SafeAreaView style={s.safe}>
      {/* Arama */}
      <View style={s.searchBar}>
        <Text style={s.searchIcon}>🔍</Text>
        <TextInput
          style={s.searchInput}
          value={query}
          onChangeText={handleQuery}
          placeholder="Arkadaş ara..."
          placeholderTextColor={Colors.textMuted}
          autoFocus
        />
      </View>

      {loading ? (
        <View style={s.center}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.otherUserId}
          renderItem={({ item }) => {
            const isOpening = openingId === item.otherUserId;
            return (
              <TouchableOpacity
                style={s.row}
                onPress={() => handleOpen(item)}
                activeOpacity={0.72}
                disabled={!!openingId}
              >
                <View style={s.avatar}>
                  <Text style={s.avatarText}>{initial(item.displayName)}</Text>
                </View>
                <Text style={s.name}>{item.displayName}</Text>
                {isOpening
                  ? <ActivityIndicator size="small" color={Colors.primary} />
                  : <Text style={s.chevron}>›</Text>
                }
              </TouchableOpacity>
            );
          }}
          ItemSeparatorComponent={() => <View style={s.sep} />}
          contentContainerStyle={filtered.length === 0 ? s.emptyContainer : s.list}
          ListEmptyComponent={
            <View style={s.center}>
              <Text style={s.emptyIcon}>👥</Text>
              <Text style={s.emptyTitle}>
                {query ? 'Eşleşen arkadaş yok' : 'Henüz arkadaşın yok'}
              </Text>
              <Text style={s.emptySub}>
                {!query && 'Oyuncu ara kısmından arkadaş ekleyebilirsin.'}
              </Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.background },

  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    margin: 16,
    paddingHorizontal: 14,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 10,
  },
  searchIcon: { fontSize: 16 },
  searchInput: {
    flex: 1,
    height: 44,
    color: Colors.text,
    fontSize: 15,
  },

  list: { paddingBottom: 20 },
  emptyContainer: { flex: 1 },

  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 14,
    gap: 12,
  },
  avatar: {
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { fontSize: 18, fontWeight: '800', color: Colors.primary },
  name: { flex: 1, fontSize: 16, fontWeight: '600', color: Colors.text },
  chevron: { fontSize: 22, color: Colors.textDim },

  sep: { height: 1, backgroundColor: Colors.border, marginHorizontal: 16 },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 40 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 17, fontWeight: '700', color: Colors.text, marginBottom: 6 },
  emptySub: { fontSize: 13, color: Colors.textMuted, textAlign: 'center' },
});

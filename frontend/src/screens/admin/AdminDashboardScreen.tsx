import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  TextInput,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { userApi } from "../../api/user.api";
import { UserSelfResponse } from "../../types/user.types";
import { AdminDashboardScreenProps } from "../../navigation/types";
import { Colors } from "../../theme/colors";
import { getErrorMessage } from "../../utils/error";
import Avatar from "../../components/ui/Avatar";

export default function AdminDashboardScreen({
  navigation,
}: AdminDashboardScreenProps) {
  const [users, setUsers] = useState<UserSelfResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [searchText, setSearchText] = useState("");
  const [activeSearch, setActiveSearch] = useState("");
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadUsers = useCallback(async (pageNum: number, search: string) => {
    try {
      const data = await userApi.getAllUsers(pageNum, 20, search || undefined);
      if (pageNum === 0) {
        setUsers(data.content);
      } else {
        setUsers((prev) => [...prev, ...data.content]);
      }
      setHasMore(!data.last);
    } catch (err) {
      Alert.alert("Hata", getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers(0, activeSearch);
  }, [loadUsers, activeSearch]);

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      setPage(0);
      setLoading(true);
      loadUsers(0, activeSearch);
    });
    return unsubscribe;
  }, [navigation, loadUsers, activeSearch]);

  const handleSearchChange = (text: string) => {
    setSearchText(text);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setPage(0);
      setLoading(true);
      setActiveSearch(text.trim());
    }, 400);
  };

  const clearSearch = () => {
    setSearchText("");
    setActiveSearch("");
    setPage(0);
    setLoading(true);
  };

  const loadMore = () => {
    if (!hasMore || loading) return;
    const nextPage = page + 1;
    setPage(nextPage);
    loadUsers(nextPage, activeSearch);
  };

  const getStatusColor = (status: string) => {
    if (status === "ACTIVE") return Colors.success;
    if (status === "SUSPENDED") return Colors.error;
    return Colors.textMuted;
  };

  if (loading && users.length === 0) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.header}>
        <Text style={styles.title}>Admin Panel</Text>
        <TouchableOpacity
          style={styles.reportsBtn}
          onPress={() => navigation.navigate("AdminReports")}
        >
          <Text style={styles.reportsBtnText}>🚩 Şikayetler</Text>
        </TouchableOpacity>
      </View>

      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <View style={styles.searchBox}>
          <Text style={styles.searchIcon}>🔍</Text>
          <TextInput
            style={styles.searchInput}
            placeholder="Ad veya e-posta ile ara..."
            placeholderTextColor={Colors.textMuted}
            value={searchText}
            onChangeText={handleSearchChange}
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="search"
          />
          {searchText.length > 0 && (
            <TouchableOpacity
              onPress={clearSearch}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.clearIcon}>✕</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>

      <FlatList
        data={users}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        onEndReached={loadMore}
        onEndReachedThreshold={0.3}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            activeOpacity={0.75}
            onPress={() =>
              navigation.navigate("AdminUserDetail", { userId: item.id })
            }
          >
            <View style={styles.cardLeft}>
              <Avatar
                uri={item.avatarUrl}
                name={item.displayName}
                size={44}
                style={styles.avatar}
                backgroundColor={Colors.primary}
                borderWidth={0}
                textColor={Colors.black}
              />
              <View style={styles.cardInfo}>
                <Text style={styles.cardName}>{item.displayName}</Text>
                <Text style={styles.cardEmail}>{item.email}</Text>
              </View>
            </View>
            <View style={styles.cardRight}>
              <View
                style={[
                  styles.statusBadge,
                  { backgroundColor: getStatusColor(item.status) + "1A" },
                ]}
              >
                <View
                  style={[
                    styles.statusDot,
                    { backgroundColor: getStatusColor(item.status) },
                  ]}
                />
                <Text
                  style={[
                    styles.statusText,
                    { color: getStatusColor(item.status) },
                  ]}
                >
                  {item.status}
                </Text>
              </View>
              <Text style={styles.roleText}>{item.role}</Text>
            </View>
          </TouchableOpacity>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: Colors.background },
  centered: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: Colors.background,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 14,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  title: { fontSize: 28, fontWeight: "800", color: Colors.text },
  reportsBtn: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    backgroundColor: Colors.error + "18",
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.error + "44",
  },
  reportsBtnText: { fontSize: 13, fontWeight: "700", color: Colors.error },
  countBadge: {
    backgroundColor: Colors.primaryGlow,
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.primary + "33",
  },
  countText: { fontSize: 13, color: Colors.primary, fontWeight: "600" },
  searchContainer: {
    paddingHorizontal: 20,
    paddingBottom: 12,
  },
  searchBox: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: Colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 14,
    height: 46,
  },
  searchIcon: { fontSize: 15, marginRight: 8 },
  searchInput: {
    flex: 1,
    fontSize: 14,
    color: Colors.text,
    paddingVertical: 0,
  },
  clearIcon: { fontSize: 14, color: Colors.textMuted, fontWeight: "600" },
  list: { paddingHorizontal: 20, paddingBottom: 32 },
  card: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: Colors.surface,
    borderRadius: 18,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardLeft: { flexDirection: "row", alignItems: "center", flex: 1 },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primary,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  cardInfo: { flex: 1 },
  cardName: { fontSize: 15, fontWeight: "700", color: Colors.text },
  cardEmail: { fontSize: 12, color: Colors.textMuted, marginTop: 2 },
  cardRight: { alignItems: "flex-end" },
  statusBadge: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusDot: { width: 6, height: 6, borderRadius: 3, marginRight: 5 },
  statusText: { fontSize: 11, fontWeight: "700", textTransform: "uppercase" },
  roleText: { fontSize: 11, color: Colors.textMuted, marginTop: 4 },
});

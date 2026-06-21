import React, { useEffect, useState, useCallback } from "react";
import { TouchableOpacity, View, Text, StyleSheet } from "react-native";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { Colors, Radius } from "../../theme/colors";
import { notificationApi } from "../../api/notification.api";

export default function NotificationBell() {
  const navigation = useNavigation<any>();
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchUnread = useCallback(async () => {
    try {
      const notifications = await notificationApi.getAll();
      setUnreadCount(notifications.filter((n) => !n.isRead).length);
    } catch {}
  }, []);

  useEffect(() => {
    fetchUnread();
  }, [fetchUnread]);

  useFocusEffect(
    useCallback(() => {
      fetchUnread();
    }, [fetchUnread]),
  );

  return (
    <TouchableOpacity
      style={styles.button}
      onPress={() => navigation.navigate("Notifications" as any)}
      activeOpacity={0.7}
      hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
    >
      <Text style={styles.icon}>🔔</Text>
      {unreadCount > 0 && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>
            {unreadCount > 99 ? "99+" : unreadCount}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 40,
    height: 40,
    borderRadius: Radius.md,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: "center",
    justifyContent: "center",
  },
  icon: {
    fontSize: 18,
  },
  badge: {
    position: "absolute",
    top: -4,
    right: -4,
    backgroundColor: Colors.error,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    paddingHorizontal: 4,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 2,
    borderColor: Colors.background,
  },
  badgeText: {
    color: Colors.white,
    fontSize: 9,
    fontWeight: "800",
  },
});

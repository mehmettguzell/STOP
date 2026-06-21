import React, { useEffect } from "react";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Text, View, StyleSheet, Platform } from "react-native";
import HomeNavigator from "./HomeNavigator";
import MatchNavigator from "./MatchNavigator";
import FriendsNavigator from "./FriendsNavigator";
import MessagesNavigator from "./MessagesNavigator";
import ProfileNavigator from "./ProfileNavigator";
import AdminNavigator from "./AdminNavigator";
import { AppTabParamList } from "./types";
import { Colors, Radius } from "../theme/colors";
import { useAuthStore } from "../store/auth.store";
import { useChatStore } from "../store/chat.store";

const Tab = createBottomTabNavigator<AppTabParamList>();

const TAB_ICONS: Record<string, string> = {
  HomeTab: "🏠",
  MatchTab: "ᯓ⚽️",
  FriendsTab: "🧟‍♂️",
  MessagesTab: "💬",
  ProfileTab: "🪪",
  AdminTab: "🥷",
};

function TabIcon({
  name,
  focused,
  unreadCount = 0,
}: {
  name: string;
  focused: boolean;
  unreadCount?: number;
}) {
  return (
    <View style={tabStyles.iconWrapper}>
      <View style={[tabStyles.iconPill, focused && tabStyles.iconPillActive]}>
        <Text style={[tabStyles.icon, focused && tabStyles.iconActive]}>
          {TAB_ICONS[name]}
        </Text>
        {unreadCount > 0 && (
          <View style={tabStyles.tabBadge}>
            <Text style={tabStyles.tabBadgeText}>
              {unreadCount > 99 ? "99+" : unreadCount}
            </Text>
          </View>
        )}
      </View>
      {focused && <View style={tabStyles.activeIndicator} />}
    </View>
  );
}

export default function AppNavigator() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === "ADMIN";
  const totalUnread = useChatStore((s) => s.totalUnread);
  const refreshUnread = useChatStore((s) => s.refreshUnread);

  useEffect(() => {
    refreshUnread();
    const interval = setInterval(refreshUnread, 30_000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: tabStyles.tabBar,
        tabBarShowLabel: true,
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: Colors.textDim,
        tabBarLabelStyle: tabStyles.label,
        tabBarItemStyle: tabStyles.tabItem,
        tabBarIcon: ({ focused }) => (
          <TabIcon
            name={route.name}
            focused={focused}
            unreadCount={route.name === "MessagesTab" ? totalUnread : 0}
          />
        ),
      })}
    >
      <Tab.Screen
        name="HomeTab"
        component={HomeNavigator}
        options={{ tabBarLabel: "Anasayfa" }}
      />
      <Tab.Screen
        name="MatchTab"
        component={MatchNavigator}
        options={{ tabBarLabel: "Maclar" }}
        listeners={({ navigation, route }) => ({
          tabPress: (e) => {
            const isFocused = navigation.isFocused();
            const state = (route as any).state;
            if (isFocused && state && state.routes && state.routes.length > 1) {
              e.preventDefault();
              navigation.navigate("MatchTab", { screen: "MatchList" });
            }
          },
        })}
      />
      <Tab.Screen
        name="FriendsTab"
        component={FriendsNavigator}
        options={{ tabBarLabel: "Arkadaslar" }}
        listeners={({ navigation, route }) => ({
          tabPress: (e) => {
            const isFocused = navigation.isFocused();
            const state = (route as any).state;
            if (isFocused && state && state.routes && state.routes.length > 1) {
              e.preventDefault();
              navigation.navigate("FriendsTab", { screen: "FriendsList" });
            }
          },
        })}
      />
      <Tab.Screen
        name="MessagesTab"
        component={MessagesNavigator}
        options={{ tabBarLabel: "Mesajlar" }}
        listeners={({ navigation, route }) => ({
          tabPress: (e) => {
            refreshUnread();
            const isFocused = navigation.isFocused();
            const state = (route as any).state;
            if (isFocused && state && state.routes && state.routes.length > 1) {
              e.preventDefault();
              navigation.navigate("MessagesTab", { screen: "ChatList" });
            }
          },
        })}
      />
      <Tab.Screen
        name="ProfileTab"
        component={ProfileNavigator}
        options={{ tabBarLabel: "Profil" }}
        listeners={({ navigation, route }) => ({
          tabPress: (e) => {
            const isFocused = navigation.isFocused();
            const state = (route as any).state;
            if (isFocused && state && state.routes && state.routes.length > 1) {
              e.preventDefault();
              navigation.navigate("ProfileTab", { screen: "ProfileView" });
            }
          },
        })}
      />
      {isAdmin && (
        <Tab.Screen
          name="AdminTab"
          component={AdminNavigator}
          options={{ tabBarLabel: "Admin" }}
          listeners={({ navigation, route }) => ({
            tabPress: (e) => {
              const isFocused = navigation.isFocused();
              const state = (route as any).state;
              if (
                isFocused &&
                state &&
                state.routes &&
                state.routes.length > 1
              ) {
                e.preventDefault();
                navigation.navigate("AdminTab", { screen: "AdminDashboard" });
              }
            },
          })}
        />
      )}
    </Tab.Navigator>
  );
}

const tabStyles = StyleSheet.create({
  tabBar: {
    backgroundColor: Colors.backgroundElevated,
    borderTopColor: Colors.divider,
    borderTopWidth: 1,
    paddingTop: 10,
    paddingBottom: Platform.OS === "ios" ? 24 : 10,
    height: Platform.OS === "ios" ? 84 : 70,
    elevation: 24,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: -8 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
  },
  tabItem: {
    paddingTop: 4,
  },
  label: {
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 0.3,
    marginTop: 2,
  },
  iconWrapper: {
    alignItems: "center",
    justifyContent: "center",
    width: 56,
    height: 32,
  },
  iconPill: {
    width: 48,
    height: 28,
    borderRadius: Radius.pill,
    alignItems: "center",
    justifyContent: "center",
  },
  iconPillActive: {
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
  },
  icon: {
    fontSize: 18,
    color: Colors.textDim,
    fontWeight: "700",
  },
  iconActive: {
    color: Colors.primary,
  },
  activeIndicator: {
    position: "absolute",
    top: -10,
    width: 24,
    height: 3,
    borderRadius: 2,
    backgroundColor: Colors.primary,
  },
  tabBadge: {
    position: "absolute",
    top: -4,
    right: -4,
    backgroundColor: Colors.error,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 3,
    borderWidth: 1.5,
    borderColor: Colors.backgroundElevated,
  },
  tabBadgeText: {
    fontSize: 10,
    fontWeight: "800",
    color: "#fff",
  },
});

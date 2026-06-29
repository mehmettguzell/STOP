import React from "react";
import { View } from "react-native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import FriendsScreen from "../screens/profile/FriendsScreen";
import SearchScreen from "../screens/search/SearchScreen";
import PlayerProfileScreen from "../screens/search/PlayerProfileScreen";
import ChatScreen from "../screens/match/ChatScreen";
import NotificationsScreen from "../screens/notifications/NotificationsScreen";
import NotificationBell from "../components/ui/NotificationBell";
import HeaderAvatar from "../components/ui/HeaderAvatar";
import BackButton from "../components/ui/BackButton";
import { FriendsStackParamList } from "./types";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator<FriendsStackParamList>();

export default function FriendsNavigator() {
  return (
    <Stack.Navigator
      screenOptions={({ navigation }) => ({
        headerStyle: { backgroundColor: Colors.backgroundElevated },
        headerTintColor: Colors.text,
        headerTitleStyle: {
          fontWeight: "700",
          fontSize: 17,
          color: Colors.text,
          letterSpacing: -0.3,
        },
        headerShadowVisible: false,
        headerLeft: ({ canGoBack }) =>
          canGoBack ? <BackButton onPress={() => navigation.goBack()} /> : null,
        contentStyle: { backgroundColor: Colors.background },
      })}
    >
      <Stack.Screen
        name="FriendsList"
        component={FriendsScreen}
        options={{
          title: "Arkadaşlarım",
          headerRight: () => (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginRight: 4 }}>
              <HeaderAvatar />
              <NotificationBell />
            </View>
          ),
        }}
      />
      <Stack.Screen
        name="Search"
        component={SearchScreen}
        options={{ title: "Oyuncu Ara" }}
      />
      <Stack.Screen
        name="PlayerProfile"
        component={PlayerProfileScreen as any}
        options={{ title: "Oyuncu Profili" }}
      />
      <Stack.Screen
        name="Chat"
        component={ChatScreen as any}
        options={({ route }) => ({ title: route.params.title })}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: "Bildirimler", presentation: "modal" }}
      />
    </Stack.Navigator>
  );
}

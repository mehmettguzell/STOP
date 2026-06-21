import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import HomeScreen from "../screens/home/HomeScreen";
import NotificationsScreen from "../screens/notifications/NotificationsScreen";
import MatchDetailScreen from "../screens/match/MatchDetailScreen";
import PlayerProfileScreen from "../screens/search/PlayerProfileScreen";
import BackButton from "../components/ui/BackButton";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator();

export default function HomeNavigator() {
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
        name="Home"
        component={HomeScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: "Bildirimler", presentation: "modal" }}
      />
      <Stack.Screen
        name="MatchDetail"
        component={MatchDetailScreen}
        options={{ title: "Maç Detayı" }}
      />
      <Stack.Screen
        name="PlayerProfile"
        component={PlayerProfileScreen as any}
        options={{ title: "Oyuncu Profili" }}
      />
    </Stack.Navigator>
  );
}

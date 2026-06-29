import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import MatchListScreen from "../screens/match/MatchListScreen";
import MatchDetailScreen from "../screens/match/MatchDetailScreen";
import CreateMatchScreen from "../screens/match/CreateMatchScreen";
import EditMatchScreen from "../screens/match/EditMatchScreen";
import PlayerProfileScreen from "../screens/search/PlayerProfileScreen";
import ChatScreen from "../screens/match/ChatScreen";
import ReportScreen from "../screens/match/ReportScreen";
import RatingScreen from "../screens/match/RatingScreen";
import NotificationsScreen from "../screens/notifications/NotificationsScreen";
import BackButton from "../components/ui/BackButton";
import { MatchStackParamList } from "./types";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator<MatchStackParamList>();

export default function MatchNavigator() {
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
        headerLeft: ({ canGoBack }) => (
          <BackButton
            onPress={() => {
              if (canGoBack) {
                navigation.goBack();
              } else {
                navigation.navigate("MatchList");
              }
            }}
          />
        ),
        contentStyle: { backgroundColor: Colors.background },
      })}
    >
      <Stack.Screen
        name="MatchList"
        component={MatchListScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="MatchDetail"
        component={MatchDetailScreen}
        options={{ title: "Maç Detayı" }}
      />
      <Stack.Screen
        name="CreateMatch"
        component={CreateMatchScreen}
        options={{ title: "Yeni Maç" }}
      />
      <Stack.Screen
        name="EditMatch"
        component={EditMatchScreen}
        options={{ title: "Maçı Düzenle" }}
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
        name="Report"
        component={ReportScreen}
        options={{ title: "Şikayet Et" }}
      />
      <Stack.Screen
        name="RateMatch"
        component={RatingScreen}
        options={({ route }) => ({ title: route.params.matchTitle })}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: "Bildirimler", presentation: "modal" }}
      />
    </Stack.Navigator>
  );
}

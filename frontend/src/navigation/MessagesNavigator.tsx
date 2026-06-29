import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import ChatListScreen from "../screens/messages/ChatListScreen";
import ChatScreen from "../screens/match/ChatScreen";
import NewMessageScreen from "../screens/messages/NewMessageScreen";
import PlayerProfileScreen from "../screens/search/PlayerProfileScreen";
import SearchScreen from "../screens/search/SearchScreen";
import BackButton from "../components/ui/BackButton";
import { MessagesStackParamList } from "./types";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator<MessagesStackParamList>();

export default function MessagesNavigator() {
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
        name="ChatList"
        component={ChatListScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="Chat"
        component={ChatScreen as any}
        options={({ route }) => ({ title: route.params.title })}
      />
      <Stack.Screen
        name="NewMessage"
        component={NewMessageScreen as any}
        options={{ title: "Yeni Mesaj" }}
      />
      <Stack.Screen
        name="Search"
        component={SearchScreen as any}
        options={{ title: "Kullanıcı Ara" }}
      />
      <Stack.Screen
        name="PlayerProfile"
        component={PlayerProfileScreen as any}
        options={{ title: "Oyuncu Profili" }}
      />
    </Stack.Navigator>
  );
}

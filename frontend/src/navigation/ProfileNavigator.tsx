import React from "react";
import { View } from "react-native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import ProfileViewScreen from "../screens/profile/ProfileViewScreen";
import CreateProfileScreen from "../screens/profile/CreateProfileScreen";
import EditProfileScreen from "../screens/profile/EditProfileScreen";
import EditAccountScreen from "../screens/profile/EditAccountScreen";
import NotificationsScreen from "../screens/notifications/NotificationsScreen";
import NotificationBell from "../components/ui/NotificationBell";
import HeaderAvatar from "../components/ui/HeaderAvatar";
import BackButton from "../components/ui/BackButton";
import { ProfileStackParamList } from "./types";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator<ProfileStackParamList>();

export default function ProfileNavigator() {
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
        name="ProfileView"
        component={ProfileViewScreen}
        options={{
          title: "Profil",
          headerRight: () => (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginRight: 4 }}>
              <HeaderAvatar />
              <NotificationBell />
            </View>
          ),
        }}
      />
      <Stack.Screen
        name="CreateProfile"
        component={CreateProfileScreen}
        options={{ title: "Profil Oluştur" }}
      />
      <Stack.Screen
        name="EditProfile"
        component={EditProfileScreen}
        options={{ title: "Profili Düzenle" }}
      />
      <Stack.Screen
        name="EditAccount"
        component={EditAccountScreen}
        options={{ title: "Hesap Ayarları" }}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: "Bildirimler", presentation: "modal" }}
      />
    </Stack.Navigator>
  );
}

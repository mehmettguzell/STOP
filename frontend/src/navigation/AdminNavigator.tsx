import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import AdminDashboardScreen from "../screens/admin/AdminDashboardScreen";
import AdminUserDetailScreen from "../screens/admin/AdminUserDetailScreen";
import AdminReportsScreen from "../screens/admin/AdminReportsScreen";
import AdminReportDetailScreen from "../screens/admin/AdminReportDetailScreen";
import MatchDetailScreen from "../screens/match/MatchDetailScreen";
import NotificationsScreen from "../screens/notifications/NotificationsScreen";
import BackButton from "../components/ui/BackButton";
import { AdminStackParamList } from "./types";
import { Colors } from "../theme/colors";

const Stack = createNativeStackNavigator<AdminStackParamList>();

export default function AdminNavigator() {
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
        name="AdminDashboard"
        component={AdminDashboardScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name="AdminUserDetail"
        component={AdminUserDetailScreen}
        options={{ title: "Kullanıcı Detayı" }}
      />
      <Stack.Screen
        name="AdminReports"
        component={AdminReportsScreen}
        options={{ title: "Şikayetler" }}
      />
      <Stack.Screen
        name="AdminReportDetail"
        component={AdminReportDetailScreen}
        options={{ title: "Şikayet Detayı" }}
      />
      <Stack.Screen
        name="AdminMatchDetail"
        component={MatchDetailScreen}
        options={{ title: "Maç Detayı" }}
      />
      <Stack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: "Bildirimler", presentation: "modal" }}
      />
    </Stack.Navigator>
  );
}

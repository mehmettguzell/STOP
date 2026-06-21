import React, { useEffect } from "react";
import { View, ActivityIndicator, StyleSheet } from "react-native";
import { useAuthStore, selectIsAuthenticated } from "../store/auth.store";
import AuthNavigator from "./AuthNavigator";
import AppNavigator from "./AppNavigator";
import { Colors } from "../theme/colors";
import { setAuthFailureHandler } from "../api/client";

export default function RootNavigator() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const isInitialized = useAuthStore((s) => s.isInitialized);

  useEffect(() => {
    setAuthFailureHandler(() => {
      useAuthStore.setState({ user: null, accessToken: null, refreshToken: null });
    });
  }, []);

  if (!isInitialized) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

  return isAuthenticated ? <AppNavigator /> : <AuthNavigator />;
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: Colors.background,
  },
});

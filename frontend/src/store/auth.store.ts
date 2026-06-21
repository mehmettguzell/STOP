import { create } from "zustand";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { authApi } from "../api/auth.api";
import { userApi } from "../api/user.api";
import { STORAGE_KEYS, setLoginInProgress, setCachedToken } from "../api/client";
import { UserSelfResponse } from "../types/user.types";
import { LoginRequest, RegisterRequest } from "../types/auth.types";
import { useProfileStore } from "./profile.store";

interface AuthState {
  user: UserSelfResponse | null;
  accessToken: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  isInitialized: boolean;

  // Actions
  initialize: () => Promise<void>;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  deleteAccount: () => Promise<void>;
  refreshUser: () => Promise<void>;
  setUser: (user: UserSelfResponse) => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isLoading: false,
  isInitialized: false,

  initialize: async () => {
    useProfileStore.getState().reset();
    try {
      const accessToken = await AsyncStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      const refreshToken = await AsyncStorage.getItem(
        STORAGE_KEYS.REFRESH_TOKEN,
      );

      if (accessToken && refreshToken) {
        setCachedToken(accessToken);
        set({ accessToken, refreshToken });
        const user = await userApi.getMe();
        set({ user });
      }
    } catch {
      setCachedToken(null);
      await AsyncStorage.multiRemove([
        STORAGE_KEYS.ACCESS_TOKEN,
        STORAGE_KEYS.REFRESH_TOKEN,
        STORAGE_KEYS.USER_ID,
      ]);
      set({ user: null, accessToken: null, refreshToken: null });
    } finally {
      set({ isInitialized: true });
    }
  },

  login: async (request: LoginRequest) => {
    useProfileStore.getState().reset();
    set({ isLoading: true });
    setLoginInProgress(true);
    try {
      const auth = await authApi.login(request);
      await AsyncStorage.multiSet([
        [STORAGE_KEYS.ACCESS_TOKEN, auth.accessToken],
        [STORAGE_KEYS.REFRESH_TOKEN, auth.refreshToken],
      ]);
      setCachedToken(auth.accessToken);
      set({ accessToken: auth.accessToken, refreshToken: auth.refreshToken });

      // getMe() başarısız olursa retry yap (circuit breaker geçici açık olabilir).
      // loginInProgress flag'i sayesinde interceptor token'ları silmez.
      let user;
      let lastError;
      for (const delay of [0, 1500, 3000]) {
        try {
          if (delay > 0) await new Promise((resolve) => setTimeout(resolve, delay));
          user = await userApi.getMe();
          lastError = undefined;
          break;
        } catch (e) {
          lastError = e;
        }
      }
      if (!user) throw lastError;
      set({ user });
    } finally {
      setLoginInProgress(false);
      set({ isLoading: false });
    }
  },

  register: async (request: RegisterRequest) => {
    useProfileStore.getState().reset();
    set({ isLoading: true, user: null, accessToken: null, refreshToken: null });
    setLoginInProgress(true);
    try {
      const auth = await authApi.register(request);
      await AsyncStorage.multiSet([
        [STORAGE_KEYS.ACCESS_TOKEN, auth.accessToken],
        [STORAGE_KEYS.REFRESH_TOKEN, auth.refreshToken],
      ]);
      setCachedToken(auth.accessToken);
      set({ accessToken: auth.accessToken, refreshToken: auth.refreshToken });

      let user;
      let lastError;
      for (const delay of [0, 1500, 3000]) {
        try {
          if (delay > 0) await new Promise((resolve) => setTimeout(resolve, delay));
          user = await userApi.getMe();
          lastError = undefined;
          break;
        } catch (e) {
          lastError = e;
        }
      }
      if (!user) throw lastError;
      set({ user });
    } finally {
      setLoginInProgress(false);
      set({ isLoading: false });
    }
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // Best-effort logout
    } finally {
      setCachedToken(null);
      await AsyncStorage.multiRemove([
        STORAGE_KEYS.ACCESS_TOKEN,
        STORAGE_KEYS.REFRESH_TOKEN,
        STORAGE_KEYS.USER_ID,
      ]);
      set({ user: null, accessToken: null, refreshToken: null });
    }
  },

  deleteAccount: async () => {
    await userApi.deleteMe();
    useProfileStore.getState().reset();
    setCachedToken(null);
    await AsyncStorage.multiRemove([
      STORAGE_KEYS.ACCESS_TOKEN,
      STORAGE_KEYS.REFRESH_TOKEN,
      STORAGE_KEYS.USER_ID,
    ]);
    set({ user: null, accessToken: null, refreshToken: null });
  },

  refreshUser: async () => {
    const user = await userApi.getMe();
    set({ user });
  },

  setUser: (user: UserSelfResponse) => set({ user }),
}));

export const selectIsAuthenticated = (state: AuthState) =>
  state.user !== null && state.accessToken !== null;

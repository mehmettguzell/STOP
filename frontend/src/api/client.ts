import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import AsyncStorage from "@react-native-async-storage/async-storage";
import Constants from "expo-constants";
import { Platform } from "react-native";

let onAuthFailure: (() => void) | null = null;
export const setAuthFailureHandler = (handler: () => void) => {
  onAuthFailure = handler;
};

// Login/reg  ister akışı sırasında interceptor token silip onAuthFailure'ı tetiklemesin.
let loginInProgress = false;
export const setLoginInProgress = (value: boolean) => {
  loginInProgress = value;
};

let cachedToken: string | null = null;
export const setCachedToken = (token: string | null) => {
  cachedToken = token;
};

const debuggerHost = Constants.expoConfig?.hostUri;
// let host = "13.50.15.181";
let host = "192.168.1.19";

// if (debuggerHost) {
//   host = debuggerHost.split(':')[0];
// } else if (Platform.OS === 'android') {
//   host = '10.0.2.2';
// } else {
//   host = 'localhost';
// }

export const BASE_URL = `http://${host}:8080/api/v1`;
const API_ORIGIN = `http://${host}:8080`;

// The backend returns avatarUrl as a client-relative path (e.g.
// "/api/v1/users/profile/avatar/x.jpg") so it never bakes in a host that can
// go stale (LAN IP changes, server IP changes...). Older records may still
// hold a full absolute URL from before this change - strip any scheme+host
// prefix so both shapes resolve against whatever host we're using right now.
export const resolveAvatarUrl = (
  value: string | null | undefined,
): string | null => {
  if (!value) return null;
  const path = value.startsWith("http")
    ? value.replace(/^https?:\/\/[^/]+/, "")
    : value;
  return `${API_ORIGIN}${path}`;
};

const rewriteAvatarUrls = (data: unknown): unknown => {
  if (Array.isArray(data)) {
    data.forEach(rewriteAvatarUrls);
    return data;
  }
  if (data && typeof data === "object") {
    for (const [key, value] of Object.entries(data as Record<string, unknown>)) {
      if (key === "avatarUrl" && typeof value === "string") {
        (data as Record<string, unknown>)[key] = resolveAvatarUrl(value);
      } else {
        rewriteAvatarUrls(value);
      }
    }
  }
  return data;
};

export const STORAGE_KEYS = {
  ACCESS_TOKEN: "@stop/access_token",
  REFRESH_TOKEN: "@stop/refresh_token",
  USER_ID: "@stop/user_id",
};

const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

// Request interceptor — attach Bearer token
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token =
      cachedToken ?? (await AsyncStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN));
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => {
    rewriteAvatarUrls(response.data);
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (error.response?.status === 401 && !originalRequest._retry) {
      // Auth endpoint'leri veya login akışı sırasında token yenileme yapma.
      // loginInProgress: getMe() ilk denemede fail ederse AsyncStorage race
      // condition nedeniyle token'lar silinmesin, retry loop'u kırılmasın.
      if (originalRequest.url?.includes("/auth/") || loginInProgress) {
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = await AsyncStorage.getItem(
          STORAGE_KEYS.REFRESH_TOKEN,
        );
        if (!refreshToken) throw new Error("No refresh token");

        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken,
        });

        const newAccessToken = data.accessToken;
        const newRefreshToken = data.refreshToken;

        await AsyncStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, newAccessToken);
        await AsyncStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken);

        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        cachedToken = null;
        await AsyncStorage.multiRemove([
          STORAGE_KEYS.ACCESS_TOKEN,
          STORAGE_KEYS.REFRESH_TOKEN,
          STORAGE_KEYS.USER_ID,
        ]);
        onAuthFailure?.();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;

import apiClient from "./client";
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from "../types/auth.types";

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>("/auth/login", request);
    return data;
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>(
      "/auth/register",
      request,
    );
    return data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post("/auth/logout");
  },

  refresh: async (refreshToken: string): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>("/auth/refresh", {
      refreshToken,
    });
    return data;
  },

  forgotPassword: async (email: string): Promise<{ devToken?: string }> => {
    const { data } = await apiClient.post<{ devToken?: string }>("/auth/forgot-password", { email });
    return data;
  },

  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await apiClient.post("/auth/reset-password", { token, newPassword });
  },
};

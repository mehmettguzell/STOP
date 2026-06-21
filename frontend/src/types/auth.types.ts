export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  phoneNumber: string;
  password: string;
  displayName: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

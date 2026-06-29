export type UserRole = "USER" | "ADMIN";
export type UserStatus = "ACTIVE" | "SUSPENDED" | "DELETED";
export type DominantFoot = "LEFT" | "RIGHT" | "BOTH";

export interface UserSelfResponse {
  id: string;
  email: string;
  phoneNumber: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  trustScore: number;
  rankScore: number;
  createdAt: string;
  updatedAt: string;
  phoneVerified: boolean;
  emailVerified: boolean;
}

export interface UserPublicResponse {
  displayName: string;
  trustScore: number;
  rankScore: number;
}

export interface UpdateUserRequest {
  email?: string;
  displayName?: string;
  phoneNumber?: string;
  password?: string;
  rePassword?: string;
}

export interface UserProfileResponse {
  userId: string;
  displayName: string;
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  city: string | null;
  position: string | null;
  heightCm: number | null;
  weightKg: number | null;
  dominantFoot: DominantFoot | null;
  bio: string | null;
  avatarUrl: string | null;
  trustScore: number | null;
  rankScore: number | null;
}

export interface UserProfileRequest {
  firstName: string;
  lastName: string;
  birthDate: string;
  city: string;
  position: string;
  heightCm: number;
  weightKg: number;
  dominantFoot: DominantFoot;
  bio?: string;
  avatarUrl?: string;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  city?: string;
  position?: string;
  heightCm?: number;
  weightKg?: number;
  dominantFoot?: DominantFoot;
  bio?: string;
  avatarUrl?: string;
}

export interface ProfileSearchParams {
  city?: string;
  position?: string;
  dominantFoot?: DominantFoot;
  displayName?: string;
  minHeight?: number;
  maxHeight?: number;
  minWeight?: number;
  maxWeight?: number;
  minTrustScore?: number;
  maxTrustScore?: number;
  minRankScore?: number;
  maxRankScore?: number;
  page?: number;
  size?: number;
}

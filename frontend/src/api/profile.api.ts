import apiClient from './client';
import { SliceResponse } from '../types/api.types';
import {
  ProfileSearchParams,
  UpdateUserProfileRequest,
  UserProfileRequest,
  UserProfileResponse,
} from '../types/user.types';

export const profileApi = {
  createProfile: async (request: UserProfileRequest): Promise<UserProfileResponse> => {
    const { data } = await apiClient.post<UserProfileResponse>('/users/profile', request);
    return data;
  },

  getProfile: async (userId: string): Promise<UserProfileResponse> => {
    const { data } = await apiClient.get<UserProfileResponse>(`/users/profile/${userId}`);
    return data;
  },

  updateProfile: async (request: UpdateUserProfileRequest): Promise<UserProfileResponse> => {
    const { data } = await apiClient.patch<UserProfileResponse>('/users/profile', request);
    return data;
  },

  searchProfiles: async (params: ProfileSearchParams = {}): Promise<SliceResponse<UserProfileResponse>> => {
    const { data } = await apiClient.get<SliceResponse<UserProfileResponse>>('/users/profile', { params });
    return data;
  },

  uploadAvatar: async (uri: string): Promise<UserProfileResponse> => {
    const filename = uri.split('/').pop() ?? 'photo.jpg';
    const extMatch = /\.(\w+)$/.exec(filename);
    const ext = extMatch ? extMatch[1].toLowerCase() : 'jpg';
    const mimeType = ext === 'png' ? 'image/png' : 'image/jpeg';

    const formData = new FormData();
    formData.append('file', {
      uri,
      name: filename,
      type: mimeType,
    } as unknown as Blob);

    const { data } = await apiClient.post<UserProfileResponse>('/users/profile/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  deleteAvatar: async (): Promise<UserProfileResponse> => {
    const { data } = await apiClient.delete<UserProfileResponse>('/users/profile/avatar');
    return data;
  },
};

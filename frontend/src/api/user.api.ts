import apiClient from './client';
import { PageResponse } from '../types/api.types';
import {
  UpdateUserRequest,
  UserPublicResponse,
  UserSelfResponse,
} from '../types/user.types';

export const userApi = {
  getMe: async (): Promise<UserSelfResponse> => {
    const { data } = await apiClient.get<UserSelfResponse>('/users/me');
    return data;
  },

  getUser: async (userId: string): Promise<UserPublicResponse | UserSelfResponse> => {
    const { data } = await apiClient.get<UserPublicResponse | UserSelfResponse>(`/users/${userId}`);
    return data;
  },

  getUsersByIds: async (userIds: string[]): Promise<UserPublicResponse[]> => {
    if (userIds.length === 0) return [];
    const { data } = await apiClient.post<UserPublicResponse[]>('/users/batch', { userIds });
    return data;
  },

  getAllUsers: async (page = 0, size = 20, search?: string): Promise<PageResponse<UserSelfResponse>> => {
    const { data } = await apiClient.get<PageResponse<UserSelfResponse>>(
      '/users/allUsers',
      { params: { page, size, ...(search ? { search } : {}) } },
    );
    return data;
  },

  updateMe: async (request: UpdateUserRequest): Promise<UserSelfResponse> => {
    const { data } = await apiClient.patch<UserSelfResponse>('/users/me', request);
    return data;
  },

  deleteMe: async (): Promise<void> => {
    await apiClient.delete('/users/me');
  },

  suspendUser: async (userId: string): Promise<UserSelfResponse> => {
    const { data } = await apiClient.patch<UserSelfResponse>(`/users/${userId}/suspend`);
    return data;
  },

  unsuspendUser: async (userId: string): Promise<UserSelfResponse> => {
    const { data } = await apiClient.patch<UserSelfResponse>(`/users/${userId}/unsuspend`);
    return data;
  },
};

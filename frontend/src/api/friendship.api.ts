import apiClient from './client';
import { SliceResponse } from '../types/api.types';
import { FriendshipResponse } from '../types/friendship.types';

const BASE = '/friends';

export const friendshipApi = {
  sendRequest: async (receiverId: string): Promise<FriendshipResponse> => {
    const { data } = await apiClient.post<FriendshipResponse>(`${BASE}/requests`, { receiverId });
    return data;
  },

  getFriends: async (): Promise<FriendshipResponse[]> => {
    const { data } = await apiClient.get<SliceResponse<FriendshipResponse>>(BASE);
    return data.content ?? [];
  },

  getIncoming: async (): Promise<FriendshipResponse[]> => {
    const { data } = await apiClient.get<SliceResponse<FriendshipResponse>>(`${BASE}/requests/incoming`);
    return data.content ?? [];
  },

  getOutgoing: async (): Promise<FriendshipResponse[]> => {
    const { data } = await apiClient.get<SliceResponse<FriendshipResponse>>(`${BASE}/requests/outgoing`);
    return data.content ?? [];
  },

  accept: async (id: string): Promise<FriendshipResponse> => {
    const { data } = await apiClient.patch<FriendshipResponse>(`${BASE}/requests/${id}/accept`);
    return data;
  },

  reject: async (id: string): Promise<FriendshipResponse> => {
    const { data } = await apiClient.patch<FriendshipResponse>(`${BASE}/requests/${id}/reject`);
    return data;
  },

  cancelRequest: async (id: string): Promise<void> => {
    await apiClient.delete(`${BASE}/requests/${id}`);
  },

  removeFriend: async (id: string): Promise<void> => {
    await apiClient.delete(`${BASE}/${id}`);
  },
};

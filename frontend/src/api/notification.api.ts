import apiClient from "./client";
import { NotificationResponse } from "../types/notification.types";

const BASE_PATH = "/notifications";

export const notificationApi = {
  getAll: async (): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.get<NotificationResponse[]>(BASE_PATH);
    return data;
  },

  markAsRead: async (id: string): Promise<NotificationResponse> => {
    const { data } = await apiClient.patch<NotificationResponse>(
      `${BASE_PATH}/${id}/read`,
    );
    return data;
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch(`${BASE_PATH}/read-all`);
  },

  deleteAll: async (): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/delete-all`);
  },
};

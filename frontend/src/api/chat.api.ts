import apiClient from './client';
import { ChatHistoryResponse, ChatMessageResponse, ConversationSummary, DirectChatResponse } from '../types/chat.types';

const BASE_PATH = '/chat';

export const chatApi = {
  /**
   * Belirli bir sohbetin mesaj geçmişi (maç ya da DM).
   * Backend { messages, nextCursor } döndürür — sadece mesaj listesini çıkar.
   */
  getHistory: async (chatId: string, before?: string): Promise<ChatMessageResponse[]> => {
    const { data } = await apiClient.get<ChatHistoryResponse>(
      `${BASE_PATH}/${chatId}/history`,
      { params: before ? { before } : undefined },
    );
    return data.messages ?? [];
  },

  /** Tüm konuşmalar (maç + DM) — son mesaj ve okunmamış sayısıyla. */
  getConversations: async (): Promise<ConversationSummary[]> => {
    const { data } = await apiClient.get<ConversationSummary[]>(`${BASE_PATH}/conversations`);
    return data;
  },

  /** Tab badge için toplam okunmamış sayısı. */
  getTotalUnread: async (): Promise<number> => {
    const { data } = await apiClient.get<number>(`${BASE_PATH}/unread-count`);
    return data;
  },

  /** DM kanalı oluştur veya mevcutu getir. */
  getOrCreateDirectChat: async (targetUserId: string): Promise<DirectChatResponse> => {
    const { data } = await apiClient.post<DirectChatResponse>(`${BASE_PATH}/direct`, { targetUserId });
    return data;
  },

  /** Sohbeti okundu olarak işaretle. */
  markAsRead: async (chatId: string): Promise<void> => {
    await apiClient.post(`${BASE_PATH}/${chatId}/read`);
  },

  /** Kullanıcıyı maç chat katılımcıları listesine kaydet (upsert). */
  joinMatchChat: async (matchId: string): Promise<void> => {
    await apiClient.post(`${BASE_PATH}/match/${matchId}/join`);
  },

  /** Sohbeti listeden gizle (sadece senin için). */
  hideConversation: async (chatId: string): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/conversations/${chatId}`);
  },

  /** Mesajı soft-delete ile sil (sadece kendi mesajın). */
  deleteMessage: async (messageId: string): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/messages/${messageId}`);
  },

  /** Maç chatinin kilitli olup olmadığını kontrol et. */
  isChatLocked: async (matchId: string): Promise<boolean> => {
    const { data } = await apiClient.get<boolean>(`${BASE_PATH}/match/${matchId}/locked`);
    return data;
  },
};

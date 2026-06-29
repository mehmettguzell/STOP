import apiClient from './client';
import { ChatMessageResponse, PollResponse } from '../types/chat.types';

const BASE = '/chat';

export const pollApi = {
  createPoll: async (
    chatId: string,
    question: string,
    options: string[],
    closesAt?: string | null,
  ): Promise<ChatMessageResponse> => {
    const { data } = await apiClient.post<ChatMessageResponse>(
      `${BASE}/${chatId}/polls`,
      { question, options, closesAt: closesAt ?? null },
    );
    return data;
  },

  vote: async (pollId: string, optionId: string): Promise<PollResponse> => {
    const { data } = await apiClient.post<PollResponse>(
      `${BASE}/polls/${pollId}/vote`,
      { optionId },
    );
    return data;
  },

  getPoll: async (pollId: string): Promise<PollResponse> => {
    const { data } = await apiClient.get<PollResponse>(`${BASE}/polls/${pollId}`);
    return data;
  },
};

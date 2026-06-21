import apiClient from "./client";
import {
  MatchParticipantResponse,
  ParticipationRequestResponse,
  TeamAssignmentRequest,
} from "../types/match.types";

const BASE_PATH = "/participation";

export const participationApi = {
  sendRequest: async (matchId: string): Promise<ParticipationRequestResponse> => {
    const { data } = await apiClient.post<ParticipationRequestResponse>(
      `${BASE_PATH}/requests`,
      { matchId },
    );
    return data;
  },

  approve: async (requestId: string): Promise<ParticipationRequestResponse> => {
    const { data } = await apiClient.post<ParticipationRequestResponse>(
      `${BASE_PATH}/requests/approve`,
      { id: requestId },
    );
    return data;
  },

  reject: async (requestId: string): Promise<ParticipationRequestResponse> => {
    const { data } = await apiClient.post<ParticipationRequestResponse>(
      `${BASE_PATH}/requests/${requestId}/reject`,
    );
    return data;
  },

  withdraw: async (requestId: string): Promise<void> => {
    await apiClient.delete(`${BASE_PATH}/requests/${requestId}/withdraw`);
  },

  leave: async (matchId: string): Promise<MatchParticipantResponse> => {
    const { data } = await apiClient.post<MatchParticipantResponse>(
      `${BASE_PATH}/leave`,
      { matchId },
    );
    return data;
  },

  removePlayer: async (matchId: string, userId: string): Promise<MatchParticipantResponse> => {
    const { data } = await apiClient.post<MatchParticipantResponse>(
      `${BASE_PATH}/remove-player`,
      { matchId, userId },
    );
    return data;
  },

  getParticipants: async (matchId: string): Promise<MatchParticipantResponse[]> => {
    const { data } = await apiClient.get<MatchParticipantResponse[]>(
      `${BASE_PATH}/matches/${matchId}`,
    );
    return data;
  },

  getMatchRequests: async (matchId: string): Promise<ParticipationRequestResponse[]> => {
    const { data } = await apiClient.get<ParticipationRequestResponse[]>(
      `${BASE_PATH}/requests/match/${matchId}`,
    );
    return data;
  },

  getMyRequest: async (matchId: string): Promise<ParticipationRequestResponse | null> => {
    const { data } = await apiClient.get<ParticipationRequestResponse | null>(
      `${BASE_PATH}/requests/my/${matchId}`,
    );
    return data;
  },

  assignTeams: async (
    matchId: string,
    request: TeamAssignmentRequest,
  ): Promise<MatchParticipantResponse[]> => {
    const { data } = await apiClient.put<MatchParticipantResponse[]>(
      `${BASE_PATH}/matches/${matchId}/teams`,
      request,
    );
    return data;
  },

  updateLineup: async (
    matchId: string,
    entries: { userId: string; positionX: number; positionY: number }[],
  ): Promise<MatchParticipantResponse[]> => {
    const { data } = await apiClient.put<MatchParticipantResponse[]>(
      `${BASE_PATH}/matches/${matchId}/lineup`,
      entries,
    );
    return data;
  },
};

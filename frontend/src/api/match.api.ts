import apiClient from "./client";
import { PageResponse } from "../types/api.types";
import {
  CreateMatchRequest,
  MatchResponse,
  MatchSearchParams,
  UpdateMatchRequest,
  TransferOrganizerRequest,
} from "../types/match.types";

const BASE_PATH = "/match";

export const matchApi = {
  create: async (request: CreateMatchRequest): Promise<MatchResponse> => {
    const { data } = await apiClient.post<MatchResponse>(BASE_PATH, request);
    return data;
  },

  search: async (params: MatchSearchParams = {}): Promise<PageResponse<MatchResponse>> => {
    const cleanParams = { ...params };

    if (Array.isArray(cleanParams.sort)) {
      const validSorts = cleanParams.sort.filter((s) => s && s.trim() !== "");
      if (validSorts.length > 0) {
        cleanParams.sort = validSorts.join(",");
      } else {
        delete cleanParams.sort;
      }
    }

    const { data } = await apiClient.get<PageResponse<MatchResponse>>(BASE_PATH, { params: cleanParams });
    return data;
  },

  getById: async (matchId: string): Promise<MatchResponse> => {
    const { data } = await apiClient.get<MatchResponse>(`${BASE_PATH}/${matchId}`);
    return data;
  },

  update: async (matchId: string, request: UpdateMatchRequest): Promise<MatchResponse> => {
    const { data } = await apiClient.patch<MatchResponse>(`${BASE_PATH}/${matchId}`, request);
    return data;
  },

  cancel: async (matchId: string): Promise<void> => {
    await apiClient.post(`${BASE_PATH}/${matchId}/cancel`);
  },

  transferOrganizer: async (matchId: string, request: TransferOrganizerRequest): Promise<MatchResponse> => {
    const { data } = await apiClient.post<MatchResponse>(`${BASE_PATH}/${matchId}/transfer-organizer`, request);
    return data;
  },

  start: async (matchId: string): Promise<MatchResponse> => {
    const { data } = await apiClient.post<MatchResponse>(`${BASE_PATH}/${matchId}/start`);
    return data;
  },

  complete: async (matchId: string, homeScore: number, awayScore: number): Promise<MatchResponse> => {
    const { data } = await apiClient.post<MatchResponse>(`${BASE_PATH}/${matchId}/complete`, { homeScore, awayScore });
    return data;
  },

  getMyMatches: async (title?: string, size = 50): Promise<MatchResponse[]> => {
    const { data } = await apiClient.get<MatchResponse[]>(
      `${BASE_PATH}/my-matches`,
      { params: { ...(title ? { title } : {}), size } },
    );
    return data;
  },
};

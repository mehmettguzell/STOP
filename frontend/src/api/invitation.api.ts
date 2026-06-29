import apiClient from "./client";
import {
  CreateInvitationRequest,
  InvitationResponse,
  UpdateInvitationRequest,
} from "../types/match.types";

const BASE_PATH = "/invitations";

export const invitationApi = {
  /** Organizer bir oyuncuya maç daveti gönderir. */
  create: async (
    request: CreateInvitationRequest,
  ): Promise<InvitationResponse> => {
    const { data } = await apiClient.post<InvitationResponse>(
      BASE_PATH,
      request,
    );
    return data;
  },

  /** Davet edilen oyuncu daveti kabul eder. */
  accept: async (matchId: string): Promise<InvitationResponse> => {
    const request: UpdateInvitationRequest = { matchId };
    const { data } = await apiClient.post<InvitationResponse>(
      `${BASE_PATH}/accept`,
      request,
    );
    return data;
  },

  /** Davet edilen oyuncu daveti reddeder. */
  reject: async (matchId: string): Promise<InvitationResponse> => {
    const request: UpdateInvitationRequest = { matchId };
    const { data } = await apiClient.post<InvitationResponse>(
      `${BASE_PATH}/reject`,
      request,
    );
    return data;
  },

  /** Giriş yapmış kullanıcıya gelen/gönderilen tüm davetleri getirir. */
  getAll: async (): Promise<InvitationResponse[]> => {
    const { data } = await apiClient.get<InvitationResponse[]>(BASE_PATH);
    return data;
  },
};

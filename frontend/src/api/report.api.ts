import apiClient from './client';
import { ReportDetailResponse, ReportResponse, ReportStatus } from '../types/report.types';
import { SliceResponse } from '../types/api.types';

const BASE = '/reports';

export const reportApi = {
  send: async (targetUserId: string, matchId: string, reason: string): Promise<ReportResponse> => {
    const { data } = await apiClient.post<ReportResponse>(BASE, { targetUserId, matchId, reason });
    return data;
  },

  // Admin
  getAll: async (status?: ReportStatus, page = 0, size = 20): Promise<SliceResponse<ReportResponse>> => {
    const { data } = await apiClient.get<SliceResponse<ReportResponse>>(BASE, {
      params: { status, page, size },
    });
    return data;
  },

  getById: async (id: string): Promise<ReportDetailResponse> => {
    const { data } = await apiClient.get<ReportDetailResponse>(`${BASE}/${id}`);
    return data;
  },

  startReview: async (id: string): Promise<ReportResponse> => {
    const { data } = await apiClient.patch<ReportResponse>(`${BASE}/${id}/status`, {
      status: 'REVIEWING',
    });
    return data;
  },

  resolve: async (id: string, note: string): Promise<ReportResponse> => {
    const { data } = await apiClient.post<ReportResponse>(`${BASE}/${id}/resolve`, { note });
    return data;
  },

  reject: async (id: string, note: string): Promise<ReportResponse> => {
    const { data } = await apiClient.post<ReportResponse>(`${BASE}/${id}/reject`, { note });
    return data;
  },
};

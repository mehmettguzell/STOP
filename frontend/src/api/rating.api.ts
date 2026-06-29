import apiClient from './client';

const BASE = '/match';

export interface SubmitRatingRequest {
  ratedId: string;
  skill: number;
  teamwork: number;
  sportsmanship: number;
}

export const ratingApi = {
  submit: async (matchId: string, request: SubmitRatingRequest): Promise<void> => {
    await apiClient.post(`${BASE}/${matchId}/ratings`, request);
  },
};

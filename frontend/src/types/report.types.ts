export type ReportStatus = 'OPEN' | 'REVIEWING' | 'RESOLVED' | 'REJECTED';

export interface ReportResponse {
  id: string;
  reporterId: string;
  targetUserId: string;
  matchId: string;
  reason: string;
  status: ReportStatus;
  createdAt: string;
}

export interface ReportDetailResponse extends ReportResponse {
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolveNote: string | null;
}

// ── Match Enums ──
export type MatchStatus = 'CREATED' | 'OPEN' | 'FULL' | 'STARTED' | 'COMPLETED' | 'CANCELLED' | 'RATINGS_CLOSED';
export type MatchVisibility = 'PUBLIC' | 'PRIVATE';
export type ParticipantStatus = 'JOINED' | 'LEFT' | 'REMOVED';
export type TeamType = 'HOME' | 'AWAY';
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'EXPIRED';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';

// ── Responses ──
export interface MatchResponse {
  id: string;
  title: string;
  description: string | null;
  location: string;
  startTime: string;
  visibility: MatchVisibility;
  status: MatchStatus;
  organizerId: string;
  capacity: number;
  participantCount: number;
  createdAt: string;
}

export interface MatchParticipantResponse {
  participantId: string;
  matchId: string;
  userId: string;
  status: ParticipantStatus;
  team?: TeamType | null;
  positionX?: number | null;
  positionY?: number | null;
}

export interface TeamAssignmentRequest {
  assignments: { userId: string; team: TeamType }[];
}

export interface ParticipationRequestResponse {
  id: string;
  userID: string;
  matchId: string;
  status: RequestStatus;
}

export interface InvitationResponse {
  matchId: string;
  senderId: string;
  receiverId: string;
  status: InvitationStatus;
}

// ── Requests ──
export interface CreateMatchRequest {
  title: string;
  description?: string;
  location: string;
  startTime: string; // ISO datetime
  visibility: MatchVisibility;
  capacity: number;
}

export interface UpdateMatchRequest {
  title?: string;
  description?: string;
  location?: string;
  startTime?: string;
  visibility?: MatchVisibility;
  capacity?: number;
}

export interface TransferOrganizerRequest {
  newOrganizerId: string;
}

export interface CreateInvitationRequest {
  matchId: string;
  receiverId: string;
}

export interface UpdateInvitationRequest {
  matchId: string;
}

// ── Search Params ──
export interface MatchSearchParams {
  title?: string;
  location?: string;
  date?: string; // YYYY-MM-DD
  status?: MatchStatus;
  visibility?: MatchVisibility;
  excludeJoined?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

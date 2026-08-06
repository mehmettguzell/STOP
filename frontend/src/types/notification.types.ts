export type NotificationType =
  | 'MATCH_STARTED'
  | 'MATCH_UPDATED'
  | 'MATCH_CANCELLED'
  | 'MATCH_COMPLETED'
  | 'INVITATION_RECEIVED'
  | 'PARTICIPANT_JOINED'
  | 'PARTICIPANT_LEFT'
  | 'FRIEND_REQUEST_RECEIVED'
  | 'FRIEND_REQUEST_ACCEPTED'
  | 'WAITLIST_PROMOTED';

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  referenceId: string | null;
  isRead: boolean;
  createdAt: string;
}

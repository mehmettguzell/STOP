export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface FriendshipResponse {
  id: string;
  requesterId: string;
  receiverId: string;
  status: FriendshipStatus;
  createdAt: string;
}

export type FriendRelation =
  | { state: 'NONE' }
  | { state: 'PENDING_SENT'; friendshipId: string }
  | { state: 'PENDING_RECEIVED'; friendshipId: string }
  | { state: 'ACCEPTED'; friendshipId: string };

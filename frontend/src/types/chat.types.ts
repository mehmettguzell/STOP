export interface PollOptionResponse {
  id: string;
  text: string;
  position: number;
  voteCount: number;
  voterIds: string[];
}

export interface PollResponse {
  id: string;
  chatId: string;
  creatorId: string;
  question: string;
  options: PollOptionResponse[];
  totalVotes: number;
  createdAt: string;
  closesAt: string | null;
}

export type MessageType = 'TEXT' | 'POLL' | 'POLL_UPDATE';

export interface ChatMessageResponse {
  id: string;
  matchId: string;   // chatId (matchId veya directChatId)
  senderId: string;
  content: string | null;
  sentAt: string;
  deleted: boolean;
  type: MessageType;
  pollId: string | null;
  pollData: PollResponse | null;
}

export interface DirectChatResponse {
  chatId: string;
  otherUserId: string;
}

export type ChatType = 'MATCH' | 'DIRECT';

export interface ChatHistoryResponse {
  messages: ChatMessageResponse[];
  nextCursor: string | null;  // ISO instant — daha fazla yükleme için before parametresi
}

export interface ConversationSummary {
  chatId: string;
  type: ChatType;
  referenceId: string;    // MATCH→matchId, DIRECT→otherUserId
  lastMessage: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
}

import { create } from "zustand";
import { chatApi } from "../api/chat.api";

interface ChatState {
  totalUnread: number;
  refreshUnread: () => Promise<void>;
  decrementUnread: (by?: number) => void;
  clearUnread: () => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  totalUnread: 0,

  refreshUnread: async () => {
    try {
      const count = await chatApi.getTotalUnread();
      set({ totalUnread: count });
    } catch {}
  },

  decrementUnread: (by = 1) => {
    set((s) => ({ totalUnread: Math.max(0, s.totalUnread - by) }));
  },

  clearUnread: () => set({ totalUnread: 0 }),
}));

import { create } from "zustand";
import { UserProfileResponse } from "../types/user.types";

interface ProfileState {
  profile: UserProfileResponse | null;
  hasProfile: boolean;
  loaded: boolean;        // ilk fetch tamamlandı mı
  needsRefresh: boolean;  // create/edit sonrası yeniden çek

  setProfile: (profile: UserProfileResponse) => void;
  setNoProfile: () => void;
  invalidate: () => void;
  reset: () => void;
}

export const useProfileStore = create<ProfileState>((set) => ({
  profile: null,
  hasProfile: false,
  loaded: false,
  needsRefresh: false,

  setProfile: (profile) =>
    set({ profile, hasProfile: true, loaded: true, needsRefresh: false }),

  setNoProfile: () =>
    set({ profile: null, hasProfile: false, loaded: true, needsRefresh: false }),

  invalidate: () => set({ needsRefresh: true }),

  reset: () =>
    set({ profile: null, hasProfile: false, loaded: false, needsRefresh: false }),
}));

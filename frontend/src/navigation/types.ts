import { NativeStackScreenProps } from "@react-navigation/native-stack";

// Auth Stack
export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
  ForgotPassword: undefined;
};

// App Tab — sadeleştirildi
export type AppTabParamList = {
  HomeTab: undefined;
  MatchTab: undefined;
  FriendsTab: undefined;
  MessagesTab: undefined;
  ProfileTab: undefined;
  AdminTab: undefined;
};

// Friends Stack (nested inside FriendsTab)
export type FriendsStackParamList = {
  FriendsList: undefined;
  Search: undefined;
  PlayerProfile: { userId: string; matchId?: string };
  Chat: { chatId: string; title: string; chatType?: "MATCH" | "DIRECT" };
  Notifications: undefined;
};

// Match Stack (nested inside MatchTab)
export type MatchStackParamList = {
  MatchList: undefined;
  MatchDetail: { matchId: string; rated?: boolean };
  CreateMatch: undefined;
  EditMatch: { matchId: string };
  PlayerProfile: { userId: string; matchId?: string };
  Chat: { chatId: string; title: string; chatType?: 'MATCH' | 'DIRECT' };
  Report: { targetUserId: string; targetName: string; matchId: string };
  RateMatch: { matchId: string; matchTitle: string };
  Notifications: undefined;
};

// Messages Stack (nested inside MessagesTab)
export type MessagesStackParamList = {
  ChatList: undefined;
  NewMessage: undefined;
  Search: undefined;
  Chat: { chatId: string; title: string; chatType?: 'MATCH' | 'DIRECT' };
  PlayerProfile: { userId: string };
};

// Profile Stack
export type ProfileStackParamList = {
  ProfileView: { userId?: string };
  CreateProfile: undefined;
  EditProfile: undefined;
  EditAccount: undefined;
  Notifications: undefined;
};

// Admin Stack
export type AdminStackParamList = {
  AdminDashboard: undefined;
  AdminUserDetail: { userId: string };
  AdminReports: undefined;
  AdminReportDetail: { reportId: string };
  AdminMatchDetail: { matchId: string };
  Notifications: undefined;
};

// Screen props helpers
export type LoginScreenProps = NativeStackScreenProps<AuthStackParamList, "Login">;
export type RegisterScreenProps = NativeStackScreenProps<AuthStackParamList, "Register">;
export type MatchListScreenProps = NativeStackScreenProps<MatchStackParamList, "MatchList">;
export type MatchDetailScreenProps = NativeStackScreenProps<MatchStackParamList, "MatchDetail">;
export type CreateMatchScreenProps = NativeStackScreenProps<MatchStackParamList, "CreateMatch">;
export type EditMatchScreenProps = NativeStackScreenProps<MatchStackParamList, "EditMatch">;
export type ProfileViewScreenProps = NativeStackScreenProps<ProfileStackParamList, "ProfileView">;
export type CreateProfileScreenProps = NativeStackScreenProps<ProfileStackParamList, "CreateProfile">;
export type EditProfileScreenProps = NativeStackScreenProps<ProfileStackParamList, "EditProfile">;
export type EditAccountScreenProps = NativeStackScreenProps<ProfileStackParamList, "EditAccount">;
export type SearchScreenProps = NativeStackScreenProps<FriendsStackParamList, "Search">;
export type PlayerProfileScreenProps = NativeStackScreenProps<FriendsStackParamList, "PlayerProfile">;
export type FriendsListScreenProps = NativeStackScreenProps<FriendsStackParamList, "FriendsList">;
export type AdminDashboardScreenProps = NativeStackScreenProps<AdminStackParamList, "AdminDashboard">;
export type AdminUserDetailScreenProps = NativeStackScreenProps<AdminStackParamList, "AdminUserDetail">;
export type ChatListScreenProps = NativeStackScreenProps<MessagesStackParamList, "ChatList">;

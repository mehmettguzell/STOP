export const Colors = {
  // ── Surfaces (deep midnight blue with slight cool tone) ──
  background: "#0A0E17",
  backgroundElevated: "#0F1422",
  surface: "#141A2A",
  surfaceElevated: "#1C2438",
  surfaceHigh: "#252E45",
  surfaceGlass: "rgba(28, 36, 56, 0.72)",
  overlay: "rgba(10, 14, 23, 0.85)",

  // ── Borders ──
  border: "#252E45",
  borderLight: "#374161",
  borderStrong: "#4A5578",
  divider: "rgba(255, 255, 255, 0.06)",

  // ── Primary (refined emerald — softer, not neon) ──
  primary: "#2DD4A5",
  primaryDark: "#14B889",
  primaryLight: "#5EEAB8",
  primaryGlow: "rgba(45, 212, 165, 0.12)",
  primaryGlowStrong: "rgba(45, 212, 165, 0.22)",
  primaryBorder: "rgba(45, 212, 165, 0.35)",

  // ── Accent (refined indigo) ──
  accent: "#6366F1",
  accentLight: "#818CF8",
  accentGlow: "rgba(99, 102, 241, 0.15)",

  // ── Tertiary (warm amber for highlights) ──
  amber: "#F59E0B",
  amberGlow: "rgba(245, 158, 11, 0.15)",

  // ── Status ──
  success: "#2DD4A5",
  successGlow: "rgba(45, 212, 165, 0.15)",
  warning: "#FBBF24",
  warningGlow: "rgba(251, 191, 36, 0.15)",
  error: "#F87171",
  errorGlow: "rgba(248, 113, 113, 0.15)",
  info: "#60A5FA",
  infoGlow: "rgba(96, 165, 250, 0.15)",

  // ── Text ──
  text: "#F8FAFC",
  textSecondary: "#CBD5E1",
  textMuted: "#94A3B8",
  textDim: "#64748B",
  textInverse: "#0A0E17",

  // ── Special ──
  white: "#FFFFFF",
  black: "#000000",
  shimmer: "rgba(255, 255, 255, 0.05)",
};

export const Spacing = {
  xxs: 2,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32,
  huge: 48,
};

export const Radius = {
  xs: 6,
  sm: 10,
  md: 14,
  lg: 18,
  xl: 22,
  xxl: 28,
  pill: 999,
};

export const Typography = {
  // Display
  displayLg: { fontSize: 36, fontWeight: "800" as const, letterSpacing: -1 },
  displayMd: { fontSize: 30, fontWeight: "800" as const, letterSpacing: -0.5 },

  // Headings
  h1: { fontSize: 26, fontWeight: "800" as const, letterSpacing: -0.4 },
  h2: { fontSize: 22, fontWeight: "700" as const, letterSpacing: -0.3 },
  h3: { fontSize: 18, fontWeight: "700" as const, letterSpacing: -0.2 },
  h4: { fontSize: 16, fontWeight: "700" as const, letterSpacing: 0 },

  // Body
  bodyLg: { fontSize: 16, fontWeight: "500" as const },
  body: { fontSize: 14, fontWeight: "500" as const },
  bodySm: { fontSize: 13, fontWeight: "500" as const },

  // Labels / captions
  label: {
    fontSize: 12,
    fontWeight: "700" as const,
    letterSpacing: 0.8,
    textTransform: "uppercase" as const,
  },
  caption: { fontSize: 11, fontWeight: "600" as const, letterSpacing: 0.3 },

  // Numbers / scores
  scoreLg: { fontSize: 38, fontWeight: "900" as const, letterSpacing: -1.5 },
  scoreMd: { fontSize: 28, fontWeight: "800" as const, letterSpacing: -0.8 },
};

export const Shadows = {
  sm: {
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.18,
    shadowRadius: 6,
    elevation: 3,
  },
  md: {
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.25,
    shadowRadius: 14,
    elevation: 6,
  },
  lg: {
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.32,
    shadowRadius: 24,
    elevation: 12,
  },
  glow: (color: string) => ({
    shadowColor: color,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.45,
    shadowRadius: 16,
    elevation: 8,
  }),
};

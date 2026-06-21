import React from "react";
import { View, Text, StyleSheet } from "react-native";
import { Colors, Radius } from "../../theme/colors";

interface ScoreBadgeProps {
  label: string;
  score: number;
  maxScore?: number;
  icon?: string;
}

function getScoreColor(score: number, max: number): string {
  const ratio = score / max;
  if (ratio >= 0.75) return Colors.success;
  if (ratio >= 0.5) return Colors.warning;
  if (ratio >= 0.25) return Colors.amber;
  return Colors.error;
}

export default function ScoreBadge({
  label,
  score,
  maxScore = 10,
  icon,
}: ScoreBadgeProps) {
  const color = getScoreColor(score, maxScore);
  const ratio = Math.min(score / maxScore, 1);
  return (
    <View style={[styles.container, { borderColor: color + "40" }]}>
      {icon && <Text style={styles.icon}>{icon}</Text>}
      <View style={styles.scoreRow}>
        <Text style={[styles.score, { color }]}>{score.toFixed(1)}</Text>
        <Text style={styles.scoreMax}>/{maxScore}</Text>
      </View>
      <View style={styles.progressTrack}>
        <View
          style={[
            styles.progressFill,
            { backgroundColor: color, width: `${ratio * 100}%` },
          ]}
        />
      </View>
      <Text style={styles.label}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    backgroundColor: Colors.surface,
    borderRadius: Radius.lg,
    paddingVertical: 18,
    paddingHorizontal: 16,
    borderWidth: 1,
  },
  icon: {
    fontSize: 18,
    marginBottom: 4,
  },
  scoreRow: {
    flexDirection: "row",
    alignItems: "baseline",
  },
  score: {
    fontSize: 32,
    fontWeight: "900",
    letterSpacing: -1.2,
  },
  scoreMax: {
    fontSize: 14,
    color: Colors.textDim,
    fontWeight: "600",
    marginLeft: 2,
  },
  progressTrack: {
    width: "100%",
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.surfaceHigh,
    marginTop: 10,
    marginBottom: 10,
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: 2,
  },
  label: {
    fontSize: 11,
    color: Colors.textMuted,
    textTransform: "uppercase",
    letterSpacing: 0.8,
    fontWeight: "700",
  },
});

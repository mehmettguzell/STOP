import React, { useRef, useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
  ScrollView,
} from "react-native";
import { participationApi } from "../../api/participant.api";
import { MatchParticipantResponse } from "../../types/match.types";
import { Colors, Radius } from "../../theme/colors";

// ─── Sabit pozisyon şablonları (% cinsinden, 0–100) ─────────────────────────
const HOME_DEFAULTS = [
  { x: 7, y: 50 },
  { x: 22, y: 28 },
  { x: 22, y: 72 },
  { x: 36, y: 20 },
  { x: 36, y: 50 },
  { x: 36, y: 80 },
  { x: 45, y: 50 },
];

const AWAY_DEFAULTS = [
  { x: 93, y: 50 },
  { x: 78, y: 28 },
  { x: 78, y: 72 },
  { x: 64, y: 20 },
  { x: 64, y: 50 },
  { x: 64, y: 80 },
  { x: 55, y: 50 },
];

const HOME_COLOR = Colors.primary;
const HOME_BG = Colors.primaryGlow;
const AWAY_COLOR = Colors.accent;
const AWAY_BG = Colors.accentGlow;
const SELECTED_COLOR = Colors.amber;

// ─── Props ───────────────────────────────────────────────────────────────────
interface Props {
  matchId: string;
  participants: MatchParticipantResponse[];
  displayNames: Record<string, string>;
  isOrganizer: boolean;
  onPositionsUpdated: (updated: MatchParticipantResponse[]) => void;
}

// ─── Token pozisyonu hesapla ─────────────────────────────────────────────────
function getPosition(
  p: MatchParticipantResponse,
  index: number,
): { x: number; y: number } {
  if (p.positionX != null && p.positionY != null) {
    return { x: p.positionX, y: p.positionY };
  }
  const defaults = p.team === "HOME" ? HOME_DEFAULTS : AWAY_DEFAULTS;
  return defaults[Math.min(index, defaults.length - 1)];
}

// ─── Bileşen ─────────────────────────────────────────────────────────────────
export default function FieldVisualization({
  matchId,
  participants,
  displayNames,
  isOrganizer,
  onPositionsUpdated,
}: Props) {
  const [editMode, setEditMode] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [localPositions, setLocalPositions] = useState<
    Record<string, { x: number; y: number }>
  >({});
  const [saving, setSaving] = useState(false);
  const fieldRef = useRef<View>(null);
  const [fieldLayout, setFieldLayout] = useState({ width: 1, height: 1 });

  const homePlayers = participants.filter((p) => p.team === "HOME");
  const awayPlayers = participants.filter((p) => p.team === "AWAY");
  const assignedPlayers = [...homePlayers, ...awayPlayers];

  // Hiç takım ataması yoksa kısmı gösterme
  const hasTeams = assignedPlayers.length > 0;

  function getEffectivePos(p: MatchParticipantResponse, index: number) {
    if (editMode && localPositions[p.userId]) {
      return localPositions[p.userId];
    }
    return getPosition(p, index);
  }

  function handleFieldPress(event: any) {
    if (!editMode || !selectedUserId) return;
    const { locationX, locationY } = event.nativeEvent;
    const x = Math.min(100, Math.max(0, (locationX / fieldLayout.width) * 100));
    const y = Math.min(100, Math.max(0, (locationY / fieldLayout.height) * 100));
    setLocalPositions((prev) => ({ ...prev, [selectedUserId]: { x, y } }));
    setSelectedUserId(null);
  }

  function handlePlayerTokenPress(userId: string) {
    if (!editMode) return;
    setSelectedUserId((prev) => (prev === userId ? null : userId));
  }

  function enterEditMode() {
    // Mevcut pozisyonları yerel kopyaya al
    const initial: Record<string, { x: number; y: number }> = {};
    assignedPlayers.forEach((p, i) => {
      initial[p.userId] = getPosition(p, i);
    });
    setLocalPositions(initial);
    setSelectedUserId(null);
    setEditMode(true);
  }

  function cancelEdit() {
    setLocalPositions({});
    setSelectedUserId(null);
    setEditMode(false);
  }

  async function saveLineup() {
    setSaving(true);
    try {
      const entries = assignedPlayers.map((p, i) => {
        const pos = localPositions[p.userId] ?? getPosition(p, i);
        return { userId: p.userId, positionX: pos.x, positionY: pos.y };
      });
      const updated = await participationApi.updateLineup(matchId, entries);
      onPositionsUpdated(updated);
      setEditMode(false);
      setLocalPositions({});
      setSelectedUserId(null);
    } catch {
      Alert.alert("Hata", "Dizilim kaydedilemedi. Tekrar deneyin.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <View style={s.wrapper}>
      {/* Başlık */}
      <View style={s.header}>
        <Text style={s.title}>⚽ Halısaha Dizilimi</Text>
        {isOrganizer && !editMode && hasTeams && (
          <TouchableOpacity onPress={enterEditMode} activeOpacity={0.75} style={s.editBtn}>
            <Text style={s.editBtnText}>Düzenle</Text>
          </TouchableOpacity>
        )}
        {editMode && (
          <View style={s.editActions}>
            <TouchableOpacity onPress={cancelEdit} activeOpacity={0.75} style={s.cancelBtn}>
              <Text style={s.cancelBtnText}>İptal</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={saveLineup}
              activeOpacity={0.75}
              style={[s.saveBtn, saving && { opacity: 0.6 }]}
              disabled={saving}
            >
              {saving ? (
                <ActivityIndicator size="small" color={Colors.textInverse} />
              ) : (
                <Text style={s.saveBtnText}>Kaydet</Text>
              )}
            </TouchableOpacity>
          </View>
        )}
      </View>

      {/* Edit modu ipucu */}
      {editMode && (
        <Text style={s.hint}>
          {selectedUserId
            ? "Sahaya dokun — oyuncuyu o noktaya koyar"
            : "Aşağıdan bir oyuncu seç, sonra sahaya dokun"}
        </Text>
      )}

      {/* ─── Saha ─────────────────────────────────────────────────── */}
      <View
        ref={fieldRef}
        style={s.field}
        onLayout={(e) => {
          const { width, height } = e.nativeEvent.layout;
          setFieldLayout({ width, height });
        }}
      >
        {/* Saha çizgileri */}
        <FieldLines />

        {/* Oyuncu tokenları — sadece takım atanmışsa */}
        {hasTeams && homePlayers.map((p, i) => {
          const pos = getEffectivePos(p, i);
          const isSelected = selectedUserId === p.userId;
          const name = displayNames[p.userId] ?? "?";
          const initials = name.slice(0, 2).toUpperCase();
          return (
            <PlayerToken
              key={p.userId}
              x={pos.x}
              y={pos.y}
              initials={initials}
              name={name}
              color={isSelected ? SELECTED_COLOR : HOME_COLOR}
              bgColor={isSelected ? Colors.amberGlow : HOME_BG}
              isSelected={isSelected}
              onPress={() => handlePlayerTokenPress(p.userId)}
              editable={editMode}
            />
          );
        })}

        {hasTeams && awayPlayers.map((p, i) => {
          const pos = getEffectivePos(p, i);
          const isSelected = selectedUserId === p.userId;
          const name = displayNames[p.userId] ?? "?";
          const initials = name.slice(0, 2).toUpperCase();
          return (
            <PlayerToken
              key={p.userId}
              x={pos.x}
              y={pos.y}
              initials={initials}
              name={name}
              color={isSelected ? SELECTED_COLOR : AWAY_COLOR}
              bgColor={isSelected ? Colors.amberGlow : AWAY_BG}
              isSelected={isSelected}
              onPress={() => handlePlayerTokenPress(p.userId)}
              editable={editMode}
            />
          );
        })}

        {/* Takım ataması yoksa overlay mesajı */}
        {!hasTeams && (
          <View style={s.noTeamsOverlay}>
            <Text style={s.noTeamsText}>Takım ataması yapılmadı</Text>
            <Text style={s.noTeamsSubtext}>
              Takımlar atandıktan sonra{"\n"}oyuncular burada görünür
            </Text>
          </View>
        )}

        {/* Şeffaf overlay — sadece oyuncu seçiliyken aktif, sahaya tıklama için */}
        {editMode && selectedUserId && (
          <TouchableOpacity
            style={s.fieldOverlay}
            onPress={handleFieldPress}
            activeOpacity={1}
          />
        )}
      </View>

      {/* Takım renk açıklaması */}
      {hasTeams && (
        <View style={s.legend}>
          <View style={s.legendItem}>
            <View style={[s.legendDot, { backgroundColor: HOME_COLOR }]} />
            <Text style={s.legendText}>Ev Sahibi ({homePlayers.length})</Text>
          </View>
          <View style={s.legendItem}>
            <View style={[s.legendDot, { backgroundColor: AWAY_COLOR }]} />
            <Text style={s.legendText}>Deplasman ({awayPlayers.length})</Text>
          </View>
        </View>
      )}

      {/* Edit modunda oyuncu listesi */}
      {editMode && hasTeams && (
        <View style={s.playerPicker}>
          <Text style={s.pickerLabel}>Konumlandırılacak oyuncuyu seç:</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.pickerScroll}>
            {homePlayers.map((p) => {
              const name = displayNames[p.userId] ?? "?";
              const isSelected = selectedUserId === p.userId;
              return (
                <TouchableOpacity
                  key={p.userId}
                  style={[
                    s.pickerChip,
                    { borderColor: isSelected ? SELECTED_COLOR : HOME_COLOR },
                    isSelected && { backgroundColor: Colors.amberGlow },
                  ]}
                  onPress={() => handlePlayerTokenPress(p.userId)}
                  activeOpacity={0.75}
                >
                  <View style={[s.chipDot, { backgroundColor: isSelected ? SELECTED_COLOR : HOME_COLOR }]} />
                  <Text style={[s.chipName, isSelected && { color: SELECTED_COLOR }]} numberOfLines={1}>
                    {name}
                  </Text>
                </TouchableOpacity>
              );
            })}
            {awayPlayers.map((p) => {
              const name = displayNames[p.userId] ?? "?";
              const isSelected = selectedUserId === p.userId;
              return (
                <TouchableOpacity
                  key={p.userId}
                  style={[
                    s.pickerChip,
                    { borderColor: isSelected ? SELECTED_COLOR : AWAY_COLOR },
                    isSelected && { backgroundColor: Colors.amberGlow },
                  ]}
                  onPress={() => handlePlayerTokenPress(p.userId)}
                  activeOpacity={0.75}
                >
                  <View style={[s.chipDot, { backgroundColor: isSelected ? SELECTED_COLOR : AWAY_COLOR }]} />
                  <Text style={[s.chipName, isSelected && { color: SELECTED_COLOR }]} numberOfLines={1}>
                    {name}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>
        </View>
      )}
    </View>
  );
}

// ─── Saha çizgileri ──────────────────────────────────────────────────────────
function FieldLines() {
  return (
    <>
      {/* Orta çizgi */}
      <View style={fl.centerLine} />
      {/* Orta daire */}
      <View style={fl.centerCircle} />
      {/* Sol kale alanı */}
      <View style={fl.leftBox} />
      {/* Sağ kale alanı */}
      <View style={fl.rightBox} />
      {/* Sol kale */}
      <View style={fl.leftGoal} />
      {/* Sağ kale */}
      <View style={fl.rightGoal} />
      {/* Ev - Dep etiketleri */}
      <Text style={fl.homeLabel}>EV</Text>
      <Text style={fl.awayLabel}>DEP</Text>
    </>
  );
}

// ─── Oyuncu tokeni ───────────────────────────────────────────────────────────
interface TokenProps {
  x: number;
  y: number;
  initials: string;
  name: string;
  color: string;
  bgColor: string;
  isSelected: boolean;
  onPress: () => void;
  editable: boolean;
}

function PlayerToken({ x, y, initials, name, color, bgColor, isSelected, onPress, editable }: TokenProps) {
  const TOKEN_SIZE = 32;
  const LABEL_WIDTH = 52;

  return (
    <View
      pointerEvents={editable ? "box-none" : "none"}
      style={[
        pt.container,
        {
          left: `${x}%` as any,
          top: `${y}%` as any,
          marginLeft: -TOKEN_SIZE / 2,
          marginTop: -TOKEN_SIZE / 2,
        },
      ]}
    >
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={editable ? 0.7 : 1}
        style={[
          pt.token,
          {
            width: TOKEN_SIZE,
            height: TOKEN_SIZE,
            borderRadius: TOKEN_SIZE / 2,
            backgroundColor: bgColor,
            borderColor: color,
            borderWidth: isSelected ? 2.5 : 1.5,
          },
        ]}
      >
        <Text style={[pt.initials, { color }]}>{initials}</Text>
      </TouchableOpacity>
      <Text
        style={[pt.label, { color, width: LABEL_WIDTH, marginLeft: -(LABEL_WIDTH - TOKEN_SIZE) / 2 }]}
        numberOfLines={1}
      >
        {name}
      </Text>
    </View>
  );
}

// ─── Styles ──────────────────────────────────────────────────────────────────
const s = StyleSheet.create({
  wrapper: {
    backgroundColor: Colors.surface,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 16,
    marginTop: 20,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: "700",
    color: Colors.text,
  },
  editBtn: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: Radius.sm,
    backgroundColor: Colors.primaryGlow,
    borderWidth: 1,
    borderColor: Colors.primaryBorder,
  },
  editBtnText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.primary,
  },
  editActions: {
    flexDirection: "row",
    gap: 8,
  },
  cancelBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radius.sm,
    backgroundColor: Colors.surfaceHigh,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cancelBtnText: {
    fontSize: 13,
    fontWeight: "600",
    color: Colors.textMuted,
  },
  saveBtn: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: Radius.sm,
    backgroundColor: Colors.primary,
    minWidth: 64,
    alignItems: "center",
  },
  saveBtnText: {
    fontSize: 13,
    fontWeight: "700",
    color: Colors.textInverse,
  },
  hint: {
    fontSize: 12,
    color: Colors.amber,
    marginBottom: 10,
    fontWeight: "500",
    textAlign: "center",
  },
  noTeamsOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.55)",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: Radius.sm,
  },
  noTeamsText: {
    fontSize: 15,
    fontWeight: "700",
    color: "rgba(255,255,255,0.9)",
    textAlign: "center",
  },
  noTeamsSubtext: {
    fontSize: 12,
    color: "rgba(255,255,255,0.65)",
    marginTop: 6,
    textAlign: "center",
    lineHeight: 18,
  },
  // Saha
  field: {
    width: "100%",
    aspectRatio: 1.65,
    backgroundColor: "#1a4a2a",
    borderRadius: Radius.sm,
    borderWidth: 2,
    borderColor: "rgba(255,255,255,0.35)",
    overflow: "hidden",
    position: "relative",
  },
  fieldOverlay: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 10,
  },
  // Açıklama
  legend: {
    flexDirection: "row",
    justifyContent: "center",
    gap: 20,
    marginTop: 10,
  },
  legendItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  legendText: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: "500",
  },
  // Oyuncu seçici (edit modunda)
  playerPicker: {
    marginTop: 14,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  pickerLabel: {
    fontSize: 12,
    color: Colors.textMuted,
    fontWeight: "600",
    marginBottom: 8,
    letterSpacing: 0.3,
  },
  pickerScroll: {
    flexDirection: "row",
  },
  pickerChip: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Radius.pill,
    borderWidth: 1.5,
    backgroundColor: Colors.surfaceElevated,
    marginRight: 8,
    gap: 5,
    maxWidth: 130,
  },
  chipDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  chipName: {
    fontSize: 12,
    fontWeight: "600",
    color: Colors.text,
    flexShrink: 1,
  },
});

const fl = StyleSheet.create({
  centerLine: {
    position: "absolute",
    left: "50%",
    top: 0,
    bottom: 0,
    width: 1.5,
    backgroundColor: "rgba(255,255,255,0.35)",
  },
  centerCircle: {
    position: "absolute",
    left: "50%",
    top: "50%",
    width: 56,
    height: 56,
    marginLeft: -28,
    marginTop: -28,
    borderRadius: 28,
    borderWidth: 1.5,
    borderColor: "rgba(255,255,255,0.35)",
  },
  leftBox: {
    position: "absolute",
    left: 0,
    top: "25%",
    width: "18%",
    height: "50%",
    borderWidth: 1.5,
    borderLeftWidth: 0,
    borderColor: "rgba(255,255,255,0.35)",
  },
  rightBox: {
    position: "absolute",
    right: 0,
    top: "25%",
    width: "18%",
    height: "50%",
    borderWidth: 1.5,
    borderRightWidth: 0,
    borderColor: "rgba(255,255,255,0.35)",
  },
  leftGoal: {
    position: "absolute",
    left: -5,
    top: "37%",
    width: 7,
    height: "26%",
    backgroundColor: "rgba(255,255,255,0.20)",
    borderWidth: 1.5,
    borderColor: "rgba(255,255,255,0.5)",
  },
  rightGoal: {
    position: "absolute",
    right: -5,
    top: "37%",
    width: 7,
    height: "26%",
    backgroundColor: "rgba(255,255,255,0.20)",
    borderWidth: 1.5,
    borderColor: "rgba(255,255,255,0.5)",
  },
  homeLabel: {
    position: "absolute",
    left: 6,
    top: 4,
    fontSize: 9,
    fontWeight: "800",
    color: "rgba(45,212,165,0.6)",
    letterSpacing: 1,
  },
  awayLabel: {
    position: "absolute",
    right: 6,
    top: 4,
    fontSize: 9,
    fontWeight: "800",
    color: "rgba(99,102,241,0.6)",
    letterSpacing: 1,
  },
});

const pt = StyleSheet.create({
  container: {
    position: "absolute",
    alignItems: "center",
    zIndex: 5,
  },
  token: {
    alignItems: "center",
    justifyContent: "center",
  },
  initials: {
    fontSize: 11,
    fontWeight: "800",
  },
  label: {
    fontSize: 9,
    fontWeight: "700",
    textAlign: "center",
    marginTop: 2,
    textShadowColor: "rgba(0,0,0,0.8)",
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 3,
  },
});

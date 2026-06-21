import React, { useRef, useEffect, useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  FlatList,
  TouchableOpacity,
  NativeSyntheticEvent,
  NativeScrollEvent,
} from 'react-native';
import { Colors } from '../../theme/colors';

const ITEM_HEIGHT = 48;
const VISIBLE = 5;
const PICKER_HEIGHT = ITEM_HEIGHT * VISIBLE;
const PAD = ITEM_HEIGHT * 2; // padding top/bottom so first/last items center

const MONTHS = [
  'Ocak', 'Subat', 'Mart', 'Nisan', 'Mayis', 'Haziran',
  'Temmuz', 'Agustos', 'Eylul', 'Ekim', 'Kasim', 'Aralik',
];

const CURRENT_YEAR = new Date().getFullYear();
const DEFAULT_YEAR_MIN = 1930;
const DEFAULT_YEAR_MAX = CURRENT_YEAR - 5;

function getDaysInMonth(month: number, year: number): number {
  return new Date(year, month, 0).getDate();
}

function formatDisplay(dateStr: string): string {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length !== 3) return dateStr;
  const y = parseInt(parts[0], 10);
  const m = parseInt(parts[1], 10);
  const d = parseInt(parts[2], 10);
  if (!y || !m || !d) return dateStr;
  return `${d} ${MONTHS[m - 1]} ${y}`;
}

// ─── Wheel column ─────────────────────────────────────────────────────────────

interface WheelColumnProps {
  items: (string | number)[];
  selectedIndex: number;
  onIndexChange: (i: number) => void;
  width?: number;
}

function WheelColumn({ items, selectedIndex, onIndexChange, width }: WheelColumnProps) {
  const listRef = useRef<FlatList>(null);
  const isMounted = useRef(false);

  // Scroll to initial position once on mount
  useEffect(() => {
    const timer = setTimeout(() => {
      listRef.current?.scrollToOffset({
        offset: selectedIndex * ITEM_HEIGHT,
        animated: false,
      });
      isMounted.current = true;
    }, 50);
    return () => clearTimeout(timer);
  }, []); // eslint-disable-line

  // Sync when selectedIndex changes externally (e.g. day clamping)
  useEffect(() => {
    if (!isMounted.current) return;
    listRef.current?.scrollToOffset({
      offset: selectedIndex * ITEM_HEIGHT,
      animated: true,
    });
  }, [selectedIndex]);

  const onScrollEnd = useCallback(
    (e: NativeSyntheticEvent<NativeScrollEvent>) => {
      const raw = e.nativeEvent.contentOffset.y;
      const idx = Math.max(0, Math.min(Math.round(raw / ITEM_HEIGHT), items.length - 1));
      onIndexChange(idx);
    },
    [items.length, onIndexChange],
  );

  return (
    <View style={[styles.column, width ? { width } : { flex: 1 }]}>
      <FlatList
        ref={listRef}
        data={items}
        keyExtractor={(_, i) => String(i)}
        showsVerticalScrollIndicator={false}
        snapToInterval={ITEM_HEIGHT}
        snapToAlignment="start"
        decelerationRate="fast"
        onMomentumScrollEnd={onScrollEnd}
        contentContainerStyle={{ paddingVertical: PAD }}
        getItemLayout={(_, index) => ({
          length: ITEM_HEIGHT,
          offset: ITEM_HEIGHT * index,
          index,
        })}
        renderItem={({ item, index }) => {
          const selected = index === selectedIndex;
          return (
            <TouchableOpacity
              style={styles.wheelItem}
              onPress={() => {
                onIndexChange(index);
                listRef.current?.scrollToOffset({ offset: index * ITEM_HEIGHT, animated: true });
              }}
              activeOpacity={0.7}
            >
              <Text style={[styles.wheelItemText, selected && styles.wheelItemSelected]}>
                {item}
              </Text>
            </TouchableOpacity>
          );
        }}
      />
    </View>
  );
}

// ─── DatePicker ────────────────────────────────────────────────────────────────

interface DatePickerProps {
  value: string; // YYYY-MM-DD
  onChange: (date: string) => void;
  label?: string;
  error?: string;
  required?: boolean;
  yearMin?: number;
  yearMax?: number;
}

export default function DatePicker({ value, onChange, label, error, required, yearMin = DEFAULT_YEAR_MIN, yearMax = DEFAULT_YEAR_MAX }: DatePickerProps) {
  const YEARS = Array.from({ length: yearMax - yearMin + 1 }, (_, i) => yearMin + i);
  const [visible, setVisible] = useState(false);

  // Temp state inside modal
  const parseValue = () => {
    if (value) {
      const parts = value.split('-');
      const y = parseInt(parts[0], 10);
      const m = parseInt(parts[1], 10);
      const d = parseInt(parts[2], 10);
      if (y && m && d) return { y, m, d };
    }
    const today = new Date();
    const todayYear = today.getFullYear();
    const defaultYear = YEARS.includes(todayYear) ? todayYear : yearMin;
    return { y: defaultYear, m: today.getMonth() + 1, d: today.getDate() };
  };

  const [tempYear, setTempYear] = useState(parseValue().y);
  const [tempMonth, setTempMonth] = useState(parseValue().m);
  const [tempDay, setTempDay] = useState(parseValue().d);

  const openModal = () => {
    const { y, m, d } = parseValue();
    setTempYear(y);
    setTempMonth(m);
    setTempDay(d);
    setVisible(true);
  };

  const days = Array.from(
    { length: getDaysInMonth(tempMonth, tempYear) },
    (_, i) => i + 1,
  );

  // Keep day in bounds when month/year changes
  useEffect(() => {
    const maxDay = getDaysInMonth(tempMonth, tempYear);
    if (tempDay > maxDay) setTempDay(maxDay);
  }, [tempMonth, tempYear, tempDay]);

  const yearIndex = YEARS.indexOf(tempYear);
  const monthIndex = tempMonth - 1;
  const dayIndex = tempDay - 1;

  const confirm = () => {
    const clampedDay = Math.min(tempDay, getDaysInMonth(tempMonth, tempYear));
    const dateStr = `${tempYear}-${String(tempMonth).padStart(2, '0')}-${String(clampedDay).padStart(2, '0')}`;
    onChange(dateStr);
    setVisible(false);
  };

  const displayText = value ? formatDisplay(value) : null;
  const hasError = !!error;

  return (
    <View style={styles.container}>
      {label && (
        <Text style={styles.label}>
          {label.toUpperCase()}
          {required && <Text style={styles.required}> *</Text>}
        </Text>
      )}

      <TouchableOpacity
        style={[styles.trigger, hasError && styles.triggerError]}
        onPress={openModal}
        activeOpacity={0.7}
      >
        <Text style={[styles.triggerText, !displayText && styles.triggerPlaceholder]}>
          {displayText || 'Tarih secin'}
        </Text>
        <Text style={styles.calendarIcon}>📅</Text>
      </TouchableOpacity>

      {hasError && <Text style={styles.errorText}>{error}</Text>}

      <Modal visible={visible} transparent animationType="slide" onRequestClose={() => setVisible(false)}>
        <TouchableOpacity style={styles.overlay} activeOpacity={1} onPress={() => setVisible(false)} />
        <View style={styles.sheet}>
          <View style={styles.sheetHeader}>
            <TouchableOpacity onPress={() => setVisible(false)} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
              <Text style={styles.cancelBtn}>Iptal</Text>
            </TouchableOpacity>
            <Text style={styles.sheetTitle}>Tarih</Text>
            <TouchableOpacity onPress={confirm} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
              <Text style={styles.confirmBtn}>Tamam</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.wheelsContainer}>
            {/* Day */}
            <WheelColumn
              items={days}
              selectedIndex={Math.max(0, Math.min(dayIndex, days.length - 1))}
              onIndexChange={(i) => setTempDay(i + 1)}
              width={60}
            />

            {/* Month */}
            <WheelColumn
              items={MONTHS}
              selectedIndex={monthIndex}
              onIndexChange={(i) => setTempMonth(i + 1)}
            />

            {/* Year */}
            <WheelColumn
              items={YEARS}
              selectedIndex={Math.max(0, yearIndex)}
              onIndexChange={(i) => setTempYear(YEARS[i])}
              width={72}
            />

            {/* Selection highlight */}
            <View style={styles.selectionHighlight} pointerEvents="none" />
          </View>
        </View>
      </Modal>
    </View>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  container: { marginBottom: 16 },
  label: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textMuted,
    marginBottom: 6,
    letterSpacing: 0.8,
  },
  required: { color: Colors.error },

  trigger: {
    height: 50,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  triggerError: { borderColor: Colors.error },
  triggerText: { fontSize: 15, color: Colors.text, fontWeight: '500' },
  triggerPlaceholder: { color: Colors.textMuted, fontWeight: '400' },
  calendarIcon: { fontSize: 16 },
  errorText: {
    fontSize: 12,
    color: Colors.error,
    marginTop: 4,
    marginLeft: 2,
    fontWeight: '500',
  },

  // Modal
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  sheet: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingBottom: 32,
  },
  sheetHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  sheetTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.text,
  },
  cancelBtn: {
    fontSize: 15,
    color: Colors.textMuted,
    fontWeight: '600',
  },
  confirmBtn: {
    fontSize: 15,
    color: Colors.primary,
    fontWeight: '700',
  },

  // Wheels
  wheelsContainer: {
    flexDirection: 'row',
    height: PICKER_HEIGHT,
    marginHorizontal: 16,
    marginTop: 8,
    overflow: 'hidden',
  },
  column: {
    overflow: 'hidden',
  },
  wheelItem: {
    height: ITEM_HEIGHT,
    justifyContent: 'center',
    alignItems: 'center',
  },
  wheelItemText: {
    fontSize: 16,
    color: Colors.textMuted,
    fontWeight: '500',
  },
  wheelItemSelected: {
    color: Colors.text,
    fontSize: 18,
    fontWeight: '700',
  },
  selectionHighlight: {
    position: 'absolute',
    top: ITEM_HEIGHT * 2,
    left: 0,
    right: 0,
    height: ITEM_HEIGHT,
    borderTopWidth: 1.5,
    borderBottomWidth: 1.5,
    borderColor: Colors.primary + '66',
    borderRadius: 8,
    backgroundColor: Colors.primaryGlow,
  },
});

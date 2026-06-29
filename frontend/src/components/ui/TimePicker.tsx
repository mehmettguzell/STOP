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
const PAD = ITEM_HEIGHT * 2;

const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTES = Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0'));

// ─── Wheel column ─────────────────────────────────────────────────────────────

interface WheelColumnProps {
  items: string[];
  selectedIndex: number;
  onIndexChange: (i: number) => void;
}

function WheelColumn({ items, selectedIndex, onIndexChange }: WheelColumnProps) {
  const listRef = useRef<FlatList>(null);
  const isMounted = useRef(false);

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
    <View style={styles.column}>
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

// ─── TimePicker ───────────────────────────────────────────────────────────────

interface TimePickerProps {
  value: string; // HH:MM
  onChange: (time: string) => void;
  label?: string;
  error?: string;
  required?: boolean;
}

export default function TimePicker({ value, onChange, label, error, required }: TimePickerProps) {
  const [visible, setVisible] = useState(false);

  const parseValue = () => {
    if (value) {
      const parts = value.split(':');
      const h = (parts[0] ?? '09').padStart(2, '0');
      const rawMin = parseInt(parts[1] ?? '0', 10);
      const roundedMin = Math.round(rawMin / 5) * 5 % 60;
      const hIdx = HOURS.indexOf(h);
      const mIdx = MINUTES.indexOf(String(roundedMin).padStart(2, '0'));
      return {
        hIdx: hIdx >= 0 ? hIdx : 9,
        mIdx: mIdx >= 0 ? mIdx : 0,
      };
    }
    return { hIdx: 9, mIdx: 0 };
  };

  const [tempHour, setTempHour] = useState(parseValue().hIdx);
  const [tempMin, setTempMin] = useState(parseValue().mIdx);

  const openModal = () => {
    const { hIdx, mIdx } = parseValue();
    setTempHour(hIdx);
    setTempMin(mIdx);
    setVisible(true);
  };

  const confirm = () => {
    onChange(`${HOURS[tempHour]}:${MINUTES[tempMin]}`);
    setVisible(false);
  };

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
        <Text style={[styles.triggerText, !value && styles.triggerPlaceholder]}>
          {value || 'Saat secin'}
        </Text>
        <Text style={styles.clockIcon}>🕐</Text>
      </TouchableOpacity>

      {hasError && <Text style={styles.errorText}>{error}</Text>}

      <Modal
        visible={visible}
        transparent
        animationType="slide"
        onRequestClose={() => setVisible(false)}
      >
        <TouchableOpacity
          style={styles.overlay}
          activeOpacity={1}
          onPress={() => setVisible(false)}
        />
        <View style={styles.sheet}>
          <View style={styles.sheetHeader}>
            <TouchableOpacity
              onPress={() => setVisible(false)}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.cancelBtn}>Iptal</Text>
            </TouchableOpacity>
            <Text style={styles.sheetTitle}>Saat</Text>
            <TouchableOpacity
              onPress={confirm}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Text style={styles.confirmBtn}>Tamam</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.wheelsContainer}>
            <WheelColumn
              items={HOURS}
              selectedIndex={tempHour}
              onIndexChange={setTempHour}
            />
            <View style={styles.separator}>
              <Text style={styles.separatorText}>:</Text>
            </View>
            <WheelColumn
              items={MINUTES}
              selectedIndex={tempMin}
              onIndexChange={setTempMin}
            />
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
  clockIcon: { fontSize: 16 },
  errorText: {
    fontSize: 12,
    color: Colors.error,
    marginTop: 4,
    marginLeft: 2,
    fontWeight: '500',
  },
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
  sheetTitle: { fontSize: 16, fontWeight: '700', color: Colors.text },
  cancelBtn: { fontSize: 15, color: Colors.textMuted, fontWeight: '600' },
  confirmBtn: { fontSize: 15, color: Colors.primary, fontWeight: '700' },
  wheelsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    height: PICKER_HEIGHT,
    marginHorizontal: 48,
    marginTop: 8,
    overflow: 'hidden',
  },
  column: { flex: 1, overflow: 'hidden' },
  separator: {
    width: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  separatorText: { fontSize: 24, fontWeight: '800', color: Colors.text },
  wheelItem: {
    height: ITEM_HEIGHT,
    justifyContent: 'center',
    alignItems: 'center',
  },
  wheelItemText: { fontSize: 20, color: Colors.textMuted, fontWeight: '500' },
  wheelItemSelected: { color: Colors.text, fontSize: 26, fontWeight: '700' },
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

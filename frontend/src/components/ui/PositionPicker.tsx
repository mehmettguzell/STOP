import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  Modal,
} from 'react-native';
import { Colors, Radius } from '../../theme/colors';
import { POSITIONS } from '../../constants/positions';

interface PositionPickerProps {
  value: string;
  onSelect: (position: string) => void;
  error?: string;
}

export default function PositionPicker({ value, onSelect, error }: PositionPickerProps) {
  const [visible, setVisible] = useState(false);

  const handleSelect = (position: string) => {
    onSelect(position);
    setVisible(false);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Pozisyon</Text>
      <TouchableOpacity
        style={[styles.selector, error ? styles.selectorError : null]}
        onPress={() => setVisible(true)}
        activeOpacity={0.85}
      >
        <Text style={[styles.selectorText, !value && styles.placeholder]}>
          {value || 'Pozisyon sec'}
        </Text>
        <Text style={styles.arrow}>›</Text>
      </TouchableOpacity>
      {error && <Text style={styles.errorText}>{error}</Text>}

      <Modal visible={visible} animationType="slide" transparent>
        <View style={styles.overlay}>
          <View style={styles.modal}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Pozisyon Sec</Text>
              <TouchableOpacity onPress={() => setVisible(false)}>
                <Text style={styles.closeBtn}>Kapat</Text>
              </TouchableOpacity>
            </View>

            <FlatList
              data={POSITIONS}
              keyExtractor={(item) => item}
              ListHeaderComponent={
                <TouchableOpacity
                  style={[
                    styles.positionItem,
                    !value && styles.positionItemSelected,
                  ]}
                  onPress={() => handleSelect('')}
                >
                  <Text
                    style={[
                      styles.positionText,
                      !value && styles.positionTextSelected,
                    ]}
                  >
                    Tumu
                  </Text>
                  {!value && <Text style={styles.check}>✓</Text>}
                </TouchableOpacity>
              }
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[
                    styles.positionItem,
                    item === value && styles.positionItemSelected,
                  ]}
                  onPress={() => handleSelect(item)}
                >
                  <Text
                    style={[
                      styles.positionText,
                      item === value && styles.positionTextSelected,
                    ]}
                  >
                    {item}
                  </Text>
                  {item === value && <Text style={styles.check}>✓</Text>}
                </TouchableOpacity>
              )}
            />
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textMuted,
    marginBottom: 8,
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  selector: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1.5,
    borderColor: Colors.border,
    paddingVertical: 14,
    paddingHorizontal: 16,
    gap: 10,
  },
  selectorError: {
    borderColor: Colors.error,
    backgroundColor: Colors.errorGlow,
  },
  selectorText: {
    fontSize: 15,
    color: Colors.text,
    flex: 1,
    fontWeight: '500',
  },
  placeholder: {
    color: Colors.textDim,
  },
  arrow: {
    fontSize: 22,
    color: Colors.textDim,
    fontWeight: '300',
  },
  errorText: {
    fontSize: 12,
    color: Colors.error,
    marginTop: 6,
    marginLeft: 4,
    fontWeight: '500',
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  modal: {
    backgroundColor: Colors.backgroundElevated,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    maxHeight: '80%',
    paddingBottom: 32,
    borderTopWidth: 1,
    borderColor: Colors.border,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 16,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: Colors.text,
    letterSpacing: -0.3,
  },
  closeBtn: {
    fontSize: 14,
    color: Colors.primary,
    fontWeight: '700',
  },
  positionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  positionItemSelected: {
    backgroundColor: Colors.primaryGlow,
  },
  positionText: {
    fontSize: 15,
    color: Colors.text,
    fontWeight: '500',
  },
  positionTextSelected: {
    color: Colors.primary,
    fontWeight: '700',
  },
  check: {
    fontSize: 16,
    color: Colors.primary,
    fontWeight: '700',
  },
});

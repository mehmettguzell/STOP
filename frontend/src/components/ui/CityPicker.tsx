import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  FlatList,
  Modal,
} from 'react-native';
import { Colors, Radius } from '../../theme/colors';
import { TURKEY_CITIES } from '../../constants/cities';

interface CityPickerProps {
  value: string;
  onSelect: (city: string) => void;
  error?: string;
}

export default function CityPicker({ value, onSelect, error }: CityPickerProps) {
  const [visible, setVisible] = useState(false);
  const [search, setSearch] = useState('');

  const filtered = search
    ? TURKEY_CITIES.filter((c) =>
        c.toLowerCase().includes(search.toLowerCase())
      )
    : TURKEY_CITIES;

  const handleSelect = (city: string) => {
    onSelect(city);
    setVisible(false);
    setSearch('');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Sehir</Text>
      <TouchableOpacity
        style={[styles.selector, error ? styles.selectorError : null]}
        onPress={() => setVisible(true)}
        activeOpacity={0.85}
      >
        <Text style={styles.locationIcon}>◉</Text>
        <Text style={[styles.selectorText, !value && styles.placeholder]}>
          {value || 'Sehir sec'}
        </Text>
        <Text style={styles.arrow}>›</Text>
      </TouchableOpacity>
      {error && <Text style={styles.errorText}>{error}</Text>}

      <Modal visible={visible} animationType="slide" transparent>
        <View style={styles.overlay}>
          <View style={styles.modal}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Sehir Sec</Text>
              <TouchableOpacity onPress={() => { setVisible(false); setSearch(''); }}>
                <Text style={styles.closeBtn}>Kapat</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.searchWrapper}>
              <TextInput
                style={styles.searchInput}
                placeholder="Sehir ara..."
                placeholderTextColor={Colors.textMuted}
                value={search}
                onChangeText={setSearch}
                autoFocus
              />
            </View>

            <FlatList
              data={filtered}
              keyExtractor={(item) => item}
              keyboardShouldPersistTaps="handled"
              ListHeaderComponent={
                !search ? (
                  <TouchableOpacity
                    style={[
                      styles.cityItem,
                      !value && styles.cityItemSelected,
                    ]}
                    onPress={() => handleSelect('')}
                  >
                    <Text
                      style={[
                        styles.cityText,
                        !value && styles.cityTextSelected,
                      ]}
                    >
                      Tumu
                    </Text>
                    {!value && <Text style={styles.check}>✓</Text>}
                  </TouchableOpacity>
                ) : null
              }
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[
                    styles.cityItem,
                    item === value && styles.cityItemSelected,
                  ]}
                  onPress={() => handleSelect(item)}
                >
                  <Text
                    style={[
                      styles.cityText,
                      item === value && styles.cityTextSelected,
                    ]}
                  >
                    {item}
                  </Text>
                  {item === value && <Text style={styles.check}>✓</Text>}
                </TouchableOpacity>
              )}
              ListEmptyComponent={
                <View style={styles.empty}>
                  <Text style={styles.emptyText}>Sonuc bulunamadi</Text>
                </View>
              }
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
  locationIcon: {
    fontSize: 14,
    color: Colors.textMuted,
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
  searchWrapper: {
    paddingHorizontal: 24,
    paddingBottom: 12,
  },
  searchInput: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: Radius.md,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 15,
    color: Colors.text,
    fontWeight: '500',
  },
  cityItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  cityItemSelected: {
    backgroundColor: Colors.primaryGlow,
  },
  cityText: {
    fontSize: 15,
    color: Colors.text,
    fontWeight: '500',
  },
  cityTextSelected: {
    color: Colors.primary,
    fontWeight: '700',
  },
  check: {
    fontSize: 16,
    color: Colors.primary,
    fontWeight: '700',
  },
  empty: {
    paddingVertical: 32,
    alignItems: 'center',
  },
  emptyText: {
    color: Colors.textMuted,
    fontSize: 14,
  },
});

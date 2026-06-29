import React from 'react';
import { TouchableOpacity, View, StyleSheet } from 'react-native';
import { Colors } from '../../theme/colors';

interface BackButtonProps {
  onPress: () => void;
  color?: string;
}

export default function BackButton({ onPress, color = Colors.text }: BackButtonProps) {
  return (
    <TouchableOpacity
      onPress={onPress}
      style={styles.wrapper}
      activeOpacity={0.6}
      hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
    >
      <View style={[styles.chevron, { borderColor: color }]} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 4,
  },
  chevron: {
    width: 11,
    height: 11,
    borderLeftWidth: 2.5,
    borderBottomWidth: 2.5,
    borderColor: Colors.text,
    transform: [{ rotate: '45deg' }],
    marginLeft: 4,
  },
});

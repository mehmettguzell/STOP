import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Modal,
  Image,
  ActivityIndicator,
  Alert,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { profileApi } from '../../api/profile.api';
import { Colors, Radius } from '../../theme/colors';
import { getErrorMessage } from '../../utils/error';

interface AvatarPickerProps {
  avatarUrl: string | null;
  displayName: string;
  size?: number;
  onChange: (avatarUrl: string | null) => void;
}

export default function AvatarPicker({
  avatarUrl,
  displayName,
  size = 96,
  onChange,
}: AvatarPickerProps) {
  const [visible, setVisible] = useState(false);
  const [uploading, setUploading] = useState(false);

  const hasPhoto = !!avatarUrl;
  const initial = (displayName || '?').charAt(0).toUpperCase();

  const upload = async (uri: string) => {
    setVisible(false);
    setUploading(true);
    try {
      const profile = await profileApi.uploadAvatar(uri);
      onChange(profile.avatarUrl);
    } catch (err) {
      Alert.alert('Yükleme Başarısız', getErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const pickFromLibrary = async () => {
    const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!perm.granted) {
      setVisible(false);
      Alert.alert('İzin Gerekli', 'Fotoğraf seçebilmek için galeri erişimine izin vermelisin.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.9,
    });
    if (!result.canceled && result.assets[0]) {
      await upload(result.assets[0].uri);
    } else {
      setVisible(false);
    }
  };

  const takePhoto = async () => {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (!perm.granted) {
      setVisible(false);
      Alert.alert('İzin Gerekli', 'Fotoğraf çekebilmek için kamera erişimine izin vermelisin.');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ['images'],
      quality: 0.9,
    });
    if (!result.canceled && result.assets[0]) {
      await upload(result.assets[0].uri);
    } else {
      setVisible(false);
    }
  };

  const confirmDelete = () => {
    setVisible(false);
    Alert.alert(
      'Fotoğrafı Sil',
      'Fotoğrafını silmek istediğine emin misin?',
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Sil',
          style: 'destructive',
          onPress: async () => {
            setUploading(true);
            try {
              const profile = await profileApi.deleteAvatar();
              onChange(profile.avatarUrl);
            } catch (err) {
              Alert.alert('Silme Başarısız', getErrorMessage(err));
            } finally {
              setUploading(false);
            }
          },
        },
      ],
    );
  };

  return (
    <View>
      <TouchableOpacity
        style={[
          styles.circle,
          { width: size, height: size, borderRadius: size / 2 },
        ]}
        onPress={() => setVisible(true)}
        activeOpacity={0.85}
        disabled={uploading}
      >
        {uploading ? (
          <ActivityIndicator color={Colors.textInverse} />
        ) : avatarUrl ? (
          <Image
            source={{ uri: avatarUrl }}
            style={{ width: size, height: size, borderRadius: size / 2 }}
          />
        ) : (
          <Text style={[styles.initial, { fontSize: size * 0.4 }]}>{initial}</Text>
        )}
      </TouchableOpacity>

      <Modal visible={visible} animationType="slide" transparent>
        <TouchableOpacity
          style={styles.overlay}
          activeOpacity={1}
          onPress={() => setVisible(false)}
        >
          <View style={styles.sheet}>
            <Text style={styles.title}>
              {hasPhoto ? 'Fotoğrafı Değiştir' : 'Fotoğraf Ekle'}
            </Text>

            <TouchableOpacity style={styles.row} onPress={pickFromLibrary}>
              <Text style={styles.rowText}>Galeriden Seç</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.row} onPress={takePhoto}>
              <Text style={styles.rowText}>Kamera ile Çek</Text>
            </TouchableOpacity>
            {hasPhoto && (
              <TouchableOpacity style={styles.row} onPress={confirmDelete}>
                <Text style={[styles.rowText, styles.deleteText]}>Fotoğrafı Sil</Text>
              </TouchableOpacity>
            )}
            <TouchableOpacity style={styles.row} onPress={() => setVisible(false)}>
              <Text style={styles.rowText}>İptal</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  circle: {
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  initial: {
    fontWeight: '800',
    color: Colors.textInverse,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: Colors.backgroundElevated,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingBottom: 32,
    paddingTop: 20,
    borderTopWidth: 1,
    borderColor: Colors.border,
  },
  title: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textMuted,
    textAlign: 'center',
    marginBottom: 12,
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
  row: {
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderTopWidth: 1,
    borderTopColor: Colors.divider,
    alignItems: 'center',
  },
  rowText: {
    fontSize: 16,
    color: Colors.text,
    fontWeight: '600',
  },
  deleteText: {
    color: Colors.error,
  },
});

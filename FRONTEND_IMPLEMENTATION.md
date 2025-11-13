# Implementación de Evidencia/Adjuntos en el Frontend

Este documento contiene los cambios necesarios para implementar la funcionalidad de adjuntos/evidencia con blur automático de caras en el frontend de VigilApp.

## 📋 Resumen de Cambios

El backend ya está implementado y soporta:
- Endpoint `POST /api/alerts/with-media` que acepta `multipart/form-data`
- Procesamiento automático de imágenes para difuminar caras usando Python + OpenCV
- Endpoint `GET /uploads/{filename}` para servir los archivos
- Todos los GETs de alertas ahora incluyen el campo `media` con los adjuntos

## 🔧 Cambios Necesarios en el Frontend

### 1. Actualizar `services/alert.service.ts`

Agregar el tipo `MediaDto` y actualizar la interface `Alert`:

```typescript
export interface MediaDto {
  id: string;
  url: string;
  mimeType: string;
  wasBlurred: boolean;
  createdAt: string;
}

export interface Alert {
  id: string;
  title: string;
  description: string;
  category: AlertCategory;
  status: AlertStatus;
  verificationStatus: VerificationStatus;
  latitude: number;
  longitude: number;
  radiusM: number;
  address?: string;
  cityId?: string;
  isAnonymous: boolean;
  createdByUserId: string;
  createdByUserName: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  media?: MediaDto[];  // <- NUEVO
}
```

Agregar método para crear alertas con media:

```typescript
/**
 * Crear alerta con archivos adjuntos (evidencia).
 * Las imágenes serán procesadas automáticamente para difuminar caras.
 */
async createAlertWithMedia(data: SaveAlertDto, files: { uri: string; name: string; type: string }[]): Promise<Alert> {
  try {
    const formData = new FormData();

    // Agregar datos de la alerta como JSON
    const alertBlob = new Blob([JSON.stringify(data)], { type: 'application/json' });
    formData.append('alert', alertBlob);

    // Agregar archivos
    if (files && files.length > 0) {
      files.forEach((file, index) => {
        const fileBlob = {
          uri: file.uri,
          name: file.name || `file_${index}.jpg`,
          type: file.type || 'image/jpeg',
        } as any;
        formData.append('files', fileBlob);
      });
    }

    const headers = await createHeaders('multipart');
    // Remove Content-Type para que el navegador lo establezca automáticamente con boundary
    delete headers['Content-Type'];

    const response = await fetch(`${API_CONFIG.BASE_URL}/alerts/with-media`, {
      method: 'POST',
      headers: headers,
      body: formData,
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Error al crear alerta con media: ${errorText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error creating alert with media:', error);
    throw error;
  }
}
```

### 2. Actualizar `services/image-picker.service.ts`

Agregar método para selección múltiple de archivos:

```typescript
/**
 * Seleccionar múltiples imágenes/videos de la galería para evidencia de alertas
 */
async pickMultipleMedia(maxFiles: number = 5): Promise<{ success: boolean; files?: ImageAsset[]; error?: string }> {
  try {
    // Verificar permisos
    const hasPermission = await this.requestGalleryPermission();
    if (!hasPermission) {
      return {
        success: false,
        error: 'Se requieren permisos de galería para continuar',
      };
    }

    // Abrir galería con selección múltiple
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images', 'videos'],
      allowsMultipleSelection: true,
      quality: 0.8,
      videoMaxDuration: 30, // Máximo 30 segundos de video
    });

    if (result.canceled) {
      return {
        success: false,
        error: 'Selección cancelada',
      };
    }

    // Limitar número de archivos
    const assets = result.assets.slice(0, maxFiles);

    const files: ImageAsset[] = assets.map((asset, index) => ({
      uri: asset.uri,
      type: asset.type === 'video' ? 'video/mp4' : 'image/jpeg',
      name: asset.type === 'video' ? `video_${Date.now()}_${index}.mp4` : `image_${Date.now()}_${index}.jpg`,
    }));

    return {
      success: true,
      files,
    };
  } catch (error) {
    console.error('[ImagePickerService] Error picking multiple media:', error);
    return {
      success: false,
      error: 'Error al seleccionar archivos',
    };
  }
}
```

### 3. Actualizar `app/create-alert.tsx`

#### 3.1. Agregar imports:

```typescript
import { imagePickerService } from '@/services/image-picker.service';
import { ImageAsset } from '@/services/types/auth.types';
import { Image } from 'react-native';
```

#### 3.2. Agregar estado para archivos:

```typescript
const [selectedFiles, setSelectedFiles] = useState<ImageAsset[]>([]);
```

#### 3.3. Agregar funciones para manejar archivos:

```typescript
const handleSelectMedia = async () => {
  try {
    const result = await imagePickerService.pickMultipleMedia(5);
    if (result.success && result.files) {
      setSelectedFiles(result.files);
      console.log(`[CreateAlert] 📸 ${result.files.length} archivos seleccionados`);
    } else if (result.error) {
      Alert.alert('Error', result.error);
    }
  } catch (error) {
    console.error('[CreateAlert] Error selecting media:', error);
    Alert.alert('Error', 'No se pudieron seleccionar los archivos');
  }
};

const handleRemoveFile = (index: number) => {
  setSelectedFiles(prev => prev.filter((_, i) => i !== index));
};
```

#### 3.4. Actualizar `handlePublish`:

```typescript
const handlePublish = async () => {
  // ... validaciones existentes ...

  setPublishing(true);
  try {
    const alertData = {
      category: mapAlertType(selectedType),
      title: title.trim(),
      description: description.trim(),
      latitude,
      longitude,
      radiusM,
      address: locationAddress,
      isAnonymous,
    };

    // Usar endpoint con media si hay archivos seleccionados
    if (selectedFiles.length > 0) {
      await alertService.createAlertWithMedia(alertData, selectedFiles);
      console.log(`[CreateAlert] ✅ Alerta creada con ${selectedFiles.length} archivos adjuntos`);
    } else {
      await alertService.createAlert(alertData);
      console.log('[CreateAlert] ✅ Alerta creada sin archivos');
    }

    Alert.alert(
      'Éxito',
      'Tu alerta ha sido publicada correctamente',
      [{ text: 'OK', onPress: () => router.push('/home') }]
    );
  } catch (error) {
    console.error('Error publishing alert:', error);
    Alert.alert('Error', 'No se pudo publicar la alerta. Intenta de nuevo.');
  } finally {
    setPublishing(false);
  }
};
```

#### 3.5. Actualizar la sección de fotos/videos en el JSX:

```typescript
{/* Add Photos/Videos */}
<View style={styles.section}>
  <ThemedText style={styles.sectionTitle}>
    Evidencia (Fotos/Videos) {selectedFiles.length > 0 && `(${selectedFiles.length}/5)`}
  </ThemedText>

  {selectedFiles.length > 0 ? (
    <View>
      <ScrollView horizontal style={styles.mediaPreviewContainer} showsHorizontalScrollIndicator={false}>
        {selectedFiles.map((file, index) => (
          <View key={index} style={styles.mediaPreviewItem}>
            <Image source={{ uri: file.uri }} style={styles.mediaPreviewImage} />
            <TouchableOpacity
              style={styles.removeMediaButton}
              onPress={() => handleRemoveFile(index)}
            >
              <Feather name="x" size={16} color="#fff" />
            </TouchableOpacity>
            {file.type.startsWith('video') && (
              <View style={styles.videoIndicator}>
                <Feather name="video" size={16} color="#fff" />
              </View>
            )}
          </View>
        ))}
      </ScrollView>
      {selectedFiles.length < 5 && (
        <TouchableOpacity style={styles.addMoreButton} onPress={handleSelectMedia}>
          <Feather name="plus" size={20} color="#005677" />
          <ThemedText style={styles.addMoreText}>Agregar más</ThemedText>
        </TouchableOpacity>
      )}
      <ThemedText style={styles.blurNotice}>
        ℹ️ Las caras en las fotos serán difuminadas automáticamente
      </ThemedText>
    </View>
  ) : (
    <TouchableOpacity style={styles.photoArea} onPress={handleSelectMedia}>
      <Feather name="image" size={48} color="#ccc" />
      <ThemedText style={styles.photoText}>Toca para agregar fotos/videos</ThemedText>
      <ThemedText style={styles.photoSubtext}>Máximo 5 archivos</ThemedText>
    </TouchableOpacity>
  )}
</View>
```

#### 3.6. Agregar estilos:

```typescript
mediaPreviewContainer: {
  marginBottom: 12,
},
mediaPreviewItem: {
  width: 120,
  height: 120,
  marginRight: 12,
  borderRadius: 8,
  overflow: 'hidden',
  position: 'relative',
},
mediaPreviewImage: {
  width: '100%',
  height: '100%',
  backgroundColor: '#e0e0e0',
},
removeMediaButton: {
  position: 'absolute',
  top: 4,
  right: 4,
  backgroundColor: 'rgba(0,0,0,0.7)',
  borderRadius: 12,
  width: 24,
  height: 24,
  alignItems: 'center',
  justifyContent: 'center',
},
videoIndicator: {
  position: 'absolute',
  bottom: 4,
  right: 4,
  backgroundColor: 'rgba(0,0,0,0.7)',
  borderRadius: 12,
  padding: 4,
},
addMoreButton: {
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  backgroundColor: '#f8f9fa',
  borderRadius: 8,
  borderWidth: 2,
  borderColor: '#005677',
  borderStyle: 'dashed',
  paddingVertical: 12,
  paddingHorizontal: 20,
  marginBottom: 12,
},
addMoreText: {
  color: '#005677',
  fontSize: 14,
  fontWeight: '600',
},
blurNotice: {
  fontSize: 12,
  color: '#666',
  textAlign: 'center',
  fontStyle: 'italic',
},
```

### 4. (Opcional) Actualizar `app/alert-detail.tsx`

Para mostrar los archivos adjuntos cuando se visualiza una alerta:

```typescript
// En el componente, agregar sección para mostrar media
{alert.media && alert.media.length > 0 && (
  <View style={styles.mediaSection}>
    <ThemedText style={styles.sectionTitle}>Evidencia adjunta</ThemedText>
    <ScrollView horizontal showsHorizontalScrollIndicator={false}>
      {alert.media.map((item) => (
        <TouchableOpacity
          key={item.id}
          onPress={() => {
            // Abrir imagen en pantalla completa o visor
          }}
        >
          <Image
            source={{ uri: `${API_CONFIG.BASE_URL}${item.url}` }}
            style={styles.mediaThumb}
          />
          {item.wasBlurred && (
            <View style={styles.blurBadge}>
              <Feather name="eye-off" size={12} color="#fff" />
            </View>
          )}
        </TouchableOpacity>
      ))}
    </ScrollView>
  </View>
)}
```

Estilos correspondientes:

```typescript
mediaSection: {
  padding: 20,
  backgroundColor: '#fff',
  marginBottom: 12,
},
mediaThumb: {
  width: 120,
  height: 120,
  borderRadius: 8,
  marginRight: 12,
  backgroundColor: '#e0e0e0',
},
blurBadge: {
  position: 'absolute',
  top: 4,
  right: 16,
  backgroundColor: 'rgba(0,0,0,0.7)',
  borderRadius: 12,
  padding: 4,
},
```

## 🔐 Detalles Técnicos del Backend

### Endpoints disponibles:

1. **POST /api/alerts/with-media**
   - Content-Type: `multipart/form-data`
   - Parámetros:
     - `alert`: JSON string con los datos de la alerta (SaveAlertDto)
     - `files`: Archivos (imágenes/videos), máximo 5
   - Respuesta: AlertDto con campo `media` poblado

2. **GET /uploads/{filename}**
   - Sirve archivos estáticos
   - Requiere autenticación
   - Retorna el archivo con el content-type apropiado

### Procesamiento de imágenes:

- El backend detecta automáticamente las caras usando `face_recognition` (Python)
- Aplica blur gaussiano intenso (kernel 99x99) a todas las caras detectadas
- Si no se detectan caras, la imagen se guarda sin modificar
- El campo `wasBlurred` en MediaDto indica si la imagen fue procesada

### Configuración:

```yaml
# application.yml (backend)
media:
  upload:
    directory: ${user.home}/vigilapp/uploads
    max-file-size: 10MB
    allowed-types: image/jpeg,image/png,image/jpg,video/mp4,video/quicktime
  blur:
    enabled: true
    auto-blur-images: true
```

## 📝 Notas Importantes

1. **Privacidad**: Las caras se difuminan automáticamente en el servidor
2. **Límites**: Máximo 5 archivos por alerta
3. **Formatos soportados**: JPEG, PNG, MP4
4. **Videos**: Máximo 30 segundos (configurable en el frontend)
5. **Tamaño máximo**: 10MB por archivo

## ✅ Testing

Para probar la funcionalidad:

1. Crear una alerta sin adjuntos (debe funcionar como antes)
2. Crear una alerta con 1 imagen (verificar que se suba y procese)
3. Crear una alerta con 5 imágenes (límite máximo)
4. Verificar que las caras estén difuminadas al ver la alerta
5. Probar con imagen sin caras (debe guardarse sin modificar)

## 🐛 Troubleshooting

- **Error 400 al subir**: Verificar que el FormData esté bien formado
- **Archivos no se muestran**: Verificar URL del backend en API_CONFIG
- **Caras no se difuminan**: Verificar que el servicio Python esté corriendo (puerto 8000)
- **CORS errors**: El backend ya está configurado para `http://localhost:*`

---

**Autor**: Claude Code
**Fecha**: 2025-11-10
**Backend version**: b7e1864 (commit con MediaController y optimizaciones)

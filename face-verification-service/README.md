# Face Verification Service

Servicio Python con FastAPI para verificación facial y validación de documentos de identidad.

## Características

- **API #1**: Validación de cédula mediante detección de contornos y análisis de relación de aspecto
- **API #2**: Comparación facial entre cédula y selfie usando embeddings

## Tecnologías

- FastAPI + Uvicorn
- OpenCV + NumPy
- face_recognition (dlib CNN)
- Pillow

## Instalación

```bash
# Crear entorno virtual
python -m venv venv

# Activar entorno (Windows)
venv\Scripts\activate

# Activar entorno (Linux/Mac)
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt
```

## Ejecutar

```bash
# Modo desarrollo (con reload)
python main.py

# O con uvicorn directamente
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

El servicio estará disponible en `http://localhost:8000`

## Documentación API

Una vez iniciado, visita:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Endpoints

### POST /validate-id
Valida que una imagen sea una cédula válida.

**Request:**
- `image`: archivo de imagen (multipart/form-data)

**Response:**
```json
{
  "is_id_document": true,
  "confidence": 0.85,
  "aspect_ratio": 1.58,
  "reasons": [
    "Documento detectado con área significativa",
    "Contorno rectangular detectado (4 vértices)",
    "Relación de aspecto válida: 1.58",
    "Rostro detectado en la imagen (1 rostro(s))"
  ]
}
```

### POST /verify-face
Compara el rostro de la cédula con un selfie.

**Request:**
- `id_image`: imagen de la cédula (multipart/form-data)
- `selfie`: selfie del usuario (multipart/form-data)

**Response:**
```json
{
  "match": true,
  "distance": 0.3524,
  "similarity": 0.6476,
  "threshold": 0.6,
  "model": "face_recognition (dlib CNN)"
}
```

## Configuración

Ajusta el umbral de similitud en `services/face_comparator.py`:
- `tolerance = 0.6` (default): Balance entre precisión y falsos negativos
- `tolerance = 0.5`: Más estricto (menos falsos positivos)
- `tolerance = 0.7`: Más permisivo (menos falsos negativos)

## Notas

- Las imágenes deben tener buena iluminación
- Los rostros deben estar claramente visibles
- Se recomienda resolución mínima de 640x480

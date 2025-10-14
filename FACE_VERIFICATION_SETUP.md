# Configuración del Sistema de Verificación Facial

Este documento explica cómo configurar y usar el sistema de verificación facial integrado en VigilApp.

## Arquitectura

El sistema consta de dos componentes:

1. **Servicio Python (FastAPI)**: Procesa imágenes y realiza la verificación facial
   - Puerto: `8000`
   - Ubicación: `face-verification-service/`
   - **Se inicia automáticamente** cuando arranca Spring Boot

2. **API Spring Boot**: Gestiona el registro de usuarios y se comunica con el servicio Python
   - Puerto: `8080` (por defecto)
   - Gestiona el ciclo de vida del servicio Python

## Instalación del Servicio Python

### 1. Requisitos previos

- Python 3.8 o superior
- pip
- CMake (requerido por dlib/face_recognition)

#### En Windows:
```bash
# Instalar CMake desde: https://cmake.org/download/
# O usar chocolatey:
choco install cmake
```

#### En Linux/Mac:
```bash
# Ubuntu/Debian
sudo apt-get install cmake

# Mac
brew install cmake
```

### 2. Configurar el entorno

```bash
# Navegar al directorio del servicio
cd face-verification-service

# Crear entorno virtual
python -m venv venv

# Activar entorno
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt
```

**Nota**: La instalación de `face_recognition` puede tardar varios minutos ya que compila dlib.

### 3. El servicio se inicia automáticamente

**No necesitas iniciar el servicio Python manualmente**. Spring Boot lo hace automáticamente cuando arranca.

El componente `FaceVerificationServiceManager` se encarga de:
- ✅ Iniciar el proceso Python al arrancar Spring Boot
- ✅ Capturar y mostrar logs del servicio Python
- ✅ Detener el proceso Python al cerrar Spring Boot

#### Inicio manual (opcional)

Si necesitas iniciar el servicio Python manualmente (por ejemplo, para desarrollo):

```bash
# Asegurarse de que el entorno virtual está activado
python main.py
```

El servicio estará disponible en `http://localhost:8000`

Verificar que funciona visitando: `http://localhost:8000/docs` (Swagger UI)

## Configuración de Spring Boot

El servicio ya está configurado en `application.yml`:

```yaml
face:
  verification:
    service:
      url: http://localhost:8000
      enabled: true                    # true = inicio automático, false = deshabilitado
      path: face-verification-service  # Ruta relativa al directorio del proyecto
      python:
        command: python                # Comando Python (python, python3, etc.)
      startup:
        wait:
          seconds: 10                  # Segundos de espera tras iniciar el servicio
```

### Configuración de inicio automático

El componente `FaceVerificationServiceManager` gestiona el ciclo de vida del servicio Python:

**Parámetros configurables:**
- `enabled`: Habilitar/deshabilitar inicio automático (default: `true`)
- `path`: Ruta al directorio del servicio Python (default: `face-verification-service`)
- `python.command`: Comando para ejecutar Python (default: `python`)
  - Windows con venv: usa automáticamente `venv/Scripts/python.exe`
  - Linux/Mac con venv: usa automáticamente `venv/bin/python`
- `startup.wait.seconds`: Tiempo de espera tras iniciar el servicio (default: `10`)

**Para deshabilitar el inicio automático:**
```yaml
face:
  verification:
    service:
      enabled: false
```

Si el servicio Python corre en otro host/puerto o lo inicias manualmente, actualiza solo la URL.

## Uso del Endpoint de Registro

### Antes (JSON):
```json
POST /api/register
Content-Type: application/json

{
  "name": "Juan Pérez",
  "email": "juan@example.com",
  "password": "password123"
}
```

### Ahora (Multipart Form):
```bash
POST /api/register
Content-Type: multipart/form-data

Fields:
- name: "Juan Pérez"
- email: "juan@example.com"
- password: "password123"
- fotoCedula: [archivo imagen]
- selfie: [archivo imagen]
```

### Ejemplo con cURL:

```bash
curl -X POST http://localhost:8080/api/register \
  -F "name=Juan Pérez" \
  -F "email=juan@example.com" \
  -F "password=password123" \
  -F "fotoCedula=@/ruta/a/cedula.jpg" \
  -F "selfie=@/ruta/a/selfie.jpg"
```

### Ejemplo con Postman:

1. Crear un nuevo request POST a `http://localhost:8080/api/register`
2. En la pestaña "Body", seleccionar "form-data"
3. Agregar los campos:
   - `name` (Text): "Juan Pérez"
   - `email` (Text): "juan@example.com"
   - `password` (Text): "password123"
   - `fotoCedula` (File): Seleccionar archivo de imagen
   - `selfie` (File): Seleccionar archivo de imagen

## Flujo de Verificación

Cuando se envía una solicitud de registro:

1. **Validación básica**: Se verifica que todos los campos estén presentes
2. **Validación de cédula**: El servicio Python analiza la imagen de la cédula:
   - Detecta contornos rectangulares
   - Verifica relación de aspecto (~1.58)
   - Opcionalmente detecta rostro
3. **Comparación facial**: Se comparan los rostros de la cédula y el selfie:
   - Se extraen embeddings de 128 dimensiones
   - Se calcula la distancia euclidiana
   - Umbral por defecto: 0.6 (match si distance < 0.6)
4. **Registro**: Si todo es válido, se crea el usuario

## Respuestas de Error

### Error 400: Imagen de cédula inválida
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "La imagen no parece ser una cédula válida. Razones: [...]"
}
```

### Error 400: Rostros no coinciden
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Los rostros no coinciden. Similitud: 45.23%, Distancia: 0.7854 (umbral: 0.60)"
}
```

### Error 400: Imagen faltante
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "La foto de la cédula es obligatoria"
}
```

## Ajustes y Configuración

### Modificar umbral de similitud

Editar `face-verification-service/services/face_comparator.py`:

```python
def compare_faces(id_image_bytes: bytes, selfie_bytes: bytes, tolerance: float = 0.6):
```

- `tolerance = 0.5`: Más estricto (menos falsos positivos)
- `tolerance = 0.6`: Balance (recomendado)
- `tolerance = 0.7`: Más permisivo (menos falsos negativos)

### Cambiar modelo de reconocimiento facial

Por defecto se usa `face_recognition` (dlib CNN). Para usar DeepFace con otros modelos:

En `face-verification-service/main.py`, modificar el import:

```python
from services.face_comparator import compare_faces_deepface as compare_faces
```

DeepFace soporta: VGG-Face, Facenet, OpenFace, DeepFace, ArcFace

## Recomendaciones para Imágenes

### Foto de cédula:
- Resolución mínima: 640x480
- Iluminación uniforme
- Cédula completamente visible
- Sin reflejos o sombras fuertes

### Selfie:
- Resolución mínima: 640x480
- Buena iluminación frontal
- Rostro centrado y claramente visible
- Sin lentes de sol u obstrucciones

## Solución de Problemas

### El servicio Python no inicia
- Verificar que CMake está instalado
- Verificar que dlib se instaló correctamente: `pip list | grep dlib`
- En Windows, puede necesitar Visual Studio Build Tools

### Error: "Connection refused" al registrar
- Verificar que el servicio Python está corriendo en el puerto 8000
- Verificar la configuración en `application.yml`

### Baja precisión en detección
- Mejorar calidad de las imágenes
- Ajustar el umbral de similitud
- Probar con otro modelo (DeepFace)

## Monitoreo

Ver logs del servicio Python para debugging:
```bash
# El servicio imprime logs en stdout
# Ver en la terminal donde se ejecutó python main.py
```

Acceder a documentación interactiva:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Despliegue en Producción

### Docker para el servicio Python

Crear `face-verification-service/Dockerfile`:

```dockerfile
FROM python:3.10-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    cmake \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["python", "main.py"]
```

Ejecutar:
```bash
docker build -t vigilapp-face-verification .
docker run -p 8000:8000 vigilapp-face-verification
```

### Consideraciones de seguridad

- Las imágenes NO se guardan en el servidor actualmente
- Para auditoría, considerar guardar las imágenes en un bucket S3/storage seguro
- Implementar rate limiting en el endpoint de registro
- Usar HTTPS en producción
- Considerar encriptar imágenes en tránsito

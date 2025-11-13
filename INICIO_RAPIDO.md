# Inicio Rápido - VigilApp con Verificación Facial

## Requisitos previos

- Java 21
- Python 3.8+
- CMake (para compilar dlib)
- PostgreSQL 15+ con PostGIS

## Pasos de instalación

### 1. Instalar dependencias Python

```bash
cd face-verification-service
python -m venv venv

# Windows
venv\Scripts\activate

# Linux/Mac
source venv/bin/activate

pip install -r requirements.txt
```

**Nota**: La instalación de `face_recognition` puede tardar 5-10 minutos.

### 2. Configurar PostgreSQL

Asegúrate de que PostgreSQL esté corriendo en `localhost:5433` con:
- Base de datos: `vigilapp`
- Usuario: `postgres`
- Contraseña: `root`

O actualiza `src/main/resources/application.yml` con tus credenciales.

### 3. Iniciar la aplicación

```bash
# Desde el directorio raíz del proyecto
./gradlew bootRun
```

**¡Eso es todo!** Spring Boot iniciará automáticamente:
1. El servicio Python de verificación facial (puerto 8000)
2. La API REST de VigilApp (puerto 8080)

### 4. Verificar que funciona

Abre tu navegador en:
- API Spring Boot: http://localhost:8080
- Servicio Python (Swagger): http://localhost:8000/docs

## Probar el registro con verificación facial

### Con cURL:

```bash
curl -X POST http://localhost:8080/api/register \
  -F "name=Juan Pérez" \
  -F "email=juan@example.com" \
  -F "password=password123" \
  -F "fotoCedula=@path/to/id_card.jpg" \
  -F "selfie=@path/to/selfie.jpg"
```

### Con Postman:

1. Crear POST request a `http://localhost:8080/api/register`
2. Seleccionar Body → form-data
3. Agregar campos:
   - `name` (Text)
   - `email` (Text)
   - `password` (Text)
   - `fotoCedula` (File)
   - `selfie` (File)

## Ver logs del servicio Python

Los logs del servicio Python aparecerán en la consola de Spring Boot con el prefijo:
```
[Python Service] INFO:     Uvicorn running on http://0.0.0.0:8000
```

## Detener la aplicación

Simplemente detén Spring Boot (Ctrl+C). El servicio Python se detendrá automáticamente.

## Configuración opcional

Para deshabilitar el inicio automático del servicio Python, edita `application.yml`:

```yaml
face:
  verification:
    service:
      enabled: false
```

## Solución de problemas

### El servicio Python no inicia
- Verifica que el venv esté creado: `face-verification-service/venv/`
- Verifica que las dependencias estén instaladas: `pip list | grep face-recognition`
- Revisa los logs de Spring Boot para ver errores

### Error de conexión
- Asegúrate de que el puerto 8000 esté libre
- Espera 10 segundos después de iniciar Spring Boot

Para más detalles, consulta `FACE_VERIFICATION_SETUP.md`.

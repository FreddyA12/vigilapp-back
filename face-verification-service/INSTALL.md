# Instalación del Servicio de Verificación Facial

El servicio de verificación facial requiere instalar dependencias de Python que incluyen `dlib` y `face_recognition`. En Windows, estas librerías requieren herramientas de compilación de C++.

## Opción 1: Instalar Visual Studio Build Tools (Recomendado)

### Paso 1: Descargar e instalar Visual Studio Build Tools

1. Descarga **Visual Studio Build Tools** desde: https://visualstudio.microsoft.com/es/downloads/
2. Busca "Herramientas de compilación para Visual Studio 2022" (o la versión más reciente)
3. Ejecuta el instalador
4. En el instalador, selecciona:
   - **Desarrollo para el escritorio con C++**
   - Asegúrate de que esté marcado: "CMake tools for Windows"
   - Asegúrate de que esté marcado: "MSVC v143 - VS 2022 C++ x64/x86 build tools"

### Paso 2: Instalar dependencias de Python

Después de instalar Build Tools, ejecuta:

```bash
cd face-verification-service
venv\Scripts\pip install -r requirements.txt
```

Si todo está correcto, el servicio se iniciará automáticamente cuando ejecutes:

```bash
cd ..
.\gradlew bootRun
```

## Opción 2: Usar versión precompilada de dlib (Más rápido)

Si no quieres instalar Visual Studio, intenta instalar dlib precompilado:

```bash
cd face-verification-service

# Actualizar pip primero
venv\Scripts\python.exe -m pip install --upgrade pip

# Instalar dlib precompilado desde Unofficial Windows Binaries
venv\Scripts\pip install https://github.com/sachadee/Dlib/raw/main/dlib-19.22.99-cp311-cp311-win_amd64.whl

# Luego instalar el resto de dependencias
venv\Scripts\pip install face-recognition fastapi uvicorn[standard] python-multipart Pillow opencv-python numpy deepface
```

## Opción 3: Usar Docker (Alternativa)

Si tienes Docker instalado:

```bash
cd face-verification-service
docker build -t face-verification-service .
docker run -p 8000:8000 face-verification-service
```

## Verificar Instalación

Para verificar que el servicio funciona correctamente:

```bash
cd face-verification-service
venv\Scripts\python main.py
```

Deberías ver:
```
INFO:     Started server process [XXXX]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8000
```

## Problemas Comunes

### Error: "CMake must be installed"
- Instala CMake: `pip install cmake`
- O instala desde: https://cmake.org/download/

### Error: "Microsoft Visual C++ 14.0 or greater is required"
- Debes instalar Visual Studio Build Tools (Opción 1)

### Error: "No module named 'face_recognition'"
- Asegúrate de activar el entorno virtual antes de ejecutar: `venv\Scripts\activate`
- Reinstala las dependencias: `pip install -r requirements.txt`

## Contacto

Si sigues teniendo problemas, verifica que:
1. Python 3.11 está instalado
2. El entorno virtual existe en `face-verification-service/venv`
3. Las herramientas de compilación de C++ están instaladas

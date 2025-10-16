# Configuración en Windows

## Problema: Error al instalar dlib

Si ves el error `ERROR: Failed building wheel for dlib`, es porque dlib requiere CMake y Visual Studio Build Tools para compilarse en Windows.

## Solución

### Opción 1: Instalar Visual Studio Build Tools (Recomendado)

1. Descargar [Visual Studio Build Tools](https://visualstudio.microsoft.com/downloads/)
2. Ejecutar el instalador
3. Seleccionar **"Desktop development with C++"**
4. Instalar CMake desde: https://cmake.org/download/ o usar `choco install cmake`
5. Reiniciar la terminal
6. Ejecutar de nuevo: `pip install -r requirements.txt`

### Opción 2: Instalar dlib precompilado

```bash
# Activar venv
venv\Scripts\activate

# Instalar dlib precompilado
pip install dlib-bin

# Instalar el resto de dependencias
pip install -r requirements.txt --no-deps
pip install fastapi uvicorn python-multipart Pillow opencv-python numpy deepface
```

### Opción 3: Usar conda (más fácil)

```bash
# Instalar Miniconda: https://docs.conda.io/en/latest/miniconda.html

# Crear entorno
conda create -n vigilapp python=3.11
conda activate vigilapp

# Instalar dlib desde conda-forge
conda install -c conda-forge dlib

# Instalar otras dependencias
pip install -r requirements.txt
```

## Verificar instalación

```bash
python -c "import dlib; print('dlib version:', dlib.__version__)"
python -c "import face_recognition; print('face_recognition OK')"
```

## Alternativa: Ejecutar sin dlib

Si no puedes instalar dlib, puedes:

1. Comentar `face-recognition` en `requirements.txt`
2. Modificar `services/face_comparator.py` para usar solo DeepFace
3. DeepFace no requiere dlib y funciona solo con TensorFlow

Ver `face_comparator.py` función `compare_faces_deepface` para usar esta alternativa.

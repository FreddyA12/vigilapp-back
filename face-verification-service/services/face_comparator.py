import cv2
import numpy as np
from io import BytesIO
from PIL import Image
import face_recognition


def compare_faces(id_image_bytes: bytes, selfie_bytes: bytes, tolerance: float = 0.6) -> dict:
    """
    Compara el rostro de la cédula con el selfie usando OpenCV (Haar Cascade).

    NOTA: Esta es una versión simplificada que usa detección de rostros con Haar Cascade.
    Para producción, se recomienda usar face_recognition o DeepFace.

    Args:
        id_image_bytes: Bytes de la imagen de la cédula
        selfie_bytes: Bytes del selfie
        tolerance: Umbral de similitud (default 0.6, más bajo = más estricto)

    Returns:
        dict con: match, distance, similarity, threshold, model
    """

    # Convertir bytes a imágenes OpenCV
    id_image = Image.open(BytesIO(id_image_bytes))
    selfie_image = Image.open(BytesIO(selfie_bytes))

    id_cv = cv2.cvtColor(np.array(id_image), cv2.COLOR_RGB2BGR)
    selfie_cv = cv2.cvtColor(np.array(selfie_image), cv2.COLOR_RGB2BGR)

    # Convertir a escala de grises
    id_gray = cv2.cvtColor(id_cv, cv2.COLOR_BGR2GRAY)
    selfie_gray = cv2.cvtColor(selfie_cv, cv2.COLOR_BGR2GRAY)

    # Detectar rostros usando Haar Cascade
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

    id_faces = face_cascade.detectMultiScale(id_gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
    selfie_faces = face_cascade.detectMultiScale(selfie_gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))

    # Validar que se detectaron rostros
    if len(id_faces) == 0:
        raise ValueError("No se detectó ningún rostro en la imagen de la cédula")

    if len(selfie_faces) == 0:
        raise ValueError("No se detectó ningún rostro en el selfie")

    # Si hay múltiples rostros, seleccionar el más grande (foto principal)
    if len(id_faces) > 1:
        # Ordenar por área (w * h) en orden descendente
        id_faces = sorted(id_faces, key=lambda face: face[2] * face[3], reverse=True)

    if len(selfie_faces) > 1:
        # Ordenar por área (w * h) en orden descendente
        selfie_faces = sorted(selfie_faces, key=lambda face: face[2] * face[3], reverse=True)

    # Extraer regiones de rostro (usar el primero, que es el más grande si había múltiples)
    (x1, y1, w1, h1) = id_faces[0]
    (x2, y2, w2, h2) = selfie_faces[0]

    id_face = id_gray[y1:y1+h1, x1:x1+w1]
    selfie_face = selfie_gray[y2:y2+h2, x2:x2+w2]

    # Redimensionar a mismo tamaño para comparación
    size = (100, 100)
    id_face_resized = cv2.resize(id_face, size)
    selfie_face_resized = cv2.resize(selfie_face, size)

    # Calcular similitud usando histogramas
    hist1 = cv2.calcHist([id_face_resized], [0], None, [256], [0, 256])
    hist2 = cv2.calcHist([selfie_face_resized], [0], None, [256], [0, 256])

    # Normalizar histogramas
    hist1 = cv2.normalize(hist1, hist1).flatten()
    hist2 = cv2.normalize(hist2, hist2).flatten()

    # Calcular correlación (0-1, donde 1 es idéntico)
    correlation = cv2.compareHist(hist1, hist2, cv2.HISTCMP_CORREL)

    # Convertir correlación a distancia (0-1, donde 0 es idéntico)
    distance = 1.0 - correlation
    similarity = correlation

    # Verificar si hay match (correlación > 0.4 = match, ajustable)
    match = correlation > (1.0 - tolerance)

    return {
        "match": bool(match),
        "distance": round(float(distance), 4),
        "similarity": round(float(similarity), 4),
        "threshold": tolerance,
        "model": "opencv-haar-cascade (basic)"
    }


def compare_faces_fr(id_image_bytes: bytes, selfie_bytes: bytes, tolerance: float = 0.6) -> dict:
    """
    Compara rostros usando face_recognition (dlib embeddings de 128 dimensiones).

    Este método es MUY PRECISO y robusto para diferentes condiciones de iluminación,
    ángulos y expresiones faciales.

    Args:
        id_image_bytes: Bytes de la imagen de la cédula
        selfie_bytes: Bytes del selfie
        tolerance: Umbral de distancia (default 0.6, más bajo = más estricto)
                  Valores típicos: 0.6 (normal), 0.5 (estricto), 0.7 (permisivo)

    Returns:
        dict con: match, distance, similarity, threshold, model
    """
    # Convertir bytes a imágenes numpy (RGB)
    id_image = Image.open(BytesIO(id_image_bytes))
    selfie_image = Image.open(BytesIO(selfie_bytes))

    id_np = np.array(id_image)
    selfie_np = np.array(selfie_image)

    # Detectar rostros y obtener encodings
    id_face_locations = face_recognition.face_locations(id_np)
    selfie_face_locations = face_recognition.face_locations(selfie_np)

    # Validar que se detectaron rostros
    if len(id_face_locations) == 0:
        raise ValueError("No se detectó ningún rostro en la imagen de la cédula")

    if len(selfie_face_locations) == 0:
        raise ValueError("No se detectó ningún rostro en el selfie")

    # Si hay múltiples rostros, seleccionar el más grande
    if len(id_face_locations) > 1:
        # Ordenar por área (bottom - top) * (right - left)
        id_face_locations = sorted(
            id_face_locations,
            key=lambda loc: (loc[2] - loc[0]) * (loc[1] - loc[3]),
            reverse=True
        )

    if len(selfie_face_locations) > 1:
        # Ordenar por área
        selfie_face_locations = sorted(
            selfie_face_locations,
            key=lambda loc: (loc[2] - loc[0]) * (loc[1] - loc[3]),
            reverse=True
        )

    # Obtener encodings (embeddings de 128 dimensiones)
    id_encodings = face_recognition.face_encodings(id_np, known_face_locations=[id_face_locations[0]])
    selfie_encodings = face_recognition.face_encodings(selfie_np, known_face_locations=[selfie_face_locations[0]])

    if len(id_encodings) == 0:
        raise ValueError("No se pudo generar encoding del rostro en la cédula")

    if len(selfie_encodings) == 0:
        raise ValueError("No se pudo generar encoding del rostro en el selfie")

    # Calcular distancia euclidiana entre encodings
    distance = face_recognition.face_distance([id_encodings[0]], selfie_encodings[0])[0]

    # Determinar si hay match (distancia < tolerance)
    match = distance <= tolerance

    # Calcular similitud como porcentaje (invertir distancia)
    # Distancia 0.0 = 100% similar, distancia 1.0 = 0% similar
    similarity = max(0.0, 1.0 - distance)

    return {
        "match": bool(match),
        "distance": round(float(distance), 4),
        "similarity": round(float(similarity), 4),
        "threshold": tolerance,
        "model": "face_recognition-dlib-128d"
    }


def compare_faces_deepface(id_image_bytes: bytes, selfie_bytes: bytes) -> dict:
    """
    Alternativa usando DeepFace (más modelos disponibles).

    DeepFace soporta múltiples modelos:
    - VGG-Face
    - Facenet
    - OpenFace
    - DeepFace
    - ArcFace

    Args:
        id_image_bytes: Bytes de la imagen de la cédula
        selfie_bytes: Bytes del selfie

    Returns:
        dict con: match, distance, similarity, threshold, model
    """
    from deepface import DeepFace
    import tempfile
    import os

    # DeepFace requiere rutas de archivo, guardar temporalmente
    with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as id_tmp:
        id_tmp.write(id_image_bytes)
        id_path = id_tmp.name

    with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as selfie_tmp:
        selfie_tmp.write(selfie_bytes)
        selfie_path = selfie_tmp.name

    try:
        # Usar Facenet como modelo por defecto (buen balance precisión/velocidad)
        result = DeepFace.verify(
            img1_path=id_path,
            img2_path=selfie_path,
            model_name="Facenet",
            distance_metric="euclidean",
            enforce_detection=True
        )

        return {
            "match": result["verified"],
            "distance": round(result["distance"], 4),
            "similarity": round(1.0 - (result["distance"] / 10.0), 4),  # Normalizar
            "threshold": result["threshold"],
            "model": result["model"]
        }

    finally:
        # Limpiar archivos temporales
        os.unlink(id_path)
        os.unlink(selfie_path)

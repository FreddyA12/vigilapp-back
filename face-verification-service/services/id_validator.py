import cv2
import numpy as np
from io import BytesIO
from PIL import Image


def validate_id_document(image_bytes: bytes) -> dict:
    """
    Valida que la imagen sea una cédula mediante:
    - Detección de contorno rectangular
    - Verificación de relación de aspecto (~1.58 para cédulas)
    - Detección de rostro (opcional)

    Args:
        image_bytes: Bytes de la imagen

    Returns:
        dict con: is_id_document, confidence, aspect_ratio, reasons
    """

    # Convertir bytes a imagen OpenCV
    image = Image.open(BytesIO(image_bytes))
    image_np = np.array(image)
    image_cv = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)

    reasons = []
    confidence = 0.0
    aspect_ratio = 0.0

    # 1. Preprocesamiento
    gray = cv2.cvtColor(image_cv, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edges = cv2.Canny(blurred, 50, 150)

    # 2. Encontrar contornos
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        reasons.append("No se detectaron contornos en la imagen")
        return {
            "is_id_document": False,
            "confidence": 0.0,
            "aspect_ratio": 0.0,
            "reasons": reasons
        }

    # 3. Buscar el contorno más grande (probablemente la cédula)
    largest_contour = max(contours, key=cv2.contourArea)
    area = cv2.contourArea(largest_contour)

    # Verificar que el contorno sea significativo (al menos 20% de la imagen)
    image_area = image_cv.shape[0] * image_cv.shape[1]
    if area < image_area * 0.2:
        reasons.append("El documento ocupa menos del 20% de la imagen")
        confidence += 0.2
    else:
        reasons.append("Documento detectado con área significativa")
        confidence += 0.4

    # 4. Aproximar a rectángulo
    perimeter = cv2.arcLength(largest_contour, True)
    approx = cv2.approxPolyDP(largest_contour, 0.02 * perimeter, True)

    # Verificar si es rectangular (4 vértices)
    if len(approx) == 4:
        reasons.append("Contorno rectangular detectado (4 vértices)")
        confidence += 0.3
    else:
        reasons.append(f"Contorno no rectangular ({len(approx)} vértices)")
        confidence += 0.1

    # 5. Calcular relación de aspecto
    rect = cv2.minAreaRect(largest_contour)
    width, height = rect[1]

    if width > 0 and height > 0:
        # Asegurar que width > height
        if height > width:
            width, height = height, width

        aspect_ratio = width / height

        # Cédulas típicas tienen relación ~1.58 (aproximadamente 85.6mm x 54mm)
        # Tolerancia: 1.4 a 1.8
        if 1.4 <= aspect_ratio <= 1.8:
            reasons.append(f"Relación de aspecto válida: {aspect_ratio:.2f}")
            confidence += 0.3
        else:
            reasons.append(f"Relación de aspecto fuera de rango: {aspect_ratio:.2f} (esperado: 1.4-1.8)")

    # 6. (Opcional) Detectar rostro usando Haar Cascade
    try:
        face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))

        if len(faces) > 0:
            reasons.append(f"Rostro detectado en la imagen ({len(faces)} rostro(s))")
            confidence = min(confidence + 0.2, 1.0)
        else:
            reasons.append("No se detectó rostro (puede ser normal en algunas cédulas)")
    except Exception as e:
        reasons.append(f"Error en detección facial: {str(e)}")

    # Normalizar confianza
    confidence = min(confidence, 1.0)

    # Considerar válido si confianza > 0.6
    is_valid = confidence >= 0.6

    return {
        "is_id_document": is_valid,
        "confidence": round(confidence, 2),
        "aspect_ratio": round(aspect_ratio, 2),
        "reasons": reasons
    }

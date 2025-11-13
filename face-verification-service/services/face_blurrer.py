"""
Face Blurring Service

Este servicio detecta caras en imágenes y las difumina para proteger la privacidad
de las personas en evidencias/adjuntos de alertas.
"""

import cv2
import numpy as np
from io import BytesIO
from PIL import Image
import face_recognition


def blur_faces_in_image(image_bytes: bytes, blur_intensity: int = 99) -> bytes:
    """
    Detecta todas las caras en una imagen y las difumina.

    Args:
        image_bytes: Bytes de la imagen a procesar
        blur_intensity: Intensidad del blur (debe ser impar, por defecto 99)

    Returns:
        bytes: Imagen procesada con caras difuminadas en formato JPEG

    Raises:
        ValueError: Si la imagen no puede ser procesada
    """
    try:
        # Convertir bytes a imagen PIL
        image_pil = Image.open(BytesIO(image_bytes))

        # Convertir a RGB si es necesario
        if image_pil.mode != 'RGB':
            image_pil = image_pil.convert('RGB')

        # Convertir PIL a numpy array (RGB)
        image_np = np.array(image_pil)

        # face_recognition usa RGB, OpenCV usa BGR
        # Detectar ubicaciones de caras usando face_recognition
        face_locations = face_recognition.face_locations(image_np, model='hog')

        # Si no se detectan caras, devolver la imagen original
        if not face_locations:
            # Convertir de vuelta a bytes
            output_buffer = BytesIO()
            image_pil.save(output_buffer, format='JPEG', quality=95)
            return output_buffer.getvalue()

        # Convertir a BGR para OpenCV
        image_bgr = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)

        # Aplicar blur a cada cara detectada
        for top, right, bottom, left in face_locations:
            # Extraer la región de la cara
            face_region = image_bgr[top:bottom, left:right]

            h, w = face_region.shape[:2]

            # Pixelación agresiva
            pixel_size = max(25, min(w, h) // 6)

            # Reducir tamaño
            small = cv2.resize(face_region, (w // pixel_size, h // pixel_size), interpolation=cv2.INTER_LINEAR)

            # Ampliar de nuevo (efecto pixelado)
            pixelated = cv2.resize(small, (w, h), interpolation=cv2.INTER_NEAREST)

            # Múltiples pasadas de blur gaussiano con sigma alto
            blurred_face = pixelated
            for _ in range(4):
                blurred_face = cv2.GaussianBlur(blurred_face, (99, 99), 50)

            # Reemplazar la región de la cara con la versión difuminada
            image_bgr[top:bottom, left:right] = blurred_face

        # Convertir de BGR de vuelta a RGB
        image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)

        # Convertir numpy array a PIL Image
        result_pil = Image.fromarray(image_rgb)

        # Convertir a bytes
        output_buffer = BytesIO()
        result_pil.save(output_buffer, format='JPEG', quality=95)

        return output_buffer.getvalue()

    except Exception as e:
        raise ValueError(f"Error al procesar la imagen: {str(e)}")


def blur_faces_advanced(image_bytes: bytes, blur_factor: float = 3.0) -> dict:
    """
    Versión avanzada que detecta caras y retorna información adicional.

    Args:
        image_bytes: Bytes de la imagen a procesar
        blur_factor: Factor de blur (multiplicador del kernel)

    Returns:
        dict: {
            "image_bytes": bytes de la imagen procesada,
            "faces_detected": número de caras detectadas,
            "faces_locations": lista de coordenadas de caras
        }
    """
    try:
        # Convertir bytes a imagen PIL
        image_pil = Image.open(BytesIO(image_bytes))

        if image_pil.mode != 'RGB':
            image_pil = image_pil.convert('RGB')

        image_np = np.array(image_pil)

        # Detectar caras
        face_locations = face_recognition.face_locations(image_np, model='hog')

        faces_count = len(face_locations)

        # Si hay caras, aplicar blur
        if faces_count > 0:
            image_bgr = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)

            # Calcular intensidad de blur basada en el factor
            blur_intensity = int(51 * blur_factor)
            if blur_intensity % 2 == 0:
                blur_intensity += 1
            blur_intensity = min(blur_intensity, 199)  # Limitar a 199

            for top, right, bottom, left in face_locations:
                face_region = image_bgr[top:bottom, left:right]
                h, w = face_region.shape[:2]

                # Pixelación agresiva
                pixel_size = max(25, min(w, h) // 6)
                small = cv2.resize(face_region, (w // pixel_size, h // pixel_size), interpolation=cv2.INTER_LINEAR)
                pixelated = cv2.resize(small, (w, h), interpolation=cv2.INTER_NEAREST)

                # Múltiples pasadas de blur
                blurred_face = pixelated
                for _ in range(4):
                    blurred_face = cv2.GaussianBlur(blurred_face, (99, 99), 50)
                image_bgr[top:bottom, left:right] = blurred_face

            image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
            result_pil = Image.fromarray(image_rgb)
        else:
            # No hay caras, usar imagen original
            result_pil = image_pil

        # Convertir a bytes
        output_buffer = BytesIO()
        result_pil.save(output_buffer, format='JPEG', quality=95)

        return {
            "image_bytes": output_buffer.getvalue(),
            "faces_detected": faces_count,
            "faces_locations": [
                {"top": top, "right": right, "bottom": bottom, "left": left}
                for top, right, bottom, left in face_locations
            ]
        }

    except Exception as e:
        raise ValueError(f"Error al procesar la imagen: {str(e)}")

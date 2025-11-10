from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
import uvicorn

from services.id_validator import validate_id_document
from services.face_comparator import compare_faces_fr
from services.face_blurrer import blur_faces_in_image, blur_faces_advanced

app = FastAPI(title="Face Verification Service", version="1.0.0")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Spring Boot default port
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {"message": "Face Verification Service is running"}


@app.post("/validate-id")
async def validate_id(image: UploadFile = File(...)):
    """
    API #1 - Valida que la imagen sea una cédula.

    Detecta:
    - Contorno rectangular
    - Relación de aspecto ~1.58
    - Presencia de rostro (opcional)

    Returns:
        {
            "is_id_document": bool,
            "confidence": float,
            "aspect_ratio": float,
            "reasons": [str]
        }
    """
    try:
        image_bytes = await image.read()
        result = validate_id_document(image_bytes)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error validating ID: {str(e)}")


@app.post("/verify-face")
async def verify_face(
    id_image: UploadFile = File(..., description="Imagen de la cédula"),
    selfie: UploadFile = File(..., description="Selfie del usuario")
):
    """
    API #2 - Compara el rostro de la cédula con el selfie.

    Detecta rostros, extrae embeddings y calcula similitud.

    Returns:
        {
            "match": bool,
            "distance": float,
            "similarity": float,
            "threshold": float,
            "model": str
        }
    """
    try:
        id_bytes = await id_image.read()
        selfie_bytes = await selfie.read()

        result = compare_faces_fr(id_bytes, selfie_bytes)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error comparing faces: {str(e)}")


@app.post("/blur-faces")
async def blur_faces(
    image: UploadFile = File(..., description="Imagen con caras a difuminar"),
    blur_intensity: int = 99
):
    """
    API #3 - Detecta y difumina caras en una imagen para proteger privacidad.

    Este endpoint es usado para procesar evidencia/adjuntos de alertas,
    asegurando que no se expongan rostros de personas.

    Args:
        image: Archivo de imagen a procesar
        blur_intensity: Intensidad del blur (debe ser impar, por defecto 99)

    Returns:
        Response: Imagen procesada con caras difuminadas (JPEG)
    """
    try:
        image_bytes = await image.read()
        blurred_image_bytes = blur_faces_in_image(image_bytes, blur_intensity)

        return Response(
            content=blurred_image_bytes,
            media_type="image/jpeg",
            headers={
                "Content-Disposition": f"inline; filename=blurred_{image.filename}"
            }
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error blurring faces: {str(e)}")


@app.post("/blur-faces-info")
async def blur_faces_with_info(
    image: UploadFile = File(..., description="Imagen con caras a difuminar"),
    blur_factor: float = 3.0
):
    """
    API #4 - Detecta y difumina caras, retornando información adicional.

    Similar a /blur-faces pero retorna información sobre las caras detectadas.

    Args:
        image: Archivo de imagen a procesar
        blur_factor: Factor de intensidad del blur (por defecto 3.0)

    Returns:
        {
            "faces_detected": int,
            "faces_locations": [{"top": int, "right": int, "bottom": int, "left": int}],
            "message": str
        }

        Headers:
            - X-Faces-Detected: número de caras detectadas

        Body: Imagen procesada (JPEG)
    """
    try:
        image_bytes = await image.read()
        result = blur_faces_advanced(image_bytes, blur_factor)

        return Response(
            content=result["image_bytes"],
            media_type="image/jpeg",
            headers={
                "Content-Disposition": f"inline; filename=blurred_{image.filename}",
                "X-Faces-Detected": str(result["faces_detected"]),
                "X-Face-Locations": str(result["faces_locations"])
            }
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error blurring faces: {str(e)}")


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

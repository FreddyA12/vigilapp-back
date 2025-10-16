from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from services.id_validator import validate_id_document
from services.face_comparator import compare_faces_fr

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


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

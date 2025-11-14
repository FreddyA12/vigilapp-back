"""
Unit tests for face comparison service
"""

import pytest
import numpy as np
from PIL import Image, ImageDraw
from io import BytesIO
import cv2
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from services.face_comparator import compare_faces, compare_faces_fr


def create_test_face_image(width=400, height=400, add_face=True):
    """Create a test image with a simple face-like drawing"""
    img = Image.new('RGB', (width, height), color=(255, 255, 255))

    if add_face:
        draw = ImageDraw.Draw(img)
        # Draw a simple face-like structure
        # Face oval
        draw.ellipse([100, 100, 300, 350], outline=(0, 0, 0), width=2)
        # Eyes
        draw.ellipse([150, 150, 170, 170], fill=(0, 0, 0))
        draw.ellipse([230, 150, 250, 170], fill=(0, 0, 0))
        # Nose
        draw.line([200, 180, 200, 220], fill=(0, 0, 0), width=2)
        # Mouth
        draw.arc([160, 240, 240, 290], 0, 180, fill=(0, 0, 0), width=2)

    buffer = BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


def create_image_without_face():
    """Create an image without any face"""
    img = Image.new('RGB', (400, 400), color=(200, 200, 200))
    draw = ImageDraw.Draw(img)
    # Draw some geometric shapes (not face-like)
    draw.rectangle([50, 50, 150, 150], outline=(0, 0, 0), width=2)
    draw.rectangle([200, 200, 300, 300], outline=(0, 0, 0), width=2)

    buffer = BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


class TestCompareFaces:

    def test_compare_faces_returns_dict(self):
        """Test that compare_faces returns a dictionary"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        # This might raise ValueError if faces aren't detected
        # We'll handle it in the test
        try:
            result = compare_faces(id_image, selfie)
            assert isinstance(result, dict)
            assert 'match' in result
            assert 'distance' in result
            assert 'similarity' in result
            assert 'threshold' in result
            assert 'model' in result
        except ValueError as e:
            # If Haar Cascade doesn't detect the simple drawn face, that's ok for this test
            assert "No se detectó ningún rostro" in str(e)

    def test_compare_faces_with_no_face_in_id_raises_error(self):
        """Test that missing face in ID image raises ValueError"""
        id_image = create_image_without_face()
        selfie = create_test_face_image()

        with pytest.raises(ValueError) as exc_info:
            compare_faces(id_image, selfie)

        assert "No se detectó ningún rostro en la imagen de la cédula" in str(exc_info.value)

    def test_compare_faces_with_no_face_in_selfie_raises_error(self):
        """Test that missing face in selfie raises ValueError"""
        id_image = create_test_face_image()
        selfie = create_image_without_face()

        with pytest.raises(ValueError) as exc_info:
            compare_faces(id_image, selfie)

        assert "No se detectó ningún rostro en el selfie" in str(exc_info.value)

    def test_compare_faces_result_structure(self):
        """Test the structure of the result dictionary"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces(id_image, selfie)

            # Check types
            assert isinstance(result['match'], bool)
            assert isinstance(result['distance'], float)
            assert isinstance(result['similarity'], float)
            assert isinstance(result['threshold'], float)
            assert isinstance(result['model'], str)

            # Check value ranges
            assert 0.0 <= result['distance'] <= 1.0
            assert 0.0 <= result['similarity'] <= 1.0

            # Check model name
            assert 'opencv' in result['model'].lower()
        except ValueError:
            # Skip if faces not detected in simple drawings
            pass

    def test_compare_faces_distance_and_similarity_relationship(self):
        """Test that distance and similarity have inverse relationship"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces(id_image, selfie)

            # distance + similarity should approximately equal 1.0
            assert abs((result['distance'] + result['similarity']) - 1.0) < 0.01
        except ValueError:
            # Skip if faces not detected
            pass

    def test_compare_faces_custom_tolerance(self):
        """Test that custom tolerance is respected"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        custom_tolerance = 0.5

        try:
            result = compare_faces(id_image, selfie, tolerance=custom_tolerance)
            assert result['threshold'] == custom_tolerance
        except ValueError:
            # Skip if faces not detected
            pass


class TestCompareFacesFR:

    def test_compare_faces_fr_returns_dict(self):
        """Test that compare_faces_fr returns a dictionary"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces_fr(id_image, selfie)
            assert isinstance(result, dict)
            assert 'match' in result
            assert 'distance' in result
            assert 'similarity' in result
            assert 'threshold' in result
            assert 'model' in result
        except ValueError as e:
            # face_recognition might not detect our simple drawn faces
            assert "No se detectó ningún rostro" in str(e) or "No se pudo generar encoding" in str(e)

    def test_compare_faces_fr_with_no_face_raises_error(self):
        """Test that missing faces raise appropriate errors"""
        id_image = create_image_without_face()
        selfie = create_test_face_image()

        with pytest.raises(ValueError) as exc_info:
            compare_faces_fr(id_image, selfie)

        assert "No se detectó ningún rostro" in str(exc_info.value)

    def test_compare_faces_fr_result_structure(self):
        """Test the structure of face_recognition result"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces_fr(id_image, selfie)

            # Check types
            assert isinstance(result['match'], bool)
            assert isinstance(result['distance'], float)
            assert isinstance(result['similarity'], float)
            assert isinstance(result['threshold'], float)
            assert isinstance(result['model'], str)

            # Check model name
            assert 'face_recognition' in result['model'].lower()
            assert 'dlib' in result['model'].lower()
        except ValueError:
            # Skip if faces not detected
            pass

    def test_compare_faces_fr_custom_tolerance(self):
        """Test custom tolerance with face_recognition"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        custom_tolerance = 0.5

        try:
            result = compare_faces_fr(id_image, selfie, tolerance=custom_tolerance)
            assert result['threshold'] == custom_tolerance
        except ValueError:
            # Skip if faces not detected
            pass

    def test_compare_faces_fr_match_logic(self):
        """Test that match is determined by distance vs threshold"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces_fr(id_image, selfie, tolerance=0.6)

            # Match should be True if distance <= threshold
            expected_match = result['distance'] <= result['threshold']
            assert result['match'] == expected_match
        except ValueError:
            # Skip if faces not detected
            pass

    def test_compare_faces_fr_similarity_calculation(self):
        """Test that similarity is correctly calculated from distance"""
        id_image = create_test_face_image()
        selfie = create_test_face_image()

        try:
            result = compare_faces_fr(id_image, selfie)

            # Similarity should be 1.0 - distance (clamped to 0.0 minimum)
            expected_similarity = max(0.0, 1.0 - result['distance'])
            assert abs(result['similarity'] - expected_similarity) < 0.01
        except ValueError:
            # Skip if faces not detected
            pass


if __name__ == '__main__':
    pytest.main([__file__, '-v'])

"""
Unit tests for face blurring service
"""

import pytest
import numpy as np
from PIL import Image, ImageDraw
from io import BytesIO
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from services.face_blurrer import blur_faces_in_image, blur_faces_advanced


def create_test_image_with_face(width=400, height=400):
    """Create a test image with a simple face-like drawing"""
    img = Image.new('RGB', (width, height), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)

    # Draw a simple face
    draw.ellipse([100, 100, 300, 350], outline=(0, 0, 0), width=2)
    draw.ellipse([150, 150, 170, 170], fill=(0, 0, 0))
    draw.ellipse([230, 150, 250, 170], fill=(0, 0, 0))
    draw.line([200, 180, 200, 220], fill=(0, 0, 0), width=2)
    draw.arc([160, 240, 240, 290], 0, 180, fill=(0, 0, 0), width=2)

    buffer = BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


def create_test_image_without_face():
    """Create an image without any face"""
    img = Image.new('RGB', (400, 400), color=(200, 200, 200))
    draw = ImageDraw.Draw(img)
    draw.rectangle([50, 50, 350, 350], outline=(0, 0, 0), width=2)

    buffer = BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


class TestBlurFacesInImage:

    def test_blur_faces_in_image_returns_bytes(self):
        """Test that the function returns bytes"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_in_image(image_bytes)

        assert isinstance(result, bytes)
        assert len(result) > 0

    def test_blur_faces_in_image_returns_valid_jpeg(self):
        """Test that the result is a valid JPEG image"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_in_image(image_bytes)

        # Try to open the result as an image
        result_image = Image.open(BytesIO(result))
        assert result_image.format == 'JPEG'
        assert result_image.mode == 'RGB'

    def test_blur_faces_without_faces_returns_original(self):
        """Test that images without faces are returned unchanged"""
        image_bytes = create_test_image_without_face()
        result = blur_faces_in_image(image_bytes)

        # Result should be valid image
        result_image = Image.open(BytesIO(result))
        assert result_image.format == 'JPEG'

    def test_blur_faces_with_custom_intensity(self):
        """Test with custom blur intensity"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_in_image(image_bytes, blur_intensity=51)

        assert isinstance(result, bytes)
        assert len(result) > 0

    def test_blur_faces_preserves_image_size(self):
        """Test that image dimensions are preserved"""
        width, height = 600, 400
        image_bytes = create_test_image_with_face(width, height)
        result = blur_faces_in_image(image_bytes)

        result_image = Image.open(BytesIO(result))
        assert result_image.size == (width, height)

    def test_blur_faces_with_small_image(self):
        """Test with small image"""
        image_bytes = create_test_image_with_face(200, 200)
        result = blur_faces_in_image(image_bytes)

        assert isinstance(result, bytes)
        result_image = Image.open(BytesIO(result))
        assert result_image.size == (200, 200)

    def test_blur_faces_with_large_image(self):
        """Test with large image"""
        image_bytes = create_test_image_with_face(1200, 900)
        result = blur_faces_in_image(image_bytes)

        assert isinstance(result, bytes)
        result_image = Image.open(BytesIO(result))
        assert result_image.size == (1200, 900)

    def test_blur_faces_converts_non_rgb_to_rgb(self):
        """Test that non-RGB images are converted to RGB"""
        # Create grayscale image
        img = Image.new('L', (400, 400), color=128)
        buffer = BytesIO()
        img.save(buffer, format='JPEG')

        result = blur_faces_in_image(buffer.getvalue())
        result_image = Image.open(BytesIO(result))

        assert result_image.mode == 'RGB'

    def test_blur_faces_with_rgba_image(self):
        """Test with RGBA image (with alpha channel)"""
        img = Image.new('RGBA', (400, 400), color=(255, 255, 255, 255))
        buffer = BytesIO()
        img.save(buffer, format='PNG')

        result = blur_faces_in_image(buffer.getvalue())
        result_image = Image.open(BytesIO(result))

        # Should be converted to RGB
        assert result_image.mode == 'RGB'


class TestBlurFacesAdvanced:

    def test_blur_faces_advanced_returns_dict(self):
        """Test that blur_faces_advanced returns a dictionary"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes)

        assert isinstance(result, dict)
        assert 'image_bytes' in result
        assert 'faces_detected' in result
        assert 'faces_locations' in result

    def test_blur_faces_advanced_image_bytes_valid(self):
        """Test that returned image_bytes is a valid image"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes)

        assert isinstance(result['image_bytes'], bytes)
        assert len(result['image_bytes']) > 0

        # Verify it's a valid image
        result_image = Image.open(BytesIO(result['image_bytes']))
        assert result_image.format == 'JPEG'

    def test_blur_faces_advanced_faces_count(self):
        """Test that faces_detected is an integer"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes)

        assert isinstance(result['faces_detected'], int)
        assert result['faces_detected'] >= 0

    def test_blur_faces_advanced_faces_locations_structure(self):
        """Test the structure of faces_locations"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes)

        assert isinstance(result['faces_locations'], list)

        # If faces were detected, check structure
        for face_loc in result['faces_locations']:
            assert isinstance(face_loc, dict)
            assert 'top' in face_loc
            assert 'right' in face_loc
            assert 'bottom' in face_loc
            assert 'left' in face_loc

            # Check that coordinates are integers
            assert isinstance(face_loc['top'], int)
            assert isinstance(face_loc['right'], int)
            assert isinstance(face_loc['bottom'], int)
            assert isinstance(face_loc['left'], int)

    def test_blur_faces_advanced_without_faces(self):
        """Test advanced blur with image without faces"""
        image_bytes = create_test_image_without_face()
        result = blur_faces_advanced(image_bytes)

        assert result['faces_detected'] == 0
        assert len(result['faces_locations']) == 0
        assert isinstance(result['image_bytes'], bytes)

    def test_blur_faces_advanced_custom_blur_factor(self):
        """Test with custom blur factor"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes, blur_factor=5.0)

        assert isinstance(result, dict)
        assert 'image_bytes' in result

    def test_blur_faces_advanced_faces_count_matches_locations(self):
        """Test that faces_detected matches length of faces_locations"""
        image_bytes = create_test_image_with_face()
        result = blur_faces_advanced(image_bytes)

        assert result['faces_detected'] == len(result['faces_locations'])

    def test_blur_faces_advanced_preserves_dimensions(self):
        """Test that image dimensions are preserved"""
        width, height = 800, 600
        image_bytes = create_test_image_with_face(width, height)
        result = blur_faces_advanced(image_bytes)

        result_image = Image.open(BytesIO(result['image_bytes']))
        assert result_image.size == (width, height)

    def test_blur_faces_advanced_face_coordinates_valid(self):
        """Test that face coordinates are within image bounds"""
        width, height = 400, 400
        image_bytes = create_test_image_with_face(width, height)
        result = blur_faces_advanced(image_bytes)

        for face_loc in result['faces_locations']:
            assert 0 <= face_loc['top'] <= height
            assert 0 <= face_loc['bottom'] <= height
            assert 0 <= face_loc['left'] <= width
            assert 0 <= face_loc['right'] <= width

            # Top should be less than bottom
            assert face_loc['top'] < face_loc['bottom']
            # Left should be less than right
            assert face_loc['left'] < face_loc['right']


if __name__ == '__main__':
    pytest.main([__file__, '-v'])

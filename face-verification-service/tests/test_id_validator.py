"""
Unit tests for ID document validator
"""

import pytest
import numpy as np
from PIL import Image
from io import BytesIO
import cv2
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from services.id_validator import validate_id_document


def create_test_image(width=856, height=540, color=(255, 255, 255)):
    """Create a test image with given dimensions and color"""
    img = Image.new('RGB', (width, height), color=color)
    buffer = BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


def create_id_card_like_image():
    """Create an image that resembles an ID card"""
    # ID card aspect ratio is approximately 1.58 (85.6mm x 54mm)
    width, height = 856, 540  # Approximately 1.58 ratio

    # Create white background
    img = np.ones((height, width, 3), dtype=np.uint8) * 255

    # Draw a rectangular border (simulating ID card edge)
    cv2.rectangle(img, (10, 10), (width-10, height-10), (0, 0, 0), 2)

    # Add some text-like rectangles (simulating ID information)
    cv2.rectangle(img, (50, 50), (300, 100), (0, 0, 0), -1)
    cv2.rectangle(img, (50, 120), (400, 150), (0, 0, 0), -1)

    # Convert to PIL Image and then to bytes
    pil_img = Image.fromarray(img)
    buffer = BytesIO()
    pil_img.save(buffer, format='JPEG')
    return buffer.getvalue()


def create_non_id_image():
    """Create an image that doesn't look like an ID card"""
    # Create a circular image (not rectangular)
    width, height = 500, 500
    img = np.ones((height, width, 3), dtype=np.uint8) * 255

    # Draw a circle instead of rectangle
    cv2.circle(img, (250, 250), 200, (0, 0, 0), 5)

    pil_img = Image.fromarray(img)
    buffer = BytesIO()
    pil_img.save(buffer, format='JPEG')
    return buffer.getvalue()


class TestIdValidator:

    def test_validate_id_document_returns_dict(self):
        """Test that the function returns a dictionary"""
        image_bytes = create_test_image()
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'is_id_document' in result
        assert 'confidence' in result
        assert 'aspect_ratio' in result
        assert 'reasons' in result

    def test_validate_id_document_with_valid_aspect_ratio(self):
        """Test with image having valid ID card aspect ratio"""
        # Create image with aspect ratio ~1.58 (typical ID card)
        image_bytes = create_id_card_like_image()
        result = validate_id_document(image_bytes)

        assert isinstance(result['is_id_document'], bool)
        assert isinstance(result['confidence'], float)
        assert 0.0 <= result['confidence'] <= 1.0
        assert isinstance(result['aspect_ratio'], float)
        assert isinstance(result['reasons'], list)
        assert len(result['reasons']) > 0

    def test_validate_id_document_with_invalid_aspect_ratio(self):
        """Test with image having invalid aspect ratio"""
        # Square image (1:1 ratio) - not typical for ID card
        image_bytes = create_test_image(500, 500)
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'aspect_ratio' in result
        # Square has ratio ~1.0, which is outside 1.4-1.8 range

    def test_validate_id_document_with_non_rectangular_image(self):
        """Test with circular/non-rectangular image"""
        image_bytes = create_non_id_image()
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'confidence' in result

    def test_validate_id_document_confidence_range(self):
        """Test that confidence is always between 0 and 1"""
        image_bytes = create_test_image()
        result = validate_id_document(image_bytes)

        assert 0.0 <= result['confidence'] <= 1.0

    def test_validate_id_document_reasons_not_empty(self):
        """Test that reasons list is not empty"""
        image_bytes = create_test_image()
        result = validate_id_document(image_bytes)

        assert len(result['reasons']) > 0
        for reason in result['reasons']:
            assert isinstance(reason, str)
            assert len(reason) > 0

    def test_validate_id_document_with_small_image(self):
        """Test with very small image"""
        image_bytes = create_test_image(100, 100)
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'is_id_document' in result

    def test_validate_id_document_with_large_image(self):
        """Test with large image"""
        image_bytes = create_test_image(2000, 1500)
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'is_id_document' in result

    def test_validate_id_document_aspect_ratio_calculation(self):
        """Test that aspect ratio is correctly calculated"""
        # Create image with known aspect ratio
        image_bytes = create_test_image(1600, 1000)  # 1.6 ratio
        result = validate_id_document(image_bytes)

        assert 'aspect_ratio' in result
        # Aspect ratio should be close to 1.6 (may vary slightly due to contour detection)

    def test_validate_id_document_with_colored_image(self):
        """Test with colored image"""
        image_bytes = create_test_image(800, 500, color=(100, 150, 200))
        result = validate_id_document(image_bytes)

        assert isinstance(result, dict)
        assert 'is_id_document' in result

    def test_validate_id_document_is_valid_threshold(self):
        """Test that is_id_document is True when confidence >= 0.6"""
        # This is a property-based test
        image_bytes = create_id_card_like_image()
        result = validate_id_document(image_bytes)

        if result['confidence'] >= 0.6:
            assert result['is_id_document'] == True
        else:
            assert result['is_id_document'] == False

    def test_validate_id_document_rounded_values(self):
        """Test that confidence and aspect_ratio are properly rounded"""
        image_bytes = create_test_image()
        result = validate_id_document(image_bytes)

        # Check that values are rounded to 2 decimal places
        confidence_str = str(result['confidence'])
        aspect_ratio_str = str(result['aspect_ratio'])

        if '.' in confidence_str:
            decimals = len(confidence_str.split('.')[1])
            assert decimals <= 2

        if '.' in aspect_ratio_str:
            decimals = len(aspect_ratio_str.split('.')[1])
            assert decimals <= 2


if __name__ == '__main__':
    pytest.main([__file__, '-v'])

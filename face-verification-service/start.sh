#!/bin/bash

echo "Starting Face Verification Service..."
echo ""

if [ ! -d "venv" ]; then
    echo "Virtual environment not found. Creating one..."
    python3 -m venv venv
    echo ""
fi

echo "Activating virtual environment..."
source venv/bin/activate

echo ""
echo "Installing/updating dependencies..."
pip install -r requirements.txt

echo ""
echo "Starting FastAPI server on http://localhost:8000"
echo "Press Ctrl+C to stop"
echo ""
python main.py

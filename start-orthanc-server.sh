#!/bin/bash

# Quick Orthanc DICOM Server Setup Script

echo "=========================================="
echo "Starting Orthanc DICOM Server with Docker"
echo "=========================================="

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed."
    echo "Please install Docker first:"
    echo "  Ubuntu/Debian: sudo apt install docker.io"
    echo "  macOS: brew install docker"
    echo "  Windows: Download from docker.com"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "Error: Docker is not running."
    echo "Please start Docker first."
    exit 1
fi

echo "Pulling Orthanc Docker image..."
docker pull orthancteam/orthanc

echo "Starting Orthanc DICOM Server..."
echo ""
echo "Server Details:"
echo "  DICOM Port: 4242"
echo "  Web Interface: http://localhost:8042"
echo "  Username: orthanc"
echo "  Password: orthanc"
echo "  AET: ORTHANC"
echo ""
echo "Configure your web application with:"
echo "  mvn jetty:run -Ddicom.server.hostname=localhost \\"
echo "                -Ddicom.server.port=4242 \\"
echo "                -Ddicom.calling.aet=WEBAPP \\"
echo "                -Ddicom.called.aet=ORTHANC"
echo ""
echo "Press Ctrl+C to stop the server"
echo "=========================================="

# Run Orthanc with persistent storage
docker run --name orthanc-dicom \
           -p 4242:4242 \
           -p 8042:8042 \
           -v orthanc-db:/var/lib/orthanc/db \
           --rm \
           orthancteam/orthanc
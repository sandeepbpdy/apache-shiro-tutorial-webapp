#!/bin/bash

# Build and Run Script for DICOM Web Application

echo "==================================="
echo "DICOM Web Application Build Script"
echo "==================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

echo "Building the application..."
mvn clean package

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo ""
    echo "Starting the application..."
    echo "Access the application at: http://localhost:8080"
    echo "DICOM Viewer at: http://localhost:8080/dicom/viewer"
    echo ""
    echo "To configure DICOM server, set these system properties:"
    echo "  -Ddicom.server.hostname=your-server"
    echo "  -Ddicom.server.port=11112"
    echo "  -Ddicom.calling.aet=WEBAPP"
    echo "  -Ddicom.called.aet=DCMSERVER"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo "==================================="
    
    # Start the Jetty server
    mvn jetty:run
else
    echo "Build failed! Please check the error messages above."
    exit 1
fi
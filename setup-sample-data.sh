#!/bin/bash

# Script to download and upload sample DICOM files to Orthanc

echo "============================================"
echo "Setting up Sample DICOM Data for Testing"
echo "============================================"

# Create directory for sample files
mkdir -p sample-dicom-files
cd sample-dicom-files

echo "Downloading sample DICOM files..."

# Download sample DICOM files from various sources
echo "1. Downloading MR Heart image..."
curl -L -o MR-MONO2-8-16x-heart.dcm "https://barre.dev/medical/samples/files/MR-MONO2-8-16x-heart.dcm" 2>/dev/null

echo "2. Downloading CT Ankle image..."
curl -L -o CT-MONO2-16-ankle.dcm "https://barre.dev/medical/samples/files/CT-MONO2-16-ankle.dcm" 2>/dev/null

echo "3. Downloading US Echo image..."
curl -L -o US-MONO2-8-8x-execho.dcm "https://barre.dev/medical/samples/files/US-MONO2-8-8x-execho.dcm" 2>/dev/null

echo ""
echo "Sample files downloaded:"
ls -la *.dcm 2>/dev/null || echo "No DICOM files found - download may have failed"

echo ""
echo "============================================"
echo "Uploading to Orthanc Server"
echo "============================================"

# Wait for user confirmation
echo "Make sure Orthanc is running first!"
echo "Press Enter to continue with upload, or Ctrl+C to cancel..."
read

# Check if Orthanc is accessible
echo "Testing Orthanc connection..."
if curl -s http://localhost:8042/system >/dev/null 2>&1; then
    echo "✓ Orthanc is accessible"
else
    echo "✗ Cannot connect to Orthanc at http://localhost:8042"
    echo "Please start Orthanc first with: ./start-orthanc-server.sh"
    exit 1
fi

# Upload each DICOM file
for file in *.dcm; do
    if [ -f "$file" ]; then
        echo "Uploading $file..."
        response=$(curl -s -X POST http://localhost:8042/instances \
                       -u orthanc:orthanc \
                       --data-binary @"$file" \
                       -H "Content-Type: application/dicom" \
                       -w "%{http_code}")
        
        if [[ "$response" == *"200"* ]] || [[ "$response" == *"201"* ]]; then
            echo "✓ Successfully uploaded $file"
        else
            echo "✗ Failed to upload $file (HTTP: $response)"
        fi
    fi
done

echo ""
echo "============================================"
echo "Upload Complete!"
echo "============================================"
echo ""
echo "You can now:"
echo "1. View uploaded images at: http://localhost:8042"
echo "2. Test your web application at: http://localhost:8080/dicom/viewer"
echo ""
echo "Sample patient data to search for:"
echo "  Patient Name: Try searching for common names"
echo "  Patient ID: Various IDs depending on the samples"
echo ""
echo "To see all studies in Orthanc:"
echo "  curl -u orthanc:orthanc http://localhost:8042/studies"

cd ..
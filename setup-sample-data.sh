#!/bin/bash

# Script to create and upload sample DICOM files to Orthanc

echo "============================================"
echo "Setting up Sample DICOM Data for Testing"
echo "============================================"

# Method 1: Try to create DICOM files with Python (recommended)
echo "Method 1: Creating synthetic DICOM files with Python..."
if command -v python3 &> /dev/null; then
    # Try to install pydicom if not available
    python3 -c "import pydicom" 2>/dev/null || {
        echo "Installing pydicom..."
        pip3 install pydicom numpy --user 2>/dev/null || {
            echo "Failed to install pydicom. Trying alternative methods..."
        }
    }
    
    # Try to create DICOM files
    if python3 -c "import pydicom" 2>/dev/null; then
        python3 ../create-test-dicom.py
        if [ $? -eq 0 ]; then
            echo "✓ Successfully created synthetic DICOM files"
            DICOM_CREATED=true
        fi
    fi
fi

# Method 2: Download from known working sources
if [ "$DICOM_CREATED" != "true" ]; then
    echo ""
    echo "Method 2: Downloading sample DICOM files..."
    
    # Create directory for sample files
    mkdir -p sample-dicom-files
    cd sample-dicom-files
    
    echo "1. Downloading from DICOM Web Viewer samples..."
    curl -L -o sample_ct.dcm "https://github.com/cornerstonejs/cornerstoneWADOImageLoader/raw/master/testImages/CTImage.dcm" 2>/dev/null
    
    echo "2. Downloading from dcmjs samples..."
    curl -L -o sample_mr.dcm "https://github.com/dcmjs-org/data/raw/master/dcm/MR-MONO2-8-16x-heart.dcm" 2>/dev/null
    
    echo "3. Downloading from OHIF samples..."
    curl -L -o sample_us.dcm "https://github.com/OHIF/Viewers/raw/master/public/dcm/1.2.840.113619.2.5.1762583153.215519.978957063.78.dcm" 2>/dev/null
    
    cd ..
fi

# Validate downloaded files
echo ""
echo "Validating DICOM files..."
cd sample-dicom-files 2>/dev/null || { echo "No sample-dicom-files directory found"; exit 1; }

valid_files=0
for file in *.dcm; do
    if [ -f "$file" ]; then
        # Check if file is valid DICOM by looking for DICOM magic bytes
        if file "$file" | grep -q "DICOM" 2>/dev/null; then
            echo "✓ $file appears to be valid DICOM"
            valid_files=$((valid_files + 1))
        elif [ $(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null) -gt 1000 ]; then
            echo "? $file might be valid (size: $(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null) bytes)"
            valid_files=$((valid_files + 1))
        else
            echo "✗ $file appears invalid (size: $(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null) bytes)"
            rm -f "$file"
        fi
    fi
done

if [ $valid_files -eq 0 ]; then
    echo ""
    echo "⚠️  No valid DICOM files found. Creating minimal test file..."
    # Create a minimal valid DICOM file manually
    python3 -c "
import struct
import os

# Create minimal DICOM file
preamble = b'\\x00' * 128
prefix = b'DICM'
# Minimal dataset with required elements
data = b'\\x08\\x00\\x05\\x00\\x40\\x00\\x00\\x00\\x31\\x2e\\x32\\x2e\\x38\\x34\\x30\\x2e\\x31\\x30\\x30\\x30\\x38\\x2e\\x31\\x2e\\x32\\x00'

with open('minimal_test.dcm', 'wb') as f:
    f.write(preamble + prefix + data)

print('Created minimal_test.dcm')
" 2>/dev/null && valid_files=1
fi

echo ""
echo "Found $valid_files valid DICOM files:"
ls -la *.dcm 2>/dev/null

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
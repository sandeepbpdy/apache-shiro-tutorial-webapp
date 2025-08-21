#!/bin/bash

# Script to upload DICOM files to Orthanc with validation

echo "=========================================="
echo "DICOM File Upload Script"
echo "=========================================="

# Function to validate DICOM file
validate_dicom() {
    local file="$1"
    
    if [ ! -f "$file" ]; then
        echo "✗ File not found: $file"
        return 1
    fi
    
    # Check file size (DICOM files should be > 132 bytes minimum)
    local size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
    if [ "$size" -lt 132 ]; then
        echo "✗ $file too small ($size bytes) - not a valid DICOM file"
        return 1
    fi
    
    # Check for DICM magic bytes at offset 128
    if command -v hexdump &> /dev/null; then
        local magic=$(hexdump -s 128 -n 4 -e '4/1 "%c"' "$file" 2>/dev/null)
        if [ "$magic" != "DICM" ]; then
            echo "? $file missing DICM magic bytes - might not be valid DICOM"
            return 2  # Warning, not error
        fi
    fi
    
    echo "✓ $file appears valid ($size bytes)"
    return 0
}

# Function to upload file to Orthanc
upload_to_orthanc() {
    local file="$1"
    
    echo "Uploading $file..."
    
    # Try to upload
    local response=$(curl -s -w "%{http_code}" -X POST http://localhost:8042/instances \
                         -u orthanc:orthanc \
                         --data-binary @"$file" \
                         -H "Content-Type: application/dicom" \
                         -o /tmp/orthanc_response.json)
    
    if [[ "$response" == "200" ]] || [[ "$response" == "201" ]]; then
        echo "✓ Successfully uploaded $file"
        # Show instance details if available
        if [ -f "/tmp/orthanc_response.json" ]; then
            local instance_id=$(grep -o '"ID":"[^"]*"' /tmp/orthanc_response.json | cut -d'"' -f4)
            if [ -n "$instance_id" ]; then
                echo "  Instance ID: $instance_id"
            fi
        fi
        return 0
    else
        echo "✗ Failed to upload $file (HTTP: $response)"
        if [ -f "/tmp/orthanc_response.json" ]; then
            echo "  Error details:"
            cat /tmp/orthanc_response.json | head -5
        fi
        return 1
    fi
}

# Main script
if [ $# -eq 0 ]; then
    echo "Usage: $0 <dicom-file1> [dicom-file2] ..."
    echo "   or: $0 /path/to/dicom/directory/"
    echo ""
    echo "Examples:"
    echo "  $0 sample.dcm"
    echo "  $0 *.dcm"
    echo "  $0 sample-dicom-files/"
    exit 1
fi

# Check if Orthanc is accessible
echo "Testing Orthanc connection..."
if ! curl -s http://localhost:8042/system >/dev/null 2>&1; then
    echo "✗ Cannot connect to Orthanc at http://localhost:8042"
    echo "Please start Orthanc first with: ./start-orthanc-server.sh"
    exit 1
fi
echo "✓ Orthanc is accessible"
echo ""

# Process arguments
uploaded=0
failed=0

for arg in "$@"; do
    if [ -d "$arg" ]; then
        # Process directory
        echo "Processing directory: $arg"
        for file in "$arg"/*.dcm "$arg"/*.DCM; do
            if [ -f "$file" ]; then
                validate_dicom "$file"
                validation_result=$?
                
                if [ $validation_result -eq 0 ] || [ $validation_result -eq 2 ]; then
                    if upload_to_orthanc "$file"; then
                        uploaded=$((uploaded + 1))
                    else
                        failed=$((failed + 1))
                    fi
                else
                    failed=$((failed + 1))
                fi
                echo ""
            fi
        done
    elif [ -f "$arg" ]; then
        # Process individual file
        validate_dicom "$arg"
        validation_result=$?
        
        if [ $validation_result -eq 0 ] || [ $validation_result -eq 2 ]; then
            if upload_to_orthanc "$arg"; then
                uploaded=$((uploaded + 1))
            else
                failed=$((failed + 1))
            fi
        else
            failed=$((failed + 1))
        fi
        echo ""
    else
        echo "✗ Not found: $arg"
        failed=$((failed + 1))
        echo ""
    fi
done

# Summary
echo "=========================================="
echo "Upload Summary:"
echo "  Successfully uploaded: $uploaded files"
echo "  Failed: $failed files"
echo "=========================================="

if [ $uploaded -gt 0 ]; then
    echo ""
    echo "You can now:"
    echo "1. View uploaded images at: http://localhost:8042"
    echo "2. Test your web application at: http://localhost:8080/dicom/viewer"
    echo ""
    echo "To see all studies in Orthanc:"
    echo "  curl -u orthanc:orthanc http://localhost:8042/studies"
fi

# Clean up
rm -f /tmp/orthanc_response.json

exit $([ $failed -eq 0 ] && echo 0 || echo 1)
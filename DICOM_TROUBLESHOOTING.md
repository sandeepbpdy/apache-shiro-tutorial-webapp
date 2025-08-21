# DICOM Upload Troubleshooting Guide

This guide helps resolve common issues when uploading DICOM files to Orthanc.

## 🚨 Error: "Cannot parse an invalid DICOM file"

### Problem
```json
{
   "Details" : "Cannot parse an invalid DICOM file (size: 2156 bytes)",
   "HttpError" : "Bad Request",
   "HttpStatus" : 400,
   "Message" : "Bad file format",
   "Method" : "POST",
   "OrthancError" : "Bad file format",
   "OrthancStatus" : 15,
   "Uri" : "/instances"
}
```

### Root Causes

1. **Downloaded HTML instead of DICOM file**
   - Web servers may return HTML error pages instead of actual files
   - File appears to exist but contains HTML content

2. **Corrupted download**
   - Network issues during download
   - Incomplete file transfer

3. **Wrong file format**
   - File is not actually a DICOM file
   - Missing DICOM header or magic bytes

4. **File too small**
   - Valid DICOM files must be at least 132+ bytes
   - Your file (2156 bytes) might be an error page

## 🔧 Solutions

### Solution 1: Use the Fixed Scripts (Recommended)

```bash
# Use the new improved script that creates valid DICOM files
./setup-sample-data.sh
```

This script now:
- ✅ Creates synthetic DICOM files with Python
- ✅ Validates files before upload
- ✅ Falls back to multiple download sources
- ✅ Checks file integrity

### Solution 2: Manual DICOM File Creation

```bash
# Install pydicom if not available
pip3 install pydicom numpy --user

# Create valid test DICOM files
python3 create-test-dicom.py

# Upload with validation
./upload-dicom-files.sh sample-dicom-files/
```

### Solution 3: Download Known Working DICOM Files

```bash
# Create directory
mkdir valid-dicom-samples
cd valid-dicom-samples

# Download from dcmjs repository (known working)
curl -L -o heart.dcm "https://github.com/dcmjs-org/data/raw/master/dcm/MR-MONO2-8-16x-heart.dcm"

# Validate before upload
../upload-dicom-files.sh heart.dcm
```

### Solution 4: Manual File Validation

```bash
# Check if file is actually DICOM
file your-file.dcm

# Check for DICOM magic bytes
hexdump -C your-file.dcm | head -10

# Look for "DICM" at offset 128 (0x80)
hexdump -s 128 -n 4 -C your-file.dcm
```

Valid DICOM files should show:
```
00000080  44 49 43 4d                                      |DICM|
```

## 🔍 Diagnostic Commands

### Check File Contents
```bash
# See what's actually in the file
head -20 your-file.dcm

# If you see HTML tags, it's not a DICOM file
```

### Validate with dcmtk (if available)
```bash
# Install dcmtk tools
sudo apt install dcmtk  # Ubuntu/Debian
brew install dcmtk       # macOS

# Validate DICOM file
dcmdump your-file.dcm
```

### Check Orthanc Logs
```bash
# If running Orthanc in Docker
docker logs orthanc-dicom

# Look for detailed error messages
```

## 🆘 Quick Fixes

### Fix 1: Clean Start
```bash
# Remove any invalid files
rm -rf sample-dicom-files/

# Create fresh valid DICOM files
python3 create-test-dicom.py

# Upload with validation
./upload-dicom-files.sh sample-dicom-files/
```

### Fix 2: Use Alternative Upload Method
```bash
# Upload via Orthanc web interface instead
# 1. Go to http://localhost:8042
# 2. Login: orthanc/orthanc  
# 3. Click "Upload" and select files
```

### Fix 3: Test with Minimal DICOM
```bash
# Create minimal valid DICOM file
python3 -c "
# Minimal DICOM file creation
preamble = b'\\x00' * 128
prefix = b'DICM'
# Basic required elements
data = b'\\x08\\x00\\x05\\x00\\x40\\x00\\x00\\x00\\x31\\x2e\\x32\\x2e\\x38\\x34\\x30\\x2e\\x31\\x30\\x30\\x30\\x38\\x2e\\x31\\x2e\\x32\\x00'

with open('minimal.dcm', 'wb') as f:
    f.write(preamble + prefix + data)
print('Created minimal.dcm')
"

# Test upload
./upload-dicom-files.sh minimal.dcm
```

## ✅ Prevention

### Always Validate Files
```bash
# Use the validation script before uploading
./upload-dicom-files.sh your-files/
```

### Check File Sizes
```bash
# DICOM files should typically be > 1KB
ls -la *.dcm
```

### Use Reliable Sources
- ✅ **dcmjs-org/data** repository
- ✅ **Synthetic files** created with pydicom
- ✅ **Medical imaging test datasets**
- ❌ Random web downloads without validation

## 🔄 Alternative Test Data Sources

### Option 1: DICOM Library Samples
```bash
# From cornerstone.js
curl -L -o ct_sample.dcm "https://github.com/cornerstonejs/cornerstoneWADOImageLoader/raw/master/testImages/CTImage.dcm"
```

### Option 2: Create Your Own
```bash
# Use the Python script (recommended)
python3 create-test-dicom.py
```

### Option 3: DCMTK Test Files
```bash
# If you have dcmtk installed
img2dcm /path/to/image.png test.dcm
```

## 📞 Getting Help

If you're still having issues:

1. **Check the file contents:**
   ```bash
   head -5 your-problem-file.dcm
   ```

2. **Validate with our script:**
   ```bash
   ./upload-dicom-files.sh your-problem-file.dcm
   ```

3. **Try the synthetic files:**
   ```bash
   python3 create-test-dicom.py
   ./upload-dicom-files.sh sample-dicom-files/
   ```

4. **Check Orthanc status:**
   ```bash
   curl -u orthanc:orthanc http://localhost:8042/system
   ```

The key is to ensure you have **valid DICOM files** before attempting upload. The error you encountered is almost always due to trying to upload non-DICOM content (like HTML error pages) as DICOM files.
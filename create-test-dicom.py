#!/usr/bin/env python3
"""
Create valid test DICOM files for testing the DICOM viewer application.
This script creates synthetic DICOM files with proper headers and pixel data.
"""

import os
import sys
import numpy as np
from datetime import datetime

try:
    import pydicom
    from pydicom.dataset import Dataset, FileDataset
    from pydicom.uid import generate_uid
except ImportError:
    print("Error: pydicom is required. Install with: pip install pydicom")
    sys.exit(1)

def create_test_dicom(filename, modality="CT", patient_name="Test^Patient", patient_id="12345"):
    """Create a valid DICOM file with synthetic image data."""
    
    print(f"Creating {filename}...")
    
    # Create file meta information
    file_meta = Dataset()
    file_meta.MediaStorageSOPClassUID = '1.2.840.10008.5.1.4.1.1.2'  # CT Image Storage
    file_meta.MediaStorageSOPInstanceUID = generate_uid()
    file_meta.ImplementationClassUID = generate_uid()
    file_meta.TransferSyntaxUID = '1.2.840.10008.1.2'  # Implicit VR Little Endian
    
    # Create the main dataset
    ds = FileDataset(filename, {}, file_meta=file_meta, preamble=b"\0" * 128)
    
    # Add patient information
    ds.PatientName = patient_name
    ds.PatientID = patient_id
    ds.PatientBirthDate = '19800101'
    ds.PatientSex = 'M'
    
    # Add study information
    ds.StudyInstanceUID = generate_uid()
    ds.StudyID = '1'
    ds.StudyDate = datetime.now().strftime('%Y%m%d')
    ds.StudyTime = datetime.now().strftime('%H%M%S')
    ds.StudyDescription = f'Test {modality} Study'
    
    # Add series information
    ds.SeriesInstanceUID = generate_uid()
    ds.SeriesNumber = '1'
    ds.SeriesDate = ds.StudyDate
    ds.SeriesTime = ds.StudyTime
    ds.SeriesDescription = f'Test {modality} Series'
    ds.Modality = modality
    
    # Add instance information
    ds.SOPInstanceUID = generate_uid()
    ds.SOPClassUID = file_meta.MediaStorageSOPClassUID
    ds.InstanceNumber = '1'
    
    # Add image information
    ds.ImageType = ['ORIGINAL', 'PRIMARY', 'AXIAL']
    ds.Rows = 256
    ds.Columns = 256
    ds.BitsAllocated = 16
    ds.BitsStored = 16
    ds.HighBit = 15
    ds.PixelRepresentation = 0  # unsigned
    ds.SamplesPerPixel = 1
    ds.PhotometricInterpretation = 'MONOCHROME2'
    
    # Create synthetic image data
    if modality == "CT":
        # CT-like image with some structure
        image = np.random.randint(0, 4096, (256, 256), dtype=np.uint16)
        # Add some circular structures
        y, x = np.ogrid[:256, :256]
        center_y, center_x = 128, 128
        mask = (x - center_x)**2 + (y - center_y)**2 < 50**2
        image[mask] = 2000
        mask2 = (x - center_x)**2 + (y - center_y)**2 < 30**2
        image[mask2] = 3000
    elif modality == "MR":
        # MR-like image
        image = np.random.randint(0, 1024, (256, 256), dtype=np.uint16)
        # Add brain-like structure
        y, x = np.ogrid[:256, :256]
        center_y, center_x = 128, 128
        mask = (x - center_x)**2 + (y - center_y)**2 < 100**2
        image[mask] = 800
    else:
        # Generic image
        image = np.random.randint(0, 1024, (256, 256), dtype=np.uint16)
    
    # Set pixel data
    ds.PixelData = image.tobytes()
    
    # Add required elements
    ds.is_little_endian = True
    ds.is_implicit_VR = True
    
    # Save the file
    ds.save_as(filename)
    print(f"✓ Created {filename} ({os.path.getsize(filename)} bytes)")

def validate_dicom_file(filename):
    """Validate that a DICOM file can be read properly."""
    try:
        ds = pydicom.dcmread(filename)
        print(f"✓ {filename} is valid DICOM")
        print(f"  Patient: {ds.PatientName}")
        print(f"  Modality: {ds.Modality}")
        print(f"  Image Size: {ds.Rows}x{ds.Columns}")
        return True
    except Exception as e:
        print(f"✗ {filename} validation failed: {e}")
        return False

def main():
    """Create test DICOM files."""
    print("Creating Test DICOM Files")
    print("========================")
    
    # Create output directory
    os.makedirs("sample-dicom-files", exist_ok=True)
    os.chdir("sample-dicom-files")
    
    # Create different types of DICOM files
    test_files = [
        ("test_ct_head.dcm", "CT", "Doe^John", "CT001"),
        ("test_mr_brain.dcm", "MR", "Smith^Jane", "MR001"),
        ("test_cr_chest.dcm", "CR", "Johnson^Bob", "CR001"),
    ]
    
    created_files = []
    for filename, modality, patient_name, patient_id in test_files:
        try:
            create_test_dicom(filename, modality, patient_name, patient_id)
            if validate_dicom_file(filename):
                created_files.append(filename)
        except Exception as e:
            print(f"✗ Failed to create {filename}: {e}")
    
    print(f"\nSuccessfully created {len(created_files)} DICOM files:")
    for filename in created_files:
        print(f"  - {filename}")
    
    print("\nThese files are ready to upload to Orthanc!")
    return len(created_files) > 0

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
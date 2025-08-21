# DICOM Server Setup Guide

This guide provides step-by-step instructions for setting up and running various DICOM servers to test with your web application.

## Option 1: Orthanc DICOM Server (Recommended - Easiest)

Orthanc is a lightweight, standalone DICOM server perfect for development and testing.

### Installation Methods

#### Method 1A: Using Docker (Fastest)

```bash
# Pull and run Orthanc with default settings
docker run -p 4242:4242 -p 8042:8042 --rm orthancteam/orthanc

# Or with persistent storage
docker run -p 4242:4242 -p 8042:8042 -v orthanc-db:/var/lib/orthanc/db --rm orthancteam/orthanc
```

**Access:**
- DICOM port: `4242`
- Web interface: http://localhost:8042
- Default credentials: `orthanc` / `orthanc`

#### Method 1B: Direct Installation

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install orthanc
sudo systemctl start orthanc
sudo systemctl enable orthanc
```

**CentOS/RHEL/Fedora:**
```bash
sudo yum install orthanc
# or for newer versions:
sudo dnf install orthanc
sudo systemctl start orthanc
```

**macOS (using Homebrew):**
```bash
brew install orthanc
orthanc
```

**Windows:**
1. Download from: https://www.orthanc-server.com/download.php
2. Extract and run `Orthanc.exe`

### Configure Your Application for Orthanc

```bash
# Run your web application with Orthanc settings
mvn jetty:run -Ddicom.server.hostname=localhost \
              -Ddicom.server.port=4242 \
              -Ddicom.calling.aet=WEBAPP \
              -Ddicom.called.aet=ORTHANC
```

---

## Option 2: DCM4CHEE Archive (Enterprise-grade)

DCM4CHEE is a more comprehensive DICOM archive solution.

### Using Docker Compose (Recommended)

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  ldap:
    image: dcm4che/slapd-dcm4chee:2.6.5-31.2
    ports:
      - "389:389"
    environment:
      STORAGE_DIR: /storage/fs1
    volumes:
      - dcm4chee-ldap:/var/lib/openldap/openldap-data
      - dcm4chee-slapd:/etc/openldap/slapd.d

  db:
    image: dcm4che/postgres-dcm4chee:15.4-31
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: pacsdb
      POSTGRES_USER: pacs
      POSTGRES_PASSWORD: pacs
    volumes:
      - dcm4chee-db:/var/lib/postgresql/data

  arc:
    image: dcm4che/dcm4chee-arc-psql:5.31.2
    ports:
      - "8080:8080"
      - "8443:8443"
      - "9990:9990"
      - "9993:9993"
      - "11112:11112"
      - "2762:2762"
      - "2575:2575"
      - "12575:12575"
    environment:
      POSTGRES_DB: pacsdb
      POSTGRES_USER: pacs
      POSTGRES_PASSWORD: pacs
      POSTGRES_HOST: db
      WILDFLY_CHOWN: /storage
      WILDFLY_WAIT_FOR: ldap:389 db:5432
    depends_on:
      - ldap
      - db
    volumes:
      - dcm4chee-arc:/storage

volumes:
  dcm4chee-ldap:
  dcm4chee-slapd:
  dcm4chee-db:
  dcm4chee-arc:
```

**Start DCM4CHEE:**
```bash
docker-compose up -d
```

**Access:**
- DICOM port: `11112`
- Web interface: http://localhost:8080/dcm4chee-arc/ui2
- Default credentials: `admin` / `admin`

### Configure Your Application for DCM4CHEE

```bash
mvn jetty:run -Ddicom.server.hostname=localhost \
              -Ddicom.server.port=11112 \
              -Ddicom.calling.aet=WEBAPP \
              -Ddicom.called.aet=DCM4CHEE
```

---

## Option 3: Quick Test Server with DCMTK

For basic testing, you can use DCMTK tools to create a simple DICOM server.

### Install DCMTK

**Ubuntu/Debian:**
```bash
sudo apt install dcmtk
```

**macOS:**
```bash
brew install dcmtk
```

### Run Simple Storage SCP

```bash
# Create a directory for DICOM files
mkdir /tmp/dicom-storage

# Start a simple DICOM storage server
storescp -v -aet STORESCP -od /tmp/dicom-storage 11112
```

---

## Testing Your DICOM Server

### 1. Test with DICOM Tools

**Test C-ECHO (connection):**
```bash
# For Orthanc
echoscu -v localhost 4242 -aet WEBAPP -aec ORTHANC

# For DCM4CHEE
echoscu -v localhost 11112 -aet WEBAPP -aec DCM4CHEE
```

**Send test DICOM file:**
```bash
# For Orthanc
storescu -v localhost 4242 -aet WEBAPP -aec ORTHANC /path/to/test.dcm

# For DCM4CHEE
storescu -v localhost 11112 -aet WEBAPP -aec DCM4CHEE /path/to/test.dcm
```

### 2. Test with Your Web Application

1. **Start your web application:**
   ```bash
   ./build-and-run.sh
   ```

2. **Open DICOM Viewer:**
   http://localhost:8080/dicom/viewer

3. **Test Connection:**
   Click "Test DICOM Server Connection"

---

## Getting Sample DICOM Files

### Download Sample Files

```bash
# Create samples directory
mkdir sample-dicom-files
cd sample-dicom-files

# Download sample DICOM files
wget https://barre.dev/medical/samples/files/MR-MONO2-8-16x-heart.dcm
wget https://barre.dev/medical/samples/files/CT-MONO2-16-ankle.dcm
wget https://barre.dev/medical/samples/files/US-MONO2-8-8x-execho.dcm
```

### Upload to Your DICOM Server

**For Orthanc (using REST API):**
```bash
curl -X POST http://localhost:8042/instances \
     -u orthanc:orthanc \
     --data-binary @MR-MONO2-8-16x-heart.dcm \
     -H "Content-Type: application/dicom"
```

**For DCM4CHEE (using storescu):**
```bash
storescu -v localhost 11112 -aet WEBAPP -aec DCM4CHEE MR-MONO2-8-16x-heart.dcm
```

---

## Troubleshooting

### Common Issues

1. **Port Already in Use:**
   ```bash
   # Check what's using the port
   sudo netstat -tlnp | grep :4242
   # or
   sudo lsof -i :4242
   ```

2. **Connection Refused:**
   - Check if DICOM server is running
   - Verify firewall settings
   - Ensure correct hostname/port configuration

3. **Authentication Failed:**
   - Verify AET (Application Entity Title) settings
   - Check server configuration for allowed calling AETs

### Debug Commands

```bash
# Check if Orthanc is running
curl http://localhost:8042/system

# Check DCM4CHEE status
curl http://localhost:8080/dcm4chee-arc/aets

# Test network connectivity
telnet localhost 4242  # for Orthanc
telnet localhost 11112 # for DCM4CHEE
```

---

## Configuration Summary

| Server | DICOM Port | Web Port | Default AET | Web Interface |
|--------|------------|----------|-------------|---------------|
| Orthanc | 4242 | 8042 | ORTHANC | http://localhost:8042 |
| DCM4CHEE | 11112 | 8080 | DCM4CHEE | http://localhost:8080/dcm4chee-arc/ui2 |
| DCMTK storescp | 11112 | - | STORESCP | - |

Choose the option that best fits your needs:
- **Orthanc**: Best for development and testing
- **DCM4CHEE**: Best for production-like environments
- **DCMTK**: Best for minimal testing scenarios
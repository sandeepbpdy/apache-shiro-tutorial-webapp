# Apache Shiro Web App with DICOM Image Reading

A web application that demonstrates Apache Shiro security features enhanced with DICOM server connectivity for medical image processing.

## Features

- **Apache Shiro Security**: Authentication and authorization framework
- **DICOM Integration**: Connect to DICOM servers and retrieve medical images
- **Web-based DICOM Viewer**: Browser-based interface for viewing DICOM images
- **Metadata Display**: View DICOM tag information and patient data
- **Multiple Server Support**: Compatible with various DICOM servers (DCM4CHEE, Orthanc, etc.)

## DICOM Capabilities

### Supported Operations
- **C-ECHO**: Test connectivity to DICOM servers
- **C-FIND**: Search for studies based on patient information
- **C-GET**: Retrieve DICOM images from the server
- **Image Conversion**: Convert DICOM images to web-displayable formats (PNG)

### Supported DICOM Servers
- DCM4CHEE
- Orthanc
- Any DICOM server supporting Study Root Query/Retrieve

## Quick Start

### Prerequisites
- Java 8 or higher
- Maven 3.x
- Access to a DICOM server (for testing)

### Build and Run

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd apache-shiro-webapp-tutorial
   mvn clean package
   ```

2. **Run with Maven Jetty plugin:**
   ```bash
   mvn jetty:run
   ```

3. **Access the application:**
   - Main page: http://localhost:8080
   - DICOM Viewer: http://localhost:8080/dicom/viewer

### DICOM Server Configuration

Configure your DICOM server connection using system properties:

```bash
mvn jetty:run -Ddicom.server.hostname=your-dicom-server \
              -Ddicom.server.port=11112 \
              -Ddicom.calling.aet=WEBAPP \
              -Ddicom.called.aet=DCMSERVER
```

Or modify the `src/main/resources/dicom.properties` file:

```properties
dicom.server.hostname=localhost
dicom.server.port=11112
dicom.calling.aet=WEBAPP
dicom.called.aet=DCMSERVER
```

## DICOM Server Setup (for Testing)

### Option 1: Orthanc (Lightweight)

1. **Install Orthanc:**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install orthanc
   
   # macOS with Homebrew
   brew install orthanc
   ```

2. **Start Orthanc:**
   ```bash
   orthanc
   ```
   Default DICOM port: 4242, Web interface: http://localhost:8042

3. **Configure application:**
   ```bash
   mvn jetty:run -Ddicom.server.hostname=localhost \
                 -Ddicom.server.port=4242 \
                 -Ddicom.called.aet=ORTHANC
   ```

### Option 2: DCM4CHEE

1. **Download and install DCM4CHEE Archive**
2. **Start the server** (default port: 11112)
3. **Configure application** with appropriate AET settings

## Usage Guide

### 1. Test Connection
- Open the DICOM Viewer
- Click "Test DICOM Server Connection"
- Verify successful connection

### 2. Search for Studies
- Enter Patient ID or Patient Name
- Click "Search Studies"
- Review found studies and their metadata

### 3. Retrieve Images
- Copy Study UID, Series UID, and SOP UID from search results
- Paste into the "Retrieve Image" section
- Click "Retrieve Image" to view the DICOM image

## API Endpoints

### REST API
- `GET /dicom/test` - Test DICOM server connection
- `GET /dicom/search?patientId=123&patientName=Doe` - Search studies
- `GET /dicom/image?studyUID=...&seriesUID=...&sopUID=...` - Retrieve image
- `GET /dicom/image?...&format=image` - Get raw image data

### Web Interface
- `GET /dicom/viewer` - DICOM viewer web interface

## Dependencies

### DICOM Processing
- **dcm4che 5.29.0**: Core DICOM library
- **dcm4che-net**: DICOM networking
- **dcm4che-imageio**: Image I/O operations
- **jai-imageio-core**: Advanced image processing

### Web Framework
- **Apache Shiro**: Security framework
- **Jackson**: JSON processing
- **Servlet API**: Web servlet support

## Troubleshooting

### Common Issues

1. **Connection Timeout**
   - Verify DICOM server is running
   - Check hostname and port configuration
   - Ensure firewall allows connections

2. **Authentication Failed**
   - Verify AET (Application Entity Title) configuration
   - Check if server requires specific calling AET

3. **Images Not Displaying**
   - Verify DICOM file contains pixel data
   - Check if image format is supported
   - Review browser console for JavaScript errors

### Logging
Enable debug logging by adding to JVM arguments:
```bash
-Dlogback.configurationFile=logback-debug.xml
```

## Development

### Project Structure
```
src/main/java/com/foo/example/dicom/
├── DicomService.java          # Core DICOM operations
├── DicomController.java       # Web controller
├── DicomServerConfig.java     # Configuration
└── DicomImage.java           # Image data model
```

### Adding New Features
1. Extend `DicomService` for new DICOM operations
2. Add REST endpoints in `DicomController`
3. Update web interface as needed

## License

Licensed under the Apache License, Version 2.0

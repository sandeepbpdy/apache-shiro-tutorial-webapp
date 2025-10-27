# 🚀 Getting Started with DICOM Image Reading

This guide will get you up and running with DICOM image reading in just a few minutes!

## 📋 What You'll Need

- **Java 8+** (check with `java -version`)
- **Maven 3.x** (check with `mvn -version`)
- **Docker** (for the DICOM server)

## 🎯 Quick Start (5 Minutes)

### Step 1: Start the DICOM Server
```bash
# This starts Orthanc DICOM server using Docker
./start-orthanc-server.sh
```

**What this does:**
- Downloads and runs Orthanc DICOM server
- Exposes DICOM port 4242 and web interface on port 8042
- Creates persistent storage for DICOM images

**You'll see:** Server startup messages ending with "Orthanc has started"

### Step 2: Add Sample DICOM Data
Open a **new terminal** and run:
```bash
# This downloads and uploads sample DICOM images
./setup-sample-data.sh
```

**What this does:**
- Downloads sample medical images (MR, CT, US)
- Uploads them to your Orthanc server
- Creates test data for your application

### Step 3: Start Your Web Application
Open **another terminal** and run:
```bash
# This builds and starts your web application
./build-and-run.sh
```

**What this does:**
- Compiles the Java application
- Starts the web server on port 8080
- Automatically configures connection to Orthanc

### Step 4: Test Everything
Open your web browser and visit:

1. **Your DICOM Viewer**: http://localhost:8080/dicom/viewer
   - Click "Test DICOM Server Connection" ✅
   - Try searching for studies
   - Retrieve and view images

2. **Orthanc Web Interface**: http://localhost:8042
   - Login: `orthanc` / `orthanc`
   - Browse uploaded DICOM studies

## 🔧 What Each Script Does

### `start-orthanc-server.sh`
- Checks if Docker is installed and running
- Pulls the official Orthanc Docker image
- Starts Orthanc with proper port mappings
- Creates persistent volume for data storage

### `setup-sample-data.sh`
- Downloads sample DICOM files from medical imaging repositories
- Uploads them to Orthanc using REST API
- Provides test data for your application

### `build-and-run.sh`
- Compiles your Java application with Maven
- Starts Jetty web server
- Configures DICOM connection parameters

## 📊 Testing Your Setup

### 1. Connection Test
In the DICOM Viewer, click "Test DICOM Server Connection"
- ✅ **Success**: Shows "Connection successful"
- ❌ **Failed**: Check if Orthanc is running

### 2. Search for Studies
Try searching with:
- **Patient Name**: Leave blank or try common names
- **Patient ID**: Leave blank to see all studies

### 3. Retrieve Images
From search results:
1. Copy the Study UID, Series UID, and SOP UID
2. Paste them in the "Retrieve Image" section
3. Click "Retrieve Image" to view the DICOM image

## 🐛 Troubleshooting

### "Connection failed" Error
```bash
# Check if Orthanc is running
curl http://localhost:8042/system

# If not working, restart Orthanc
docker stop orthanc-dicom
./start-orthanc-server.sh
```

### "Port already in use" Error
```bash
# Find what's using the port
sudo lsof -i :8080  # for web app
sudo lsof -i :4242  # for DICOM
sudo lsof -i :8042  # for Orthanc web

# Stop conflicting services or use different ports
```

### No Studies Found
```bash
# Check if data was uploaded correctly
curl -u orthanc:orthanc http://localhost:8042/studies

# Re-run sample data setup
./setup-sample-data.sh
```

### Build Errors
```bash
# Clean and rebuild
mvn clean compile
mvn clean package
```

## 🌐 Web Interfaces

| Service | URL | Purpose | Credentials |
|---------|-----|---------|-------------|
| **Your DICOM App** | http://localhost:8080 | Main application | None |
| **DICOM Viewer** | http://localhost:8080/dicom/viewer | DICOM image viewer | None |
| **Orthanc Web** | http://localhost:8042 | DICOM server admin | orthanc/orthanc |

## 🔄 Starting/Stopping Services

### Start Everything
```bash
# Terminal 1: Start DICOM server
./start-orthanc-server.sh

# Terminal 2: Start web application  
./build-and-run.sh
```

### Stop Everything
- **Web Application**: Press `Ctrl+C` in terminal 2
- **DICOM Server**: Press `Ctrl+C` in terminal 1

### Restart Just the Web App
```bash
# Stop with Ctrl+C, then restart
./build-and-run.sh
```

## 📁 Project Structure
```
├── src/main/java/com/foo/example/dicom/
│   ├── DicomService.java          # DICOM operations
│   ├── DicomController.java       # Web endpoints
│   ├── DicomServerConfig.java     # Configuration
│   └── DicomImage.java           # Data model
├── start-orthanc-server.sh       # Start DICOM server
├── setup-sample-data.sh          # Add test data
├── build-and-run.sh              # Build and run app
└── DICOM_SERVER_SETUP.md         # Detailed server setup
```

## 🎉 Success!

If everything is working, you should be able to:
- ✅ Connect to the DICOM server
- ✅ Search for patient studies  
- ✅ Retrieve and display DICOM images
- ✅ View DICOM metadata

**Next Steps:**
- Try connecting to your own DICOM server
- Modify the code to add new features
- Explore the Orthanc REST API
- Add more sample data

## 🆘 Need Help?

1. **Check the logs** in the terminal where you started each service
2. **Review the detailed setup guide**: `DICOM_SERVER_SETUP.md`
3. **Test individual components** using the troubleshooting commands above
4. **Verify prerequisites** (Java, Maven, Docker versions)

Happy DICOM imaging! 🏥📸
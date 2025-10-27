package com.foo.example.dicom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web controller for handling DICOM-related HTTP requests
 */
@WebServlet(urlPatterns = {"/dicom/*"})
public class DicomController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DicomController.class);
    
    private DicomService dicomService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        
        // Initialize DICOM service with default configuration
        // In production, these values should come from configuration files
        DicomServerConfig config = new DicomServerConfig();
        
        // You can override defaults here or read from system properties
        String hostname = System.getProperty("dicom.server.hostname", "localhost");
        int port = Integer.parseInt(System.getProperty("dicom.server.port", "11112"));
        String callingAET = System.getProperty("dicom.calling.aet", "WEBAPP");
        String calledAET = System.getProperty("dicom.called.aet", "DCMSERVER");
        
        config.setHostname(hostname);
        config.setPort(port);
        config.setCallingAET(callingAET);
        config.setCalledAET(calledAET);
        
        this.dicomService = new DicomService(config);
        this.objectMapper = new ObjectMapper();
        
        logger.info("DICOM Controller initialized with server: {}:{}", hostname, port);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null) {
            response.sendRedirect(request.getContextPath() + "/dicom/viewer");
            return;
        }

        switch (pathInfo) {
            case "/test":
                handleTestConnection(request, response);
                break;
            case "/search":
                handleSearch(request, response);
                break;
            case "/image":
                handleGetImage(request, response);
                break;
            case "/viewer":
                handleViewer(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Test DICOM server connection
     */
    private void handleTestConnection(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean connected = dicomService.testConnection();
            result.put("success", connected);
            result.put("message", connected ? "Connection successful" : "Connection failed");
            
            response.setStatus(connected ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(result));
        out.flush();
    }

    /**
     * Search for studies
     */
    private void handleSearch(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String patientId = request.getParameter("patientId");
        String patientName = request.getParameter("patientName");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<DicomImage> studies = dicomService.findStudies(patientId, patientName);
            result.put("success", true);
            result.put("studies", studies);
            result.put("count", studies.size());
            
        } catch (Exception e) {
            logger.error("Error searching studies", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(result));
        out.flush();
    }

    /**
     * Retrieve DICOM image
     */
    private void handleGetImage(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String studyUID = request.getParameter("studyUID");
        String seriesUID = request.getParameter("seriesUID");
        String sopUID = request.getParameter("sopUID");
        String format = request.getParameter("format"); // "json" or "image"
        
        if (studyUID == null || seriesUID == null || sopUID == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
            return;
        }
        
        try {
            DicomImage dicomImage = dicomService.retrieveImage(studyUID, seriesUID, sopUID);
            
            if (dicomImage == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }
            
            if ("image".equals(format) && dicomImage.getImageData() != null) {
                // Return raw image data
                response.setContentType("image/" + dicomImage.getImageFormat().toLowerCase());
                response.setContentLength(dicomImage.getImageData().length);
                response.getOutputStream().write(dicomImage.getImageData());
                response.getOutputStream().flush();
                
            } else {
                // Return JSON with metadata and base64 image
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("studyInstanceUID", dicomImage.getStudyInstanceUID());
                result.put("seriesInstanceUID", dicomImage.getSeriesInstanceUID());
                result.put("sopInstanceUID", dicomImage.getSopInstanceUID());
                result.put("patientName", dicomImage.getPatientName());
                result.put("patientID", dicomImage.getPatientID());
                result.put("studyDate", dicomImage.getStudyDate());
                result.put("modality", dicomImage.getModality());
                result.put("metadata", dicomImage.getMetadata());
                
                if (dicomImage.getImageData() != null) {
                    String base64Image = java.util.Base64.getEncoder().encodeToString(dicomImage.getImageData());
                    result.put("imageData", "data:image/" + dicomImage.getImageFormat().toLowerCase() + ";base64," + base64Image);
                }
                
                PrintWriter out = response.getWriter();
                out.print(objectMapper.writeValueAsString(result));
                out.flush();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving image", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving image: " + e.getMessage());
        }
    }

    /**
     * Serve the DICOM viewer web page
     */
    private void handleViewer(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>DICOM Image Viewer</title>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("        .container { max-width: 1200px; margin: 0 auto; }");
        out.println("        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }");
        out.println("        .form-group { margin: 10px 0; }");
        out.println("        label { display: inline-block; width: 120px; font-weight: bold; }");
        out.println("        input, button { padding: 8px; margin: 5px; }");
        out.println("        button { background: #007cba; color: white; border: none; border-radius: 3px; cursor: pointer; }");
        out.println("        button:hover { background: #005a87; }");
        out.println("        .status { padding: 10px; margin: 10px 0; border-radius: 3px; }");
        out.println("        .success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }");
        out.println("        .error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }");
        out.println("        .image-container { text-align: center; margin: 20px 0; }");
        out.println("        .dicom-image { max-width: 100%; height: auto; border: 1px solid #ddd; }");
        out.println("        .metadata { background: #f8f9fa; padding: 10px; margin: 10px 0; border-radius: 3px; }");
        out.println("        .metadata pre { margin: 0; white-space: pre-wrap; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>DICOM Image Viewer</h1>");
        out.println("        ");
        out.println("        <div class='section'>");
        out.println("            <h2>Connection Test</h2>");
        out.println("            <button onclick='testConnection()'>Test DICOM Server Connection</button>");
        out.println("            <div id='connectionStatus'></div>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div class='section'>");
        out.println("            <h2>Search Studies</h2>");
        out.println("            <div class='form-group'>");
        out.println("                <label>Patient ID:</label>");
        out.println("                <input type='text' id='patientId' placeholder='Enter Patient ID'>");
        out.println("            </div>");
        out.println("            <div class='form-group'>");
        out.println("                <label>Patient Name:</label>");
        out.println("                <input type='text' id='patientName' placeholder='Enter Patient Name'>");
        out.println("            </div>");
        out.println("            <button onclick='searchStudies()'>Search Studies</button>");
        out.println("            <div id='searchResults'></div>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div class='section'>");
        out.println("            <h2>Retrieve Image</h2>");
        out.println("            <div class='form-group'>");
        out.println("                <label>Study UID:</label>");
        out.println("                <input type='text' id='studyUID' placeholder='Study Instance UID'>");
        out.println("            </div>");
        out.println("            <div class='form-group'>");
        out.println("                <label>Series UID:</label>");
        out.println("                <input type='text' id='seriesUID' placeholder='Series Instance UID'>");
        out.println("            </div>");
        out.println("            <div class='form-group'>");
        out.println("                <label>SOP UID:</label>");
        out.println("                <input type='text' id='sopUID' placeholder='SOP Instance UID'>");
        out.println("            </div>");
        out.println("            <button onclick='retrieveImage()'>Retrieve Image</button>");
        out.println("            <div id='imageResults'></div>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("    ");
        out.println("    <script>");
        out.println("        function testConnection() {");
        out.println("            const statusDiv = document.getElementById('connectionStatus');");
        out.println("            statusDiv.innerHTML = 'Testing connection...';");
        out.println("            ");
        out.println("            fetch('/dicom/test')");
        out.println("                .then(response => response.json())");
        out.println("                .then(data => {");
        out.println("                    const className = data.success ? 'success' : 'error';");
        out.println("                    statusDiv.innerHTML = `<div class='status ${className}'>${data.message}</div>`;");
        out.println("                })");
        out.println("                .catch(error => {");
        out.println("                    statusDiv.innerHTML = `<div class='status error'>Error: ${error.message}</div>`;");
        out.println("                });");
        out.println("        }");
        out.println("        ");
        out.println("        function searchStudies() {");
        out.println("            const patientId = document.getElementById('patientId').value;");
        out.println("            const patientName = document.getElementById('patientName').value;");
        out.println("            const resultsDiv = document.getElementById('searchResults');");
        out.println("            ");
        out.println("            if (!patientId && !patientName) {");
        out.println("                resultsDiv.innerHTML = `<div class='status error'>Please enter Patient ID or Patient Name</div>`;");
        out.println("                return;");
        out.println("            }");
        out.println("            ");
        out.println("            resultsDiv.innerHTML = 'Searching...';");
        out.println("            ");
        out.println("            const params = new URLSearchParams();");
        out.println("            if (patientId) params.append('patientId', patientId);");
        out.println("            if (patientName) params.append('patientName', patientName);");
        out.println("            ");
        out.println("            fetch(`/dicom/search?${params}`)");
        out.println("                .then(response => response.json())");
        out.println("                .then(data => {");
        out.println("                    if (data.success) {");
        out.println("                        let html = `<div class='status success'>Found ${data.count} studies</div>`;");
        out.println("                        data.studies.forEach(study => {");
        out.println("                            html += `<div class='metadata'>");
        out.println("                                <strong>Study UID:</strong> ${study.studyInstanceUID}<br>");
        out.println("                                <strong>Patient:</strong> ${study.patientName} (${study.patientID})<br>");
        out.println("                                <strong>Date:</strong> ${study.studyDate}");
        out.println("                            </div>`;");
        out.println("                        });");
        out.println("                        resultsDiv.innerHTML = html;");
        out.println("                    } else {");
        out.println("                        resultsDiv.innerHTML = `<div class='status error'>${data.message}</div>`;");
        out.println("                    }");
        out.println("                })");
        out.println("                .catch(error => {");
        out.println("                    resultsDiv.innerHTML = `<div class='status error'>Error: ${error.message}</div>`;");
        out.println("                });");
        out.println("        }");
        out.println("        ");
        out.println("        function retrieveImage() {");
        out.println("            const studyUID = document.getElementById('studyUID').value;");
        out.println("            const seriesUID = document.getElementById('seriesUID').value;");
        out.println("            const sopUID = document.getElementById('sopUID').value;");
        out.println("            const resultsDiv = document.getElementById('imageResults');");
        out.println("            ");
        out.println("            if (!studyUID || !seriesUID || !sopUID) {");
        out.println("                resultsDiv.innerHTML = `<div class='status error'>Please enter all UIDs</div>`;");
        out.println("                return;");
        out.println("            }");
        out.println("            ");
        out.println("            resultsDiv.innerHTML = 'Retrieving image...';");
        out.println("            ");
        out.println("            const params = new URLSearchParams({");
        out.println("                studyUID: studyUID,");
        out.println("                seriesUID: seriesUID,");
        out.println("                sopUID: sopUID");
        out.println("            });");
        out.println("            ");
        out.println("            fetch(`/dicom/image?${params}`)");
        out.println("                .then(response => response.json())");
        out.println("                .then(data => {");
        out.println("                    if (data.success) {");
        out.println("                        let html = `<div class='status success'>Image retrieved successfully</div>`;");
        out.println("                        ");
        out.println("                        if (data.imageData) {");
        out.println("                            html += `<div class='image-container'>");
        out.println("                                <img src='${data.imageData}' class='dicom-image' alt='DICOM Image'>");
        out.println("                            </div>`;");
        out.println("                        }");
        out.println("                        ");
        out.println("                        html += `<div class='metadata'>");
        out.println("                            <h3>Image Information</h3>");
        out.println("                            <strong>Patient:</strong> ${data.patientName} (${data.patientID})<br>");
        out.println("                            <strong>Study Date:</strong> ${data.studyDate}<br>");
        out.println("                            <strong>Modality:</strong> ${data.modality}<br>");
        out.println("                            <strong>Study UID:</strong> ${data.studyInstanceUID}<br>");
        out.println("                            <strong>Series UID:</strong> ${data.seriesInstanceUID}<br>");
        out.println("                            <strong>SOP UID:</strong> ${data.sopInstanceUID}");
        out.println("                        </div>`;");
        out.println("                        ");
        out.println("                        resultsDiv.innerHTML = html;");
        out.println("                    } else {");
        out.println("                        resultsDiv.innerHTML = `<div class='status error'>${data.message}</div>`;");
        out.println("                    }");
        out.println("                })");
        out.println("                .catch(error => {");
        out.println("                    resultsDiv.innerHTML = `<div class='status error'>Error: ${error.message}</div>`;");
        out.println("                });");
        out.println("        }");
        out.println("    </script>");
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }

    @Override
    public void destroy() {
        if (dicomService != null) {
            dicomService.shutdown();
        }
        super.destroy();
    }
}
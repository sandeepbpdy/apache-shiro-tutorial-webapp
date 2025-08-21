package com.foo.example.dicom;

import org.dcm4che3.data.*;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.util.TagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Service class for connecting to DICOM servers and retrieving images
 */
public class DicomService {
    private static final Logger logger = LoggerFactory.getLogger(DicomService.class);
    
    private final DicomServerConfig config;
    private Device device;
    private Connection conn;
    private ApplicationEntity ae;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    public DicomService(DicomServerConfig config) {
        this.config = config;
        initializeDevice();
    }

    private void initializeDevice() {
        device = new Device("dicom-webapp");
        conn = new Connection();
        ae = new ApplicationEntity(config.getCallingAET());
        
        device.addConnection(conn);
        device.addApplicationEntity(ae);
        ae.addConnection(conn);
        
        executorService = Executors.newCachedThreadPool();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        device.setExecutor(executorService);
        device.setScheduledExecutor(scheduledExecutorService);
    }

    /**
     * Test connection to DICOM server using C-ECHO
     */
    public boolean testConnection() {
        Association as = null;
        try {
            as = connect();
            DimseRSP rsp = as.cecho();
            rsp.next();
            return rsp.getCommand().getInt(Tag.Status, -1) == Status.Success;
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return false;
        } finally {
            if (as != null) {
                try {
                    as.release();
                } catch (IOException e) {
                    logger.warn("Error releasing association", e);
                }
            }
        }
    }

    /**
     * Search for studies based on patient ID or name
     */
    public List<DicomImage> findStudies(String patientId, String patientName) {
        List<DicomImage> studies = new ArrayList<>();
        Association as = null;
        
        try {
            as = connect();
            
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            keys.setString(Tag.StudyInstanceUID, VR.UI);
            keys.setString(Tag.PatientName, VR.PN);
            keys.setString(Tag.PatientID, VR.LO);
            keys.setString(Tag.StudyDate, VR.DA);
            keys.setString(Tag.Modality, VR.CS);
            
            if (patientId != null && !patientId.isEmpty()) {
                keys.setString(Tag.PatientID, VR.LO, patientId);
            }
            if (patientName != null && !patientName.isEmpty()) {
                keys.setString(Tag.PatientName, VR.PN, patientName);
            }

            DimseRSP rsp = as.cfind(UID.StudyRootQueryRetrieveInformationModelFind, Priority.NORMAL, keys);
            
            while (rsp.next()) {
                Attributes match = rsp.getDataset();
                if (match != null) {
                    DicomImage study = new DicomImage();
                    study.setStudyInstanceUID(match.getString(Tag.StudyInstanceUID));
                    study.setPatientName(match.getString(Tag.PatientName));
                    study.setPatientID(match.getString(Tag.PatientID));
                    study.setStudyDate(match.getString(Tag.StudyDate));
                    studies.add(study);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error finding studies", e);
        } finally {
            if (as != null) {
                try {
                    as.release();
                } catch (IOException e) {
                    logger.warn("Error releasing association", e);
                }
            }
        }
        
        return studies;
    }

    /**
     * Retrieve DICOM image by SOP Instance UID
     */
    public DicomImage retrieveImage(String studyInstanceUID, String seriesInstanceUID, String sopInstanceUID) {
        Association as = null;
        
        try {
            as = connect();
            
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
            keys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
            keys.setString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUID);
            keys.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUID);

            // Create a temporary file to store the retrieved DICOM
            File tempFile = File.createTempFile("dicom_", ".dcm");
            tempFile.deleteOnExit();

            DimseRSP rsp = as.cget(UID.StudyRootQueryRetrieveInformationModelGet, Priority.NORMAL, keys,
                    UID.ImplicitVRLittleEndian, new FileOutputStream(tempFile));
            
            while (rsp.next()) {
                // Process response
            }

            // Read the DICOM file and extract image
            return readDicomFile(tempFile);
            
        } catch (Exception e) {
            logger.error("Error retrieving image", e);
            return null;
        } finally {
            if (as != null) {
                try {
                    as.release();
                } catch (IOException e) {
                    logger.warn("Error releasing association", e);
                }
            }
        }
    }

    /**
     * Read DICOM file and extract image data
     */
    private DicomImage readDicomFile(File dicomFile) {
        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            Attributes attrs = dis.readDataset(-1, -1);
            
            DicomImage dicomImage = new DicomImage();
            dicomImage.setStudyInstanceUID(attrs.getString(Tag.StudyInstanceUID));
            dicomImage.setSeriesInstanceUID(attrs.getString(Tag.SeriesInstanceUID));
            dicomImage.setSopInstanceUID(attrs.getString(Tag.SOPInstanceUID));
            dicomImage.setPatientName(attrs.getString(Tag.PatientName));
            dicomImage.setPatientID(attrs.getString(Tag.PatientID));
            dicomImage.setStudyDate(attrs.getString(Tag.StudyDate));
            dicomImage.setModality(attrs.getString(Tag.Modality));

            // Extract metadata
            Map<String, Object> metadata = new HashMap<>();
            for (int tag : attrs.tags()) {
                VR vr = attrs.getVR(tag);
                String tagName = TagUtils.toString(tag);
                Object value = attrs.getValue(tag);
                if (value != null) {
                    metadata.put(tagName, value.toString());
                }
            }
            dicomImage.setMetadata(metadata);

            // Convert DICOM to BufferedImage
            BufferedImage image = convertDicomToImage(dicomFile);
            dicomImage.setImage(image);

            // Convert image to byte array for web transfer
            if (image != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                dicomImage.setImageData(baos.toByteArray());
                dicomImage.setImageFormat("PNG");
            }

            return dicomImage;
            
        } catch (Exception e) {
            logger.error("Error reading DICOM file", e);
            return null;
        }
    }

    /**
     * Convert DICOM file to BufferedImage
     */
    private BufferedImage convertDicomToImage(File dicomFile) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(dicomFile);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                
                DicomImageReadParam param = (DicomImageReadParam) reader.getDefaultReadParam();
                return reader.read(0, param);
            }
        } catch (Exception e) {
            logger.error("Error converting DICOM to image", e);
        }
        return null;
    }

    /**
     * Establish connection to DICOM server
     */
    private Association connect() throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
        Connection remoteConn = new Connection();
        remoteConn.setHostname(config.getHostname());
        remoteConn.setPort(config.getPort());

        AAssociateRQ rq = new AAssociateRQ();
        rq.setCallingAET(config.getCallingAET());
        rq.setCalledAET(config.getCalledAET());
        
        // Add presentation contexts for common DICOM services
        rq.addPresentationContext(new PresentationContext(1, UID.Verification, UID.ImplicitVRLittleEndian));
        rq.addPresentationContext(new PresentationContext(3, UID.StudyRootQueryRetrieveInformationModelFind, UID.ImplicitVRLittleEndian));
        rq.addPresentationContext(new PresentationContext(5, UID.StudyRootQueryRetrieveInformationModelGet, UID.ImplicitVRLittleEndian));

        return ae.connect(conn, remoteConn, rq);
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (device != null) {
            device.unbindConnections();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }
}
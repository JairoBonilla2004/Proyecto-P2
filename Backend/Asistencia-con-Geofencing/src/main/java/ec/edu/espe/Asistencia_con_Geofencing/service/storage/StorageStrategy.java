package ec.edu.espe.Asistencia_con_Geofencing.service.storage;

import java.io.IOException;


public interface StorageStrategy {
    

    String uploadFile(String localFilePath, String destinationFileName) throws IOException;

    void deleteFile(String fileName) throws IOException;
 
    String getPublicUrl(String fileName);
  
    boolean isConfigured();
 
    String getStorageType();
}

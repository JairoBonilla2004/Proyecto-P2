package ec.edu.espe.SecureFrameGallery.modules.steganography.services;


public interface MetadataCleaner {

    byte[] stripMetadata(byte[] imageBytes, String mimeType) throws java.io.IOException;
}

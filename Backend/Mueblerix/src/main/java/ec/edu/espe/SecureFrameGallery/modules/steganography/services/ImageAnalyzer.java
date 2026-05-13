package ec.edu.espe.SecureFrameGallery.modules.steganography.services;

import ec.edu.espe.SecureFrameGallery.modules.steganography.dtos.StegoAnalysisResultDto;

public interface ImageAnalyzer {
    StegoAnalysisResultDto analyze(byte[] imageBytes, String mimeType);
}

package ec.edu.espe.Asistencia_con_Geofencing.service.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import ec.edu.espe.Asistencia_con_Geofencing.dto.request.GenerateQrRequest;
import ec.edu.espe.Asistencia_con_Geofencing.dto.response.QrTokenResponse;
import ec.edu.espe.Asistencia_con_Geofencing.exception.ResourceNotFoundException;
import ec.edu.espe.Asistencia_con_Geofencing.model.AttendanceSession;
import ec.edu.espe.Asistencia_con_Geofencing.model.QrToken;
import ec.edu.espe.Asistencia_con_Geofencing.repository.AttendanceSessionRepository;
import ec.edu.espe.Asistencia_con_Geofencing.repository.QrTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrServiceImpl implements QrService {

    private final QrTokenRepository qrTokenRepository;
    private final AttendanceSessionRepository sessionRepository;

    @Transactional
    public QrTokenResponse generateQrToken(GenerateQrRequest request, UUID teacherId) {
        AttendanceSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada"));

        if (!session.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("No tienes permiso para generar QR de esta sesión");
        }

        if (!session.getActive()) {
            throw new RuntimeException("La sesión no está activa");
        }

        LocalDateTime now = LocalDateTime.now();
        qrTokenRepository.invalidateSessionTokens(session.getId(), now, now.minusMinutes(1));
        log.info("Tokens anteriores de la sesión {} invalidados", session.getId());

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime expiresAt = now.plusMinutes(request.getExpiresInMinutes());

        QrToken qrToken = new QrToken();
        qrToken.setSession(session);
        qrToken.setToken(token);
        qrToken.setExpiresAt(expiresAt);
        qrToken = qrTokenRepository.save(qrToken);
        String qrBase64 = generateQrImage(token);
        return QrTokenResponse.builder()
                .qrId(qrToken.getId())
                .token(token)
                .sessionId(session.getId())
                .expiresAt(expiresAt)
                .qrCodeBase64(qrBase64)
                .build();
    }

    private String generateQrImage(String token) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(token, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.error("Error generando QR", e);
            throw new RuntimeException("Error generando código QR");
        }
    }
}
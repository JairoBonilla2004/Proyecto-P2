package ec.edu.espe.SecureFrameGallery.shared.utils.stego;

public final class StegoEofUtil {

    private static final byte[] JPEG_EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};
    private static final byte[] PNG_EOF_MARKER = {
            (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
            (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82
    };

    private StegoEofUtil() {}

    public static boolean detectEofAnomaly(byte[] bytes, String mimeType) {
        if (bytes == null || mimeType == null) return false;

        return switch (mimeType) {
            case "image/jpeg" -> {
                int pos = findLastSequence(bytes, JPEG_EOF_MARKER);
                yield pos != -1 && pos != bytes.length - JPEG_EOF_MARKER.length;
            }
            case "image/png" -> {
                int pos = findLastSequence(bytes, PNG_EOF_MARKER);
                yield pos != -1 && pos != bytes.length - PNG_EOF_MARKER.length;
            }
            default -> false;
        };
    }

    private static int findLastSequence(byte[] data, byte[] seq) {
        int last = -1;
        outer:
        for (int i = 0; i <= data.length - seq.length; i++) {
            for (int j = 0; j < seq.length; j++) {
                if (data[i + j] != seq[j]) continue outer;
            }
            last = i;
        }
        return last;
    }
}

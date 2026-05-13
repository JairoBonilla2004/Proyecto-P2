package ec.edu.espe.Asistencia_con_Geofencing.utils.pdf;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;


@Component
public class DateTimeFormatters {
    
    public static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    public static final DateTimeFormatter DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    public static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    public DateTimeFormatter getDateFormatter() {
        return DATE_FORMATTER;
    }
    
    public DateTimeFormatter getDateTimeFormatter() {
        return DATETIME_FORMATTER;
    }
    
    public DateTimeFormatter getTimeFormatter() {
        return TIME_FORMATTER;
    }
    
    public DateTimeFormatter getFileTimestampFormatter() {
        return FILE_TIMESTAMP_FORMATTER;
    }
}

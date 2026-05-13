package ec.edu.espe.Asistencia_con_Geofencing.service.pdf.factory;

import ec.edu.espe.Asistencia_con_Geofencing.service.pdf.strategy.PdfReportStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PdfReportStrategyFactory {

    private final Map<String, PdfReportStrategy> strategies;

    public PdfReportStrategyFactory(List<PdfReportStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        PdfReportStrategy::getReportType,
                        strategy -> strategy
                ));

        log.info("PdfReportStrategyFactory inicializado con {} estrategias: {}",
                strategies.size(),
                strategies.keySet());
    }
    

    public PdfReportStrategy getStrategy(String reportType) {
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de reporte no puede ser nulo o vacío");
        }
        
        PdfReportStrategy strategy = strategies.get(reportType);
        
        if (strategy == null) {
            String availableTypes = String.join(", ", strategies.keySet());
            String errorMessage = String.format(
                    "Tipo de reporte desconocido: '%s'. Tipos disponibles: [%s]",
                    reportType,
                    availableTypes
            );
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        log.debug("Estrategia obtenida para tipo: {}", reportType);
        return strategy;
    }

    public List<String> getAvailableReportTypes() {
        return List.copyOf(strategies.keySet());
    }
    public boolean isReportTypeAvailable(String reportType) {
        return strategies.containsKey(reportType);
    }
}

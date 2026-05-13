package ec.edu.espe.Asistencia_con_Geofencing.utils.auth.factory;

import ec.edu.espe.Asistencia_con_Geofencing.exception.BadRequestException;
import ec.edu.espe.Asistencia_con_Geofencing.model.enums.OAuthProvider;
import ec.edu.espe.Asistencia_con_Geofencing.utils.auth.strategy.OAuthLoginStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuthStrategyFactory {

    private final List<OAuthLoginStrategy> strategies;
    private Map<OAuthProvider, OAuthLoginStrategy> strategyMap;

    public OAuthLoginStrategy getStrategy(OAuthProvider provider) {
        if (strategyMap == null) {
            initializeStrategyMap();
        }
        OAuthLoginStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new BadRequestException("Proveedor OAuth no soportado: " + provider);
        }

        return strategy;
    }

    private void initializeStrategyMap() {
        strategyMap = new HashMap<>();
        for (OAuthLoginStrategy strategy : strategies) {
            strategyMap.put(strategy.getOAuthProvider(), strategy);
        }
    }
}

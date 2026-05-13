package ec.edu.espe.SecureFrameGallery.application.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                    .title("SecureFrame Gallery API")
                    .version("1.0.0")
                    .description("Backend seguro de galería de imágenes con detección de esteganografía")
                    .contact(new Contact()
                        .name("SecureFrame Team")
                        .email("support@secureframe.local")
                    )
                )
                .components(new Components()
                    .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Bearer token obtenido tras login o registro")
                    )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

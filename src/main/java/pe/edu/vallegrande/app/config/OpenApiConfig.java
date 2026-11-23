package pe.edu.vallegrande.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * OpenAPI / Swagger configuration for the FUT system REST API.
 */
@Configuration
public class OpenApiConfig {

    /** Auxiliary schemas that should not appear in the Schemas section of Swagger UI. */
    private static final Set<String> HIDDEN_SCHEMAS = Set.of("GestionTramiteFut", "Area");

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FUT System — I.E. N.° 20191 Alfonso Ugarte")
                        .version("1.0")
                        .description(
                            "REST API for the Single Procedure Form (FUT) System " +
                            "of I.E. N.° 20191 «Alfonso Ugarte» — San Vicente de Cañete.\n\n" +
                            "**Transactional tables:** FUT Request, FUT Processing Management\n\n" +
                            "**Master tables:** Applicant, Procedure Type\n\n" +
                            "All endpoints can be tested directly from this interface."
                        ))
                // Display order in Swagger UI:
                // 1. FUT Request API
                // 2. FUT Processing Management API
                // 3. Applicant API
                // 4. Procedure Type API
                .tags(List.of(
                        new Tag()
                            .name("FUT Request API")
                            .description("Transactional Table — Registration and querying of Single Procedure Form (FUT) requests"),
                        new Tag()
                            .name("FUT Processing Management API")
                            .description("Transactional Table — Internal management of FUT procedure processing"),
                        new Tag()
                            .name("Applicant API")
                            .description("Master Table — Management of persons who submit FUT procedure requests"),
                        new Tag()
                            .name("Procedure Type API")
                            .description("Master Table — Administration of procedure types available in the FUT system")
                ));
    }

    /**
     * Removes GestionTramiteFut and Area from the Schemas section of the generated spec.
     * Endpoints that use them continue to work; they only disappear from the visual panel.
     */
    @Bean
    public OpenApiCustomizer hideAuxiliarySchemas() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                HIDDEN_SCHEMAS.forEach(name ->
                    openApi.getComponents().getSchemas().remove(name)
                );
            }
        };
    }
}

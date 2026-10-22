package com.servicehub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI servicehubOpenAPI(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("API de gestão de prestadores, serviços, ordens de serviço e avaliações — "
                                + "projeto PBL da disciplina Desenvolvimento de API Back-End (UniEVANGÉLICA). "
                                + "Erros seguem o formato ProblemDetail (RFC 9457). Para chamar rotas protegidas, faça login "
                                + "em POST /api/v1/auth/login e clique em Authorize com o accessToken.")
                        .version(props != null ? props.getVersion() : "dev")
                        .contact(new Contact().name("Prof. Eduardo Dias Pereira"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Execução local"),
                        new Server().url("http://localhost:8090").description("Docker Compose")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Cole o accessToken retornado pelo login (sem o prefixo Bearer).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Toda operação protegida passa a documentar 401 e 403 em ProblemDetail.
     * Operações públicas declaram @SecurityRequirements vazio e ficam de fora.
     */
    @Bean
    public OpenApiCustomizer securityResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
            boolean isPublic = op.getSecurity() != null && op.getSecurity().isEmpty();
            if (isPublic) {
                return;
            }
            op.getResponses().putIfAbsent("401", problem("Token ausente, inválido, expirado ou de conta desativada"));
            op.getResponses().putIfAbsent("403", problem("Sem permissão para esta operação (papel ou dono)"));
        }));
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType(
                "application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }

    @Bean
    public GroupedOpenApi v1Api(OpenApiCustomizer securityResponsesCustomizer) {
        return GroupedOpenApi.builder()
                .group("v1")
                .displayName("ServiceHub API v1")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(securityResponsesCustomizer)
                .build();
    }

    /** Documentação para quem só consome o catálogo: login/cadastro e leitura de serviços (Aula 5, slide 16). */
    @Bean
    public GroupedOpenApi publicApi(OpenApiCustomizer securityResponsesCustomizer) {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("ServiceHub API — pública")
                .pathsToMatch("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/services", "/api/v1/services/*",
                        "/api/v1/services/*/reviews", "/api/v1/reviews/*", "/api/v1/info")
                .pathsToExclude("/api/v1/services/*/status")
                .addOpenApiCustomizer(securityResponsesCustomizer)
                .build();
    }
}

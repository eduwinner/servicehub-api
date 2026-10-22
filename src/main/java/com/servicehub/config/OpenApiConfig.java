package com.servicehub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI servicehubOpenAPI(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("API de gestão de prestadores, serviços, ordens de serviço e avaliações — "
                                + "projeto PBL da disciplina Desenvolvimento de API Back-End (UniEVANGÉLICA). "
                                + "Erros seguem o formato ProblemDetail (RFC 9457).")
                        .version(props != null ? props.getVersion() : "dev")
                        .contact(new Contact().name("Prof. Eduardo Dias Pereira"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Execução local"),
                        new Server().url("http://localhost:8090").description("Docker Compose")));
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .displayName("ServiceHub API v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}

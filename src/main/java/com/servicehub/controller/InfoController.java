package com.servicehub.controller;

import com.servicehub.model.dto.InfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/info")
@Tag(name = "Status", description = "Verificação de status e versão da API")
public class InfoController {

    private final String version;

    public InfoController(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
    }

    @Operation(summary = "Consultar status da API",
            description = "Retorna o status, a versão do build em execução e uma mensagem. Não exige autenticação.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "API operacional")
    @GetMapping
    public InfoResponse info() {
        return new InfoResponse("OK", version, "ServiceHub API operacional");
    }
}

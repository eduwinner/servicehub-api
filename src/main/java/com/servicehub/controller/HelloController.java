package com.servicehub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.servicehub.model.StatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(
  name = "Status",
  description = "Endpoints de verificação de saúde e status da API"
)
public class HelloController {

    @Operation(summary = "Hello", description = "Endpoint de teste simples")
    @ApiResponse(responseCode = "200", description = "API respondendo")
    @GetMapping("/hello")
    public String hello() {
        return "ServiceHub API está no ar!🚀";
    }

    @Operation(summary = "Status simples", description = "Retorna status e versão em texto plano")
    @ApiResponse(responseCode = "200", description = "Status retornado com sucesso")
    @GetMapping("/status")
    public String status() {
        return "OK - v1.0";
    }

    @Operation(summary = "Status detalhado", description = "Retorna status, versão e mensagem em JSON")
    @ApiResponse(responseCode = "200", description = "Status detalhado retornado com sucesso")
    @GetMapping("/info")
    public StatusResponse info() {
    return new StatusResponse("OK", "1.0.0",
        "ServiceHub API operacional");
}

}

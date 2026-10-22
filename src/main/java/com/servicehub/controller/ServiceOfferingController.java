package com.servicehub.controller;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.dto.ServiceFilter;
import com.servicehub.model.dto.ServiceOfferingDTO;
import com.servicehub.model.dto.ServiceOfferingResponseDTO;
import com.servicehub.service.ServiceOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Serviços", description = "Catálogo de serviços oferecidos pelos prestadores da plataforma")
public class ServiceOfferingController {

    private static final String PROBLEM = "application/problem+json";

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @Operation(summary = "Cadastrar serviço",
            description = "Publica um novo serviço no catálogo. Responde com o serviço criado e o header Location apontando para ele.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos, categoria inexistente ou JSON malformado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Prestador informado não existe",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ServiceOfferingResponseDTO> create(@Valid @RequestBody ServiceOfferingDTO dto) {
        ServiceOfferingResponseDTO body = serviceOfferingService.createService(dto);
        return ResponseEntity.created(URI.create("/api/v1/services/" + body.getId())).body(body);
    }

    @Operation(summary = "Buscar serviço por ID", description = "Retorna os dados completos de um serviço, incluindo o nome do prestador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ServiceOfferingResponseDTO findById(
            @Parameter(description = "ID do serviço", example = "12") @PathVariable Long id) {
        return serviceOfferingService.findById(id);
    }

    @Operation(summary = "Listar serviços",
            description = "Lista paginada dos serviços ativos. Todos os filtros são opcionais e combináveis. "
                    + "Paginação: page (a partir de 0), size (padrão 20, máximo 100) e sort (ex.: price,asc).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de serviços"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de filtro com formato inválido",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public Page<ServiceOfferingResponseDTO> findAll(
            @Parameter(description = "Categoria do serviço", example = "ELETRICA")
            @RequestParam(required = false) ServiceCategory category,
            @Parameter(description = "ID do prestador", example = "7")
            @RequestParam(required = false) Long providerId,
            @Parameter(description = "Preço mínimo (inclusive)", example = "50.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Preço máximo (inclusive)", example = "300.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Trecho do título, sem diferenciar maiúsculas", example = "tomada")
            @RequestParam(required = false) String q,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return serviceOfferingService.search(new ServiceFilter(category, providerId, minPrice, maxPrice, q), pageable);
    }

    @Operation(summary = "Atualizar serviço", description = "Substitui título, descrição, preço e categoria de um serviço existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ServiceOfferingResponseDTO update(
            @Parameter(description = "ID do serviço", example = "12") @PathVariable Long id,
            @Valid @RequestBody ServiceOfferingDTO dto) {
        return serviceOfferingService.updateService(id, dto);
    }

    @Operation(summary = "Remover serviço", description = "Remove um serviço do catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço removido"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID do serviço", example = "12") @PathVariable Long id) {
        serviceOfferingService.deleteService(id);
    }
}

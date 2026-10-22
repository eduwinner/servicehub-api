package com.servicehub.controller;

import com.servicehub.model.RequestStatus;
import com.servicehub.model.dto.ServiceRequestCreateDTO;
import com.servicehub.model.dto.ServiceRequestResponseDTO;
import com.servicehub.model.dto.StatusUpdateDTO;
import com.servicehub.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/service-requests")
@Tag(name = "Ordens de serviço", description = "Contratação de serviços e o ciclo PENDING → ACCEPTED → IN_PROGRESS → COMPLETED")
public class ServiceRequestController {

    private static final String PROBLEM = "application/problem+json";
    static final Set<String> SORTABLE = Set.of("id", "createdAt", "scheduledAt", "status", "totalPrice");

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @Operation(summary = "Abrir ordem de serviço",
            description = "CLIENT ou PROVIDER contratam um serviço ativo de outra pessoa. A ordem nasce PENDING e o preço "
                    + "do serviço é copiado para totalPrice (não muda se o prestador reajustar depois).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem criada"),
            @ApiResponse(responseCode = "400", description = "serviceId ausente, data no passado ou observação longa demais",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN não abre ordens",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Serviço inativo ou do próprio usuário",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ServiceRequestResponseDTO> create(@Valid @RequestBody ServiceRequestCreateDTO dto) {
        ServiceRequestResponseDTO body = service.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/service-requests/" + body.id())).body(body);
    }

    @Operation(summary = "Listar minhas ordens",
            description = "Ordens em que o usuário é cliente ou prestador (ADMIN vê todas). Filtros opcionais por papel e status. "
                    + "Ordenação por id, createdAt, scheduledAt, status ou totalPrice.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de ordens"),
            @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public Page<ServiceRequestResponseDTO> list(
            @Parameter(description = "Lado da ordem: client (contratei) ou provider (recebi)", example = "provider")
            @RequestParam(required = false) String role,
            @Parameter(description = "Status da ordem", example = "PENDING")
            @RequestParam(required = false) RequestStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(role, status, SortGuard.requireAllowed(pageable, SORTABLE));
    }

    @Operation(summary = "Buscar ordem", description = "Só o cliente da ordem, o prestador do serviço ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem encontrada"),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ServiceRequestResponseDTO findById(@Parameter(description = "ID da ordem", example = "31") @PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Mudar status da ordem",
            description = "Prestador: PENDING→ACCEPTED, ACCEPTED→IN_PROGRESS, IN_PROGRESS→COMPLETED. Cliente ou prestador: "
                    + "PENDING/ACCEPTED→CANCELLED. ADMIN: qualquer transição válida. Transição fora da máquina de estados: 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado"),
            @ApiResponse(responseCode = "400", description = "Status ausente ou desconhecido",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Não participa da ordem ou seu papel não permite essa transição",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public ServiceRequestResponseDTO changeStatus(@Parameter(description = "ID da ordem", example = "31") @PathVariable Long id,
                                                  @Valid @RequestBody StatusUpdateDTO dto) {
        return service.changeStatus(id, dto.status());
    }
}

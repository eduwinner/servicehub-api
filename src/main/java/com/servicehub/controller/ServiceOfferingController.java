package com.servicehub.controller;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.dto.ServiceFilter;
import com.servicehub.model.dto.ServiceOfferingDTO;
import com.servicehub.model.dto.ServiceOfferingResponseDTO;
import com.servicehub.service.ServiceOfferingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/services")
@Tag(
  name = "Serviços",
  description = "Operações de gerenciamento de serviços cadastrados na plataforma ServiceHub"
)
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @Operation(summary = "Cadastrar serviço", description = "Cria um novo serviço oferecido na plataforma")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Usuário (provedor) não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @PostMapping
    public ResponseEntity<ServiceOfferingResponseDTO> create(@Valid @RequestBody ServiceOfferingDTO dto) {
        ServiceOfferingResponseDTO body = serviceOfferingService.createService(dto);
        return ResponseEntity.created(URI.create("/api/v1/services/" + body.getId())).body(body);
    }

    @Operation(summary = "Buscar serviço por ID", description = "Retorna os dados de um serviço específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
        @ApiResponse(responseCode = "500", description = "Serviço não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @GetMapping("/{id}")
    public ServiceOfferingResponseDTO findById(@PathVariable Long id) {
        return serviceOfferingService.findById(id);
    }

    @Operation(summary = "Listar serviços", description = "Retorna todos os serviços cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    @GetMapping
    public Page<ServiceOfferingResponseDTO> findAll(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return serviceOfferingService.search(new ServiceFilter(category, providerId, minPrice, maxPrice, q), pageable);
    }

    @Operation(summary = "Atualizar serviço", description = "Atualiza os dados de um serviço existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Serviço não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @PutMapping("/{id}")
    public ServiceOfferingResponseDTO update(@PathVariable Long id, @Valid @RequestBody ServiceOfferingDTO dto) {
        return serviceOfferingService.updateService(id, dto);
    }

    @Operation(summary = "Remover serviço", description = "Exclui um serviço da plataforma")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Serviço removido com sucesso"),
        @ApiResponse(responseCode = "500", description = "Serviço não encontrado (exceção não tratada, sem handler dedicado ainda)")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        serviceOfferingService.deleteService(id);
    }
}

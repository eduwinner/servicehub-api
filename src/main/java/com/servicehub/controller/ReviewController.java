package com.servicehub.controller;

import com.servicehub.model.dto.ReviewCreateDTO;
import com.servicehub.model.dto.ReviewResponseDTO;
import com.servicehub.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@Tag(name = "Avaliações", description = "Notas de 1 a 5 dadas por quem contratou, depois da conclusão")
public class ReviewController {

    private static final String PROBLEM = "application/problem+json";
    static final Set<String> SORTABLE = Set.of("id", "createdAt", "rating");

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Avaliar ordem concluída", description = "Só o cliente da ordem, só em COMPLETED, uma vez por ordem.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada"),
            @ApiResponse(responseCode = "400", description = "Nota fora de 1..5 ou comentário longo demais",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Só quem contratou avalia",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Ordem não encontrada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Ordem não concluída ou já avaliada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/api/v1/service-requests/{id}/reviews")
    public ResponseEntity<ReviewResponseDTO> create(@Parameter(description = "ID da ordem", example = "31") @PathVariable Long id,
                                                    @Valid @RequestBody ReviewCreateDTO dto) {
        ReviewResponseDTO body = reviewService.create(id, dto);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + body.id())).body(body);
    }

    @Operation(summary = "Avaliações de um serviço", description = "Público. Ordenação por id, createdAt ou rating.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de avaliações"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/services/{id}/reviews")
    public Page<ReviewResponseDTO> byService(
            @Parameter(description = "ID do serviço", example = "12") @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.byService(id, SortGuard.requireAllowed(pageable, SORTABLE));
    }

    @Operation(summary = "Buscar avaliação", description = "Público.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação encontrada"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/api/v1/reviews/{id}")
    public ReviewResponseDTO findById(@Parameter(description = "ID da avaliação", example = "4") @PathVariable Long id) {
        return reviewService.findById(id);
    }
}

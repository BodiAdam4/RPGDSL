package hu.bodiadam.rpg.rpg_backend.api;

import hu.bodiadam.rpg.rpg_backend.dsl.validation.Diagnostic;
import java.util.List;
import org.rpg_backend.model.DiagnosticDto;
import org.rpg_backend.api.DslApi;
import org.rpg_backend.model.ValidateRequestDto;
import org.rpg_backend.model.ValidateResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DslApiController implements DslApi {

    private final DslValidationService validationService;

    public DslApiController(DslValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<ValidateResponseDto> validateDsl(ValidateRequestDto validateRequestDto) {
        ValidationOutcome outcome = validationService.validate(validateRequestDto.getSource());
        return ResponseEntity.ok(new ValidateResponseDto(toDiagnosticDtos(outcome.diagnostics())));
    }

    private List<DiagnosticDto> toDiagnosticDtos(List<Diagnostic> diagnostics) {
        return diagnostics.stream().map(this::toDiagnosticDto).toList();
    }

    private DiagnosticDto toDiagnosticDto(Diagnostic diagnostic) {
        DiagnosticDto dto = new DiagnosticDto(
                diagnostic.line(),
                diagnostic.character(),
                DiagnosticDto.SeverityEnum.fromValue(diagnostic.severity().name()),
                diagnostic.message());
        if (diagnostic.code() != null) {
            dto.code(diagnostic.code());
        }
        return dto;
    }
}

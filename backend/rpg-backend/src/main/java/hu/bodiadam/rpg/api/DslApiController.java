package hu.bodiadam.rpg.api;

import hu.bodiadam.rpg.dsl.validation.Diagnostic;
import hu.bodiadam.rpg.generated.api.DslApi;
import hu.bodiadam.rpg.generated.model.DiagnosticDto;
import hu.bodiadam.rpg.generated.model.ValidateRequestDto;
import hu.bodiadam.rpg.generated.model.ValidateResponseDto;
import java.util.List;
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

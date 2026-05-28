package hu.bodiadam.rpg.api;

import hu.bodiadam.rpg.dsl.validation.Diagnostic;
import hu.bodiadam.rpg.generated.api.GameApi;
import hu.bodiadam.rpg.generated.model.ChatMessageDto;
import hu.bodiadam.rpg.generated.model.DiagnosticDto;
import hu.bodiadam.rpg.generated.model.StartRequestDto;
import hu.bodiadam.rpg.generated.model.StartResponseDto;
import hu.bodiadam.rpg.generated.model.TurnRequestDto;
import hu.bodiadam.rpg.generated.model.TurnResponseDto;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameApiController implements GameApi {

    private final DslValidationService validationService;
    private final StatelessGameService statelessGameService;

    public GameApiController(DslValidationService validationService, StatelessGameService statelessGameService) {
        this.validationService = validationService;
        this.statelessGameService = statelessGameService;
    }

    @Override
    public ResponseEntity<StartResponseDto> startGame(StartRequestDto startRequestDto) {
        ValidationOutcome outcome = validationService.validate(startRequestDto.getSource());
        if (outcome.hasErrors()) {
            return ResponseEntity.unprocessableEntity().body(toStartResponse(outcome));
        }

        String openingText = statelessGameService.startGame(outcome.compiledGameContext());
        return ResponseEntity.ok(toStartResponse(outcome).text(openingText));
    }

    @Override
    public ResponseEntity<TurnResponseDto> submitTurn(TurnRequestDto turnRequestDto) {
        ValidationOutcome outcome = validationService.validate(turnRequestDto.getSource());
        if (outcome.hasErrors()) {
            throw new IllegalStateException("Cannot continue game because the DSL contains validation errors.");
        }

        List<ChatMessageDto> history = turnRequestDto.getHistory() == null ? List.of() : turnRequestDto.getHistory();
        String response = statelessGameService.continueGame(outcome.compiledGameContext(), history);
        return ResponseEntity.ok(new TurnResponseDto(response));
    }

    private StartResponseDto toStartResponse(ValidationOutcome outcome) {
        return new StartResponseDto(toDiagnosticDtos(outcome.diagnostics()));
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

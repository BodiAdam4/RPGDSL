package hu.bodiadam.rpg.rpg_backend.api;

import hu.bodiadam.rpg.RpgDslParser;
import hu.bodiadam.rpg.rpg_backend.dsl.model.GameDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.parser.DslParseException;
import hu.bodiadam.rpg.rpg_backend.dsl.parser.DslParserFacade;
import hu.bodiadam.rpg.rpg_backend.dsl.validation.Diagnostic;
import hu.bodiadam.rpg.rpg_backend.dsl.validation.DiagnosticSeverity;
import hu.bodiadam.rpg.rpg_backend.dsl.validation.GameDefinitionSemanticValidator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DslValidationService {

    private static final Pattern LINE_AND_CHARACTER_PATTERN = Pattern.compile("line\\s+(\\d+)(?::(\\d+))?");

    private final DslParserFacade parserFacade;
    private final GameDefinitionSemanticValidator semanticValidator;
    private final ModelSnapshotFormatter snapshotFormatter;

    public DslValidationService(
            DslParserFacade parserFacade,
            GameDefinitionSemanticValidator semanticValidator,
            ModelSnapshotFormatter snapshotFormatter) {
        this.parserFacade = parserFacade;
        this.semanticValidator = semanticValidator;
        this.snapshotFormatter = snapshotFormatter;
    }

    public ValidationOutcome validate(String source) {
        RpgDslParser.ProgramContext programContext;
        try {
            programContext = parserFacade.parseTree(source);
        } catch (DslParseException exception) {
            return ValidationOutcome.invalid(List.of(toDiagnostic(exception, "SYNTAX_ERROR")));
        }

        GameDefinition definition;
        try {
            definition = parserFacade.build(programContext);
        } catch (RuntimeException exception) {
            return ValidationOutcome.invalid(List.of(toDiagnostic(exception, "MODEL_BUILD_ERROR")));
        }

        List<Diagnostic> diagnostics = new ArrayList<>(semanticValidator.validate(programContext, definition).diagnostics());
        if (hasErrors(diagnostics)) {
            return ValidationOutcome.invalid(sortDiagnostics(diagnostics));
        }

        if (diagnostics.isEmpty()) {
            diagnostics.add(new Diagnostic(0, 0, DiagnosticSeverity.INFO, "VALIDATION_OK", "Validation successful."));
        } else {
            diagnostics = sortDiagnostics(diagnostics);
        }

        return ValidationOutcome.valid(diagnostics, definition, snapshotFormatter.format(definition));
    }

    private boolean hasErrors(List<Diagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }

    private List<Diagnostic> sortDiagnostics(List<Diagnostic> diagnostics) {
        return diagnostics.stream()
                .sorted(Comparator.comparingInt(Diagnostic::line)
                        .thenComparingInt(Diagnostic::character)
                        .thenComparing(Diagnostic::code))
                .toList();
    }

    private Diagnostic toDiagnostic(RuntimeException exception, String defaultCode) {
        Matcher matcher = LINE_AND_CHARACTER_PATTERN.matcher(exception.getMessage() == null ? "" : exception.getMessage());
        int line = 0;
        int character = 0;
        if (matcher.find()) {
            line = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                character = Integer.parseInt(matcher.group(2));
            }
        }
        return new Diagnostic(
                line,
                character,
                DiagnosticSeverity.ERROR,
                defaultCode,
                exception.getMessage() == null ? defaultCode : exception.getMessage());
    }
}

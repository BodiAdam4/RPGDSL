package hu.bodiadam.rpg.api;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.validation.Diagnostic;
import hu.bodiadam.rpg.dsl.validation.DiagnosticSeverity;
import java.util.List;

public record ValidationOutcome(
        List<Diagnostic> diagnostics,
        GameDefinition definition,
        String compiledGameContext) {

    public ValidationOutcome {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static ValidationOutcome invalid(List<Diagnostic> diagnostics) {
        return new ValidationOutcome(diagnostics, null, null);
    }

    public static ValidationOutcome valid(
            List<Diagnostic> diagnostics,
            GameDefinition definition,
            String compiledGameContext) {
        return new ValidationOutcome(diagnostics, definition, compiledGameContext);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}

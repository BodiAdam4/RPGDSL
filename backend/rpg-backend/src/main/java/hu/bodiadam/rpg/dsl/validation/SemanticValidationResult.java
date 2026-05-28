package hu.bodiadam.rpg.dsl.validation;

import java.util.List;

public record SemanticValidationResult(List<Diagnostic> diagnostics) {

    public SemanticValidationResult {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public boolean isValid() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR);
    }
}

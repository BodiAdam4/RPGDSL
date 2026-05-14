package hu.bodiadam.rpg.rpg_backend.dsl.validation;

public record Diagnostic(
        int line,
        int character,
        DiagnosticSeverity severity,
        String code,
        String message) {
}

package hu.bodiadam.rpg.dsl.validation;

public record Diagnostic(
        int line,
        int character,
        DiagnosticSeverity severity,
        String code,
        String message) {
}

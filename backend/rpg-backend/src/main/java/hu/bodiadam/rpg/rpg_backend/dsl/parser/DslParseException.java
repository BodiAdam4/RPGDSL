package hu.bodiadam.rpg.rpg_backend.dsl.parser;

public class DslParseException extends RuntimeException {

    public DslParseException(String message) {
        super(message);
    }

    public DslParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

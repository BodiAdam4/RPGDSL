package hu.bodiadam.rpg.dsl.parser;

public class DslParseException extends RuntimeException {

    public DslParseException(String message) {
        super(message);
    }

    public DslParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

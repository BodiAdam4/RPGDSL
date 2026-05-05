package hu.bodiadam.rpg.rpg_backend.dsl.parser;

import hu.bodiadam.rpg.RpgDslLexer;
import hu.bodiadam.rpg.RpgDslParser;
import hu.bodiadam.rpg.rpg_backend.dsl.model.GameDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.springframework.stereotype.Component;

/**
 * Egyszerű belépési pont a DSL parse-olásához.
 */
@Component
public class DslParserFacade {

    // parse tree -> domain definíció
    private final GameDefinitionBuilderVisitor builderVisitor = new GameDefinitionBuilderVisitor();

    public GameDefinition parse(String source) {

        return parse(CharStreams.fromString(source));
    }

    //Filebol parseolas
    public GameDefinition parse(Path path) {
        try {
            return parse(CharStreams.fromPath(path));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read DSL file: " + path, exception);
        }
    }

    //Charstream parseolas
    private GameDefinition parse(CharStream input) {
        // Lexer
        var lexer = new RpgDslLexer(input);
        // Parser
        var parser = new RpgDslParser(new CommonTokenStream(lexer));
        var errorListener = new ThrowingErrorListener();

        // Saját hibakezelőt használunk, hogy kivételként kapjuk vissza a syntax hibákat.
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        return builderVisitor.build(parser.program());
    }

    /**
     * Az ANTLR hibáit a saját exception típusunkká alakítja.
     */
    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException exception) {
            throw new DslParseException("Syntax error at line " + line + ":" + charPositionInLine + " - " + msg, exception);
        }
    }
}

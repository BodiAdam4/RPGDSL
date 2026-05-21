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
        return build(parseTree(source));
    }

    //Filebol parseolas
    public GameDefinition parse(Path path) {
        try {
            return build(parseTree(path));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read DSL file: " + path, exception);
        }
    }

    public GameDefinition build(RpgDslParser.ProgramContext programContext) {
        return builderVisitor.build(programContext);
    }

    public RpgDslParser.ProgramContext parseTree(String source) {
        return parseProgram(CharStreams.fromString(source));
    }

    public RpgDslParser.ProgramContext parseTree(Path path) throws IOException {
        return parseProgram(CharStreams.fromPath(path));
    }

    //Charstream parseolas
    private RpgDslParser.ProgramContext parseProgram(CharStream input) {
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

        return parser.program();
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
            throw new DslParseException(
                    "Syntax error at line " + line + ":" + (charPositionInLine + 1) + " - " + msg,
                    exception);
        }
    }
}

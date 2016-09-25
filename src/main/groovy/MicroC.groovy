import groovy.util.logging.Slf4j
import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.CommonTokenStream

@Slf4j
class MicroC {

    static void main(String... args) throws Exception {
        if (!args) {
            log.error 'Error: No program specified.'
            return
        }

        MicroCLexer lex = new MicroCLexer(new ANTLRFileStream(args[0]))
        CommonTokenStream tokens = new CommonTokenStream(lex)
        MicroCParser parser = new MicroCParser(tokens)
        MicroCParser.ProgramContext context = parser.program()

        log.info context.toString()
    }

}

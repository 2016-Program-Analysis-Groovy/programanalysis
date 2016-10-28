import groovy.util.logging.Slf4j
import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

@Slf4j
class MicroC {

    static void main(String... args) throws Exception {
        if (!args) {
            log.error 'Error: No program specified.'
            return
        }

        args.each {
            log.info 'Starting Analysis for program: ' + it
            try {
                MicroCLexer lex = new MicroCLexer(new ANTLRFileStream(it))
                CommonTokenStream tokens = new CommonTokenStream(lex)
                MicroCParser parser = new MicroCParser(tokens)
                parser.setErrorHandler(new BailErrorStrategy())
                ParseTree tree = parser.program()
                ParseTreeWalker walker = new ParseTreeWalker()
                walker.walk(new MicroCWalker(), tree)
            }
            catch (FileNotFoundException ex) {
                log.error "Error: File $it not found. Skipping..."
            }
        }
    }

}

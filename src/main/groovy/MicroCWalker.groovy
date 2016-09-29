import MicroCParser.DeclContext
import groovy.util.logging.Slf4j

@Slf4j
class MicroCWalker extends MicroCBaseListener {

    @SuppressWarnings('NoDef')
    void enterProgram(MicroCParser.ProgramContext ctx) {
        List<Block> program = []
        ctx.children.eachWithIndex { def context, Integer i ->
            Block block = new Block()
            block.label = "l$i"
            block.statement = context.text
            processStatement(context, block)
            program << block

        }
        log.info program.toString()
    }

    @SuppressWarnings('UnusedMethodParameter')
    void exitProgram(MicroCParser.ProgramContext ctx) {
        log.info 'finished program'
    }

    private Block processStatement(DeclContext ctx, Block b) {
        b.variableAssigned = ctx.identifier().text
        return b
    }

    @SuppressWarnings(['NoDef', 'UnusedPrivateMethodParameter'])
    private Block processStatement(def ctx, Block b) {
        return b
    }
}

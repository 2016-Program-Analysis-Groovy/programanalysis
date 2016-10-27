import MicroCParser.BreakStmtContext
import MicroCParser.DeclContext
import MicroCParser.IfelseStmtContext
import MicroCParser.WriteStmtContext
import MicroCParser.WhileStmtContext
import MicroCParser.ReadStmtContext
import MicroCParser.AssignStmtContext
import MicroCParser.ContinueStmtContext
import MicroCParser.StmtContext
import groovy.util.logging.Slf4j
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import programanalysis.Block
import programanalysis.blocktypes.Assignment
import programanalysis.blocktypes.Break
import programanalysis.blocktypes.Declaration
import programanalysis.blocktypes.Read
import programanalysis.blocktypes.While
import programanalysis.blocktypes.Write

@Slf4j
class MicroCWalker extends MicroCBaseListener {

    final Integer labelCounter = 1

    @SuppressWarnings('NoDef')
    void enterProgram(MicroCParser.ProgramContext ctx) {
        List children = ctx.children
        children.removeAll { it.class == TerminalNodeImpl }
        def firstProgramBlock = children.first()
        def program = processStatement(firstProgramBlock)
        if (program.class == ArrayList ) {
            program = program.first()
        }
        program.outputs = children.tail().collect { def context ->
            processStatement(context)
        }
        log.info (program*.toString().join('\n'))
    }

    @SuppressWarnings('UnusedMethodParameter')
    void exitProgram(MicroCParser.ProgramContext ctx) {
        log.info 'finished program'
    }

    Block processStatement(DeclContext ctx) {
        Declaration b = new Declaration()
        b = init(b)

        b.variableType = ctx.type().text
        b.variableAssigned = ctx.identifier().text
        b.statement = "$b.variableType $b.variableAssigned"
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(WhileStmtContext ctx) {
        While w = new While()
        w = init(w)
        w.statement = ctx.expr().text
        w.outputs = ctx.children.findAll { it.class == StmtContext }.collectMany {
            processStatement(it)
        }
        return w
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(AssignStmtContext ctx) {
        Assignment a = new Assignment()
        a = init(a)
        a.variableAssigned = ctx.identifier().text
        a.statement = a.variableAssigned + ' = ' + ctx.expr().text.first()
        return a
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(WriteStmtContext ctx) {
        Write w = new Write()
        w.statement = 'write: ' + ctx.expr().text
        w = init(w)
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(ReadStmtContext ctx) {
        Read r = new Read()
        r = init(r)
        r.statement = 'read: ' + (ctx.expr()?.text ?: ctx.identifier().text)
        r = init(r)
    }

    @SuppressWarnings('[UnusedMethodParameter]')
    List<Block> processStatement(StmtContext ctx) {
        List<Block> subStatements = []
        ctx.children.each { context ->
           subStatements << processStatement(context)
        }
        return subStatements
    }

    Block processStatement(IfelseStmtContext ctx) {
        Block b = new Block()
        b = init(b)
        b.statement = 'if: ' + ctx.expr().text
        b.outputs = ctx.children.findAll { it.class == StmtContext }.collect { context ->
            processStatement(context)
        }
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(BreakStmtContext ctx) {
        Break b = new Break()
        b = init(b)
        b.statement = 'break'
        // assign breakTo

        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(ContinueStmtContext ctx) {
        Break b = new Break()
        b = init(b)
        b.statement = 'continue'
        // assign breakTo

        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(TerminalNode ctx) {
        return null
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(TerminalNodeImpl ctx) {
        assert ctx
        return null
    }

    @SuppressWarnings('NoDef')
    def init(b) {
        b.label = "l$labelCounter"
        if (labelCounter == 1) {
            b.isInitialBlock = true
        }
        labelCounter++
        return b
    }
}

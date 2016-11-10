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
import programanalysis.ReachingDefinitions
import programanalysis.blocktypes.Assignment
import programanalysis.blocktypes.Break
import programanalysis.blocktypes.Continue
import programanalysis.blocktypes.Declaration
import programanalysis.blocktypes.If
import programanalysis.blocktypes.Read
import programanalysis.blocktypes.While
import programanalysis.blocktypes.Write

@Slf4j
class MicroCWalker extends MicroCBaseListener {

    final Integer labelCounter = 1
    List<Block> program = []

    @SuppressWarnings('NoDef')
    void enterProgram(MicroCParser.ProgramContext ctx) {
        List children = ctx.children
        children.removeAll { it.class == TerminalNodeImpl }
        def firstProgramBlock = children.first()
        def initBlock = processStatement(firstProgramBlock)
        if (initBlock.class == ArrayList) {
            initBlock = initBlock.first()
        }

        processListOfBlocks(initBlock, children.tail(), true)
        List<Break> breakBlocks = program.findAll { it.class == Break }
        breakBlocks.each { breakBlock ->
            While whileLoop = findParentOfType(breakBlock, While)

            //covers case where there is a break within a while loop
            if (whileLoop) {
                breakBlock.breakTo = whileLoop.breakTo
                breakBlock.outputs = [whileLoop.breakTo]
            }
        }

        List<Break> continueBlocks = program.findAll { it.class == Continue }
        continueBlocks.each { continueBlock ->
            While whileLoop = findParentOfType(continueBlock, While)

            if (whileLoop) {
                continueBlock.breakTo = whileLoop.label
                continueBlock.outputs = [whileLoop.label]
            }
        }

        log.info(program*.toString().join('\n'))

        // run analyses
        ReachingDefinitions rdAnalysis = new ReachingDefinitions()
        rdAnalysis.rdAnalysisWithFIFO(program)
    }

    void processListOfBlocks(Block block, List contexts, Boolean isRootContext = false,
                             Boolean isHierarchical = false) {
        if (contexts.empty) {

            //case where program ends
            if (isRootContext) {
                block.isTerminalBlock = true
                return
            }

            // case: final(s)
            Block whileBlock = findParentOfType(block, While)
            if (whileBlock) {
                whileBlock.breakTo = 'l' + labelCounter
                if (block.class != Break) {
                    block.outputs << whileBlock.label
                } else {
                    block.outputs = whileBlock.breakTo
                }
            }

            Block ifStatement = findParentOfType(block, If)
            if (ifStatement && block.class != Break && block.class != Continue && block.label != ifStatement.breakTo) {
                block.endOfStatement = ifStatement.label
            }
            return
        }

        Block childBlock = processStatement(contexts.first())

        if (block.class == If && !isHierarchical) {
            List<Block> finalStatementBlocks = program.findAll { it.endOfStatement == block.label }
            finalStatementBlocks.each {
                it.outputs << childBlock.label
            }
            childBlock.inputs.addAll(finalStatementBlocks*.label)
            block.breakTo = childBlock.label
        } else {
            block.outputs << childBlock.label
            childBlock.inputs << block.label
        }
        childBlock.outputs = childBlock.outputs - null

        processListOfBlocks(childBlock, contexts.tail(), isRootContext, isHierarchical)
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
        processListOfBlocks(w, ctx.children.findAll { it.class == StmtContext })
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
        r.statement = 'read: ' +
                (ctx.expr()?.text ? ctx.identifier().text + '[' + ctx.expr().text + ']' : ctx.identifier().text)
        r.variableAssigned = ctx.identifier().text
        r.variablesUsed = ctx.expr()?.text ? [ctx.expr().text] : null
        r = init(r)
    }

    @SuppressWarnings('[UnusedMethodParameter]')
    Block processStatement(StmtContext ctx) {
        //S1
        Block b = processStatement(ctx.children.first())
        return b
    }

    Block processStatement(IfelseStmtContext ctx) {
        If b = new If()
        b = init(b)
        b.statement = 'if: ' + ctx.expr().text
        //find first conditional aka if statement
        Integer elseNode = ctx.children.findIndexOf { it.class == TerminalNodeImpl && it.text == 'else' }
        List ifCondition = ctx.children[0..elseNode]
        processListOfBlocks(b, ifCondition.findAll { it.class == StmtContext }, false, true)
        //find second conditional aka else statement
        List elseCondition = ctx.children - ifCondition
        processListOfBlocks(b, elseCondition.findAll { it.class == StmtContext }, false, true)
        b.outputs = b.outputs - null
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(BreakStmtContext ctx) {
        Break b = new Break()
        b = init(b)
        b.statement = 'break'
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block processStatement(ContinueStmtContext ctx) {
        Continue b = new Continue()
        b = init(b)
        b.statement = 'continue'

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
        program << b
        return b
    }

    Block findParentOfType(Block child, Class type) {
        List<Block> parents = program.findAll {
            it.label in child?.inputs
        }
        Block parent = parents.find { it.class == type }
        if (parent) {
            return parent
        }
        List<Block> directParents = parents
        List<Block> allParentsOfType = directParents.collect {
            findParentOfType(it, type)
        }
        return allParentsOfType ? allParentsOfType.first() : null
    }
}

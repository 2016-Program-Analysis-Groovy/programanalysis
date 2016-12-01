import MicroCParser.AexprContext
import MicroCParser.Bexpr1Context
import MicroCParser.Bexpr2Context
import MicroCParser.BreakStmtContext
import MicroCParser.DeclContext
import MicroCParser.Expr2Context
import MicroCParser.ExprContext
import MicroCParser.ExprnegateContext
import MicroCParser.IdentifierContext
import MicroCParser.IfelseStmtContext
import MicroCParser.IntegerContext
import MicroCParser.WriteStmtContext
import MicroCParser.WhileStmtContext
import MicroCParser.ReadStmtContext
import MicroCParser.AssignStmtContext
import MicroCParser.ContinueStmtContext
import MicroCParser.Expr1Context
import MicroCParser.StmtContext
import groovy.util.logging.Slf4j
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import programanalysis.Block
import programanalysis.DetectionOfSigns
import programanalysis.ReachingDefinitions
import programanalysis.blocktypes.Addition
import programanalysis.blocktypes.And
import programanalysis.blocktypes.ArrayIdentifier
import programanalysis.blocktypes.Assignment
import programanalysis.blocktypes.Break
import programanalysis.blocktypes.Continue
import programanalysis.blocktypes.Declaration
import programanalysis.blocktypes.Division
import programanalysis.blocktypes.Equal
import programanalysis.blocktypes.GreaterThan
import programanalysis.blocktypes.GreaterThanEqual
import programanalysis.blocktypes.Identifier
import programanalysis.blocktypes.If
import programanalysis.blocktypes.IntegerBlock
import programanalysis.blocktypes.LessThan
import programanalysis.blocktypes.LessThanEqual
import programanalysis.blocktypes.Minus
import programanalysis.blocktypes.Multiplication
import programanalysis.blocktypes.Negation
import programanalysis.blocktypes.NotEqual
import programanalysis.blocktypes.OR
import programanalysis.blocktypes.Read
import programanalysis.blocktypes.SubBlock
import programanalysis.blocktypes.Subtraction
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
        def initBlock = visit(firstProgramBlock)
        if (initBlock.class == ArrayList) {
            initBlock = initBlock.first()
        }

        visitListOfBlocks(initBlock, children.tail(), true)
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
        Map rdAnalysisWithFifoResult = rdAnalysis.rdAnalysis(program, 'FIFO')
        String output
        output = 'Result of RD Analysis by Block Label(FIFO): \n\n' + rdAnalysisWithFifoResult.collect { key, value ->
            '\nkey: ' + key + '     ' + value.toString()
        }
        log.info output

        Map rdAnalysisWithRPOResult = rdAnalysis.rdAnalysis(program, 'RPO')
        output = 'Result of RD Analysis by Block Label(RPO): \n\n' + rdAnalysisWithRPOResult.collect { key, value ->
            '\nkey: ' + key + '     ' + value.toString()
        }
        log.info output

        String dsOutput
        DetectionOfSigns dsAnalysis = new DetectionOfSigns()
        Map dsAnalysisWithFifoResult = dsAnalysis.dsAnalysisWithFIFO(program)
        dsOutput = 'Result of DS Analysis by Block Label: \n\n' + dsAnalysisWithFifoResult.collect { key, value ->
            '\nkey: ' + key + '     ' + value.toString()
        }
        log.info dsOutput
    }

    void visitListOfBlocks(Block block, List contexts, Boolean isRootContext = false,
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
                whileBlock.breakTo = 'L' + labelCounter
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

        Block childBlock = visit(contexts.first())

        if (block.class == If && !isHierarchical) {
            List<Block> finalStatementBlocks = program.findAll { it.endOfStatement == block.label }
            finalStatementBlocks.each {
                it.outputs << childBlock.label
            }
            childBlock.inputs.addAll(finalStatementBlocks*.label)
            block.breakTo = childBlock.label
            block.outputs << childBlock.label
            childBlock.inputs << block.label
        } else {
            block.outputs << childBlock.label
            childBlock.inputs << block.label
        }
        childBlock.outputs = childBlock.outputs - null

        visitListOfBlocks(childBlock, contexts.tail(), isRootContext, isHierarchical)
    }

    @SuppressWarnings('UnusedMethodParameter')
    void exitProgram(MicroCParser.ProgramContext ctx) {
        log.info 'finished program'
    }

    Block visit(DeclContext ctx) {
        Declaration b = new Declaration()
        b = init(b)

        b.variableType = ctx.type().text
        b.variableAssigned = visit(ctx.identifier())
        b.statement = "$b.variableType $b.variableAssigned"

        IntegerBlock block = new IntegerBlock()
        block.statement = 0
        b.variablesUsed = [block]
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(WhileStmtContext ctx) {
        While w = new While()
        w = init(w)
        w.statement = ctx.expr().text
        visitListOfBlocks(w, ctx.children.findAll { it.class == StmtContext })
        return w
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(AssignStmtContext ctx) {
        Assignment a = new Assignment()
        a = init(a)
        if (ctx.expr().size() > 1) {
            a.variableAssigned = visit(ctx.identifier(), ctx.expr(0))
            a.variablesUsed = ctx.expr().tail().collect { visit(it) }
        } else {
            a.variableAssigned = visit(ctx.identifier())
            a.variablesUsed = ctx.expr().collect { visit(it) }
        }
        a.statement = ctx.text
        return a
    }

    Block visit(IdentifierContext identifierContext, ExprContext exprContext) {
        ArrayIdentifier arrayIdentifier = new ArrayIdentifier()
        arrayIdentifier.index = visit(exprContext)
        arrayIdentifier.statement = identifierContext.text
        arrayIdentifier.identifier = visit(identifierContext)
        return arrayIdentifier
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(WriteStmtContext ctx) {
        Write w = new Write()
        w.statement = 'write: ' + ctx.expr().text
        w = init(w)
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(ReadStmtContext ctx) {
        Read r = new Read()
        init(r)
        r.statement = 'read: ' +
                (ctx.expr()?.text ? ctx.identifier().text + '[' + ctx.expr().text + ']' : ctx.identifier().text)
        if (ctx.expr()) {
            r.variableAssigned = visit(ctx.identifier(), ctx.expr())
        } else {
            r.variableAssigned = visit(ctx.identifier())
        }
        return r
    }

    @SuppressWarnings('[UnusedMethodParameter]')
    Block visit(StmtContext ctx) {
        //S1
        Block b = visit(ctx.getChild(0))
        return b
    }

    @SuppressWarnings('[UnusedMethodParameter]')
    Block visit(MicroCParser.BlockStmtContext ctx) {
        //S1
        List children = ctx.children.findAll {
            it.class != TerminalNodeImpl
        }
        Block b = visit(children.first())
        visitListOfBlocks(b, children.tail())
        return b
    }

    Block visit(IfelseStmtContext ctx) {
        If b = new If()
        b = init(b)
        b.statement = 'if: ' + ctx.expr().text
        //find first conditional aka if statement
        Integer elseNode = ctx.children.findIndexOf { it.class == TerminalNodeImpl && it.text == 'else' }
        List ifCondition = ctx.children[0..elseNode]
        visitListOfBlocks(b, ifCondition.findAll { it.class == StmtContext }, false, true)
        //find second conditional aka else statement
        List elseCondition = ctx.children - ifCondition
        visitListOfBlocks(b, elseCondition.findAll { it.class == StmtContext }, false, true)
        b.outputs = b.outputs - null
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(BreakStmtContext ctx) {
        Break b = new Break()
        b = init(b)
        b.statement = 'break'
        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(ContinueStmtContext ctx) {
        Continue b = new Continue()
        b = init(b)
        b.statement = 'continue'

        return b
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(TerminalNode ctx) {
        return null
    }

    @SuppressWarnings('UnusedMethodParameter')
    Block visit(TerminalNodeImpl ctx) {
        assert ctx
        return null
    }

    @SuppressWarnings(['NoDef', 'Instanceof'])
    def init(b) {
        b.label = "L$labelCounter"
        if (labelCounter == 1) {
            b.isInitialBlock = true
        }
        labelCounter++
        if (!(b instanceof SubBlock)) {
            program << b
        }
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

    Block visit(ExprContext exprContext) {
        if (exprContext.childCount > 1) {
            OR or = new OR()
            or.left = visit(exprContext.getChild(0))
            or.right = visit(exprContext.getChild(2))
            return or
        }
        return visit(exprContext.getChild(0))
    }

    Block visit(AexprContext aexprContext) {
        if (aexprContext.childCount > 1) {
            String operator = aexprContext.getChild(1)
            if (operator == '+') {
                Addition addition = new Addition()
                addition.left = visit(aexprContext.getChild(0))
                addition.right = visit(aexprContext.getChild(2))
                return addition
            }
            Subtraction subtraction = new Subtraction()
            subtraction.left = visit(aexprContext.getChild(0))
            subtraction.right = visit(aexprContext.getChild(2))
            return subtraction
        }
        return visit(aexprContext.getChild(0))
    }

    Block visit(Bexpr1Context bexpr1Context) {
        if (bexpr1Context.childCount > 1) {
            And and = new And()
            and.left = visit(bexpr1Context.getChild(0))
            and.right = visit(bexpr1Context.getChild(1))
            return and
        }
        return visit(bexpr1Context.getChild(0))
    }

    Block visit(Bexpr2Context bexpr2Context) {
        if (bexpr2Context.childCount > 1) {
            String operator = bexpr2Context.getChild(1)
            if (operator == '>') {
                GreaterThan op = new GreaterThan()
                op.left = bexpr2Context.getChild(0)
                op.right = bexpr2Context.getChild(2)
                return op
            } else if (operator == '>=') {
                GreaterThanEqual op = new GreaterThanEqual()
                op.left = bexpr2Context.getChild(0)
                op.right = bexpr2Context.getChild(2)
                return op
            } else if (operator == '<') {
                LessThan op = new LessThan()
                op.left = bexpr2Context.getChild(0)
                op.right = bexpr2Context.getChild(2)
                return op
            } else if (operator == '<=') {
                LessThanEqual op = new LessThanEqual()
                op.left = bexpr2Context.getChild(0)
                op.right = bexpr2Context.getChild(2)
                return op
            } else if (operator == '==') {
                Equal op = new Equal()
                op.left = bexpr2Context.getChild(0)
                op.right = bexpr2Context.getChild(2)
                return op
            }
            NotEqual op = new NotEqual()
            op.left = bexpr2Context.getChild(0)
            op.right = bexpr2Context.getChild(2)
            return op
        }
        return visit(bexpr2Context.getChild(0))
    }

    Block visit(Expr1Context expr1Context) {
        if (expr1Context.childCount > 1) {
            String operator = expr1Context.getChild(1)
            if (operator == '*') {
                Multiplication multiplication = new Multiplication()
                multiplication.left = visit(expr1Context.getChild(0))
                multiplication.right = visit(expr1Context.getChild(2))
                return multiplication
            }
            Division division = new Division()
            division.left = visit(expr1Context.getChild(0))
            division.right = visit(expr1Context.getChild(2))
            return division
        }
        return visit(expr1Context.getChild(0))
    }

    Block visit(Expr2Context expr2Context) {
        if (expr2Context.childCount > 1) {
            Assignment assignment = new Assignment()
            assignment.left = visit(expr2Context.getChild(0))
            assignment.right = visit(expr2Context.getChild(2))
        }
        return visit(expr2Context.getChild(0))
    }

    Block visit(ExprnegateContext exprnegateContext) {
        String operator = exprnegateContext.getChild(0)
        if (operator == '-') {
            Minus minus = new Minus()
            minus.operand = visit(exprnegateContext.getChild(1))
            return minus
        }
        Negation negation = new Negation()
        negation.operand = visit(exprnegateContext.getChild(1))
        return negation
    }

    Block visit(IntegerContext integerContext) {
        IntegerBlock block = new IntegerBlock()
        block.statement = integerContext.text
        return block
    }

    Block visit(IdentifierContext identifierContext) {
        Identifier identifier = new Identifier()
        identifier.statement = identifierContext.text
        return identifier
    }
}

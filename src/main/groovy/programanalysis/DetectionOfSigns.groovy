package programanalysis

import programanalysis.blocktypes.Addition
import programanalysis.blocktypes.And
import programanalysis.blocktypes.Division
import programanalysis.blocktypes.Equal
import programanalysis.blocktypes.GreaterThan
import programanalysis.blocktypes.GreaterThanEqual
import programanalysis.blocktypes.Identifier
import programanalysis.blocktypes.IntegerBlock
import programanalysis.blocktypes.LessThan
import programanalysis.blocktypes.LessThanEqual
import programanalysis.blocktypes.Minus
import programanalysis.blocktypes.Multiplication
import programanalysis.blocktypes.Negation
import programanalysis.blocktypes.NotEqual
import programanalysis.blocktypes.OR
import programanalysis.blocktypes.Subtraction

class DetectionOfSigns {

    List<Tuple> workList = []
    Map dsEntries = [:]
    Map dsExit = [:]
    //Set defaultValues = ["+", "-", "0"]
    List<Block> program

    Map<String, List<Tuple>> dsAnalysisWithFIFO(List<Block> program) {
        this.program = program
        program.each { Block block ->
            dsEntries[block.label] = []
            addEdgesToEndOfWorkList(block)
        }

        String workListToString = 'worklist:\n\n' + workList.join('\n')
        log.info workListToString

        while (!workList.empty) {
            Tuple workListItem = workList.first()
            workList = workList.drop(1)
            String l = workListItem.first()
            //String lPrime = workListItem.last()
            calculateSolution(l)
//            if (lPrime) {
//                if (dsExit[l].any { !(it in dsEntries[lPrime]) }) {
//                    List<String> signsNotInResult = dsExit[l].findAll { !(it in dsEntries[lPrime]) }
//                    dsEntries[lPrime].addAll(signsNotInResult)
//                    Block lPrimeBlock = program.find { it.label == workListItem.last() }
//                    addEdgesToEndOfWorkList(lPrimeBlock)
//                }
//            }
        }

        //to calculate exit of last block
        calculateSolution(new Tuple(program.last().label) )

        return dsExit
    }

    private addEdgesToEndOfWorkList(Block block) {
        block.outputs.each {
            workList << new Tuple(block.label, it)
        }
    }

    void calculateSolution(String label) {
        Block block = program.find { it.label == label }
        if (!block.variableAssigned) {
            dsExit[label] = dsEntries[label]
            return
        }
        dsExit[label] = dsAnalysis(dsEntries[label][block.variableAssigned], block.variablesUsed)
    }

    Map dsAnalysis(Map previousSigns, List<Block> variablesUsed) {
        variablesUsed.collect {
            dsAnalysis(previousSigns, it)
        }
        return [:]
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, IntegerBlock integerBlock) {
        switch (integerBlock.statement.toInteger()) {
            case 0 :
                return ["0"] as Set
            case { it > 0 } :
                return ["+"] as Set
            case { it < 0 } :
                return ["-"] as Set
        }
    }

    @SuppressWarnings('UnusedMethodParameter')
    Set dsAnalysis(Set previousSigns, Identifier identifier) {
        // Return the sign-set of the identifier
        return []
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Addition addition) {
        Set left = dsAnalysis(addition.left)
        Set right = dsAnalysis(addition.right)
        Set resultSet = []

        if (left.contains("-") || right.contains("-")) {
            resultSet << "-"
        }
        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
        }
        if (left.contains("+") || right.contains("+")) {
            resultSet << "+"
        }
        if (left.contains("+") && right.contains("-")) {
            resultSet << "0"
        }
        if (left.contains("-") && right.contains("+")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Subtraction subtraction) {
        Set left = dsAnalysis(subtraction.left)
        Set right = dsAnalysis(subtraction.right)
        Set resultSet = []

        if (left.contains("-") || right.contains("+")) {
            resultSet << "-"
        }
        if (left.contains("+") || right.contains("-")) {
            resultSet << "+"
        }
        if (left.contains("-") && right.contains("-")) {
            resultSet << "0"
        }
        if (left.contains("+") && right.contains("+")) {
            resultSet << "0"
        }
        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Multiplication multiplication) {
        Set left = dsAnalysis(multiplication.left)
        Set right = dsAnalysis(multiplication.right)
        Set resultSet = []

        if (left.contains("0") || right.contains("0")) {
            resultSet << "0"
        }
        if (left.contains("-") && right.contains("+")) {
            resultSet << "-"
        }
        if (left.contains("+") && right.contains("-")) {
            resultSet << "-"
        }
        if (left.contains("+") && right.contains("+")) {
            resultSet << "+"
        }
        if (left.contains("-") && right.contains("-")) {
            resultSet << "+"
        }
        return resultSet
    }

    // Add ability to handle division by zero!
    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Division division) {
        Set left = dsAnalysis(division.left)
        Set right = dsAnalysis(division.right)
        Set resultSet = []

        if (left.contains("0")) {
            resultSet << "0"
        }
        if (left.contains("-") && right.contains("+")) {
            resultSet << "-"
        }
        if (left.contains("+") && right.contains("-")) {
            resultSet << "-"
        }
        if (left.contains("+") && right.contains("+")) {
            resultSet << "+"
        }
        if (left.contains("-") && right.contains("-")) {
            resultSet << "+"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, And and) {
        Set left = dsAnalysis(and.left)
        Set right = dsAnalysis(and.right)
        Set resultSet = []

        if (left.contains("0") && left.size() == 1) {
            return ["0"]
        }
        if (right.contains("0") && right.size() == 1) {
            return ["0"]
        }
        if (left.contains("0") || right.contains("0")) {
            resultSet << "0"
        }
        resultSet << "+"
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, OR or) {
        Set left = dsAnalysis(or.left)
        Set right = dsAnalysis(or.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        resultSet << "+"
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Minus minus) {
        Set operand = dsAnalysis(minus.operand)
        Set resultSet = []

        if (operand.contains("0")) {
            resultSet << "0"
        }
        if (operand.contains("-")) {
            resultSet << "+"
        }
        if (operand.contains("+")) {
            resultSet << "-"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Negation negation) {
        Set operand = dsAnalysis(negation.operand)
        Set resultSet = []

        if (operand.contains("0")) {
            resultSet << "+"
        }
        if (operand.contains("-") || operand.contains("+")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, GreaterThan greaterThan) {
        Set left = dsAnalysis(greaterThan.left)
        Set right = dsAnalysis(greaterThan.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
        }
        if (left.contains("+") || right.contains("-")) {
            resultSet << "+"
        }
        if (left.contains("-") || right.contains("+")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, LessThan lessThan) {
        Set left = dsAnalysis(lessThan.left)
        Set right = dsAnalysis(lessThan.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
        }
        if (left.contains("-") || right.contains("+")) {
            resultSet << "+"
        }
        if (left.contains("+") || right.contains("-")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, GreaterThanEqual greaterThanEqual) {
        Set left = dsAnalysis(greaterThanEqual.left)
        Set right = dsAnalysis(greaterThanEqual.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "+"
        }
        if (left.contains("+") || right.contains("-")) {
            resultSet << "+"
        }
        if (left.contains("-") || right.contains("+")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, LessThanEqual lessThanEqual) {
        Set left = dsAnalysis(lessThanEqual.left)
        Set right = dsAnalysis(lessThanEqual.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "+"
        }
        if (left.contains("-") || right.contains("+")) {
            resultSet << "+"
        }
        if (left.contains("+") || right.contains("-")) {
            resultSet << "0"
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, Equal equal) {
        Set left = dsAnalysis(equal.left)
        Set right = dsAnalysis(equal.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "+"
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        if (left.contains("-") && right.contains("-")) {
            resultSet << "+"
        }
        if (left.contains("+") && right.contains("+")) {
            resultSet << "+"
        }
        resultSet << "0"
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    Set dsAnalysis(Set previousSigns, NotEqual notEqual) {
        Set left = dsAnalysis(notEqual.left)
        Set right = dsAnalysis(notEqual.right)
        Set resultSet = []

        if (left.contains("0") && right.contains("0")) {
            resultSet << "0"
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        if (left.contains("-") && right.contains("-")) {
            resultSet << "0"
        }
        if (left.contains("+") && right.contains("+")) {
            resultSet << "0"
        }
        resultSet << "+"
        return resultSet
    }
}

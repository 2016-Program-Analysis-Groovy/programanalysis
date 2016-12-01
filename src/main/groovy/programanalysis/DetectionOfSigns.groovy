package programanalysis

import groovy.util.logging.Slf4j
import programanalysis.blocktypes.Addition
import programanalysis.blocktypes.And
import programanalysis.blocktypes.ArrayIdentifier
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

@Slf4j
class DetectionOfSigns {

    List<Tuple> currentWorkList
    List<Tuple> pendingWorkList
    Map<String, Map<String, Set>> dsEntries = [:]
    Map<String, Map<String, Set>> dsExit = [:]
    Set defaultValues = ['+', '-', '0']
    List<Block> program
    int counter

    List algorithms = ['FIFO', 'RPO', 'LIFO']

    @SuppressWarnings(['NoDef', 'NestedBlockDepth'])
    Map<String, List<Tuple>> runDsAnalysis(List<Block> program, String wlAlgorithm) {
        if (!(wlAlgorithm in algorithms)) {
            log.error 'work list algorithm: ' + wlAlgorithm + ' is not supported'
            return [:]
        }

        currentWorkList = []
        pendingWorkList = []
        this.program = program
        program.each { Block block ->
            dsEntries[block.label] = [:]
            addEdgesToEndOfWorkList(block, wlAlgorithm)
        }

        counter = 0
        while (!isWorklistDone(wlAlgorithm)) {
            Tuple workListItem = extractFromWorklist(wlAlgorithm)
            String l = workListItem.first()
            String lPrime = workListItem.last()

            counter++
//            log.info "worklist item: (" + l + "," + lPrime + ')'

            calculateSolution(l)
            if (lPrime) {
                if (dsExit[l] != dsEntries[lPrime]) {
                    dsExit[l].each { lKey, lValue ->
                        // if the variable exists but doesn't contain the new value(s)
                        def signsAtVariableLKey = dsEntries[lPrime].find { it.key == lKey }
                        if (signsAtVariableLKey) {
                            lValue.each {
                                if (!(it in signsAtVariableLKey.value)) {
                                    signsAtVariableLKey.value.add(it)
                                }
                            }
                        } else {
                            dsEntries[lPrime].put(lKey, lValue)
                        }
                    }
                    Block lPrimeBlock = program.find { it.label == workListItem.last() }
                    addEdgesToEndOfWorkList(lPrimeBlock, wlAlgorithm)
                }
            }
        }

        log.info 'Number of iterations through the worklist: ' + counter

        //to calculate exit of last block
        calculateSolution(program.last().label)

        return dsExit
    }

    private addEdgesToEndOfWorkList(Block block, String algorithm) {
        if (algorithm == 'FIFO') {
            block.outputs.each {
                currentWorkList << new Tuple(block.label, it)
            }
        } else if (algorithm == 'RPO') {
            block.outputs.each {
                pendingWorkList << new Tuple(block.label, it)
            }
        } else if (algorithm == 'LIFO') {
            block.outputs.each {
                currentWorkList.add(0, new Tuple(block.label, it))
            }
        }
    }

    private void calculateSolution(String label) {
        Block block = program.find { it.label == label }
        if (!block) {
            log.error "Block: $label not found!!!"
            return
        }
        //dsExit[label] = dsEntries[label] <<< Don't do this!
        dsExit[label] = [:]
        dsEntries[label].keySet().each { key ->
            Set valueSet = [] as Set
            dsEntries[label][key].each { value ->
                valueSet.add(value)
            }
            dsExit[label].put(key, valueSet)
        }

        if (!block.variableAssigned) {
            return
        }
        if (block.variableAssigned.class == Identifier || !dsExit[label]?.getAt(block.variableAssigned.statement)) {
            dsExit[label].put(block.variableAssigned.statement, dsAnalysis(label, block.variablesUsed))
        } else {
            Set result = dsAnalysis(label, block.variablesUsed)
            if (result) {
                dsExit[label][block.variableAssigned.statement].addAll(result)
            } else {
                dsExit[label].put(block.variableAssigned.statement, result)
            }
        }
    }

    @SuppressWarnings('IfStatementCouldBeTernary')
    private Boolean isWorklistDone(String algorithm) {
        if (algorithm in ['FIFO', 'LIFO'] && currentWorkList.empty) {
            return true
        } else if (algorithm == 'RPO' && currentWorkList.empty && pendingWorkList.empty) {
            return true
        }
        return false
    }

    private Tuple extractFromWorklist(String algorithm) {
        switch (algorithm) {
            case 'LIFO':
            case 'FIFO':
                return extractFromFIFO()
            case 'RPO':
                return extractFromRPO()
        }
        return null
    }

    private Tuple extractFromFIFO() {
        Tuple workListItem = currentWorkList.first()
        currentWorkList = currentWorkList.drop(1)
        return workListItem
    }

    private Tuple extractFromRPO() {
        Tuple workListItem
        if (currentWorkList) {
            workListItem = currentWorkList.first()
            currentWorkList = currentWorkList.drop(1)
        } else {
            promoteWorklist()
            workListItem = currentWorkList.first()
            currentWorkList = currentWorkList.drop(1)
        }
        return workListItem
    }

    private void promoteWorklist() {
        currentWorkList = sortByRPO(pendingWorkList)
        pendingWorkList = []
    }

    private List sortByRPO(List list) {
        list.sort { Tuple a, Tuple b -> a.last() <=> b.first() ?: a.first() <=> b.last() }
    }

    @SuppressWarnings('NoDef')
    private Set dsAnalysis(String l, List<Block> variablesUsed) {
        Set results = [] as Set

        //for reads where there are no values to determine what is assigned
        if (!variablesUsed) {
            return defaultValues
        }

        variablesUsed.each {
            def result = dsAnalysis(l, it)
            if (result) {
                results << result
            }
        }
        return results.flatten()
    }

    @SuppressWarnings(['UnusedPrivateMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, IntegerBlock integerBlock) {
        switch (integerBlock.statement.toInteger()) {
            case 0 :
                return ['0'] as Set
            case { it > 0 } :
                return ['+'] as Set
            case { it < 0 } :
                return ['-'] as Set
        }
    }

    @SuppressWarnings('UnusedMethodParameter')
    private Set dsAnalysis(String l, Identifier identifier) {
        return dsEntries[l]?.get(identifier.statement) ?: defaultValues
    }

    private Set dsAnalysis(String l, ArrayIdentifier arrayIdentifier) {
        if (dsAnalysis(l, arrayIdentifier.index).contains('-')) {
            return []
        }
        return dsAnalysis(l, arrayIdentifier.identifier)
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Addition addition) {
        Set left = dsAnalysis(l, addition.left)
        Set right = dsAnalysis(l, addition.right)
        Set resultSet = []

        if (left.contains('-') || right.contains('-')) {
            resultSet << '-'
        }

        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
        }
        if (left.contains('+') || right.contains('+')) {
            resultSet << '+'
        }
        if (left.contains('+') && right.contains('-')) {
            resultSet << '0'
        }
        if (left.contains('-') && right.contains('+')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Subtraction subtraction) {
        Set left = dsAnalysis(l, subtraction.left)
        Set right = dsAnalysis(l, subtraction.right)
        Set resultSet = []

        if (left.contains('-') || right.contains('+')) {
            resultSet << '-'
        }
        if (left.contains('+') || right.contains('-')) {
            resultSet << '+'
        }
        if (left.contains('-') && right.contains('-')) {
            resultSet << '0'
        }
        if (left.contains('+') && right.contains('+')) {
            resultSet << '0'
        }
        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Multiplication multiplication) {
        Set left = dsAnalysis(l, multiplication.left)
        Set right = dsAnalysis(l, multiplication.right)
        Set resultSet = []

        if (left.contains('0') || right.contains('0')) {
            resultSet << '0'
        }
        if (left.contains('-') && right.contains('+')) {
            resultSet << '-'
        }
        if (left.contains('+') && right.contains('-')) {
            resultSet << '-'
        }
        if (left.contains('+') && right.contains('+')) {
            resultSet << '+'
        }
        if (left.contains('-') && right.contains('-')) {
            resultSet << '+'
        }
        return resultSet
    }

    private Set dsAnalysis(String l, Division division) {
        Set left = dsAnalysis(l, division.left)
        Set right = dsAnalysis(l, division.right)
        Set resultSet = []

        if (right.contains('0')) {
            // division by zero is undefined
            return []
        }
        if (left.contains('0')) {
            resultSet << '0'
        }
        if (left.contains('-') && right.contains('+')) {
            resultSet << '-'
        }
        if (left.contains('+') && right.contains('-')) {
            resultSet << '-'
        }
        if (left.contains('+') && right.contains('+')) {
            resultSet << '+'
        }
        if (left.contains('-') && right.contains('-')) {
            resultSet << '+'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, And and) {
        Set left = dsAnalysis(l, and.left)
        Set right = dsAnalysis(l, and.right)
        Set resultSet = []

        if (left.contains('0') && left.size() == 1) {
            return ['0']
        }
        if (right.contains('0') && right.size() == 1) {
            return ['0']
        }
        if (left.contains('0') || right.contains('0')) {
            resultSet << '0'
        }
        resultSet << '+'
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, OR or) {
        Set left = dsAnalysis(l, or.left)
        Set right = dsAnalysis(l, or.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        resultSet << '+'
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Minus minus) {
        Set operand = dsAnalysis(l, minus.operand)
        Set resultSet = []

        if (operand.contains('0')) {
            resultSet << '0'
        }
        if (operand.contains('-')) {
            resultSet << '+'
        }
        if (operand.contains('+')) {
            resultSet << '-'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Negation negation) {
        Set operand = dsAnalysis(l, negation.operand)
        Set resultSet = []

        if (operand.contains('0')) {
            resultSet << '+'
        }
        if (operand.contains('-') || operand.contains('+')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, GreaterThan greaterThan) {
        Set left = dsAnalysis(l, greaterThan.left)
        Set right = dsAnalysis(l, greaterThan.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
        }
        if (left.contains('+') || right.contains('-')) {
            resultSet << '+'
        }
        if (left.contains('-') || right.contains('+')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, LessThan lessThan) {
        Set left = dsAnalysis(l, lessThan.left)
        Set right = dsAnalysis(l, lessThan.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
        }
        if (left.contains('-') || right.contains('+')) {
            resultSet << '+'
        }
        if (left.contains('+') || right.contains('-')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, GreaterThanEqual greaterThanEqual) {
        Set left = dsAnalysis(l, greaterThanEqual.left)
        Set right = dsAnalysis(l, greaterThanEqual.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '+'
        }
        if (left.contains('+') || right.contains('-')) {
            resultSet << '+'
        }
        if (left.contains('-') || right.contains('+')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, LessThanEqual lessThanEqual) {
        Set left = dsAnalysis(l, lessThanEqual.left)
        Set right = dsAnalysis(l, lessThanEqual.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '+'
        }
        if (left.contains('-') || right.contains('+')) {
            resultSet << '+'
        }
        if (left.contains('+') || right.contains('-')) {
            resultSet << '0'
        }
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, Equal equal) {
        Set left = dsAnalysis(l, equal.left)
        Set right = dsAnalysis(l, equal.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '+'
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        if (left.contains('-') && right.contains('-')) {
            resultSet << '+'
        }
        if (left.contains('+') && right.contains('+')) {
            resultSet << '+'
        }
        resultSet << '0'
        return resultSet
    }

    @SuppressWarnings(['UnusedMethodParameter', 'UnnecessaryGString'])
    private Set dsAnalysis(String l, NotEqual notEqual) {
        Set left = dsAnalysis(l, notEqual.left)
        Set right = dsAnalysis(l, notEqual.right)
        Set resultSet = []

        if (left.contains('0') && right.contains('0')) {
            resultSet << '0'
            if (left.size() == 1 && right.size() == 1) {
                return resultSet
            }
        }
        if (left.contains('-') && right.contains('-')) {
            resultSet << '0'
        }
        if (left.contains('+') && right.contains('+')) {
            resultSet << '0'
        }
        resultSet << '+'
        return resultSet
    }
}

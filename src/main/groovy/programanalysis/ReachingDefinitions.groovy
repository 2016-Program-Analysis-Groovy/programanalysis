package programanalysis

import groovy.util.logging.Slf4j

@Slf4j
class ReachingDefinitions {
    List<Tuple> currentWorkList
    Set<Tuple> pendingWorkList
    Map rdEntries = [:]
    Map rdExit = [:]
    Map<String, List<Tuple>> rdKill = [:]
    Map<String, Tuple> rdGen = [:]
    int counter = 0

    List algorithms = ['FIFO', 'RPO', 'LIFO']

    Map<String, List<Tuple>> rdAnalysis(List<Block> program, String wlAlgorithm) {
        if (!(wlAlgorithm in algorithms)) {
            log.error 'work list algorithm: ' + wlAlgorithm + ' is not supported'
            return [:]
        }
        currentWorkList = []
        pendingWorkList = []
        program.each { Block block ->
            rdEntries[block.label] = []
            addEdgesToEndOfWorkList(block, wlAlgorithm)
            if (block.variableAssigned) {
                List<Tuple> influencedBlocks = rdKill.values().find {
                    it.findAll { it.first() == block.variableAssigned.statement }
                }
                influencedBlocks.each { Tuple blockTuple ->
                    String variable = blockTuple.first()
                    String influencedBlockLabel = blockTuple.last()
                    rdKill[influencedBlockLabel] = rdKill[influencedBlockLabel] + [new Tuple(variable, block.label)]
                }
                rdKill[block.label] = influencedBlocks ?
                        influencedBlocks + [new Tuple(block.variableAssigned.statement, block.label)] :
                        [new Tuple(block.variableAssigned.statement, block.label)]

                rdGen[block.label] = [new Tuple(block.variableAssigned.statement, block.label)]
            }
        }

//        if (wlAlgorithm == 'FIFO') {
//            Collections.shuffle(currentWorkList)
//        }

        counter = 0
        String workListOutput =''
        while (!isWorklistDone(wlAlgorithm)) {

            Tuple workListItem = extractFromWorklist(wlAlgorithm)

            String l = workListItem.first()
            String lPrime = workListItem.last()
//            workListOutput += 'worklist item: (' + l + ',' + lPrime + ')\n'
            counter++
            calculateSolution(l)
            if (lPrime) {
                if (rdExit[l].any { !(it in rdEntries[lPrime]) }) {
                    List<Tuple> tuplesNotInResult = rdExit[l].findAll { !(it in rdEntries[lPrime]) }
                    rdEntries[lPrime].addAll(tuplesNotInResult)
                    Block lPrimeBlock = program.find { it.label == lPrime }
                    addEdgesToEndOfWorkList(lPrimeBlock, wlAlgorithm)
                }
            }
        }
        log.info workListOutput + '\nTotal count: ' + counter

        //to calculate exit of last block
        calculateSolution(new Tuple(program.last().label) )

        return rdExit
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

    private void calculateSolution(String l) {
        rdExit[l] = (rdEntries[l] - rdKill[l])
        if (rdGen[l]) {
            rdExit[l] = rdExit[l] + rdGen[l]
        }
        rdExit[l].sort { a, b -> a.first() <=> b.first() ?: a.last() <=> b.last() }
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

    @SuppressWarnings('SpaceAroundOperator')
    private List sortByRPO(list) {
        list.sort {
            Tuple a, Tuple b -> extractCounterFromBlockLabel(a.first()) <=> extractCounterFromBlockLabel(b.first()) ?:
                extractCounterFromBlockLabel(a.first()) <=> extractCounterFromBlockLabel(b.last()) }
    }

    private Integer extractCounterFromBlockLabel(String label) {
        label[1..label.size() - 1].toInteger()
    }
}

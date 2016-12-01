package programanalysis

import groovy.util.logging.Slf4j

@Slf4j
class ReachingDefinitions {
    List<Tuple> currentWorkList
    List<Tuple> pendingWorkList
    Map rdEntries = [:]
    Map rdExit = [:]
    Map<String, List<Tuple>> rdKill = [:]
    Map<String, Tuple> rdGen = [:]

    List algorithms = ['FIFO', 'RPO']

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

//        String currentWorkListToString = 'current worklist:\n\n' + currentWorkList.join('\n')
//        String pendingWorkListToString = 'pending worklist:\n\n' + pendingWorkList.join('\n')
//        log.info currentWorkListToString
//        log.info pendingWorkListToString

        while (!isWorklistDone(wlAlgorithm)) {
            Tuple workListItem = extractFromWorklist(wlAlgorithm)
            String l = workListItem.first()
            String lPrime = workListItem.last()
            log.info 'worklist item: (' + l + ',' + lPrime + ')'
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
        }
    }

    void calculateSolution(String l) {
        rdExit[l] = (rdEntries[l] - rdKill[l])
        if (rdGen[l]) {
            rdExit[l] = rdExit[l] + rdGen[l]
        }
    }

    @SuppressWarnings('IfStatementCouldBeTernary')
    Boolean isWorklistDone(String algorithm) {
       if (algorithm == 'FIFO' && currentWorkList.empty) {
                    return true
        } else if (algorithm == 'RPO' && currentWorkList.empty && pendingWorkList.empty) {
            return true
        }
        return false
    }

    Tuple extractFromWorklist(String algorithm) {
        switch (algorithm) {
            case 'FIFO':
                return extractFromFIFO()
            case 'RPO':
                return extractFromRPO()
        }
        return null
    }

    Tuple extractFromFIFO() {
        Tuple workListItem = currentWorkList.first()
        currentWorkList = currentWorkList.drop(1)
        return workListItem
    }

    Tuple extractFromRPO() {
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

    void promoteWorklist() {
        currentWorkList = sortByRPO(pendingWorkList)
        pendingWorkList = []
    }

    List sortByRPO(List list) {
        list.sort { Tuple a, Tuple b -> a.last() <=> b.first() ?: a.first() <=> b.last() }
    }
}

package programanalysis

import groovy.util.logging.Slf4j

@Slf4j
class ReachingDefinitions {
    List<Tuple> workList = []
    Map rdEntries = [:]
    Map rdExit = [:]
    Map rdKill = [:]
    Map rdGen = [:]

    Map<String, List<Tuple>> rdAnalysisWithFIFO(List<Block> program) {
        program.each { Block block ->
            rdEntries[block.label] = []
            addEdgesToWorkList(block)
            if (block.variableAssigned){
                List<Tuple> influencedBlocks = rdKill.values().findAll{ it.first == block.variableAssigned}
                influencedBlocks.each { String variable, String influencedBlockLabel ->
                    rdKill[influencedBlockLabel] = rdKill[influencedBlockLabel] + new Tuple(variable, block.label)
                    rdKill[block.label] = rdKill[block.label] + new Tuple(variable, influencedBlockLabel)
                }
                rdKill[block.label] = rdKill[block.label] + new Tuple(block.variableAssigned, block.label)

                rdGen[block.label] = new Tuple(block.variableAssigned, block.label)
            }
        }

        while(!workList.empty){
            Tuple workListItem = workList.first()
            String l = workListItem.first()
            String lPrime = workListItem.last()
            rdExit[l] = (rdEntries[l] - rdKill[l] ) + rdGen[l]
            if (rdExit[l].any{
                !(it in rdEntries[lPrime])
            }) {
                rdEntries[lPrime] += rdExit[l]
                Block lPrimeBlock = program.find{it.label == lPrime}
                addEdgesToWorkList(lPrimeBlock)
            }
        }
        String workListToString = workList.collect{it.toString()}.join('\n')
        log.info workListToString
        return rdExit

    }

    private addEdgesToWorkList(Block block){
        block.outputs.each {
            workList << new Tuple(block.label, it)
        }
    }
}

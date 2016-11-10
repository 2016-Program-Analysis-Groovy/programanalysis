package programanalysis

import groovy.util.logging.Slf4j

@Slf4j
class ReachingDefinitions {
    List<Tuple> workList = []

    List<Tuple> runRDAnalysis(List<Block> program) {
        program.each {
            //do stuff
        }
        return workList //not for real, just to make the compiler happy

    }
}

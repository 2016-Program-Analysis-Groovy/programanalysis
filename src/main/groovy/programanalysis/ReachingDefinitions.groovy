package programanalysis

import groovy.util.logging.Slf4j

@Slf4j
class ReachingDefinitions {
    List<Tuple> workList = []
    Map<String, List<String>> killTable = [:]
    Map<String, Closure> equations = [:]

    List<Tuple> runRDAnalysis(Block program) {
        //set initial result set
        List<Tuple> result = []

        calculateEquations(program)

        workList.each { log.info it.toString() }

        //iterate through worklist
        while (!workList.empty) {
            //pop first element
            Tuple item = workList.pop()

            //update result set based on equations for each variable at that edge
            result = equations[item.first()].call()
        }

        //return final result
        return result
    }

    void checkBlockForVariableAssignment(Block block) {
        List<String> outputLabels = block.outputs*.label.flatten()
        outputLabels.each { label ->
            workList << new Tuple(block.label, label)
        }
//        if (block.variableAssigned) {
//           killTable[block.variableAssigned].push(block.label)
//        }
    }

    void calculateEquations(Block block) {
        if (!block) {
            return
        }
        //check each block for variable assignment
        checkBlockForVariableAssignment(block)

        //calculate equations for each variable at that block label
        equations[block.label] = {
            // if variable assigned, then kill/gen and add element to worklist
            Map<String, List<String>> variableAssigned = killTable.find {
                it.value.contains(block.label)
            }
            if (variableAssigned) {
                result.removeAll(variableAssigned.values())
                result += new Tuple(variableAssigned.keySet().first(), block.label)
            }

            // if no variable assigned, then use the inputs
            block.inputs*.label.collect {
                equations[it]
            }
        }
        block.outputs.each { calculateEquations(it) }
    }
}

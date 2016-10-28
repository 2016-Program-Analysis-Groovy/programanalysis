package programanalysis

class ReachingDefinitions {
    List<Tuple> workList = []
    Map<String,List<String>> killTable = [:]
    Map<String,Closure> equations = [:]

    List<Tuple> runRDAnalysis(Block program) {
        //set initial result set
        List<Tuple> result = []

        //check initial block
        checkBlockForVariableAssignment(program)

        //set first equation
        equations[program.label] = {
            return result
        }

        //iterate through blocks
        program.outputs.each { Block block ->
            //check each block for variable assignment
            checkBlockForVariableAssignment(block)

            //calculate equations for each variable at that block label
            equations[block.label] = {
                // if variable assigned, then kill/gen and add element to worklist

                // if no variable assigned, then use the inputs
                block.inputs*.label.collect {
                    equations[it]
                }
            }
        }

        //iterate through worklist
        while (!workList.empty) {
            //pop first element
            Tuple item = workList.pop()

            //update result set based on equations for each variable at that edge
            result = equations[item].call()
        }

        //return final result
        return result
    }

    void checkBlockForVariableAssignment(Block block) {
        if (block.variableAssigned) {
            List<String> outputLabels = block.outputs*.label
            outputLabels.each { label ->
                workList << new Tuple(block.label, label)
            }
            killTable[block.variableAssigned].add(block.label)
        }
    }
}

package programanalysis

class Block {
    String label
    String statement
    List<String> inputs = []
    List<String> outputs = []
    List<Block> variablesUsed = []
    Block variableAssigned
    Boolean isInitialBlock = false
    Boolean isTerminalBlock = false
    String endOfStatement

    String toString() {
        String output = """
        \n---- $properties
        |label: ${this.label}
        |type: ${this.class.simpleName}
        |statement: ${this.statement}\n""".stripMargin()

        output += variables

        if (!this.isInitialBlock && this.inputs) {
            output += 'inputs: ' + this.inputs.toString() + '\n'
        }
        if (!this.isTerminalBlock && this.outputs) {
            output += 'outputs: ' + this.outputs.toString() + '\n'
        }

        return output
    }

    String getProperties() {
        String output = ''
        if (isInitialBlock) {
            output += 'Initial Block ----\n'
        }
        if (isTerminalBlock) {
            output += 'Terminal Block ----\n'
        }
        return output
    }

    String getVariables() {
        String output = ''
        if (variableAssigned) {
            output += 'variables assigned: ' + variableAssigned + '\n'
        }
        if (variablesUsed) {
            output += 'variables used: ' + variablesUsed + '\n'
        }
        return output
    }
}

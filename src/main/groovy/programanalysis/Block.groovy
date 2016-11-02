package programanalysis

class Block {
    String label
    String statement
    List<Block> inputs = []
    List<Block> outputs = []
    List<String> variablesUsed
    String variableAssigned
    Boolean isInitialBlock = false
    Boolean isTerminalBlock = false

    String toString() {
        String output = """
        \n---- $properties
        |label: ${this.label}
        |type: ${this.class.simpleName}
        |statement: ${this.statement}\n""".stripMargin()

        output += variables

        if (!this.isInitialBlock && this.inputs) {
            output += 'inputs: ' + this.inputs + '\n'
        }
        if (!this.isTerminalBlock && this.outputs) {
            output += 'outputs: \n' + this.outputs*.toString().join('')
        }

        return output + '----'
    }

    String getProperties() {
        String output = ''
        if (isTerminalBlock) {
            output += 'Terminal Block\n'
        }
        if (isInitialBlock) {
            output += 'Initial Block\n'
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

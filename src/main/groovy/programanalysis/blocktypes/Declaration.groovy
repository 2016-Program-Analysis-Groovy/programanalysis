package programanalysis.blocktypes

import programanalysis.Block

class Declaration extends Block {
    String variableType
    String variableName

    String setStatement() {
        "${variableType} + ${this.variableName}"
    }
}

enum VariableType {
    INT,
    VOID
}

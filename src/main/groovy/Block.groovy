/**
 * Created by Signe Geisler on 29-Sep-16.
 */
class Block {

    String label
    String statement
    List<Block> inputs
    List<Block> outputs
    List<String> variablesUsed
    String variableAssigned
    Boolean isInitialBlock
    Boolean isTerminalBlock
    Block breakTo

    String toString() {
        return """
            ----
            label: ${this.label}
            statement: ${this.statement}
            ----\n
        """

    }
}

package programanalysis.blocktypes

import programanalysis.Block

class Continue extends Block {
    String breakTo

    @Override
    String toString() {
        super.toString() + "\nbreakTo: ${this.breakTo}\n"
    }
}

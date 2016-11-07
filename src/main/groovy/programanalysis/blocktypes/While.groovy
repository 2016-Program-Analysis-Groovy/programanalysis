package programanalysis.blocktypes

import programanalysis.Block

class While extends Block {
    String breakTo

    @Override
    String toString() {
        super.toString() + "\nbreakTo: ${this.breakTo}\n"
    }
}

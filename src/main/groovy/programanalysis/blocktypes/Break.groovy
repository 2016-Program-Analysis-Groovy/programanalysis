package programanalysis.blocktypes

import programanalysis.Block

class Break extends Block {
    Block breakTo

    @Override
    String toString() {
        super.toString() + "\nbreakTo: ${this.breakTo?.label}\n"
    }
}

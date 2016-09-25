import spock.lang.Specification
import spock.lang.Unroll

class MicroCTest extends Specification {

    @Unroll
    def "test bad inputs"() throws FileNotFoundException {
        setup:
        MicroC microC = new MicroC()

        when:
        microC.main(filename)

        then:
        thrown FileNotFoundException

        where:
        filename << ['', 'does not exist']
    }

    def "test valid input"() {
        setup: MicroC microC = new MicroC()

        when:
        microC.main('src/test/microc/test1.microc')

        then:
        0 * _ // nothing should happen on success
    }
}

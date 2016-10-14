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
}

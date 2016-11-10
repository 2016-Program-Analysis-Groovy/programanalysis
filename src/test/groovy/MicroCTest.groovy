import org.antlr.v4.runtime.misc.ParseCancellationException

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class MicroCTest extends Specification {

    @Shared
    List<String> filenames = []
    MicroC microC = new MicroC()

    def setupSpec() {
        new File(this.class.getResource('microc/successes').file).eachFile { File file ->
            filenames << file.path
        }
    }

    @Unroll
    def "test example programs succeed #filename"() {
        when:
        microC.main(filename)

        then:
        notThrown FileNotFoundException

        where:
        filename << filenames
    }

    def "test unparseable file"() {
        when:
        microC.main(this.class.getResource('microc/failures/unparseable.microC').path)

        then:
        thrown ParseCancellationException
    }
}

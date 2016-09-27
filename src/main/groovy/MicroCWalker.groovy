import groovy.util.logging.Slf4j

@Slf4j
class MicroCWalker extends MicroCBaseListener {

    void enterProgram(MicroCParser.ProgramContext ctx) {
        log.info 'starting program analysis: '
        log.info 'declarations: \n'
        ctx.decl().each {
            log.info it.text
        }

        log.info '\n statements: \n'
        ctx.stmt().each {
            log.info it.text
        }
    }

    @SuppressWarnings('UnusedMethodParameter')
    void exitProgram(MicroCParser.ProgramContext ctx) {
        log.info 'finished program'
    }

}

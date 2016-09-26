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
            if(it.childCount > 0) {
                log.info it.c
            }
            log.info it.text
        }
    }

    @SuppressWarnings('UnusedMethodParameter')
    void exitProgram(MicroCParser.ProgramContext ctx) {
        log.info 'finished program'
    }

}

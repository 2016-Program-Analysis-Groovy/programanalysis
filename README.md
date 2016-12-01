# programanalysis
static analysis of microc programs written in groovy for Program Analysis at DTU.

----

This is a basic groovy project with a gradle wrapper. no downloading necessary!

To build and run the test programs `./gradlew build` (unix) or `gradlew build` (windows)

To parse a microc program `./gradlew run -Pfiles='filepath/filename1,filepath/filename2'` (unix) or `gradlew run -Pfiles='filepath/filename1,filepath/filename2'` (windows)
files is a comma separated list of arguments to pass to the parser.
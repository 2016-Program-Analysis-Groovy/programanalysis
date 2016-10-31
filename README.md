# programanalysis
static analysis of microc programs written in groovy for Program Analysis at DTU.jfjkddsa

----

This is a basic groovy project with a gradle wrapper. no downloading necessary!

To build and run the tests `./gradlew build` (unix) or `gradlew build` (windows)

To parse a microc program `./gradlew run -Pfiles='filepath/filename1,filepath/filename2'` (unix) or `gradlew run -Pfiles='filepath/filename1,filepath/filename2'` (windows)
files is a comma separated list of arguments to pass to the parser.

To debug in IntelliJ run `./gradlew run --debug-jvm ...` (unix) or `gradlew run --debug-jvm` (windows)

### Helpful Github commands

To get the new changes on your machine run `git checkout master` to switch to the main branch. 

Then run `git fetch upstream` to pull from the server. 

Then run `git merge upstream/master` to merge the changes into your local copy.

# programanalysis
static analysis of microc programs written in groovy for Program Analysis at DTU.

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

## Commiting

### Create a new branch

Assuming you are on master and did not create a new branch yet, run

`git checkout -b my-new-branch-name`

### Add all modified files

`git add .`

Check that all files are included

`git status`

In most terminals it uses green for added and red for not added.

### Commit the changes

`git commit -m "my wonderful commit message goes here"`

Alternatively use `git commit` then type a longer message in vim.

### Push to Github

`git push origin my-new-branch-name`

This will push the changes to your fork's remote repository on github.

### Create a pull request

In the github ui, it may prompt you automatically to create a pull request.  If it doesn't look for the tab pull
requests, then click new pull request on the right side.  Verify all changes are correct and then create pull request.

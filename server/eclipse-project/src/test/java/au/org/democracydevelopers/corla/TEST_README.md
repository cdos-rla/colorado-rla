# Democracy Developers Tests for IRV Extensions

Almost all the tests in this directory are intended to run automatically. Simply 
open the project in your IDE, right-click on 
`server/eclipse-project/src/test/java` and select "run all tests." Use TestNG rather than JUnit.

If you prefer to run the tests from the command line, use
```
mvn test -Dtest="au.org.democracydevelopers.corla.**"
```
Depending on your setup, you may also need to add `-Dnet.bytebuddy.experimental=true`

## Tests for manual or command-line runs
There are three tests intended to be run manually, all of which have matching automated versions.
- `src/test/java/au/org/democracydevelopers/corla/workflows/WorkflowRunnerWithRaire.java` runs any 
workflow instance from the command line, but calls out to the 
raire-service rather than mocking it (which the WorkflowRunner does). This is useful if you
want to test a full run-through with access to the raire-server, to combine the workflow with some 
manual logins, to download reports, or to inspect the database. Instructions are at the top of the file.
- `src/test/resources/workflows/IntelliJ-http-workflows/README.md`
contains instructions for manually running a toy IRV example.
- `src/test/java/au/org/democracydevelopers/corla/workflows/manualWorkflowWithTTimeoutAndAssertionReplacement.md` 
contains instructions for testing the case when assertion generation times out. This matches the 
workflow in `src/test/java/au/org/democracydevelopers/corla/workflows/WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement.java`.


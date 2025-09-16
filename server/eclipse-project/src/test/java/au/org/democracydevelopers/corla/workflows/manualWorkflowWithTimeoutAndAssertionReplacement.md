# Workflow to test replacement of assertions and summaries
This workflow tests that assertions and assertion generation summaries are properly replaced
- after an assertion generation failure, and
- after a successful assertion generation, but with changed CVRs.
It matches the automated test in `src/test/java/au/org/democracydevelopers/corla/workflows/WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement.java`.

## CSV data upload
Log in as countyadmin1 and upload the Byron Mayoral CSV and matching manifest:
- src/test/resources/CSVs/NewSouthWales21/Byron_Mayoral.csv
- src/test/resources/CSVs/NewSouthWales21/Byron_Mayoral.manifest.csv
You can use the .sha256sum files or recompute the sha256sum yourself.

Any other example with a large number of candidates will also work.

## Initializing the audit
Log in as stateadmin1 and follow the `Define Audit` flow. Any canonicalization file is fine, but do
not actually do any canonicalization (leave it at 'no change'). Stop at the Generate Assertions page.

## Generate Assertions with too-short timeout (expected to fail)
We will interfere with the generate assertions request and give raire a very short timeout, causing
it to fail.

These instructions are for Firefox, but the same operations are available in Chrome, possibly under
slightly different names or menus.

In the CDOS Dashboard on the Generate Assertions page, open the Developer for Firefox: click on the main hamburger menu, then
`More Tools` -> `Web Developer Tools`. You need the web browser hamburger menu, not the Colorado RLA hamburger menu.
(You can also use (Chrome's the equivalent tools)[https://developer.chrome.com/docs/devtools/open] or Safari's, 
but the details about how to achieve the required result may be slightly different.)

Select the “Sources” tab, and navigate to Main Thread → localhost:3000 → static → bundle.js.
Right-click on “bundle.js” and choose “add script override”. You’ll be prompted to save a local copy of the file – the Downloads folder is fine.

Open your saved copy of bundle.js, in a simple editor such as notepad. Search for the line “generationTimeOutSeconds: 5.” (This is line 101771 in my browser, but may be different depending on your setup.)

Change the 5 to 0.00001. The lines should now look like
```
_this.state = {
    generatingAssertions: false,
    generationTimeOutSeconds: 0.00001,
    generationWasRun: false,
};
```

Save the file.

Go back to the client and click the “Generate Assertions” button.
You should see a failure with a “TIME_OUT_GENERATING_ASSERTIONS” message.
If you have access to the raire-service logs, the TIME_OUT_GENERATING_ASSERTIONS error should appear there too.

If you like, export the assertions (JSON or CSV) and check that there are none.

## Generating assertions with a longer (default) timeout.

The Generate Assertions Page should now show you a box that allows you to set the timeout. Set it back to something reasonable (5 seconds is a good default). Click ‘Generate Assertions’ again.

You should now see a success response. At this point, the audit could progress successfully and audit this contest. The winner should be LYON Michael.

Export the JSON assertions and put them into the (Assertion Explainer)[https://democracydevelopers.github.io/raire-rs/WebContent/explain_assertions.html].
You should see all 8 candidates other than LYON Michael ruled out.

## Overwriting the assertions after replacing the CSVs

- Log in as countyadmin1.
- Replace the CSV file with Byron_Mayoral_Swivel_Lyon_Swapped.csv

(No need to replace the manifest).

- If you still have the `Generate Assertions` page up, you can just reuse it with a 5 second timeout.
- If you have logged out of the SoS dashboard, log back in as stateadmin,
restart the ‘define audit’ process and click through canonicalisation (no change), until you reach the
`Generate Assertions` page.
- Click `Generate Assertions.`
- You should see a successful generate assertions record with winner “SWIVEL SwappedWithLyon".

If you have access to the raire-service logs, you should see successful assertion generation again.

Export the JSON assertions and put them into the  (Assertion Explainer)[https://democracydevelopers.github.io/raire-rs/WebContent/explain_assertions.html].
You should see all 8 candidates other than SWIVEL SwappedWithLyon ruled out.


import {Breadcrumb, Button, Card, Intent, Spinner} from '@blueprintjs/core';
import dashboardRefresh from 'corla/action/dos/dashboardRefresh';
import * as _ from 'lodash';
import * as React from 'react';

import exportAssertionsAsCsv from 'corla/action/dos/exportAssertionsAsCsv';
import exportAssertionsAsJson from 'corla/action/dos/exportAssertionsAsJson';
import generateAssertions from 'corla/action/dos/generateAssertions';
import DOSLayout from 'corla/component/DOSLayout';

const generationTimeoutParam = 'timeLimitSeconds';

const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS'/>></li>
        <li><Breadcrumb href='/sos/audit' text='Audit Admin'/></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Generate Assertions'/></li>
    </ul>
);

interface GenerateAssertionsPageProps {
    dosState: DOS.AppState;
    forward: OnClick;
    readyToGenerate: boolean;
}

interface GenerateAssertionsPageState {
    generatingAssertions: boolean;
    generationTimeOutSeconds: number;
    generationWasRun: boolean;
}

class GenerateAssertionsPage extends React.Component<GenerateAssertionsPageProps, GenerateAssertionsPageState> {
    public constructor(props: GenerateAssertionsPageProps) {
        super(props);

        this.state = {
            generatingAssertions: false,
            generationTimeOutSeconds: 5,
            generationWasRun: false,
        };
    }

    public render() {
        const {
            dosState,
            forward,
            readyToGenerate,
        } = this.props;

        const generate = async () => {
            this.setState({generatingAssertions: true});
            this.render();

            const timeoutQueryParams = new URLSearchParams();
            timeoutQueryParams.set(generationTimeoutParam, this.state.generationTimeOutSeconds.toString());
            generateAssertions(timeoutQueryParams).then()
            .catch(reason => {
                alert('generateAssertions error in fetchAction ' + reason);
            }).finally(() => {
                this.setState({generatingAssertions: false});
                this.setState({generationWasRun: true});
                // Refresh dashboard to collect full assertion info
                dashboardRefresh();
            });
        };

        const main =
            <div>
                <Breadcrumbs/>
                <h1 className={'mediumHeader'}>Generate Assertions for IRV Contests</h1>
                <p>
                    Assertions will be generated for all IRV contests to
                    support opportunistic discrepancy computation.
                </p>
                <div className='control-buttons mt-default'>
                    <Button onClick={generate}
                            disabled={this.state.generatingAssertions}
                            className='pt-button pt-intent-primary'>
                        Generate Assertions
                    </Button>
                </div>
                {this.displayTimeoutInput() &&
                    <div>
                        <label htmlFor='timeOut'>Timeout (in seconds): </label>
                        <input
                            name='timeOut'
                            type='number'
                            min={5}
                            max={999}
                            className='pt-input'
                            style={{width: '60px'}}
                            value={this.state.generationTimeOutSeconds}
                            onChange={this.onTimeoutChange}>
                        </input>
                    </div>
                }
                <div className='control-buttons mt-default'>
                    <Button onClick={forward}
                            disabled={this.disableNextButton()}
                            className='pt-button pt-intent-primary'>
                        Next
                    </Button>
                </div>
                <div>
                    {this.getAssertionGenerationStatusTable()}
                </div>
            </div>;

        return <DOSLayout main={main}/>;
    }

    private displayTimeoutInput() {
        return this.props.dosState.assertionGenerationStatuses &&
            this.props.dosState.assertionGenerationStatuses.some(ags => !ags.succeeded && ags.retry);
    }

    private onTimeoutChange = (event: React.FormEvent<HTMLInputElement>) => {
        const timeout = (event.target as HTMLInputElement).value;
        this.setState({generationTimeOutSeconds: Number(timeout)});
    }

    private disableNextButton() {
        const assertionsAlreadyGenerated = this.props.dosState.generateAssertionsSummaries.length > 0;
        const atLeastOneIrvContest = Object.values(this.props.dosState.contests)
            .some(contest => contest.description === 'IRV');

        // Activate button if any attempt to generate is made
        if (this.state.generationWasRun) {
            return false;
        }

        return (!assertionsAlreadyGenerated && atLeastOneIrvContest);
    }

    private getAssertionGenerationStatusTable() {

        interface CombinedData {
            contestName: string;
            county: string;
            succeeded: boolean | undefined;
            retry: boolean | undefined;
            winner: string;
            error: string;
            warning: string;
            message: string;
        }

        interface CombinationSummary {
            combinedData: CombinedData;
        }

        const CombinedTableRow = (input: CombinationSummary) => {
            const {combinedData} = input;

            // Succeeded and retry can be undefined - if so this leaves the space blank.
            // {combinedData.succeeded != undefined ? {combinedData.succeeded ? 'Success' : 'Failure'} : ''}
            const successString = combinedData.succeeded ? 'Success' : 'Failure';
            const retryString = combinedData.retry ? 'Yes' : 'No';
            return (
                <tr>
                    <td>{combinedData.contestName}</td>
                    <td>{combinedData.county}</td>
                    <td style={{color: combinedData.succeeded ? 'green' : 'red'}}>
                        {combinedData.succeeded === undefined ? '' : successString}
                    </td>
                    <td>{combinedData.retry === undefined ? '' : retryString}</td>
                    <td>{combinedData.winner}</td>
                    <td>{combinedData.error}</td>
                    <td>{combinedData.warning}</td>
                    <td>{combinedData.message}</td>
                </tr>
            );
        };

        // Make a CombinedData structure out of a GenerateAssertionsSummary by filling in blank status.
        const fillBlankStatus = (s: DOS.GenerateAssertionsSummaryWithCounty): CombinedData => {
            return {
                contestName: s.summary.contestName,
                county: s.countyName,
                error: s.summary.error,
                message: s.summary.message,
                retry: undefined,
                succeeded: undefined,
                warning: s.summary.warning,
                winner: s.summary.winner,
            };
        };

        // Make a CombinedData structure out of an AssertionsStatus by filling in blank summary data.
        const fillBlankSummary = (s: DOS.AssertionStatus): CombinedData => {
            return {
                contestName: s.contestName,
                county: '',
                error: '',
                message: '',
                retry: s.retry,
                succeeded: s.succeeded,
                warning: '',
                winner: '',
            };
        };

        // Make a CombinedData structure out of an AssertionsStatus and a GenerateAssertionsSummary.
        const combineSummaryAndStatus = (s: DOS.AssertionStatus, t: DOS.GenerateAssertionsSummaryWithCounty): CombinedData => {
            return {
                contestName: s.contestName,
                county: t.countyName,
                error: t.summary.error,
                message: t.summary.message,
                retry: s.retry,
                succeeded: s.succeeded,
                warning: t.summary.warning,
                winner: t.summary.winner,
            };
        };

        // Join up the rows by contest name if matching. If there is no matching contest name in the other
        // list, add a row with blanks for the missing data.
        // Various kinds of absences are possible, because there may be empty summaries at the start;
        // conversely, in later phases we may rerun generation (and hence get status) for only a few contests.
        const joinRows = (statuses: DOS.AssertionGenerationStatuses
            | undefined,  summaries: DOS.GenerateAssertionsSummaryWithCounty[]) => {
            summaries.sort((a, b) => a.summary.contestName < b.summary.contestName ? -1 : 1);
            const rows: CombinedData[] = [];
            let i = 0;
            let j = 0;

            if (statuses === undefined) {
                // No status yet. Just print summary data.
                return summaries.map(s => {
                    return fillBlankStatus(s);
                });

            } else {
                // Iterate along the two sorted lists at once, combining them if the contest name matches, and
                // filling in blank data otherwise.
                statuses.sort((a, b) => a.contestName < b.contestName ? -1 : 1);
                while (i < statuses.length && j < summaries.length) {
                    if (statuses[i].contestName === summaries[j].summary.contestName) {
                        // Matching contest names. Join the rows and move indices along both lists.
                        rows.push(combineSummaryAndStatus(statuses[i++], summaries[j++]));
                    } else if (statuses[i].contestName < summaries[j].summary.contestName) {
                        // We have a status with no matching summary. Fill in the summary with blanks.
                        // increment status index only.
                        rows.push(fillBlankSummary(statuses[i++]));
                    } else if (statuses[i].contestName < summaries[j].summary.contestName) {
                        // We have a summary with no matching status. Fill in status 'undefined'.
                        // Increment summary index only.
                        rows.push(fillBlankStatus(summaries[j++]));
                    }
                }
                while (i < statuses.length) {
                    // We ran out of summaries. Fill in the rest of the statuses with summary blanks.
                    rows.push(fillBlankSummary(statuses[i++]));
                }
                while (j < summaries.length) {
                    // We ran out of statuses. Fill in the rest of the summaries with status blanks.
                    rows.push(fillBlankStatus(summaries[j++]));
                }
            }
            return rows;
        };

        const combinedRows = _.map(joinRows(this.props.dosState.assertionGenerationStatuses,
            this.props.dosState.generateAssertionsSummaries), d => (
            <CombinedTableRow key={d.contestName} combinedData={d}/>
        ));

        if (this.state.generatingAssertions) {
            return (
                <Card className='mt-default'>
                    <Spinner className='pt-medium' intent={Intent.PRIMARY}/>
                    <div>Generating Assertions...</div>
                </Card>
            );
        } else if (this.props.dosState.assertionGenerationStatuses
            || this.props.dosState.generateAssertionsSummaries.length > 0) {
            return (
                <div>
                <span className='generate-assertions-exports'>
                    <b>Export Assertions: </b>
                    <a href='#' onClick={exportAssertionsAsCsv}>CSV</a>
                    &nbsp;|&nbsp;
                    <a href='#' onClick={exportAssertionsAsJson}>JSON</a>
                </span>
                    <table className='pt-html-table pt-html-table-striped rla-table mt-default'>
                        <thead>
                        <tr>
                            <th>Contest</th>
                            <th>County</th>
                            <th>Assertion Generation Status</th>
                            <th>Advise Retry</th>
                            <th>Winner</th>
                            <th>Error</th>
                            <th>Warning</th>
                            <th>Message</th>
                        </tr>
                        </thead>
                        <tbody>{combinedRows}</tbody>
                    </table>
                </div>
            );
        } else {
            return <div/>;
        }
    }

}

export default GenerateAssertionsPage;

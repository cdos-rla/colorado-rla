import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import * as _ from 'lodash';

import { Breadcrumb, Button, Card, Intent, Spinner, ProgressBar } from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';
import counties from 'corla/data/counties';
import { formatLocalDate } from 'corla/date';

const MyProgressBar = () => (
    <ProgressBar intent={Intent.SUCCESS}/> 
);


const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS' />&gt;</li>
        <li><Breadcrumb href='/sos/audit' text='Audit Admin' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Review' /></li>
    </ul>
);

function formatReason(reason: AuditReason): string {
    if (reason === 'STATE_WIDE_CONTEST') {
        return 'State Contest';
    }

    return 'County Contest';
}

interface SelectedContestsProps {
    auditedContests: DOS.AuditedContests;
    contests: DOS.Contests;
}

const SelectedContests = (props: SelectedContestsProps) => {
    const { auditedContests, contests } = props;

    const rows = _.map(props.auditedContests, audited => {
        const contest = contests[audited.id];
        const countyName = counties[contest.countyId].name;

        return (
            <tr key={contest.id}>
                <td>{countyName}</td>
                <td>{contest.name}</td>
                <td>{formatReason(audited.reason)}</td>
            </tr>
        );
    });

    return (
        <Card>
            <h3>Selected Contests</h3>
            <Card>
                <table className='pt-html-table pt-html-table-bordered pt-small'>
                    <thead>
                        <tr>
                            <th>County</th>
                            <th>Name</th>
                            <th>Reason</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows}
                    </tbody>
                </table>
            </Card>
        </Card>
    );
};

interface AuditReviewProps {
    back: OnClick;
    publishBallotsToAudit: OnClick;
    saveAndDone: OnClick;
    dosState: DOS.AppState;
}

function riskLimitPercent(dosState: DOS.AppState) {
    return (dosState.riskLimit! * 100).toFixed(2);
}

interface ReviewPageState {
    loading: boolean;
}


class AuditReviewContainer extends React.Component<AuditReviewProps, ReviewPageState> {

    constructor(props: AuditReviewProps) {
        super(props);
        this.state = { loading: false };
    }

    public render() {
        const { back, publishBallotsToAudit, saveAndDone, dosState } = this.props;

        const launch = () => {
            this.setState({ loading: true });
            publishBallotsToAudit().then((response: Object) => {
                this.setState({ loading: false });
                saveAndDone();
            })
            .catch((error: Error) => {
                console.log("Error on publishBallots " + error.message);
                this.setState({ loading: false });
                saveAndDone();
            });
        };

        const disableLaunchButton = !dosState.seed;

        let SpinnerOrNot;
        if (!this.state.loading) {
            SpinnerOrNot = <div>
                <br />
                <SelectedContests auditedContests={dosState.auditedContests}
                    contests={dosState.contests} /><br />
                <Card>
                    <Button onClick={back}>Back</Button>
                    <Button className='ml-default'
                        disabled={disableLaunchButton || this.state.loading}
                        intent={Intent.PRIMARY}
                        onClick={launch}>
                        Launch Audit
                    </Button>
                </Card>
            </div>;
        } else {
            SpinnerOrNot = <Card>
                <h3>Launching Audit</h3>
                <div>
                    The audit is being launched. 
                    The application will <b>refresh</b> once the operation is complete. 
                    If there are too many audits, this might take a couple minutes.
                </div><br/>
                <MyProgressBar />
                
            </Card>;
        }

        const main =
            <div>
            <IdleDialog />
                <Breadcrumbs />
                <h1 className={'mediumHeader'}>Audit</h1>
                <h2 className={'smallHeader'}>Audit Definition Review</h2>
                <div>
                    This is the set of audit data which will be used to define the list of
                    ballots to audit for each county. Once this is submitted, it will be released
                    to the counties and the previous pages will not be editable.
                In particular, you will not be able to change which contests are under audit.<br/><br/>
                </div>
                <Card>
                    <table className='pt-html-table'>
                        <tbody>
                            <tr>
                                <td>Public Meeting Date:</td>
                                <td>{dosState.publicMeetingDate
                                    && formatLocalDate(dosState.publicMeetingDate)}</td>
                            </tr>
                            <tr>
                                <td>Election Date:</td>
                                <td>{dosState.election && dosState.election.date
                                    && formatLocalDate(dosState.election.date)}</td>
                            </tr>
                            <tr>
                                <td>Election Type:</td>
                                <td>{dosState.election && dosState.election.type}</td>
                            </tr>
                            <tr>
                                <td>Risk Limit:</td>
                                <td>{riskLimitPercent(dosState)}%</td>
                            </tr>
                            <tr>
                                <td>Random Number Generator Seed:</td>
                                <td>{dosState.seed}</td>
                            </tr>
                        </tbody>
                    </table>
                </Card><br/>
                {SpinnerOrNot}
            </div>;
        return <DOSLayout main={main} />;
    }
};


// export default AuditReview;
export default AuditReviewContainer;

import * as _ from 'lodash';

import * as React from 'react';

import { Button, Card, IButtonProps } from '@blueprintjs/core';

import action from 'corla/action/';
import CountyLayout from 'corla/component/CountyLayout';
import { auditBoardSlice } from 'corla/selector/county/currentBallotNumber';

import FinalReviewDialog from './FinalReviewDialog';

interface ReviewButtonProps {
    cvr: JSON.CVR;
    open: (cvr: JSON.CVR) => void;
}

const ReviewButton = (props: ReviewButtonProps) => {
    const handler: IButtonProps['onClick'] = () => {
        props.open(props.cvr);
    };

    return <Button text='Re-audit' onClick={ handler } />;
};

interface FinalReviewPageProps {
    auditBoardIndex: number;
    ballotSequenceAssignment?: object[];
    cvrsToAudit?: JSON.CVR[];
}

interface FinalReviewPageState {
    dialogIsOpen: boolean;
    cvr?: JSON.CVR;
}

const reviewCompleteHandler = (auditBoardIndex: number) => {
    return () => {
        action('FINAL_REVIEW_COMPLETE', auditBoardIndex);
    };
};

const rowRenderer = (open: (cvr: JSON.CVR) => void) => {
    return (cvr: JSON.CVR) => {
        return (
            <tr key={ cvr.db_id }>
                <td>{ cvr.storage_location }</td>
                <td>{ cvr.scanner_id }</td>
                <td>{ cvr.batch_id }</td>
                <td>{ cvr.record_id }</td>
                <td>{ cvr.ballot_type }</td>
                <td><ReviewButton cvr={ cvr }
                                  open={ open } /></td>
            </tr>
        );
    };
};

class FinalReviewPage extends React.Component<FinalReviewPageProps, FinalReviewPageState> {
    constructor(props: FinalReviewPageProps) {
        super(props);
        this.state = { dialogIsOpen: false, cvr: undefined };
    }

    public render() {
        const {
            auditBoardIndex,
            ballotSequenceAssignment,
            cvrsToAudit,
        } = this.props;

        const renderRow = rowRenderer(this.openDialog);

        const main =
            <Card>
                <FinalReviewDialog cvr={ this.state.cvr }
                                   isOpen={ this.state.dialogIsOpen }
                                   onClose={ this.closeDialog } />

                <h1 className={'smallHeader'}>Audit Board { auditBoardIndex + 1 }: Final Review</h1>

                <Card>
                    <p>
                        This screen allows you to re-audit ballots previously audited in
                        this round. If you choose to re-audit a ballot, you will be
                        presented with blank data entry and review screens for that ballot -
                        data from the previous audit will not be prefilled. Once you submit
                        a re-audited ballot, the most recent data will replace older
                        entries. You will be able to re-audit multiple ballots if you wish.
                    </p>

                    <p>
                        <b>
                            If you are satisfied with your initial data entry and
                            wish to complete the round:
                        </b>
                    </p>

                    <ul>
                        <li><b>Click the button below labeled "Review Complete - Finish Round"</b></li>
                    </ul>

                    <p>
                        <b>
                            If you wish to re-audit a ballot:
                        </b>
                    </p>

                    <ul>
                        <li>
                            <b>
                                Click the "re-audit" button next to the
                                appropriate ballot card in the list below
                            </b>
                        </li>
                    </ul>

                    <p>
                        When you are finished, click "Review Complete - Finish Round."
                        Once clicked, ballot data from this round of the audit is no longer editable.
                    </p>

                    <Button onClick={ reviewCompleteHandler(auditBoardIndex) }>
                        Review Complete - Finish Round
                    </Button>
                </Card>

                <table className='pt-html-table pt-html-table-bordered pt-small pt-interactive'
                       style={{ marginLeft: 'auto', marginRight: 'auto' }}>
                    <thead>
                        <tr>
                            <th>Storage bin</th>
                            <th>Scanner</th>
                            <th>Batch</th>
                            <th>Ballot position</th>
                            <th>Ballot type</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    { _.chain(auditBoardSlice(
                           cvrsToAudit,
                           ballotSequenceAssignment,
                           auditBoardIndex,
                       ))
                       .filter(cvr => !cvr.previously_audited)
                       .map(renderRow)
                       .value() }
                    </tbody>
                </table>
            </Card>;

        return <CountyLayout main={ main } />;
    }

    private closeDialog: () => void = () => {
        this.setState({ dialogIsOpen: false, cvr: undefined });
    }

    private openDialog: (cvr: JSON.CVR) => void = cvr => {
        this.setState({ dialogIsOpen: true, cvr });
    }
}

export default FinalReviewPage;

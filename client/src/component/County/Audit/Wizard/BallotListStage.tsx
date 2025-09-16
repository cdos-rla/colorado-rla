import * as React from 'react';

import * as _ from 'lodash';

import { Button, Card, Intent } from '@blueprintjs/core';

import downloadCvrsToAuditCsv from 'corla/action/county/downloadCvrsToAuditCsv';

interface BallotListStageProps {
    auditBoardIndex: number;
    countyInfo: CountyInfo;
    countyState: County.AppState;
    cvrsToAudit: JSON.CVR[];
    nextStage: OnClick;
}

const BallotListStage = (props: BallotListStageProps) => {
    const { auditBoardIndex,
            countyInfo,
            countyState,
            cvrsToAudit,
            nextStage } = props;

    const roundNumber = countyState.currentRound!.number;

    if (!cvrsToAudit) {
        return <div />;
    }

    const ballotListRows = _.map(cvrsToAudit, cvr => {
        const audited = cvr.audited ? '✔' : '';

        return (
            <tr key={ cvr.imprinted_id }>
                <td>{ cvr.storage_location }</td>
                <td>{ cvr.scanner_id }</td>
                <td>{ cvr.batch_id }</td>
                <td>{ cvr.record_id }</td>
                <td>{ cvr.ballot_type }</td>
                <td>{ audited }</td>
                <td>{ typeof cvr.audit_board_index === 'number'
                      ? `${cvr.audit_board_index + 1}`
                      : '' }</td>
            </tr>
        );
    });

    const contestsUnderAuditListItems = _.map(countyState.contestsUnderAudit, c => {
        const riskLimitPercent = countyState.riskLimit
                               ? `${countyState.riskLimit * 100}%`
                               : '';
        return (
            <li key={ c.id }>
                { c.name } – { riskLimitPercent }
            </li>
        );
    });

    const downloadCsv = () => downloadCvrsToAuditCsv(roundNumber);

    return (
        <div className='rla-page'>
            <h1 className={'mediumHeader'}>Audit Board { `${auditBoardIndex + 1 }` }: Ballot Cards to Audit</h1>
            <Card>
                    <div>
                        The Secretary of State has established the following risk limit(s) for
                        the following ballot contest(s) to audit:
                    </div>
                    <ul>
                        { contestsUnderAuditListItems }
                    </ul>
            </Card>
            <Card>
                The Secretary of State has randomly selected { cvrsToAudit.length } ballot cards
                for the { countyInfo.name } County Audit Board(s) to examine in Round
                <span>{ ' ' + roundNumber }</span> to satisfy the risk limit(s) for the audited contest(s).
            </Card>
            <Card>
                The Audit Board(s) must locate and retrieve, or observe a county staff member
                locate and retrieve, the following randomly selected ballot cards for the initial
                round of this risk-limiting audit:
            </Card>
            <Card>
                Audit Board { `${auditBoardIndex + 1 }` }: Click Start audit to
                begin reporting the votes you observe on each of the below
                ballot cards that have been assigned to you.
            </Card>
            <Button intent={ Intent.PRIMARY }
                    onClick={ nextStage }>
                Start audit
            </Button>
            <Card>
                <Button intent={ Intent.PRIMARY } onClick={ downloadCsv } >
                   Download ballot list as CSV
                </Button>
                <Card>
                    <table className='pt-html-table pt-html-table-bordered pt-small'>
                        <thead>
                            <tr>
                                <th>Storage bin</th>
                                <th>Scanner</th>
                                <th>Batch</th>
                                <th>Ballot position</th>
                                <th>Ballot type</th>
                                <th>Audited</th>
                                <th>Audit board</th>
                            </tr>
                        </thead>
                        <tbody>
                            { ballotListRows }
                        </tbody>
                    </table>
                </Card>
            </Card>
        </div>
    );
};

export default BallotListStage;

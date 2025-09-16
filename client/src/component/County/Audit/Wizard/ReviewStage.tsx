import * as React from 'react';

import * as _ from 'lodash';

import { Button, Icon, Intent } from '@blueprintjs/core';

import IrvChoiceForm from 'corla/component/County/Audit/Wizard/IrvChoiceForm';
import SubmittingACVR from './SubmittingACVR';

interface EditButtonProps {
    back: OnClick;
}

const EditButton = ({ back }: EditButtonProps) => {
    return (
        <a onClick={ back }>
            <Icon icon='edit' className='review-edit-selection' />
            Edit
        </a>
    );
};

interface InstructionsProps {
    countyState: County.AppState;
    currentBallot: CVR;
}

const AuditInstructions = (props: InstructionsProps) => {
    const {
        countyState,
        currentBallot,
    } = props;

    const { currentRound } = countyState;
    const isCurrentCvr = (cvr: JSON.CVR) => cvr.db_id === currentBallot.id;
    const fullCvr = _.find(countyState.cvrsToAudit, isCurrentCvr);
    const storageBin = fullCvr ? fullCvr.storage_location : '—';

    return (
        <div>
            <div className='current-ballot-info'>
                <h2 className='sidebar-heading'>Current ballot:</h2>
                <ul className='current-ballot-stats pt-list-unstyled'>
                    <li>
                        <span className='current-ballot-stats-label'>Storage bin</span>
                        <span className='current-ballot-stats-value'>{ storageBin }</span>
                    </li>
                    <li>
                        <span className='current-ballot-stats-label'>Tabulator</span>
                        <span className='current-ballot-stats-value'>{ currentBallot.scannerId }</span>
                    </li>
                    <li>
                        <span className='current-ballot-stats-label'>Batch</span>
                        <span className='current-ballot-stats-value'>{ currentBallot.batchId }</span>
                    </li>
                    <li>
                        <span className='current-ballot-stats-label'>Ballot position</span>
                        <span className='current-ballot-stats-value'>{ currentBallot.recordId }</span>
                    </li>
                    <li>
                        <span className='current-ballot-stats-label'>Ballot type</span>
                        <span className='current-ballot-stats-value'>{ currentBallot.ballotType }</span>
                    </li>
                </ul>
            </div>
        </div>
    );
};

interface BallotContestReviewProps {
    back: OnClick;
    contest: Contest;
    marks: County.ACVRContest;
}

const BallotContestReview = (props: BallotContestReviewProps) => {
    const { back, contest, marks } = props;
    const { comments, noConsensus } = marks;
    const { votesAllowed, description } = contest;

    const markedChoices: County.ACVRChoices = _.pickBy(marks.choices);
    const votesMarked = _.size(markedChoices);

    const noConsensusDiv = (
        <div>
            <Icon icon='disable' className='no-consensus' />
            No audit board consensus
        </div>
    );

    const noMarksDiv = (
        <div>
            <Icon icon='cross' className='blank-vote' />
            Blank vote - no mark
        </div>
    );

    const commentsDiv = (
      <div className='comments-container'>
        <div className='comments-head'>COMMENT:</div>
        <div className='comments-body'>{ comments }</div>
      </div>
    );

    const markedChoiceDivs = _.map(markedChoices, (value, name) => {
        return (
            <div className='rla-contest-choice' key={ name }>
                <span className='choice-name'>{ name }</span>
            </div>
        );
    });

    const renderMarkedChoices = () => {
        if (votesMarked > votesAllowed) {
            return (
                <div>
                    <div className='contest-choice-selection contest-choice-review'>
                        { markedChoiceDivs.length ? markedChoiceDivs : noMarksDiv }
                    </div>
                    <strong>Overvote</strong> for this contest.
                </div>
            );
        }

        if (description === 'IRV') {
            // convert props to ChoicesProps for IRV Display
            const irvChoicesProps: ChoicesProps = {
                choices: contest.choices,
                description,
                marks,
                noConsensus,
                // null OnClick for review stage since clicking should not change markings
                updateBallotMarks: () => null,
            };

            return (
                <div className='contest-choice-selection'>
                    { IrvChoiceForm(irvChoicesProps) }
                </div>
            );
        }

        return (
            <div className='contest-choice-selection contest-choice-review'>
                { markedChoiceDivs.length ? markedChoiceDivs : noMarksDiv }
            </div>
        );
    };

    return (
      <div className='contest-row'>
        <div className='contest-info'>
          <strong><div className='contest-name'>{ contest.name }</div></strong>
          <strong><div>{ contest.description }</div></strong>
        </div>
        <div className={'contest-choice-review-grid' + (description === 'IRV' ? '-irv' : '')}>
          { noConsensus ? noConsensusDiv : renderMarkedChoices() }
          <div className='edit-button'>
            <EditButton back={ back } />
          </div>
        </div>
        { comments ? commentsDiv : null }
      </div>
    );
};

interface BallotReviewProps {
    back: OnClick;
    countyState: County.AppState;
    marks: County.ACVR;
}

const BallotReview = (props: BallotReviewProps) => {
    const { back, countyState, marks } = props;

    const contestReviews = _.map(marks, (m, contestId) => {
        const contest = countyState.contestDefs![contestId];

        return (
          <BallotContestReview
              back={ back }
              contest={ contest }
              key={ contestId }
              marks={ m }
          />
        );
    });

    return <div>{ contestReviews }</div>;
};

interface ReviewStageProps {
    auditBoardIndex: number;
    comment?: string;
    countyState: County.AppState;
    currentBallot: County.CurrentBallot;
    currentBallotNumber?: number;
    isReAuditing?: boolean;
    marks: County.ACVR;
    nextStage: OnClick;
    prevStage: OnClick;
    uploadAcvr: OnClick;
    totalBallotsForBoard?: number;
}

const ReviewStage = (props: ReviewStageProps) => {
    const {
        auditBoardIndex,
        comment,
        countyState,
        currentBallot,
        currentBallotNumber,
        isReAuditing,
        marks,
        nextStage,
        prevStage,
        uploadAcvr,
        totalBallotsForBoard,
    } = props;

    const onClick = () => {
        if (isReAuditing) {
            uploadAcvr(marks, currentBallot, auditBoardIndex, true, comment);
        } else {
            uploadAcvr(marks, currentBallot, auditBoardIndex);
        }

        nextStage();
    };

    if (currentBallot.submitted) {
        return <SubmittingACVR />;
    }

    return (
        <div className='rla-page'>
            <div className='audit-page-container'>
                <div className='audit-page-header'>
                    <h1 className='audit-page-title'>Ballot Card Verification</h1>
                    <div className='audit-page-subtitle'>Review ballot</div>
                    <div className='ballot-number'>
                        Auditing ballot card { currentBallotNumber } of { totalBallotsForBoard }
                    </div>
                </div>

                <div className='col-layout'>
                    <div className='col1'>
                        <AuditInstructions countyState={ countyState } currentBallot={ currentBallot } />
                    </div>
                    <div className='col2'>
                        <p>
                            Confirm that the information displayed accurately
                            reflects its interpretation for each contest and
                            choice from the corresponding paper ballot.
                        </p>
                        <p>
                            If there are any discrepancies, click the
                            <b> Edit</b> button located to the right of the
                            audited contest’s selections, and reenter the voter
                            markings for the ballot.
                        </p>
                        <p>
                            If the review page accurately reflects the audit
                            board’s interpretation of all votes in all contests,
                            click the <b>Submit</b> button at the bottom of the
                            page.
                        </p>
                        <BallotReview countyState={ countyState } marks={ marks } back={ prevStage } />
                        <div className='button-container button-container-left'>
                          <Button large className={'blackButtonText'} intent={ Intent.SUCCESS } onClick={ onClick }>
                              Submit & Next Ballot Card
                          </Button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ReviewStage;

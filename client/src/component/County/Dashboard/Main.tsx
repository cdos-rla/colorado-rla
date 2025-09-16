import * as _ from 'lodash';

import * as React from 'react';

import { History } from 'history';

import { Button, Callout, Intent } from '@blueprintjs/core';

import action from 'corla/action';

import AuditBoardNumberSelector from 'corla/component/County/Dashboard/AuditBoardNumberSelector';

import FileUploadContainer from './FileUploadContainer';

import downloadCvrsToAuditCsv from 'corla/action/county/downloadCvrsToAuditCsv';

import fetchReport from 'corla/action/county/fetchReport';

import FileDownloadButtons from 'corla/component/FileDownloadButtons';

interface AuditBoardButtonsProps {
    auditBoardCount: number;
    auditBoards: object;
    history: History;
    isShown: boolean;
}

const AuditBoardButtons = (props: AuditBoardButtonsProps) => {
    const { auditBoardCount, auditBoards, history, isShown } = props;

    if (!isShown) {
        return null;
    }

    const handleButtonClick = (e: any, boardIndex: number, hasBoard: boolean) => {
        e.preventDefault();

        let canRedirect = true;

        if (hasBoard) {
          const message = `Audit board ${boardIndex + 1} is already signed in.` +
                          ' Are you sure you want to proceed?';
          canRedirect = confirm(message);
        }

        if (canRedirect) {
            action('SET_AUDIT_BOARD', { auditBoardIndex: boardIndex });
            history.push('/county/board/' + boardIndex);
        }
    };

    const boardButton = (boardIndex: number, hasBoard: boolean) => {
        const buttonIntent = hasBoard ? Intent.WARNING : Intent.PRIMARY;

        return (
            <Button
                    className={'blackButtonText'}
                    icon='people'
                    intent={ buttonIntent }
                    key={ boardIndex.toString() }
                    onClick={ (e: any) => handleButtonClick(e, boardIndex, hasBoard) }>
                Audit Board { boardIndex + 1 }
            </Button>
        );
    };

    const buttons = [];
    for (let i = 0; i < auditBoardCount; i++) {
        buttons.push(boardButton(i, _.has(auditBoards, i)));
    }

    return (
        <div className='mt-default'>
            <h2 className='pt-ui-text-large font-weight-bold tinyHeader'>Sign in to an audit board</h2>
            <div className='pt-button-group pt-large rla-spaced'>{ buttons }</div>
        </div>
    );
};

interface MainProps {
    auditBoardButtonDisabled: boolean;
    auditComplete: boolean;
    auditStarted: boolean;
    canRenderReport: boolean;
    countyState: County.AppState;
    currentRoundNumber: number;
    history: History;
    name: string;
}

const Main = (props: MainProps) => {
    const {
        auditBoardButtonDisabled,
        auditComplete,
        auditStarted,
        canRenderReport,
        countyState,
        currentRoundNumber,
        history,
        name,
    } = props;

    let directions = 'Upload the ballot manifest and cast vote record (CVR) files. These need to be CSV files.'
                   + '\n\nAfter uploading the files wait for an email from the Department of State saying that you can'
                   + ' continue the audit.';

    if (!auditBoardButtonDisabled && !currentRoundNumber && !auditComplete) {
        directions = 'Please wait for the Department of State to proceed with the audit.';
    } else if (currentRoundNumber && !auditComplete) {
        directions = `You may now perform round ${currentRoundNumber} of the audit.`;
    } else if (auditComplete) {
        directions = 'You have successfully completed the Risk-Limiting Audit! Print all pages of your final audit'
            + ' report. Have the judges and county clerk sign the last page of the report and email it to'
            + ' RLA@coloradosos.gov. You can now proceed to canvass!';
    }

    const fileUploadContainer = auditStarted
                              ? <div />
                              : <FileUploadContainer />;

    const fileDownloadButtons = auditStarted
                              ? <FileDownloadButtons status={ countyState } allowDelete={ false } />
                              : <div />;

    const reportType = auditComplete
                     ? 'Final'
                     : 'Intermediate';

    const downloadCsv = () => downloadCvrsToAuditCsv(currentRoundNumber);

    return (
        <div>
            <h1 className='mt-default mb-default'>Hello, { name } County!</h1>
            <div className='mb-default'>
                <Callout icon='info-sign'>
                    <span className='font-weight-bold pt-ui-text-large'>{ directions }</span>
                </Callout>
            </div>
            <hr />
            { fileUploadContainer }
            { fileDownloadButtons }
            <div>
                <h2 className='form-container-heading'>{ reportType } audit report (CSV)</h2>
                <button
                    className='pt-button  pt-intent-primary'
                    disabled={ !canRenderReport }
                    onClick={ fetchReport }>
                    Download
                </button>
                <hr />
            </div>
            <div className='mt-default'>
                <h2 className='form-container-heading'>List of ballots to audit (CSV)</h2>
                <button
                    className='pt-button pt-intent-primary'
                    disabled={ typeof countyState.auditBoardCount !== 'number' }
                    onClick={ downloadCsv }>
                    Download
                </button>
            </div>
            <AuditBoardNumberSelector auditBoardCount={ countyState.auditBoardCount || 1 }
                                      numberOfBallotsToAudit={ countyState.ballotsRemainingInRound }
                                      isShown={ !auditBoardButtonDisabled && !!currentRoundNumber }
                                      isEnabled={ !countyState.auditBoardCount } />
            <AuditBoardButtons auditBoardCount={ countyState.auditBoardCount || 1 }
                               auditBoards={ countyState.auditBoards }
                               history={ history }
                               isShown={ typeof countyState.auditBoardCount === 'number'
                                         && !!currentRoundNumber } />
        </div>
    );
};

export default Main;

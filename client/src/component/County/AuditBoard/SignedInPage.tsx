import * as React from 'react';

import { Button, Card, Intent } from '@blueprintjs/core';

import CountyLayout from 'corla/component/CountyLayout';

import auditBoardSignOut from 'corla/action/county/auditBoardSignOut';

interface PageProps {
    auditBoardStatus: AuditBoardStatus;
    auditBoardIndex: number;
    auditBoardStartOrContinue: () => void;
    countyName: string;
    hasAuditedAnyBallot: boolean;
}

const SignedInPage = (props: PageProps) => {
    const {
        auditBoardStatus,
        auditBoardIndex,
        auditBoardStartOrContinue,
        countyName,
        hasAuditedAnyBallot,
    } = props;

    const members = auditBoardStatus.members;

    const startOrContinueText = hasAuditedAnyBallot ? 'Continue Audit' : 'Start Audit';

    const main =
        <div>
            <div>
                <h1 className={'mediumHeader'}>Audit Board { auditBoardIndex + 1 }</h1>
                <Card>
                    <b>The Audit Board members below are signed in.
                        To sign the Audit Board out, click the "Sign Out" button below.</b>
                </Card>
            </div>
            <Card>
                <h2 className={'tinyHeader'}>Board Member 1:</h2>
                <div>
                    Name: { members[0].firstName } { members[0].lastName }
                </div>
                <div>
                    Political party: { members[0].party }
                </div>
            </Card>
            <Card>
                <h2 className={'tinyHeader'}>Board Member 2:</h2>
                <div>
                    Name: { members[1].firstName } { members[1].lastName }
                </div>
                <div>
                    Political party: { members[1].party }
                </div>
            </Card>
            <div>
                <Button disabled
                        intent={ Intent.PRIMARY }>
                    Submit
                </Button>
                <Button className='ml-default'
                        intent={ Intent.PRIMARY }
                        onClick={ () => auditBoardSignOut(auditBoardIndex) }>
                    Sign Out
                </Button>
                <Button className='ml-default'
                        intent={ Intent.PRIMARY }
                        onClick={ auditBoardStartOrContinue }>
                    { startOrContinueText }
                </Button>
            </div>
        </div>;

    return <CountyLayout main={ main } />;
};

export default SignedInPage;

import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import { History } from 'history';

import CountyLayout from 'corla/component/CountyLayout';

import Main from './Main';

interface PageProps {
    auditComplete: boolean;
    auditStarted: boolean;
    canAudit: boolean;
    canRenderReport: boolean;
    canSignIn: boolean;
    countyInfo: CountyInfo;
    countyState: County.AppState;
    currentRoundNumber: number;
    history: History;
}

const CountyDashboardPage = (props: PageProps) => {
    const {
        auditComplete,
        auditStarted,
        canAudit,
        canRenderReport,
        canSignIn,
        countyInfo,
        countyState,
        currentRoundNumber,
        history,
    } = props;

    const auditBoardButtonDisabled = !canSignIn;

    const main =
    <div>
    <IdleDialog />
    <Main auditComplete={ auditComplete }
              auditStarted={ auditStarted }
              canRenderReport={ canRenderReport }
              countyState={ countyState }
              currentRoundNumber={ currentRoundNumber }
              history={ history }
              name={ countyInfo.name }
              auditBoardButtonDisabled={ auditBoardButtonDisabled } />
    </div>;
    return <CountyLayout main={ main } />;
};

export default CountyDashboardPage;

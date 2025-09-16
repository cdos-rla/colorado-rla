import * as React from 'react';
import IdleDialog from '../../IdleDialogState';

import {Alignment, Tab, Tabs} from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';

import ContestUpdates from './ContestUpdates';
import CountyUpdates from './CountyUpdates';
import MainContainer from './MainContainer';

interface PageProps {
    auditStarted: boolean;
    contests: DOS.Contests;
    countyStatus: DOS.CountyStatuses;
    dosState: DOS.AppState;
    seed: string;
}

const DOSDashboardPage = (props: PageProps) => {
    const { auditStarted, contests, countyStatus, dosState, seed } = props;

    const main =
        <div>
            <IdleDialog />
            <MainContainer />
            <Tabs className='mt-default'
                  id='updates'
                  large>
                <Tab id='county-updates'
                     title='County Updates'
                     panel={ <CountyUpdates auditStarted={ auditStarted }
                                            countyStatus={ countyStatus } /> } />
                <Tab id='contest-updates'
                     title='Contest Updates'
                     panel={ <ContestUpdates contests={ contests }
                                             seed={ seed }
                                             dosState={ dosState } /> } />
            </Tabs>
        </div>;

    return <DOSLayout main={ main } />;
};

export default DOSDashboardPage;

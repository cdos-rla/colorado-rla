import * as React from 'react';
import { Provider, Store } from 'react-redux';
import {
    BrowserRouter as Router,
    Route,
    Switch,
} from 'react-router-dom';

import LoginRoute from './LoginRoute';
import RootRedirectContainer from './RootRedirectContainer';

import AuditBoardPageContainer from './County/AuditBoard/PageContainer';

import CountyAuditPageContainer from './County/Audit/PageContainer';
import CountyDashboardPageContainer from './County/Dashboard/PageContainer';

import LoginContainer from './Login/Container';


import DOSDefineAuditEstimateSampleSizesPageContainer from './DOS/DefineAudit/EstimateSampleSizesPageContainer';
import DOSDefineAuditGenerateAssertionsPageContainer from './DOS/DefineAudit/GenerateAssertionsPageContainer';
import DOSDefineAuditReviewPageContainer from './DOS/DefineAudit/ReviewPageContainer';
import DOSDefineAuditSeedPageContainer from './DOS/DefineAudit/SeedPageContainer';
import DOSDefineAuditSelectContestsPageContainer from './DOS/DefineAudit/SelectContestsPageContainer';
import DOSDefineAuditStandardizeChoicesPageContainer from './DOS/DefineAudit/StandardizeChoicesPageContainer';
import DOSDefineAuditStandardizeContestsPageContainer from './DOS/DefineAudit/StandardizeContestsPageContainer';
import DOSDefineAuditStartPageContainer from './DOS/DefineAudit/StartPageContainer';

import DOSContestDetailPageContainer from './DOS/Contest/DetailPageContainer';
import DOSContestOverviewPageContainer from './DOS/Contest/OverviewPageContainer';
import DOSCountyDetailPageContainer from './DOS/County/DetailPageContainer';
import DOSCountyOverviewPageContainer from './DOS/County/OverviewPageContainer';

import DOSDashboardContainer from './DOS/Dashboard/PageContainer';

export interface RootContainerProps {
    store: Store<AppState>;
}

export class RootContainer extends React.Component<RootContainerProps> {
    public render() {
        const { store } = this.props;

        return (
            <Provider store={ store }>
                <Router>
                    <Switch>
                        <Route exact
                               path='/login'
                               component={ LoginContainer } />
                        <LoginRoute exact
                                    path='/'
                                    page={ RootRedirectContainer } />
                        <LoginRoute exact
                                    path='/county'
                                    page={ CountyDashboardPageContainer } />
                        <LoginRoute exact
                                    path='/county/board/:id'
                                    page={ AuditBoardPageContainer } />
                        <LoginRoute exact
                                    path='/county/audit/:id'
                                    page={ CountyAuditPageContainer } />
                        <LoginRoute exact
                                    path='/sos'
                                    page={ DOSDashboardContainer } />
                        <LoginRoute exact
                                    path='/sos/audit'
                                    page={ DOSDefineAuditStartPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/seed'
                                    page={ DOSDefineAuditSeedPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/standardize-contests'
                                    page={ DOSDefineAuditStandardizeContestsPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/standardize-choices'
                                    page={ DOSDefineAuditStandardizeChoicesPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/generate-assertions'
                                    page={ DOSDefineAuditGenerateAssertionsPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/estimate-sample-sizes'
                                    page={ DOSDefineAuditEstimateSampleSizesPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/select-contests'
                                    page={ DOSDefineAuditSelectContestsPageContainer } />
                        <LoginRoute exact
                                    path='/sos/audit/review'
                                    page={ DOSDefineAuditReviewPageContainer } />
                        <LoginRoute exact
                                    path='/sos/contest'
                                    page={ DOSContestOverviewPageContainer } />
                        <LoginRoute exact
                                    path='/sos/contest/:contestId'
                                    page={ DOSContestDetailPageContainer } />
                        <LoginRoute exact
                                    path='/sos/county'
                                    page={ DOSCountyOverviewPageContainer } />
                        <LoginRoute exact
                                    path='/sos/county/:countyId'
                                    page={ DOSCountyDetailPageContainer } />
                    </Switch>
                </Router>
            </Provider>
        );
    }
}

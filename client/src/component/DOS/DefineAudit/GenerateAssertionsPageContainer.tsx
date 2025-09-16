import * as React from 'react';

import { Redirect } from 'react-router-dom';

import { History } from 'history';

import GenerateAssertionsPage from './GenerateAssertionsPage';

import withDOSState from 'corla/component/withDOSState';
import withSync from 'corla/component/withSync';
import * as _ from 'lodash';

interface ContainerProps {
    dosState: DOS.AppState;
    history: History;
    readyToGenerate: boolean;
}

class GenerateAssertionsPageContainer extends React.Component<ContainerProps> {

    public render() {
        const {
            dosState,
            history,
            readyToGenerate,
        } = this.props;

        if (!dosState) {
            return <div />;
        }

        if (!dosState.asm) {
            return <div />;
        }

        if (dosState.asm === 'DOS_AUDIT_ONGOING') {
            return <Redirect to='/sos' />;
        }

        const props = {
            dosState,
            forward: () => history.push('/sos/audit/estimate-sample-sizes'),
            readyToGenerate,
        };

        return <GenerateAssertionsPage { ...props } />;
    }
}

const mapStateToProps = (dosState: DOS.AppState) => {
    if (!dosState) { return {}; }

    const canonicalContests = dosState.canonicalContests;
    const canonicalChoices = dosState.canonicalChoices;

    const canonicalizationComplete = !_.isEmpty(canonicalChoices)
        && !dosState.settingAuditInfo && !_.isEmpty(canonicalContests)
        && !dosState.standardizingContests;

    const readyToGenerate = canonicalizationComplete && !dosState.generatingAssertions
        && !dosState.assertionsGenerated;

    return {
        dosState,
        readyToGenerate,
    };
};

export default withSync(
    withDOSState(GenerateAssertionsPageContainer),
    'DOS_GENERATE_ASSERTIONS_SYNC',
    mapStateToProps,
);

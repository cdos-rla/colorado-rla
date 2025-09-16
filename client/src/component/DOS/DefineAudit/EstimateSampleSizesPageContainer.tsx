import * as React from 'react';

import { Redirect } from 'react-router-dom';

import { History } from 'history';

import EstimateSampleSizesPage from './EstimateSampleSizesPage';

import withDOSState from 'corla/component/withDOSState';
import withSync from 'corla/component/withSync';

interface ContainerProps {
    dosState: DOS.AppState;
    history: History;
}

class EstimateSampleSizesPageContainer extends React.Component<ContainerProps> {

    public render() {
        const {
            dosState,
            history,
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
            forward: () => history.push('/sos/audit/select-contests'),
        };

        return <EstimateSampleSizesPage { ...props } />;
    }
}

const mapStateToProps = (dosState: DOS.AppState) => {
    if (!dosState) { return {}; }

    return {
        dosState,
    };
};

export default withSync(
    withDOSState(EstimateSampleSizesPageContainer),
    'DOS_ESTIMATE_SAMPLE_SIZES_SYNC',
    mapStateToProps,
);
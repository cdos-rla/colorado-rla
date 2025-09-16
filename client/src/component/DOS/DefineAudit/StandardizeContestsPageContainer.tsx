import * as React from 'react';

import * as _ from 'lodash';

import { Redirect } from 'react-router-dom';

import { History } from 'history';

import standardizeContests from 'corla/action/dos/standardizeContests';

import StandardizeContestsPage from './StandardizeContestsPage';

import withDOSState from 'corla/component/withDOSState';
import withPoll from 'corla/component/withPoll';
import resetAudit from 'corla/action/dos/resetAudit';

import counties from 'corla/data/counties';

// The next URL path to transition to.
const NEXT_PATH = '/sos/audit/standardize-choices';

// The previous URL path to transition to.
const PREV_PATH = '/sos/audit';

const contestsToDisplay = (
    contestsIgnoringManifests: DOS.Contests,
    canonicalContests: DOS.CanonicalContests,
): DOS.Contests => {
    return _.reduce(contestsIgnoringManifests, (acc: DOS.Contests, contest: Contest) => {
        const countyName = counties[contest.countyId].name;
        const countyStandards = canonicalContests[countyName] || [];

        if (!_.isEmpty(countyStandards)
            && !_.includes(countyStandards, contest.name)) {
            acc[contest.id] = contest;
        }

        return acc;
    }, {});
};

interface Props {
    areCVRsLoaded: boolean;
    asm: DOS.ASMState;
    contestsIgnoringManifests: DOS.Contests;
    canonicalContests: DOS.CanonicalContests;
    history: History;
}

const StandardizeContestsPageContainer = (props: Props) => {
    const {
        areCVRsLoaded,
        asm,
        canonicalContests,
        contestsIgnoringManifests,
        history,
    } = props;

    const nextPage = (data: DOS.Form.StandardizeContests.FormData) => {
        standardizeContests(contestsIgnoringManifests, data).then(function(r) {
            // use the result here
            if (r.ok) {
                history.push(NEXT_PATH);
            }
        })
        .catch(function(reason){
            console.log("standardizeContests error in submitAction " + reason);
        });
    };

    const previousPage = async() => {
        await resetAudit();
        history.push('/sos/audit');
    };

    if (asm === 'DOS_AUDIT_ONGOING') {
        return <Redirect to='/sos' />;
    }

    let filteredContests = {};

    if (areCVRsLoaded) {
        filteredContests = contestsToDisplay(contestsIgnoringManifests, canonicalContests);

        if (_.isEmpty(filteredContests)) {
            return <Redirect to={ NEXT_PATH } />;
        }
    }

    return <StandardizeContestsPage areCVRsLoaded={ areCVRsLoaded }
                                    back={ previousPage }
                                    canonicalContests={ canonicalContests }
                                    contestsIgnoringManifests={ filteredContests }
                                    forward={ nextPage } />;
};

const mapStateToProps = (state: DOS.AppState) => {
    const canonicalContests = state.canonicalContests;
    const contestsIgnoringManifests = state.contestsIgnoringManifests;
    const contests = state.contests; // VT: Now apparently unused.
    const areCVRsLoaded = !_.isEmpty(contestsIgnoringManifests)
        && !_.isEmpty(canonicalContests)
        && !state.settingAuditInfo;

    return {
        areCVRsLoaded,
        asm: state.asm,
        canonicalContests,
        contestsIgnoringManifests,
    };
};

export default withPoll(
    withDOSState(StandardizeContestsPageContainer),
    'DOS_SELECT_CONTESTS_POLL_START',
    'DOS_SELECT_CONTESTS_POLL_STOP',
    mapStateToProps,
);

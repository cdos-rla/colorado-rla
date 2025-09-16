import * as React from 'react';

import * as _ from 'lodash';

import { Redirect } from 'react-router-dom';

import { History } from 'history';

import standardizeChoices from 'corla/action/dos/standardizeChoices';

import resetAudit from 'corla/action/dos/resetAudit';
import StandardizeChoicesPage from './StandardizeChoicesPage';

import withDOSState from 'corla/component/withDOSState';
import withPoll from 'corla/component/withPoll';

import counties from 'corla/data/counties';

/**
 * Denormalize the DOS.Contests data structure from the application state into
 * something that can be easily displayed in a tabular format.
 */
const flattenContests = (
    contestsIgnoringManifests: DOS.Contests,
    canonicalChoices: DOS.CanonicalChoices,
): DOS.Form.StandardizeChoices.Row[] => {
    return _.flatMap(contestsIgnoringManifests, (contest: Contest) => {
        return _.map(contest.choices, (choice: ContestChoice, idx) => {
            return {
                choiceIndex: idx,
                choiceName: choice.name,
                choices: canonicalChoices[contest.name],
                contestId: contest.id,
                contestName: contest.name,
                countyName: counties[contest.countyId].name,
            };
        });
    });
};

/**
 * Remove rows with no canonical choices.
 */
const filterRows = (
    rows: DOS.Form.StandardizeChoices.Row[],
): DOS.Form.StandardizeChoices.Row[] => {
    return _.filter(rows, row => {
        if (_.isEmpty(row.choices)) {
            return false;
        }

        return !_.includes(row.choices, row.choiceName);
    });
};

interface Props {
    areChoicesLoaded: boolean;
    asm: DOS.ASMState;
    contestsIgnoringManifests: DOS.Contests;
    canonicalChoices: DOS.CanonicalChoices;
    history: History;
}

const PageContainer = (props: Props) => {
    const {
        areChoicesLoaded,
        asm,
        canonicalChoices,
        contestsIgnoringManifests,
        history,
    } = props;

    let isRequestInProgress = false;
    const nextPage = (data: DOS.Form.StandardizeChoices.FormData) => {
        if (isRequestInProgress) {
            return;
        }
        isRequestInProgress = true;

        standardizeChoices(contestsIgnoringManifests, data).then(r => {
            isRequestInProgress = false;
            // use the result here
            if (r.ok) {
                history.push(getNextPath());
            }
       })
        .catch(reason => {
            alert('standardizeChoices error in submitAction ' + reason);
        });
    };

    const previousPage = async () => {
        await resetAudit();
        history.push('/sos/audit');
    };

    function getNextPath() {
        // Only route to Assertion Generation page if IRV contests exist
        if (Object.values(contestsIgnoringManifests).some(contest => contest.description === 'IRV')) {
            return '/sos/audit/generate-assertions';
        } else {
            return '/sos/audit/estimate-sample-sizes';
        }
    }

    if (asm === 'DOS_AUDIT_ONGOING') {
        return <Redirect to='/sos' />;
    }

    let rows: DOS.Form.StandardizeChoices.Row[] = [];

    if (areChoicesLoaded) {
        rows = filterRows(flattenContests(contestsIgnoringManifests, canonicalChoices));

        if (_.isEmpty(rows)) {
            return <Redirect to={ getNextPath() } />;
        }
    }

    return <StandardizeChoicesPage areChoicesLoaded={ areChoicesLoaded }
                                   back={ previousPage }
                                   contestsIgnoringManifests={ contestsIgnoringManifests }
                                   rows={ rows }
                                   forward={ nextPage } />;
};

const mapStateToProps = (state: DOS.AppState) => {
    const contestsIgnoringManifests = state.contestsIgnoringManifests;
    const canonicalChoices = state.canonicalChoices;
    const areChoicesLoaded = !_.isEmpty(contestsIgnoringManifests)
        && !_.isEmpty(canonicalChoices)
        && !state.standardizingContests;

    return {
        areChoicesLoaded,
        asm: state.asm,
        canonicalChoices,
        contestsIgnoringManifests,
    };
};

export default withPoll(
    withDOSState(PageContainer),
    'DOS_SELECT_CONTESTS_POLL_START',
    'DOS_SELECT_CONTESTS_POLL_STOP',
    mapStateToProps,
);

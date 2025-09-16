export function countyState(): County.AppState {
    return {
        acvrs: {},
        asm: {
            auditBoard: 'AUDIT_INITIAL_STATE',
            county: 'COUNTY_INITIAL_STATE',
        },
        auditBoards: {},
        contests: [],
        cvrImportPending: {
            alerted: false,
            started: new Date(),
        },
        cvrImportStatus: {
            state: 'NOT_ATTEMPTED',
            timestamp: new Date(),
        },
        finalReview: {
            complete: false,
        },
        rounds: [],
        type: 'County',
    };
}

export function dosState(): DOS.AppState {
    return {
        asm: 'DOS_INITIAL_STATE',
        auditTypes: {},
        auditedContests: {},
        contests: {},
        contestsIgnoringManifests: {},
        countyStatus: {},
        type: 'DOS',
        generateAssertionsSummaries: [],
        raireServiceStatus: '',
    };
}

export function loginState(): LoginAppState {
    return { type: 'Login' };
}

export default {
    county: countyState,
    dos: dosState,
    login: loginState,
};

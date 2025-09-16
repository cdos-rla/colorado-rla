import { all, takeLatest } from 'redux-saga/effects';

import * as config from 'corla/config';

import createPollSaga from 'corla/saga/createPollSaga';

import dashboardRefresh from 'corla/action/dos/dashboardRefresh';
import fetchContests from 'corla/action/dos/fetchContests';
import fetchContestsIgnoreManifests from 'corla/action/dos/fetchContestsIgnoreManifests';

function* contestOverviewSaga() {
    yield takeLatest('DOS_CONTEST_OVERVIEW_SYNC', () => {
        fetchContests();
        fetchContestsIgnoreManifests();
        dashboardRefresh();
    });
}

function* contestDetailSaga() {
    yield takeLatest('DOS_COUNTY_DETAIL_SYNC', () => {
        fetchContests();
        fetchContestsIgnoreManifests();
        dashboardRefresh();
    });
}

function* countyDetailSaga() {
    yield takeLatest('DOS_COUNTY_DETAIL_SYNC', () => dashboardRefresh());
}

function* countyOverviewSaga() {
    yield takeLatest('DOS_COUNTY_OVERVIEW_SYNC', () => dashboardRefresh());
}

const DOS_POLL_DELAY = config.pollDelay;

// TODO (VT): I belive this is also unused.
const dashboardPollSaga = createPollSaga(
    [dashboardRefresh, fetchContests],
    'DOS_DASHBOARD_POLL_START',
    'DOS_DASHBOARD_POLL_STOP',
    () => DOS_POLL_DELAY,
);

function* generateAssertionsSaga() {
    yield takeLatest('DOS_GENERATE_ASSERTIONS_SYNC', () => dashboardRefresh());
}

function* defineAuditSaga() {
    yield takeLatest('DOS_DEFINE_AUDIT_SYNC', () => dashboardRefresh());
}

const selectContestsPollSaga = createPollSaga(
    [dashboardRefresh, fetchContests],
    'DOS_SELECT_CONTESTS_POLL_START',
    'DOS_SELECT_CONTESTS_POLL_STOP',
    () => DOS_POLL_DELAY,
);

function* randomSeedSaga() {
    yield takeLatest('DOS_DEFINE_AUDIT_RANDOM_SEED_SYNC', () => dashboardRefresh());
}

function* defineAuditReviewSaga() {
    yield takeLatest('DOS_DEFINE_AUDIT_REVIEW_SYNC', () => dashboardRefresh());
}

export default function* pollSaga() {
    yield all([
        contestOverviewSaga(),
        countyDetailSaga(),
        countyOverviewSaga(),
        dashboardPollSaga(),
        defineAuditSaga(),
        generateAssertionsSaga(),
        defineAuditReviewSaga(),
        randomSeedSaga(),
        selectContestsPollSaga(),
    ]);
}

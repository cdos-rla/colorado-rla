import { endpoint } from 'corla/config';

import createFetchAction from 'corla/action/createFetchAction';

// A copy of `fetchContests.ts`, except that it sets ignoreManifests=true.
const url = endpoint('contest') + '?ignoreManifests=true'

export default createFetchAction({
    failType: 'DOS_FETCH_CONTESTS_IGNORE_MANIFESTS_FAIL',
    networkFailType: 'DOS_FETCH_CONTESTS_IGNORE_MANIFESTS_NETWORK_FAIL',
    okType: 'DOS_FETCH_CONTESTS_IGNORE_MANIFESTS_OK',
    sendType: 'DOS_FETCH_CONTESTS_IGNORE_MANIFESTS_SEND',
    url,
});

import { endpoint } from 'corla/config';

import createFetchAction from 'corla/action/createFetchAction';

const url = endpoint('generate-assertions');

export default createFetchAction({
    failType: 'GENERATE_ASSERTIONS_FAIL',
    networkFailType: 'GENERATE_ASSERTIONS_NETWORK_FAIL',
    okType: 'GENERATE_ASSERTIONS_OK',
    sendType: 'GENERATE_ASSERTIONS_SEND',
    url,
});

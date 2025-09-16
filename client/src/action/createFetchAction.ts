import { empty } from 'corla/util';

import action from '.';

interface CreateFetchConfig {
    failType: string;
    networkFailType: string;
    okType: string;
    sendType: string;
    url: string;
}

function createFetchAction(config: CreateFetchConfig) {
    const {
        failType,
        networkFailType,
        okType,
        sendType,
        url,
    } = config;

    async function fetchAction(queryParams: URLSearchParams | null = null) {
        action(sendType);

        const init: RequestInit = {
            credentials: 'include',
            method: 'get',
        };

        try {
            // append query string if provided
            const fullUrl = queryParams ? `${url}?${queryParams.toString()}` : url;

            const r = await fetch(fullUrl, init);

            if (!r.ok) {
                action(failType);
            }

            if (r.status === 401) {
                action('NOT_AUTHORIZED');
            }

            const data = await r.json().catch(empty);

            action(okType, data);
        } catch (e) {
            action(networkFailType);

            throw e;
        }
    }

    return fetchAction;
}

export default createFetchAction;

import { parse } from 'corla/adapter/contestFetch';

// Identical to contestFetch, but setting the contestsIgnoringManfiests field.
// There's no need to change .DOSFectchContestOK or the parser, because both of these are the same
// regardless of whether we ignore manifests - i.e. the contest data is the same.
export default function contestFetchOk(
    state: DOS.AppState,
    action: Action.DOSFetchContestsIgnoreManifestsOk,
): DOS.AppState {
    const nextState = { ...state };

    nextState.contestsIgnoringManifests = parse(action.data);

    return nextState;
}

import { endpoint } from 'corla/config';

export default () => {
    const url = `${endpoint('get-assertions')}`;

    window.location.replace(url);
};
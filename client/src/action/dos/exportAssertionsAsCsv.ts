import { endpoint } from 'corla/config';

export default () => {
    const url = `${endpoint('get-assertions?format=csv')}`;

    window.location.replace(url);
};
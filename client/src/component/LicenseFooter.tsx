import * as React from 'react';

import { projectUrl } from 'corla/config';

const License = () => {
    return (
        <div className='license'>
            The <em>Colorado RLA Software</em> is Copyright © 2019 the Colorado
            Department of State, and is licensed under the AGPLv3 with a
            classpath exception. See the
            { ' ' }<a className='text-link' href={ projectUrl }>project site</a> for more information.
        </div>
    );
};

const LicenseFooter = () => {
    return (
        <footer className='l-footer'>
            <License />
        </footer>
    );
};

export default LicenseFooter;

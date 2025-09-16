import * as React from 'react';

import DOSNav from 'corla/component/DOS/Nav';
import LicenseFooter from 'corla/component/LicenseFooter';

interface Props {
    main: React.ReactNode;
}

const DOSLayout = (props: Props) => {
    return (
        <div className='l-wrapper'>
            <DOSNav />
            <div id='main' className='l-main'>
                { props.main }
            </div>
            <LicenseFooter />
        </div>
    );
};

export default DOSLayout;

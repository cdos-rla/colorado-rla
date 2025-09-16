import * as React from 'react';

import CountyNav from 'corla/component/County/Nav';
import LicenseFooter from 'corla/component/LicenseFooter';

interface Props {
    main: React.ReactNode;
}

const CountyLayout = (props: Props) => {
    return (
        <div className='l-wrapper'>
            <CountyNav />
            <div id='main' className='l-main'>
                { props.main }
            </div>
            <LicenseFooter />
        </div>
    );
};

export default CountyLayout;

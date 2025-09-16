import SkipToMain from 'corla/component/SkipToMain';
import * as React from 'react';

import LicenseFooter from 'corla/component/LicenseFooter';
import Helpdesk from 'corla/component/Helpdesk';

interface Props {
    main: React.ReactNode;
}

const LoginLayout = (props: Props) => {
    return (
        <div className='l-wrapper'>
            <SkipToMain />
            <div id='main' className='l-main'>
                { props.main }
                <Helpdesk/>
            </div>
            <LicenseFooter />
        </div>
    );
};

export default LoginLayout;

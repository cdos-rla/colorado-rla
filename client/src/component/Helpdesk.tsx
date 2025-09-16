import * as React from 'react';
import *as config from 'corla/config';

const Helpdesk = () => {
    let emailLink="mailto:" + config.helpEmail;
    let phoneLink="tel:" + config.helpTel;
    return (
        <div style={{marginTop: "40px"}} className='login'>
            <ul style ={{padding: "0px",listStyleType: "disc"}}>
            <li>To log into the RLA Software please use your SCORE username and password plus your grid card</li>                 
            <li>To reset your password, please contact SCORE Customer Support</li>             
            <li>To add a new county user, please contact <a className='text-link' href={emailLink}>{config.helpEmail}</a></li>
            </ul>
        </div>
    );
};

export default Helpdesk;
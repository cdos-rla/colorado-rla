import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import { Breadcrumb } from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';

import ElectionDateForm from './ElectionDateForm';
import ElectionTypeForm from './ElectionTypeForm';
import PublicMeetingDateForm from './PublicMeetingDateForm';
import RiskLimitForm from './RiskLimitForm';
import UploadFileButton from './UploadFileButton';

const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Audit Admin' /></li>
    </ul>
);

function round(val: number, digits: number) {
    const factor = Math.pow(10, digits);
    return Math.round(val * factor) / factor;
}

interface SaveButtonProps {
    disabled: boolean;
    nextPage: OnClick;
}

const SaveButton = (props: SaveButtonProps) => {
    const { disabled, nextPage } = props;

    const buttonClick = () => {
        nextPage();
    };

    return (
        <button
            disabled={ disabled }
            onClick={ buttonClick }
            className='pt-button pt-intent-primary'>
            Save & Next
        </button>
    );
};

interface PageProps {
    electionDate: Date;
    isFormValid: boolean;
    publicMeetingDate: Date;
    riskLimit: number;
    nextPage: OnClick;
    type: ElectionType;
    setElectionDate: (d: Date) => void;
    setPublicMeetingDate: (d: Date) => void;
    setRiskLimit: (l: number) => void;
    setType: (t: ElectionType) => void;
    setUploadedFiles: (fs: string[]) => void;
}

const AuditPage = (props: PageProps) => {
    const {
        electionDate,
        isFormValid,
        publicMeetingDate,
        riskLimit,
        type,
        nextPage,
        setElectionDate,
        setPublicMeetingDate,
        setRiskLimit,
        setType,
        setUploadedFiles,
    } = props;

    const disableButton = !isFormValid;

    const main =
        <div>
            <IdleDialog />
            <Breadcrumbs />

            <h1 className='page-heading'>Administer an Audit</h1>

            <div>
                <h2 className='section-heading'>Election Info</h2>
                <div className='mb-default'>Enter the date the election will take place, and the type of election.</div>
                <div className='mb-default'>
                    <ElectionDateForm onChange={ setElectionDate }
                                  initDate={ electionDate } />
                </div>
                <div className='mb-default'>
                    <ElectionTypeForm onChange={ setType }
                                  initType={ type } />
                </div>
            </div>
            <hr />

            <div>
                <h2 className='section-heading'>Public Meeting Date</h2>
                <div className='mb-default'>Enter the date of the public meeting to establish the random seed.</div>
                <PublicMeetingDateForm onChange={ setPublicMeetingDate }
                                       initDate={ publicMeetingDate } />
            </div>
            <hr />

            <div>
                <h2 className='section-heading'>Risk Limit</h2>
                <div className='mb-default'>
                  Enter the risk limit for comparison audits as a percentage.
                </div>
                <RiskLimitForm onChange={ setRiskLimit }
                               riskLimit={ riskLimit } />

            </div>
            <hr />
            <div>
                <h3 className='section-heading'>Contests</h3>
                <UploadFileButton onChange={ setUploadedFiles } />
            </div>

            <div className='control-buttons mt-default'>
              <SaveButton disabled={ disableButton }
                          nextPage={ nextPage } />
            </div>
        </div>;

    return <DOSLayout main={ main } />;
};

export default AuditPage;

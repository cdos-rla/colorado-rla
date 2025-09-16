import * as React from 'react';

import * as _ from 'lodash';

import { Breadcrumb, NonIdealState } from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';
import FileDownloadButtons from 'corla/component/FileDownloadButtons';
import { formatCountyASMState } from 'corla/format';
import IdleDialog from '../../IdleDialog';

interface BreadcrumbProps {
    county: CountyInfo;
}

const Breadcrumbs = ({ county }: BreadcrumbProps) => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb text='SoS' href='/sos' /></li>
        <li><Breadcrumb text='Counties' href='/sos/county' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text={ county.name } /></li>
    </ul>
);

function formatMember(member: AuditBoardMember): string {
    const { firstName, lastName, party } = member;

    return `${firstName} ${lastName} (${party})`;
}

function formatAuditBoardRow(board: AuditBoardStatus) {
    return (
        <tr>
            <td>{ formatMember(board.members[0]) }</td>
            <td>{ formatMember(board.members[1]) }</td>
            <td>{ `${board.signIn}` }</td>
        </tr>
    );
}

interface AuditBoardsProps {
    auditBoards: AuditBoards;
}

const AuditBoards = (props: AuditBoardsProps) => {
    const { auditBoards } = props;

    return (
        <div className='mt-default'>
             <IdleDialog />
            <h3>Audit boards</h3>
            <table className='pt-html-table pt-html-table-striped rla-table'>
                <thead>
                    <tr>
                        <th>Board member #1</th>
                        <th>Board member #2</th>
                        <th>Sign-in time</th>
                    </tr>
                </thead>
                <tbody>
                    { _.map(auditBoards, formatAuditBoardRow) }
                </tbody>
            </table>
        </div>
    );
};

const NoAuditBoard = () => {
    return (
        <div className='mt-default'>
            <h3>Audit boards</h3>
            <NonIdealState title='Audit boards are not signed in.'
                           visual='people' />
        </div>
    );
};

interface DetailsProps {
    county: CountyInfo;
    status: DOS.CountyStatus;
}

const CountyDetails = (props: DetailsProps) => {
    const { county, status } = props;
    const { auditBoards } = status;
	
console.log('----------------------------------------');
console.log(status);

    const countyState = formatCountyASMState(status.asmState);
    const submitted = status.auditedBallotCount;



    const auditedCount = _.get(status, 'discrepancyCount.audited') || '—';
    const unauditedCount = _.get(status, 'discrepancyCount.unaudited') || '—';

    const auditBoardSection = auditBoards
                            ? <AuditBoards auditBoards={ auditBoards } />
                            : <NoAuditBoard />;

    return (
        <div>
            <table className='pt-html-table pt-html-table-striped rla-table'>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Status</th>
                        <th>Audited discrepancies</th>
                        <th>Non-audited discrepancies</th>
                        <th>Submitted</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td className='ellipsize'>{ county.name }</td>
                        <td className='ellipsize'>{ countyState }</td>
                        <td>{ auditedCount }</td>
                        <td>{ unauditedCount }</td>
                        <td>{ submitted }</td>
                    </tr>
                </tbody>
            </table>
            <FileDownloadButtons status={ status } allowDelete={ status.asmState !== 'COUNTY_AUDIT_UNDERWAY' } />
            { auditBoardSection }
        </div>
    );
};

interface PageProps {
    county: CountyInfo;
    status: DOS.CountyStatus;
}

const CountyDetailPage = (props: PageProps) => {
    const { county, status } = props;

    const main =
        <div>
            <Breadcrumbs county={ county } />
            <h1 className='smallHeader'>{ county.name } County Info</h1>
            <CountyDetails county={ county } status={ status } />
        </div>;

    return <DOSLayout main={ main } />;
};

export default CountyDetailPage;

import * as React from 'react';
import AuditReportForm from 'corla/component/AuditReportForm';

interface StatusProps {
    auditIsComplete: boolean;
    canRenderReport: boolean;
    currentRound: number;
    finishedCountiesCount: number;
    totalCountiesCount: number;
}

const Status = (props: StatusProps) => {
    const {
        auditIsComplete,
        canRenderReport,
        currentRound,
        finishedCountiesCount,
        totalCountiesCount,
    } = props;

    return (
        <div className='state-dashboard-round'>
            <div>
                { !auditIsComplete && <div style={{fontSize: '18px', fontWeight: 'bold'}}>Round { currentRound } in progress</div> }
                { auditIsComplete && <div style={{fontSize: '18px', fontWeight: 'bold'}}>Congratulations! The audit is complete.</div> }
                <span className='state-dashboard-round-summary'>
                    { finishedCountiesCount } of { totalCountiesCount } counties
                    have finished this round.
                </span>
            </div>
            <div>
                {canRenderReport && (<AuditReportForm
                canRenderReport={canRenderReport}
                /> 
                )}
            </div>
        </div>
    );
};

export default Status;

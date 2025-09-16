import * as React from 'react';

import RoundContainer from './RoundContainer';
import {Button} from "@blueprintjs/core";

interface RaireStatusProps {
    raireServiceStatus: string,
}

const RaireStatus = ( { raireServiceStatus }: RaireStatusProps) => {
    return (
        <div className ='state-dashboard-audit-definition' >
            <dl>
                <dt>Raire service</dt>
                <dd>{ raireServiceStatus }</dd>
            </dl>
        </div>
    )
}


interface AuditDefinitionProps {
    riskLimit?: number;
    seed?: string;
}

const AuditDefinition = ({ riskLimit, seed }: AuditDefinitionProps) => {
    return (
        <div className='state-dashboard-audit-definition'>
           <dl>
                <dt>Target risk limit</dt>
                <dd>{ (riskLimit || 0) * 100 }%</dd>
                <dt>Seed</dt>
                <dd>{ seed }</dd>
            </dl>
        </div>
    );
};

interface MainProps {
    auditDefined: boolean;
    canRenderReport: boolean;
    dosState: DOS.AppState;
}


const Main = (props: MainProps) => {
    const { auditDefined, canRenderReport, dosState } = props;

    if (!dosState.asm) {
        return null;
    }

    const auditIsComplete = dosState.asm === 'DOS_AUDIT_COMPLETE';

    return (
        <div className='sos-notifications'>
            <RaireStatus { ...dosState }/>
            { auditDefined && <AuditDefinition { ...dosState } /> }
            <RoundContainer auditIsComplete={ auditIsComplete }
                            canRenderReport={ canRenderReport } />
        </div>
    );
};

export default Main;

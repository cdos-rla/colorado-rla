import * as React from 'react';

import { Button, Card, Elevation, Intent, ProgressBar } from '@blueprintjs/core';
import startNextRound from 'corla/action/dos/startNextRound';
import AuditReportForm from 'corla/component/AuditReportForm';
import exportAssertionsAsJson from "corla/action/dos/exportAssertionsAsJson";
import exportAssertionsAsCsv from "corla/action/dos/exportAssertionsAsCsv";

interface ControlProps {
    canRenderReport: boolean;
    currentRound: number;
}

interface ControlState {
    roundStartSent: boolean;
}

class Control extends React.Component<ControlProps, ControlState>  {
    constructor(props: ControlProps) {
        super(props);
        this.state = { roundStartSent: false };
    }

    public render() {
        const { canRenderReport, currentRound } = this.props;

        const waitForNextRound = () => {
            this.setState({ roundStartSent: true });
            startNextRound().then(function() {
                // use the result here
                this.setState({ roundStartSent: false });
            })
                .catch(function(reason) {
                    this.setState({ roundStartSent: false });
                });
        };

        const ButtonDiv = () => {
            return (
                <div className='state-dashboard-round'>
                    <div>
                        <h4>Round {currentRound} completed</h4>
                        <Button intent={Intent.PRIMARY}
                                onClick={waitForNextRound}>
                            Start round {currentRound + 1}
                        </Button>
                    </div>
                    <div>
                        <Button onClick={exportAssertionsAsJson} className='pt-button pt-intent-primary'>
                            Export Assertions as JSON
                        </Button>
                    </div>
                    <div>
                        <Button onClick={exportAssertionsAsCsv} className='pt-button pt-intent-primary'>
                            Export Assertions as CSV
                        </Button>
                    </div>
                    <div>
                        <div>
                            {canRenderReport && (<AuditReportForm
                                    canRenderReport={canRenderReport}
                                />
                            )}
                        </div>
                    </div>
                </div>
            );
        };

        function ProgressDiv() {
            return (
                <Card interactive={false} elevation={Elevation.TWO}>
                    <span style={{display: 'inline-block', marginBottom: '20px'}}>
                    Round {currentRound + 1} has been started. Please wait for the operation to complete.
                        It might take a couple minutes. Once complete, page will refresh.
                    </span>
                    <div>
                     <ProgressBar  intent={Intent.SUCCESS}/>
                     </div>
                </Card>
            );
        }

        if (this.state.roundStartSent) {
            return (
                <ProgressDiv />
            );

        } else {
            return (
                <div>
                    <ButtonDiv />
                </div>
            );
        }
    }
}

export default Control;

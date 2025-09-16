import * as React from 'react';

import { Button, Classes, Popover, Checkbox, FormGroup, Position, Intent } from '@blueprintjs/core';
import fetchAuditReport from 'corla/action/dos/fetchAuditReport';

interface ReportType {
    key: string;
    label: string;
}

interface FormProps {
    canRenderReport: boolean;
}

interface FormState {
    checkedReports: {[key:string]:boolean};
    checkAll: boolean
}

const REPORT_TYPES: ReportType[] = [
    {key:'batch_count_comparison', label:'Batch count comparison'},
    {key:'contest', label:'Contest'},
    {key:'contest_comparison', label:'Contest comparison'},
    {key:'contest_selection', label:'Contest selection'},
    {key:'contests_by_county', label:'Contests by county'},
    {key:'seed', label:'Seed'},
    {key:'tabulate_plurality', label:'Tabulate'},
    {key:'tabulate_county_plurality', label:'Tabulate county'},
    {key:'upload_status', label:'Upload status'},
    {key:'ResultReport', label:'Result Report'},
    {key:'ActivityReport', label:'Activity Report'},
    {key:'StateReport', label:'State Report'},
    {key:'JSON', label:'Json Reports'},
    {key:'summarize_IRV', label:'IRV summaries'},
    {key:'ranked_ballot_interpretation', label:'Ranked vote interpretation'},
    {key:'assertions_json', label:'Assertions (json)'},
    {key:'assertions_csv', label:'Assertions (csv)'}
];


class AuditReportForm extends React.Component<FormProps, FormState> {
    checkedReports: {[key:string]:boolean};
   

    public constructor(props: FormProps, state:FormState) {
        super(props);

        this.state = {
             checkedReports: {},
             checkAll: false
        };
    }

    private handleCheckboxChange = (event: React.FormEvent<HTMLInputElement>) => {
        const key = event.currentTarget.value;
        const isChecked = event.currentTarget.checked;
        if (isChecked) {
        const newCheckedReports = {
            ...this.state.checkedReports,
            [key]: isChecked
            };
            // Update the component state with the new object
            this.setState({ checkedReports: newCheckedReports });
        } else {
            //uses destructuring and the rest operator to create a new object called 
            //newCheckedReports that contains all the properties except the one with the key 
            const { [key]: _, ...newCheckedReports } = this.state.checkedReports;
            this.setState({ checkedReports: newCheckedReports });

        }
      
 
    }


 
    public render() {
 
        return (
            <Popover position={Position.BOTTOM_LEFT} canEscapeKeyClose={true}
                     enforceFocus={false}>

                <Button large disabled={!this.props.canRenderReport} intent={Intent.PRIMARY}>
                    Choose report to download</Button>
                <div key="text" style={{margin: "10px", minWidth: "300px", maxWidth: "370px"}}>
                    <h5>Audit reports</h5>
                    <FormGroup>
                        {REPORT_TYPES.map(ty => {
                            let key = ty.key;
                            let label = ty.label;
                            return <div className='checkbox'><Checkbox key={key}
                                                                       label={label}
                                                                       value={key}
                                                                       checked={this.state.checkedReports[key] || false}
                                                                       onChange={this.handleCheckboxChange}
                                                                       style={{minWidth: '10px', paddingLeft: '30px'}}/>
                            </div>
                        })
                        }
                    </FormGroup>
                    <div style={{display: "flex", flexDirection:"row", justifyContent: "space-between", marginTop: 15}}>
                        <Button className={Classes.POPOVER_DISMISS}
                                intent={Intent.PRIMARY}
                                icon='import'
                                onClick={() => fetchAuditReport(REPORT_TYPES.map(rt => rt.key).join(","))}>
                            Download All
                        </Button>
                        <Button className={Classes.POPOVER_DISMISS}
                                intent={Intent.PRIMARY}
                                icon='import'
                                onClick={() => fetchAuditReport(Object.keys(this.state.checkedReports).join(","))}>
                            Download Selected
                        </Button>
                    </div>
                </div>
            </Popover>

        );
    }
}

export default AuditReportForm;

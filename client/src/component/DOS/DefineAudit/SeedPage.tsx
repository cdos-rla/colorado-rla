import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import { Breadcrumb, Button, Intent } from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';

import SeedForm from './SeedForm';

const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS' />></li>
        <li><Breadcrumb href='/sos/audit' text='Audit Admin' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Seed' /></li>
    </ul>
);

interface PageProps {
    back: OnClick;
    nextPage: OnClick;
    formattedPublicMeetingDate: string;
    seed: string;
    uploadRandomSeed: OnClick;
}

class AuditSeedPage extends React.Component<PageProps> {
    public state: any;

    constructor(props: PageProps) {
        super(props);

        this.state = {
            form: { seed: props.seed },
            formValid: false,
        };
    }

    public setValid(valid: boolean)  {
        this.setState({ formValid: valid });
    }

    public render() {
        const main =
            <div>
                <IdleDialog />
                <Breadcrumbs />
                <div className='mb-default'>
                    <h1 className={'smallHeader'}>Audit Definition - Enter Random Seed</h1>
                    <div>
                        Enter the random seed generated from the public meeting
                        on { this.props.formattedPublicMeetingDate }.
                    </div>
                    <hr />
                    <div>
                        <SeedForm initSeed={ this.state.form.seed }
                                  updateForm={ (seed: string) => { this.state.form.seed = seed; } }
                                  setValid={ (v: boolean) => { this.setValid(v); } } />
                    </div>
                </div>
                <div>
                    <Button onClick={ this.props.back }>Back</Button>
                    <Button className='ml-default'
                            intent={ Intent.PRIMARY }
                            disabled={!this.state.formValid}
                            onClick={ this.onSaveAndNext }>
                        Save & Next
                    </Button>
                </div>
            </div>;

        return <DOSLayout main={ main } />;
    }

    private onSaveAndNext = () => {
        this.props.uploadRandomSeed(this.state.form.seed);
        this.props.nextPage();
    }
}

export default AuditSeedPage;

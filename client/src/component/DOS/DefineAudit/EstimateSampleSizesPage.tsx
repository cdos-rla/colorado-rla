import {endpoint} from 'corla/config';
import * as React from 'react';

import {Breadcrumb, Button, Card, Intent, Spinner} from '@blueprintjs/core';
import DOSLayout from 'corla/component/DOSLayout';

const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS' />></li>
        <li><Breadcrumb href='/sos/audit' text='Audit Admin' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Estimate Sample Sizes' /></li>
    </ul>
);

interface EstimateSampleSizesPageProps {
    forward: OnClick;
}

interface EstimateSampleSizesPageState {
    generatingEstimatedSampleSizes: boolean;
    generationFailed: boolean;
    generationSucceeded: boolean;
}

class EstimateSampleSizesPage extends React.Component<EstimateSampleSizesPageProps, EstimateSampleSizesPageState> {
    public constructor(props: EstimateSampleSizesPageProps) {
        super(props);

        this.state = {
            generatingEstimatedSampleSizes: false,
            generationFailed: false,
            generationSucceeded: false,
        };
    }

    public render() {
        const {
            forward,
        } = this.props;

        const main =
            <div>
                <Breadcrumbs/>
                <h1 className={'mediumHeader'}>Estimate Sample Sizes for all Contests</h1>
                <p>
                    Clicking on the estimate button will produce a CSV file containing
                    sample sizes for all contests in the database, which will be
                    automatically downloaded.
                </p>
                <div>
                    {this.getSampleSizeGenerationStatus()}
                </div>
                <div className='control-buttons mt-default'>
                    <Button onClick={this.downloadEstimateSampleSizeSheet}
                            className='pt-button pt-intent-primary'>
                        Estimate
                    </Button>
                </div>

                <div className='control-buttons mt-default'>
                    <Button onClick={forward}
                            className='pt-button pt-intent-primary'>
                        Next
                    </Button>
                </div>
            </div>;

        return <DOSLayout main={ main } />;
    }

    private getSampleSizeGenerationStatus() {
        let statusHtml;

        if (this.state.generatingEstimatedSampleSizes) {
            statusHtml =
                <Card className='mt-default'>
                    <Spinner className='pt-medium' intent={Intent.PRIMARY}/>
                    <div>Estimating Sample Sizes...</div>
                </Card>;
        } else if (this.state.generationSucceeded) {
            statusHtml =
                <div style={{color: 'green'}}>
                    Estimated Sample Sizes CSV Downloaded
                </div>;
        } else if (this.state.generationFailed) {
            statusHtml =
                <div style={{color: 'red'}}>
                    Error: Download failed. Please try again.
                </div>;
        }
        return statusHtml;
    }

    private downloadEstimateSampleSizeSheet = async () => {
        try {
            this.setState({generatingEstimatedSampleSizes: true});
            this.setState({generationFailed: false});
            this.setState({generationSucceeded: false});
            const response = await fetch(endpoint('estimate-sample-sizes'), { credentials: 'include'});
            if (!response.ok || !response.body) {
                this.setState({generationFailed: true});
                return;
            }

            // Download sample size estimate csv file
            const reader = response.body.getReader();
            const chunks = [];

            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    break;
                }
                chunks.push(value);
            }

            const downloadBlob = new Blob(chunks);
            const downloadUrl = URL.createObjectURL(downloadBlob);

            // Dummy anchor element for download
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = 'sample_sizes.csv';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(downloadUrl);

            this.setState({generatingEstimatedSampleSizes: false});
            this.setState({generationSucceeded: true});
        } catch (error) {
            this.setState({generatingEstimatedSampleSizes: false});
            this.setState({generationFailed: true});
        }
    }
}

export default EstimateSampleSizesPage;

import * as _ from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';

import { Button, Card, Intent } from '@blueprintjs/core';
import BallotManifestForm from './Form';
import Uploading from './Uploading';

import uploadBallotManifest from 'corla/action/county/uploadBallotManifest';

import { Spinner } from '@blueprintjs/core';

import deleteFile from 'corla/action/county/deleteFile';
import ballotManifestUploadedSelector from 'corla/selector/county/ballotManifestUploaded';

interface UploadedProps {
    deleting: boolean | undefined;
    enableReupload: OnClick;
    file: UploadedFile;
    handleDeleteFile: OnClick;
}

const UploadedBallotManifest = (props: UploadedProps) => {
    const { enableReupload, handleDeleteFile, file } = props;

    if (props.deleting) {
        return (
            <Card>
                <Spinner className='pt-large' intent={ Intent.PRIMARY } />
                <div>Deleting file...</div>
            </Card>
        );
    } else {
        return (
            <div>
                <h2 className='form-container-heading'>Ballot Manifest</h2>
                <div className='mb-default'>
                    <div><strong>File name: </strong>"{ file.fileName }"</div>
                    <div><strong>SHA-256 hash: </strong> { file.hash }</div>
                </div>
                <div className='error'>
                    <strong>{file.result.success ? '' : 'Error Message: ' }</strong>
                    { file.result.errorMessage }
                </div>
                <div className='error rowNum'>
                    <strong>{file.result.success ? '' : 'Error row number: ' }</strong>
                    { file.result.errorRowNum }
                </div>
                <div className='error rowContent'>
                    <strong>{file.result.success ? '' : 'Error row content: ' }</strong>
                    { file.result.errorRowContent }
                </div>
                <Button intent={ Intent.PRIMARY } onClick={ handleDeleteFile }>
                    Delete File
                </Button>
                <span>&nbsp;&nbsp; </span>
                <Button intent={ Intent.PRIMARY } onClick={ enableReupload }>
                    Re-upload
                </Button>
                <hr />
            </div>
        );
    }
};

interface ContainerProps {
    countyState: County.AppState;
    fileUploaded: boolean;
    uploadingFile: boolean;
}

interface ContainerState {
    fileDeleted: boolean;
    form: {
        file?: File;
        hash: string;
    };
    reupload: boolean;
}

class BallotManifestFormContainer extends React.Component<ContainerProps, ContainerState> {
    public constructor(props: ContainerProps) {
        super(props);

        this.state = {
            fileDeleted: false,
            form: {
                file: undefined,
                hash: '',
            },
            reupload: false,
        };
    }

    public render() {
        const { countyState, fileUploaded, uploadingFile } = this.props;

        if (uploadingFile) {
            return <Uploading />;
        }

        if (fileUploaded && !this.state.reupload && countyState.ballotManifest) {
            return (
                // shameless reuse of state variable uploading=deleting
                <UploadedBallotManifest enableReupload={ this.enableReupload }
                                        handleDeleteFile={ this.handleDeleteFile.bind(this) }
                                        deleting={ countyState.uploadingBallotManifest }
                                        file={ countyState.ballotManifest } />
            );
        }

        return (
            <BallotManifestForm disableReupload={ this.disableReupload }
                                fileUploaded={ fileUploaded }
                                fileDeleted={ this.state.fileDeleted }
                                form={ this.state.form }
                                onFileChange={ this.onFileChange }
                                handleOnDrop= {this.handleOnDrop}
                                onHashChange={ this.onHashChange }
                                upload={ this.upload } />
        );
    }

    private disableReupload = () => {
        this.setState({ reupload: false });
    }

    private enableReupload = () => {
        this.setState({ reupload: true });
    }

    private onFileChange = (e: React.ChangeEvent<any>) => {
        const s = { ...this.state };

        s.form.file = e.target.files[0];

        this.setState(s);
    }

    private handleOnDrop = (files:any) => {
        const s = { ...this.state };

        s.form.file = files[0];
        this.setState(s);
    }

    private onHashChange = (hash: string) => {
        const s = { ...this.state };

        s.form.hash = hash;

        this.setState(s);
    }

    private handleDeleteFile = () => {
        const result = deleteFile('bmi');

        if (result) {
            // clear form
            const s = { ...this.state };
            s.form.hash = '';
            s.form.file = undefined;
            s.fileDeleted = true; // don't show the cancel button momentarily
            this.setState(s);
        }

    }

    private upload = () => {
        const { countyState } = this.props;
        const { file, hash } = this.state.form;

        if (file) {
            uploadBallotManifest(countyState.id!, file, hash);

            this.disableReupload();
        }
    }
}

const mapStateToProps = (countyState: County.AppState) => {
    const uploadingFile = !!countyState.uploadingBallotManifest;

    return {
        countyState,
        fileUploaded: ballotManifestUploadedSelector(countyState),
        uploadingFile,
    };
};

export default connect(mapStateToProps)(BallotManifestFormContainer);

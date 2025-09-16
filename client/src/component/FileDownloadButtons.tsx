import * as React from 'react';

import { Button, Callout, Intent, Popover, Spinner } from '@blueprintjs/core';

import deleteFileForCounty from 'corla/action/dos/deleteFileForCounty';
import downloadFile from 'corla/action/downloadFile';

interface UploadedFileProps {
    allowDelete: boolean;
    description: string;
    file: UploadedFile | undefined | null;
    fileType: string;
}

interface UploadedFileState {
    deleting: boolean;
}

class UploadedFileCard extends React.Component<UploadedFileProps, UploadedFileState> {
    constructor(props: UploadedFileProps) {
        super(props);
        this.state = { deleting: false };
    }

    public componentDidUpdate() {
        if (this.state.deleting && !this.props.file) {
            this.setState({ deleting: false });
        }
    }

    public render() {
        const { description, file, fileType, allowDelete } = this.props;
        if (null === file || undefined === file) {
            return (
                <div className='uploaded-file mt-default'>
                    <h2 style={{fontSize: '18px'}}>{ description }</h2>
                    <p>Not yet uploaded</p>
                </div>
            );
        } else {

            const onClick = () => downloadFile(file.id);
            const onDelete = () => {
                this.setState({ deleting: true });
                deleteFileForCounty(fileType, file.countyId);
            };

            const downloadButton = (
                <div className='uploaded-file-footer-action'>
                    <Button intent={ Intent.PRIMARY }
                            onClick={ onClick }>
                        Download
                    </Button>
                </div>);

            const deleteButton = (
                <div className='uploaded-file-footer-action'>
                { allowDelete ?
                  <Button intent={ Intent.PRIMARY }
                    onClick={ onDelete }>
                    Delete File
                    </Button>
                    : <div></div> }
                </div> );

            const successCard = (
                <Callout className='uploaded-file-footer'>
                    <Callout className='uploaded-file-footer-status'
                             intent={ Intent.SUCCESS }
                             icon='tick-circle'>
                        File successfully uploaded
                    </Callout>
                    { file.timestamp.toLocaleString()  + '\xa0'}
                    { downloadButton }
                    { deleteButton }
                </Callout>
            );

            const errorCard = (
                <Callout className='uploaded-file-footer'>
                    <Callout className='uploaded-file-footer-status'
                             intent={ Intent.DANGER }
                             icon='error'>
                        <p>
                            <strong>Error: </strong>
                            { file.result.errorMessage ? file.result.errorMessage : 'unknown' }
                            { file.result.errorRowNum &&
                              <Popover className='uploaded-file-popover-target'
                                       popoverClassName='uploaded-file-popover'>
                                  <span>at row { file.result.errorRowNum }</span>
                                  <div>
                                      <h4>Row { file.result.errorRowNum }</h4>
                                      <p>The content of row { file.result.errorRowNum } is displayed below:</p>
                                      <pre>{ file.result.errorRowContent }</pre>
                                  </div>
                              </Popover>
                            }
                        </p>
                    </Callout>
                    { downloadButton }
                </Callout>
            );

            const pendingCard = (msg: string) => {
                return (
                    <Callout className='uploaded-file-footer'>
                      <Callout className='uploaded-file-footer-status'>
                        { msg }
                        <Spinner className='pt-medium' intent={ Intent.PRIMARY } />
                      </Callout>
                    </Callout>
                ); };

            const resultCard = () => {
                // this behavior lines up with ImportFileController.java
                if (file.result.success === true) {
                    return successCard;
                    /* if result.success === undefined would be nice, but is */
                    /* prevented by the GSON during db storage which sets null to false */
                    /* this is a cheap way to infer pending */
                } else if (file.result.errorMessage === undefined) {
                    return pendingCard('File upload in progress...');
                } else {
                    return errorCard;
                }
            };

            return (
                <div className='uploaded-file mt-default'>
                    <h2 style={{fontSize: '18px'}}>{ description }</h2>
                    <dl className='uploaded-file-details'>
                        <dt>File name</dt>
                        <dd>{ file.fileName }</dd>

                        <dt>SHA-256 hash</dt>
                        <dd>{ file.hash }</dd>
                    </dl>
                    { this.state.deleting ?
                      pendingCard('Deleting file...')
                      : resultCard() }
                </div>
            );
        }
    }
}

interface DownloadButtonsProps {
    status: County.AppState | DOS.CountyStatus;
    allowDelete: boolean;
}

const FileDownloadButtons = (props: DownloadButtonsProps) => {
    const { status, allowDelete } = props;

    if (!status) {
        return <div />;
    }

    const { ballotManifest, cvrExport } = status;

    return (
        <div className='mt-default'>
            {/* fileType matches DeleteFileController.java */}
            <UploadedFileCard description='Ballot Manifest' file={ ballotManifest }
                          fileType='bmi' allowDelete={ allowDelete } />
            <UploadedFileCard description='CVR Export' file={ cvrExport }
                          fileType='cvr' allowDelete={ allowDelete } />
        </div>
    );
};

export default FileDownloadButtons;

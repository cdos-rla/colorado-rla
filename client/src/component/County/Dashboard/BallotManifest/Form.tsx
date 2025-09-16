import * as React from 'react';


import {
    Button,
    EditableText,
    FormGroup,
    Intent,
} from '@blueprintjs/core';

import Dropzone from 'react-dropzone'

interface FormProps {
    disableReupload: OnClick;
    fileUploaded: boolean;
    fileDeleted: boolean;
    form: {
        file?: File;
        hash: string;
    };
    onFileChange: OnClick;
    handleOnDrop: OnClick;
    onHashChange: OnClick;
    upload: OnClick;
}
const dropStyle = {
    border: '2px dashed rgb(102, 102, 102)',
    borderRadius: '5px',
    marginBottom: '10px',
    padding: '1em',
    width: '500px',
};

const activeStyle = {
    backgroundColor: 'rgb(245, 245, 245)',
    border: '2px solid green',
    borderRadius: '5px',
};

const BallotManifestForm = (props: FormProps) => {
    const {
        disableReupload,
        fileUploaded,
        form,
        onFileChange,
        handleOnDrop,
        onHashChange,
        upload,
        fileDeleted,
    } = props;

    const { file, hash } = form;

    let fileName = file ? file.name : '';
 
    const cancelButton = (
        <Button intent={ Intent.WARNING } onClick={ disableReupload }>
            Cancel
        </Button>
    );



    // fileDeleted allows us to not wait for a dashboard refresh to get the asm
    // state, which is what fileUploaded is based on
    // then, we won't show the cancel button momentarily, which looks weird
    const renderedCancelButton = fileUploaded && !fileDeleted
                               ? cancelButton
                               : '';

    return (
        <div>
            <div style={{ width: '500px' }}>
                <div className='mb-default'>
                    <label>
                        <span className='form-group-label pt-ui-text-large font-weight-bold'>
                                    Ballot Manifest
                        </span>
                        <div className='dropzone'>
                        <Dropzone onDrop={ handleOnDrop }
                                  activeStyle={ activeStyle }
                                  multiple={ false }
                                  style={ dropStyle }>
                            <div>
                                <strong>
                                Click here or drag-and-drop your Ballot Manifest .csv file to upload it
                                </strong>

                            </div>
                        </Dropzone>
                        </div>
                    </label>
                   <FormGroup>
                        <aside>
                        <div className='import-file'
                         style={ file ? { display: 'inline' } : {} }>

                         <span className='uploaded-file-name' key={ fileName }>
                                        { fileName } { file? "(" + file.size + "bytes.)" :'' } 
                        </span>
                  
                        </div>
                        </aside>
                   </FormGroup>
                 </div>
                <div className='mb-default'>
                    <label>
                        <FormGroup label={
                            <span className='form-group-label pt-ui-text-large font-weight-bold'>
                                SHA-256 hash for Ballot Manifest
                            </span>
                        }>
                        <EditableText className='pt-input'
                                        minWidth={ 600 }
                                        maxLength={ 64 }
                                        value={ hash }
                                        onChange={ onHashChange } />
                        </FormGroup>
                    </label>
                </div>
            </div>
            <div className='form-controls'>
                { renderedCancelButton }
                <Button intent={ Intent.PRIMARY } onClick={ upload }>
                    Upload
                </Button>
            </div>
            <hr />
        </div>
    );
};

export default BallotManifestForm;

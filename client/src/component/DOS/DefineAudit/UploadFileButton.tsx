import * as _ from 'lodash';
import * as React from 'react';
import Dropzone from 'react-dropzone';

interface UploadFileButtonProps {
    onChange: (fileContents: string[]) => void;
}

interface UploadFileButtonState {
    files: File[];
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

class UploadFileButton extends React.Component<UploadFileButtonProps, UploadFileButtonState> {
    constructor(props: UploadFileButtonProps) {
        super(props);

        this.state = {
            files: [],
        };

        this.onDrop = this.onDrop.bind(this);
        this.areFilesReady = this.areFilesReady.bind(this);
    }

    public render() {
        return (
            <div>
                <div className='dropzone'>
                    <label>
                        <Dropzone onDrop={ this.onDrop }
                                  activeStyle={ activeStyle }
                                  multiple={ false }
                                  style={ dropStyle }>
                            <div>
                                <strong>
                                    Drag and drop or click here to select the file you wish to use as the source
                                    for standardized contest names across jurisdictions.
                                </strong>
                                <div className='mt-default'>File requirements:</div>
                                <ul>
                                    <li>File must be CSV formatted, with a <em>.csv</em> or <em>.txt</em>
                                        &nbsp;extension. Other file types are not accepted</li>
                                    <li>The file must contain a header row consisting of <em>CountyName</em>
                                        &nbsp;and <em>ContestName</em>.</li>
                                    <li>The file may not contain duplicate records</li>
                                </ul>
                            </div>
                        </Dropzone>
                    </label>
                </div>

                <aside>
                    <div className='import-file'
                         style={ this.areFilesReady() ? { display: 'inline' } : {} }>
                        <div className='mt-default'><strong>Ready to import:</strong></div>
                        {
                            this.state.files.map((file: File) => {
                                return (
                                    <span className='uploaded-file-name' key={ file.name }>
                                        { file.name } ({ file.size } bytes.)
                                    </span>
                                );
                            })
                        }
                    </div>
                </aside>
            </div>
        );
    }

    private areFilesReady() {
        return _.size(this.state.files) > 0;
    }

    private onDrop(files: File[]) {
        const fileContents: string[] = [];

        files.forEach((file, i) => {
            const reader = new FileReader();

            reader.onload = () => {
                fileContents[i] = reader.result;

                if (_.size(fileContents) === _.size(files) && _.every(fileContents)) {
                    this.setState({ files });

                    this.props.onChange(fileContents);
                }
            };

            reader.readAsBinaryString(file);
        });
    }
}

export default UploadFileButton;

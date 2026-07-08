import * as React from 'react';
import { connect } from 'react-redux';

import { Button, Intent, NumericInput } from '@blueprintjs/core';

import setNumberOfAuditBoards from 'corla/action/county/setNumberOfAuditBoards';
import * as Modal from 'react-modal';
Modal.setAppElement('#root');

/**
 * Minimum number of audit boards that can participate.
 */
const MIN_AUDIT_BOARDS = 1;

const modalStyles = {
    content : {
        width                 : '600px',
        top                   : '50%',
        left                  : '50%',
        right                 : 'auto',
        bottom                : 'auto',
        marginRight           : '-50%',
        transform             : 'translate(-50%, -50%)'
    }
};

interface AuditBoardNumberSelectorProps {
    auditBoardCount: number;
    numberOfBallotsToAudit?: number;
    isShown: boolean;
    isEnabled: boolean;
    setNumberOfAuditBoards: (x: { auditBoardCount: number }) => void;
}

interface AuditBoardNumberSelectorState {
    auditBoardCount: number;
    isEnabled: boolean;
    enterTask: boolean;
}

class AuditBoardNumberSelector
    extends React.Component<AuditBoardNumberSelectorProps,
                            AuditBoardNumberSelectorState> {
    constructor(props: AuditBoardNumberSelectorProps) {
        super(props);

        this.state = {
          auditBoardCount: props.auditBoardCount,
          isEnabled: props.isEnabled,
          enterTask: false,
        };

        this.handleChangeAuditBoards = this.handleChangeAuditBoards.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    public componentWillReceiveProps(nextProps: AuditBoardNumberSelectorProps) {
        if (!nextProps.isEnabled) {
            this.setState({ auditBoardCount: nextProps.auditBoardCount,
                            isEnabled: false });
        }
    }

    public render() {
        const { isShown, numberOfBallotsToAudit } = this.props;
        const { isEnabled } = this.state;

        if (!isShown) {
            return null;
        }

        return (
            <div className='mt-default'>
                <Modal isOpen={this.state.enterTask}
                       style={modalStyles} >
                    <h2>Confirm Audit Board Amount</h2>
                    <p> Please confirm you’d like to use { this.state.auditBoardCount } audit board{this.state.auditBoardCount > 1 ? 's' : ''} for this round of the audit. <br />
                        Remember, once the number is confirmed you may not change it until another round begins.
                    </p>
                    <br />
                    <div>
                        <div className='audit-board-button-container'>
                            <button
                                className='cancel-button'
                                onClick={() => this.setState({ enterTask: false })}>
                                go back
                            </button>
                            <button
                                className='confirmation-button'
                                onClick={() => this.handleConfirmationBox()}>
                                confirm
                            </button>
                        </div>
                    </div>
                </Modal>
                <form>
                    <div className='pt-form-group'>
                        <label className='pt-label pt-ui-text-large font-weight-bold'
                               htmlFor='number-of-audit-boards-input'>
                            How many audit boards will be auditing?
                        </label>
                        <div className='pt-control-group'>
                            <NumericInput id='number-of-audit-boards-input'
                                          min={ MIN_AUDIT_BOARDS }
                                          value={ this.state.auditBoardCount }
                                          onValueChange={ this.handleChangeAuditBoards }
                                          disabled={ !isEnabled } />
                            <Button disabled={ !isEnabled } intent={ Intent.PRIMARY } type='button' onClick={ () => this.handleConfirmationBox()} >Enter</Button>
                        </div>
                        { this.helperText(numberOfBallotsToAudit) }
                    </div>
                </form>
            </div>
        );
    }

    private handleConfirmationBox() {
        if (!this.state.enterTask) {
            this.handleConfirm();
        } else {
            this.setState({ enterTask: false });
            this.handleSubmit();
        }
    }

    private handleConfirm() {

        const bgItems: any = document.getElementsByClassName('confirm-bg');
        for (let i = 0; i < bgItems.length; i++) {
            const element = bgItems[i];
            element.style.display = 'flex';
        }

        const containerItems: any = document.getElementsByClassName('container');
        for (let i = 0; i < containerItems.length; i++) {
            const element = containerItems[i];
            element.style.display = 'flex';
        }
        this.setState({ enterTask: true });

    }

    private  handleGoBack() {
        const bgItems: any = document.getElementsByClassName('confirm-bg');
        for (let i = 0; i < bgItems.length; i++) {
            const element = bgItems[i];
            element.style.display = 'none';
        }

        const containerItems: any = document.getElementsByClassName('container');
        for (let i = 0; i < containerItems.length; i++) {
            const element = containerItems[i];
            element.style.display = 'none';
        }

        this.setState({ enterTask: false });
    }

    private handleChangeAuditBoards(asNumber: number, asString: string) {
        if (asNumber >= 1) {
          this.setState({ auditBoardCount: asNumber });
        }
    }

   // private handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    private handleSubmit() {

      //  e.preventDefault();

        const { auditBoardCount } = this.state;

        this.setState({ isEnabled: false });
        this.props.setNumberOfAuditBoards({ auditBoardCount });
    }

    private helperText(toAudit?: number) {
        if (!toAudit) {
            return null;
        }

        if (1 === toAudit) {
            return (
                <div className='pt-form-helper-text'>
                    There is <b>1</b> ballot card to audit in this round.
                </div>
            );
        }

        return (
            <div className='pt-form-helper-text'>
                There are <b>{ toAudit }</b> ballot cards to audit in this
                round.
            </div>
        );
    }
}

const mapDispatchToProps = () => {
    return { setNumberOfAuditBoards };
};

export default connect(null, mapDispatchToProps)(AuditBoardNumberSelector);

import * as React from 'react';

import { Card, EditableText, Radio, RadioGroup } from '@blueprintjs/core';

interface FormProps {
    elector: AuditBoardMember;
    onFirstNameChange: OnClick;
    onLastNameChange: OnClick;
    onPartyChange: OnClick;
    onTextConfirm: OnClick;
}

const SignInForm = (props: FormProps) => {
    const {
        elector,
        onFirstNameChange,
        onLastNameChange,
        onPartyChange,
        onTextConfirm,
    } = props;

    const { firstName, lastName, party } = elector;

    return (
        <div>
            <h2 className={'smallHeader'}>Audit Board Member</h2>
            <Card>
                <label>
                    <strong>First Name: </strong>
                    <EditableText
                        className='pt-input'
                        value={ firstName }
                        onChange={ onFirstNameChange }
                        onConfirm={ onTextConfirm }
                    />
                    <strong> Last Name: </strong>
                    <EditableText
                        className='pt-input'
                        value={ lastName }
                        onChange={ onLastNameChange }
                        onConfirm={ onTextConfirm }
                    />
                </label>
            </Card>
            <Card>
                <RadioGroup
                    className='rla-radio-group'
                    label='Party Affiliation'
                    onChange={ onPartyChange }
                    selectedValue={ party }>
                    <Radio
                        label='Democratic Party'
                        value='Democratic Party'
                    />
                    <Radio
                        label='Republican Party'
                        value='Republican Party'
                    />
                    <Radio
                        label='Other Party'
                        value='Other Party'
                    />
                    <Radio
                        label='Unaffiliated'
                        value='Unaffiliated'
                    />
                </RadioGroup>
            </Card>
        </div>
    );
};

export default SignInForm;

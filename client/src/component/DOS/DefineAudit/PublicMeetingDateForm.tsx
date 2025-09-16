import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import { DateInput, IDateFormatProps } from '@blueprintjs/datetime';

import { formatLocalDate, parseLocalDate } from 'corla/date';

function blueprintFormatter(): IDateFormatProps {
    return {
        formatDate: d => d ? formatLocalDate(d) : formatLocalDate(new Date()),
        parseDate: s => parseLocalDate(s),
        placeholder: formatLocalDate(new Date()),
    };
}

interface FormProps {
    onChange: (d: Date) => void;
    initDate: Date;
}

interface FormState {
    date: string;
}

class PublicMeetingDateForm extends React.Component<FormProps, FormState> {
    constructor(props: FormProps) {
        super(props);

        this.state = {
            date: formatLocalDate(props.initDate),
        };

        this.onDateChange = this.onDateChange.bind(this);
    }

    public render() {
        return (
            <div>
                <IdleDialog />
                <label>
                    <div className='mb-default'>Public Meeting Date</div>
                    <DateInput { ...blueprintFormatter() }
                               onChange={ this.onDateChange }
                               value={ parseLocalDate(this.state.date) } />
                </label>
            </div>
        );
    }

    private onDateChange(selectedDate: Date) {
        this.setState({
            date: formatLocalDate(selectedDate),
        });

        this.props.onChange(selectedDate);
    }
}

export default PublicMeetingDateForm;

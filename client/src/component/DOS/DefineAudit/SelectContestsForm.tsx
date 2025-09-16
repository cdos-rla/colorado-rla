import * as React from 'react';
import IdleDialog from '../../IdleDialog';

import * as _ from 'lodash';

import {
    Button,
    Checkbox,
    Classes,
    EditableText,
    Icon,
    MenuItem,
} from '@blueprintjs/core';

import { ItemRenderer, Select } from '@blueprintjs/select';

import counties from 'corla/data/counties';

import { naturalSortBy } from 'corla/util';

const auditReasons: DOS.Form.SelectContests.Reason[] = [
    // County contest is first because there are typically more of them, so this
    // arrangement saves clicks.
    { id: 'county_wide_contest', text: 'County Contest' },
    { id: 'state_wide_contest', text: 'State Contest' },
];

const AuditReasonSelect = Select.ofType<DOS.Form.SelectContests.Reason>();

interface RowProps {
    contest: Contest;
    onAuditChange: OnClick;
    onHandCountChange: OnClick;
    onReasonChange: OnClick;
    status: DOS.Form.SelectContests.ContestStatus;
}

const TiedContestRow = (props: RowProps) => {
    const { contest } = props;

    const countyName = counties[contest.countyId].name;

    return (
        <tr>
            {/* TODO: Removed for the time being, see related comments in this file. */ }
            {/* <td>{ countyName }</td> */}
            <td>{ contest.name }</td>
            <td>
                <Checkbox checked={ false }
                          disabled={ true } />
            </td>
            <td>
                Contest cannot be audited due to a reported tie
                {
                    (contest.description === 'IRV')
                        ? ' or assertion generation error.'
                        : '.'
                }
            </td>
        </tr>
    );
};

interface MenuItemData {
    handleClick: OnClick;
    item: DOS.Form.SelectContests.Reason;
    isActive: boolean;
}

const ContestRow = (props: RowProps) => {
    const {
        status,
        contest,
        onAuditChange,
        onHandCountChange,
        onReasonChange,
    } = props;

    if (!status) {
        return null;
    }

    const renderItem: ItemRenderer<DOS.Form.SelectContests.Reason> = (
        item,
        { handleClick, modifiers },
    ) => {
        return (
            <MenuItem
                className={ modifiers.active ? Classes.ACTIVE : '' }
                key={ item.id }
                onClick={ handleClick }
                text={ item.text } />
        );
    };

    const popoverClassName = Classes.MINIMAL;

    const auditReasonSelect = (
        <AuditReasonSelect
            filterable={ false }
            key={ contest.id }
            items={ auditReasons }
            itemRenderer={ renderItem }
            onItemSelect={ onReasonChange }
            popoverProps={ { popoverClassName } }>
            <Button
                text={ status.reason.text }
                rightIcon='double-caret-vertical' />
        </AuditReasonSelect>
    );

    const { handCount } = status;
    const toAudit = !handCount && status.audit;

    const countyName = counties[contest.countyId].name;

    const htmlProps = {
        'aria-label': `Select ${contest.name} for auditing.`,
    };

    return (
        <tr>
            {/* This is a shim for future work where the ui is less
            County/Contest centric and more ContestResult Centric
             removing for now */}
            {/* <td>{ countyName }</td> */}
            <td>{ contest.name }</td>
            <td>
                <Checkbox
                    {...htmlProps}
                    disabled={ handCount }
                    checked={ toAudit }
                    onChange={ onAuditChange } />
            </td>
            <td>
                { status.audit ? auditReasonSelect : '' }
            </td>
        </tr>
    );
};

type SortKey = 'contest' | 'county';

type SortOrder = 'asc' | 'desc';

interface ContestData {
    county: string;
    contest: string;
    props: RowProps;
}

interface FormProps {
    contests: DOS.Contests;
    auditedContests: DOS.AuditedContests;
    forms: DOS.Form.SelectContests.Ref;
    isAuditable: OnClick;
}

interface FormState {
    filter: string;
    form: DOS.Form.SelectContests.FormData;
    order: SortOrder;
    sort: SortKey;
}

class SelectContestsForm extends React.Component<FormProps, FormState> {
    constructor(props: FormProps) {
        super(props);
        const {auditedContests} = props;
        const auditedContestIds = _.map(auditedContests, ac => ac.id);

        this.state = {
            filter: '',
            form: {},
            order: 'asc',
            sort: 'county',
        };

        _.forEach(props.contests, (c, key) => {
            const auditable = props.isAuditable(c.id);

            if (auditable) {
                this.state.form[c.id] = {
                    // by using the dosState we can fix mistakes to selected contests
                    audit: auditedContestIds.includes(c.id),
                    handCount: false,
                    reason: { ...auditReasons[0] },
                };
            }
        });
    }

    public componentWillReceiveProps(nextProps: FormProps) {
        if (!_.isEqual(nextProps.contests, this.props.contests)) {
            this.resetForm(nextProps.contests);
        }
    }

    public render() {
        const { contests, isAuditable } = this.props;

        this.props.forms.selectContestsForm = this.state.form;

        const contestData: ContestData[] = _.map(contests, (c): ContestData => {
            const props = {
                contest: c,
                key: c.id,
                onAuditChange: this.onAuditChange(c),
                onHandCountChange: this.onHandCountChange(c),
                onReasonChange: this.onReasonChange(c),
                status: this.state.form[c.id],
            };

            const countyName = counties[c.countyId].name;

            return {
                contest: c.name,
                county: countyName,
                props,
            };
        });

        const sortedData = naturalSortBy(contestData, (d: ContestData) => {
            return d[this.state.sort];
        });

        if (this.state.order === 'desc') {
            _.reverse(sortedData);
        }

        const filterFunc = (d: ContestData) => {
            const { county, contest } = d;

            const s = this.state.filter.toLowerCase();

            return contest.toLowerCase().includes(s)
                || county.toLowerCase().includes(s);
        };

        const filteredData = _.filter(sortedData, filterFunc);
        const uniqData = _.uniqBy(filteredData, (d: ContestData) => d.contest);

        const contestRows = _.map(uniqData, (d: ContestData) => {
            const props = d.props;
            const { contest } = props;

            const auditable = isAuditable(contest);

            if (auditable) {
                return <ContestRow { ...props } />;
            } else {
                return <TiedContestRow { ...props } />;
            }
        });

        return (
            <div className='mb-default'>
                <IdleDialog />
                <div>
                   <p>According to Colorado statute, at least one statewide contest and
                    one countywide contest must be chosen for audit. The Secretary of State
                    will select other ballot contests for audit if in any particular election
                    there is no statewide contest or a countywide contest in any county. Once
                    these contests for audit have been selected and published, they cannot be
                    changed. The Secretary of State can decide that a contest must witness a
                    full hand count at any time.</p>
                </div>
                <hr />
                <div>
                    <strong>Filter by Contest Name:</strong>
                    <span> </span>
                    <EditableText
                        className='pt-input'
                        minWidth={ 200 }
                        value={ this.state.filter }
                        onChange={ this.onFilterChange } />
                </div>
                <div className='mt-default mb-default'>
                    Click on the "Contest" column name to sort by that
                    column's data. To reverse sort, click on the column name again.
                </div>
                <div>
                    <table className='pt-html-table pt-html-table-bordered pt-small'>
                        <thead>
                            <tr>
                                {/* see comment above */}
                                {/* <th onClick={ this.sortBy('county') }> */}
                                {/* County */}
                                {/* <span> </span> */}
                                {/* { this.sortIconForCol('county') } */}
                                {/* </th> */}
                                <th onClick={ this.sortBy('contest') }>
                                    Contest Name
                                    <span> </span>
                                    { this.sortIconForCol('contest') }
                                </th>
                                <th>Audit?</th>
                                <th>Reason</th>
                            </tr>
                        </thead>
                        <tbody>
                            { contestRows }
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    private sortIconForCol = (col: string) => {
        if (col !== this.state.sort) {
            return null;
        }

        return this.state.order === 'asc'
             ? <Icon icon='sort-asc' />
             : <Icon icon='sort-desc' />;
    }

    private resetForm(contests: DOS.Contests) {
        const form: DOS.Form.SelectContests.FormData = {};

        _.forEach(contests, (c, key) => {
            form[c.id] = {
                audit: false,
                handCount: false,
                reason: { ...auditReasons[0] },
            };
        });

        this.setState({ form });
    }

    private onAuditChange = (contest: Contest) => () => {
        const s = { ...this.state };

        const { audit } = s.form[contest.id];
        s.form[contest.id].audit = !audit;

        this.setState(s);
    }

    private onFilterChange = (filter: string) => {
        this.setState({ filter });
    }

    private onHandCountChange = (contest: Contest) => () => {
        const s = { ...this.state };

        const { handCount } = s.form[contest.id];
        s.form[contest.id].handCount = !handCount;

        this.setState(s);
    }

    private onReasonChange = (contest: Contest) => (reason: DOS.Form.SelectContests.Reason) => {
        const s = { ...this.state };

        s.form[contest.id].reason = { ...reason };

        this.setState(s);
    }

    private reverseOrder() {
        this.setState({order: this.state.order === 'asc' ? 'desc' : 'asc'});
    }

    private sortBy(sort: SortKey) {
        return () => {
            if (this.state.sort === sort) {
                this.reverseOrder();
            } else {
                const order = 'asc';
                this.setState({ sort, order });
            }
        };
    }
}

export default SelectContestsForm;

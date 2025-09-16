import * as React from 'react';

import * as _ from 'lodash';
import IdleDialog from '../../IdleDialogState';

import { Button, Icon, InputGroup, Intent, Tooltip } from '@blueprintjs/core';

import setHandCount from 'corla/action/dos/setHandCount';

import { naturalSortBy } from 'corla/util';

const RemainingToAuditHeader = () => {
    const content =
        'Estimated number of ballots to audit to meet risk limit.';

    return (
        <Tooltip
            content={ content }>
            <div>
                <span>Remaining to Audit </span>
                <Icon icon='help' />
            </div>
        </Tooltip>
    );
};

interface ButtonProps {
    contest: Contest;
}

const HandCountButton = (props: ButtonProps) => {
    const { contest } = props;

    const onClick = () => {
        const msg = `You have selected "${contest.name}" to hand count - are you sure you want to proceed?`
                  + ` This action cannot be undone if you choose to hand count "${contest.name}."`;

        if (confirm(msg)) {
            setHandCount(contest.id);
        }
    };

    return (
        <Button intent={ Intent.PRIMARY }
                onClick={ onClick }>
            <Icon icon='hand-up' />
        </Button>
    );
};

type SortKey = 'name'
             | 'discrepancyCount'
             | 'estimatedBallotsToAudit';

type SortOrder = 'asc' | 'desc';

interface RowData {
    name: string;
    discrepancyCount: number;
    estimatedBallotsToAudit: number;
    contest: Contest;
}

interface UpdatesProps {
    contests: DOS.Contests;
    seed: string;
    dosState: DOS.AppState;
}

interface UpdatesState {
    filter: string;
    order: SortOrder;
    sort: SortKey;
}

class ContestUpdates extends React.Component<UpdatesProps, UpdatesState> {
    public constructor(props: UpdatesProps) {
        super(props);

        this.state = {
            filter: '',
            order: 'asc',
            sort: 'name',
        };

        this.onFilterChange = this.onFilterChange.bind(this);
        this.reverseOrder = this.reverseOrder.bind(this);
        this.rowFilterName = this.rowFilterName.bind(this);
        this.sortBy = this.sortBy.bind(this);
        this.sortClassForCol = this.sortClassForCol.bind(this);
        this.sortIconForCol = this.sortIconForCol.bind(this);
    }

    public render() {
        const { contests, dosState, seed } = this.props;

        const selectedContests: DOS.Contests =
            _.values(_.pick(contests, _.keys(dosState.auditedContests)));

        const rowData: RowData[] = _.map(selectedContests, (c): RowData => {
            const discrepancyCount: number = _.sum(_.values(dosState.discrepancyCounts![c.id]));
            const estimatedBallotsToAudit = dosState.estimatedBallotsToAudit![c.id];

            return {
                contest: c,
                discrepancyCount,
                estimatedBallotsToAudit,
                name: c.name,
            };
        });

        const selector = (row: RowData) => row[this.state.sort];

        const filteredData = _.filter(rowData, this.rowFilterName);

        const sortedData = naturalSortBy(filteredData, selector);

        if (this.state.order === 'desc') {
            _.reverse(sortedData);
        }

        const contestStatuses = _.map(sortedData, row => {
            const {
                name,
                discrepancyCount,
                estimatedBallotsToAudit,
                contest,
            } = row;

            return (
                <tr key={ contest.id }>
                    <td className={ this.sortClassForCol('name') + ' ellipsize' }><span>{ name }</span></td>
                    <td className={ this.sortClassForCol('discrepancyCount') }>{ discrepancyCount }</td>
                    <td className={ this.sortClassForCol('estimatedBallotsToAudit') }>{ estimatedBallotsToAudit }</td>
                    <td><HandCountButton contest={ contest } /></td>
                </tr>
            );
        });

        return (
            <div>
                <IdleDialog />
                <div className='state-dashboard-updates-preface'>
                    <div className='state-dashboard-updates-preface-description'>
                        <h1 className='smallHeader'>Contest Updates</h1>
                        <p>
                            Click on a column name to sort by that column’s data. To
                            reverse sort, click on the column name again.
                        </p>
                    </div>
                    <div className='state-dashboard-updates-preface-search'>
                        <InputGroup leftIcon='search'
                                    type='search'
                                    placeholder='Filter by contest name'
                                    value={ this.state.filter }
                                    onChange={ this.onFilterChange } />
                    </div>
                </div>
                <table className='pt-html-table pt-html-table-striped rla-table mt-default'>
                    <thead>
                        <tr>
                            <th className={ this.sortClassForCol('name') }
                                onClick={ this.sortBy('name') }>
                                <div className='rla-table-sortable-wrapper'>
                                    Name
                                    { this.sortIconForCol('name') }
                                </div>
                            </th>
                            <th className={ this.sortClassForCol('discrepancyCount') }
                                onClick={ this.sortBy('discrepancyCount') }>
                                <div className='rla-table-sortable-wrapper'>
                                    Discrepancies
                                    { this.sortIconForCol('discrepancyCount') }
                                </div>
                            </th>
                            <th className={ this.sortClassForCol('estimatedBallotsToAudit') }
                                onClick={ this.sortBy('estimatedBallotsToAudit') }>
                                <div className='rla-table-sortable-wrapper'>
                                    Est. Ballots to Audit
                                   { this.sortIconForCol('estimatedBallotsToAudit') }
                                </div>
                            </th>
                            <th>Hand Count</th>
                        </tr>
                    </thead>
                    <tbody>{ ...contestStatuses }</tbody>
                </table>
            </div>
        );
    }

    private rowFilterName(row: RowData) {
        const contestName = row.name.toLowerCase();
        const s = this.state.filter.toLowerCase();

        return contestName.includes(s);
    }

    private sortClassForCol(col: string) {
        return col === this.state.sort ? 'is-sorted' : '';
    }

    private sortIconForCol(col: string) {
        if (col !== this.state.sort) {
            return <Icon icon='double-caret-vertical' />;
        }

        return this.state.order === 'asc'
             ? <Icon icon='symbol-triangle-down' />
             : <Icon icon='symbol-triangle-up' />;
    }

    private onFilterChange(e: React.ChangeEvent<HTMLInputElement>) {
        this.setState({ filter: e.target.value });
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

    private reverseOrder() {
        this.setState({ order: this.state.order === 'asc' ? 'desc' : 'asc' });
    }
}

export default ContestUpdates;

import * as React from 'react';

import * as _ from 'lodash';

import { Breadcrumb, Button, Card, Intent } from '@blueprintjs/core';

import DOSLayout from 'corla/component/DOSLayout';
import counties from 'corla/data/counties';

import { findBestMatch } from 'string-similarity';

/**
 * The maximum percentage match at or above which a contest will be assumed to
 * match a given canonical contest.
 *
 * The algorithm used is not defined, so this may need to change if the
 * algorithm is changed.
 */
const MIN_MATCH_THRESHOLD = 0.67;

/**
 * Returns the default selection for `name` given `canonicalNames` to choose
 * from.
 *
 * The default selection will be the empty string if there was not a better
 * choice in `canonicalNames` for the given contest name.
 */
const defaultCanonicalName = (
    name: string,
    canonicalNames: string[],
): string => {
    const loweredName = name.toLowerCase();
    const loweredCanonicals = _.map(canonicalNames, s => s.toLowerCase());

    const { bestMatch, bestMatchIndex } = findBestMatch(
        loweredName,
        loweredCanonicals,
    );

    if (bestMatch.rating < MIN_MATCH_THRESHOLD) {
        return '';
    }

    return canonicalNames[bestMatchIndex];
};

const Breadcrumbs = () => (
    <ul className='pt-breadcrumbs mb-default'>
        <li><Breadcrumb href='/sos' text='SoS' />></li>
        <li><Breadcrumb href='/sos/audit' text='Audit Admin' /></li>
        <li><Breadcrumb className='pt-breadcrumb-current' text='Standardize Contest Names' /></li>
    </ul>
);

interface UpdateFormMessage {
    id: number;
    name: string;
}

interface TableProps {
    contestsIgnoringManifests: DOS.Contests;
    canonicalContests: DOS.CanonicalContests;
    formData: DOS.Form.StandardizeContests.FormData;
    updateFormData: (msg: UpdateFormMessage) => void;
}

const StandardizeContestsTable = (props: TableProps) => {
    const { canonicalContests, contestsIgnoringManifests, formData, updateFormData } = props;

    return (
        <table className='pt-html-table pt-html-table-striped'>
            <thead>
                <tr>
                    <th>County</th>
                    <th>Current Contest Name</th>
                    <th>Standardized Contest Name</th>
                </tr>
            </thead>
            <ContestBody contestsIgnoringManifests={ contestsIgnoringManifests }
                         canonicalContests={ canonicalContests }
                         formData={ formData }
                         updateFormData={ updateFormData } />
        </table>
    );
};

interface BodyProps {
    contestsIgnoringManifests: DOS.Contests;
    canonicalContests: DOS.CanonicalContests;
    formData: DOS.Form.StandardizeContests.FormData;
    updateFormData: (msg: UpdateFormMessage) => void;
}

const ContestBody = (props: BodyProps) => {
    const { canonicalContests, contestsIgnoringManifests, formData, updateFormData } = props;

    const rows = _.map(contestsIgnoringManifests, c => {
        return <ContestRow key={ c.id }
                           contest={ c }
                           canonicalContests={ canonicalContests }
                           formData={ formData }
                           updateFormData={ updateFormData } />;
    });

    return (
      <tbody>{ rows }</tbody>
    );
};

interface ContestRowProps {
    contest: Contest;
    canonicalContests: DOS.CanonicalContests;
    formData: DOS.Form.StandardizeContests.FormData;
    updateFormData: (msg: UpdateFormMessage) => void;
}

const ContestRow = (props: ContestRowProps) => {
    const { canonicalContests, contest, formData, updateFormData } = props;
    const countyName = counties[contest.countyId].name;
    const standards = canonicalContests[countyName];

    const changeHandler = (e: React.FormEvent<HTMLSelectElement>) => {
        const v = e.currentTarget.value;

        updateFormData({ id: contest.id, name: v });
    };

    return (
        <tr>
            <td>{ counties[contest.countyId].name }</td>
            <td>{ contest.name }</td>
            <td>
                <form>
                    <select className='max-width-select'
                            name={ String(contest.id) }
                            aria-label={ `Standardize Contest Name For ${contest.name}` }
                            onChange={ changeHandler }
                            value={ _.get(formData, `${contest.id}.name`, '') }>
                        <option value=''>-- No change --</option>
                        {
                          _.map(standards, n => <option value={ n }>{ n }</option>)
                        }
                    </select>
                </form>
            </td>
        </tr>
    );
};

interface PageProps {
    areCVRsLoaded: boolean;
    canonicalContests: DOS.CanonicalContests;
    contestsIgnoringManifests: DOS.Contests;
    forward: (x: DOS.Form.StandardizeContests.FormData) => void;
    back: () => void;
}

interface PageState {
    formData: DOS.Form.StandardizeContests.FormData;
}

class StandardizeContestsPage extends React.Component<PageProps, PageState> {
    public constructor(props: PageProps) {
        super(props);

        this.state = {
            formData: {},
        };

        this.updateFormData = this.updateFormData.bind(this);
    }

    /*
     * Run when contest and canonical contest data loads in and populate the
     * form state with initial contest guesses.
     */
    public componentDidUpdate(prevProps: PageProps, prevState: PageState) {
        if (!prevProps.areCVRsLoaded && this.props.areCVRsLoaded) {
            const {
                canonicalContests,
                contestsIgnoringManifests,
            } = this.props;

            const formData: DOS.Form.StandardizeContests.FormData = {};

            _.forEach(contestsIgnoringManifests, contest => {
                const countyName = counties[contest.countyId].name;
                const standards = canonicalContests[countyName];

                const defaultName = defaultCanonicalName(contest.name, standards);

                if (!_.isEmpty(defaultName)) {
                    formData[contest.id] = { name: defaultName };
                }
            });

            this.setState({ formData });
        }
    }

    public render() {
        const {
            areCVRsLoaded,
            back,
            canonicalContests,
            contestsIgnoringManifests,
            forward,
        } = this.props;

        let main = null;

        if (areCVRsLoaded) {
            main =
                <div>
                    <Breadcrumbs />
                    <h2 className='mb-default'>Standardize Contest Names</h2>
                    <Card>
                        <p>
                            Contest names must be standardized to group records
                            correctly across jurisdictions. Below is a list of
                            contests that do not match the standardized contest
                            names provided by the state. For each of the contests
                            listed, please choose the appropriate standardized
                            version from the options provided, then save your
                            selections and move forward.
                        </p>

                        <StandardizeContestsTable canonicalContests={ canonicalContests }
                                                  contestsIgnoringManifests={ contestsIgnoringManifests }
                                                  formData={ this.state.formData }
                                                  updateFormData={ this.updateFormData } />
                    </Card>
                    <Button onClick={ back }>Back</Button>
                    <Button className='ml-default'
                            intent={ Intent.PRIMARY }
                            onClick={ () => forward(this.state.formData) }>
                        Save & Next
                    </Button>
                </div>;
        } else {
            main =
                <div>
                    <Breadcrumbs />
                    <h2 className='mb-default'>Standardize Contest Names</h2>
                    <div  className='mb-default'>
                        <Card>
                            Waiting for counties to upload contest data.
                        </Card>
                    </div>
                    <Button onClick={ back }>Back</Button>
                    <Button className='ml-default'
                            disabled
                            intent={ Intent.PRIMARY }>
                        Save & Next
                    </Button>
                </div>;
        }

        return <DOSLayout main={ main } />;
    }

    private updateFormData(msg: UpdateFormMessage) {
        const { id, name } = msg;

        const formData = this.state.formData;

        if (_.isEmpty(name)) {
            delete formData[id];
        } else {
            formData[id] = { name };
        }

        this.setState({ formData });
    }
}

export default StandardizeContestsPage;

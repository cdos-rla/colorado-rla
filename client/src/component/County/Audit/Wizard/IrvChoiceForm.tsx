import * as _ from 'lodash';
import * as React from 'react';

import {Checkbox} from '@blueprintjs/core';

interface IrvChoiceFormRanking {
    name: string;
    rank: number;
    value: string;
    label: string;
}

const IrvChoiceForm = (props: ChoicesProps) => {
    const {
        choices,
        marks,
        noConsensus,
        updateBallotMarks,
        description,
    } = props;

    const numberOfRankChoices = choices.length;

    function getNoRankButton(candidateName: string) {
        function candidateHasRanking() {
            for (let i = 0; i < numberOfRankChoices; i++) {
                if (marks.choices[candidateName + `(${i + 1})`]) {
                    return true;
                }
            }
            return false;
        }

        function uncheckCandidateRankings(e: React.ChangeEvent<HTMLInputElement>) {
            const checkbox = e.target;

            if (checkbox.checked) {
                // Uncheck any other rankings in that candidate's row
                for (let i = 0; i < numberOfRankChoices; i++) {
                    updateBallotMarks({ choices: { [candidateName + `(${i + 1})`]: false } });
                }
            }
        }

        return (
            <div className='contest-choice-selection'>
                <Checkbox
                    className='rla-contest-choice'
                    disabled={noConsensus}
                    checked={!candidateHasRanking()}
                    onChange={uncheckCandidateRankings}>
                    <span className='choice-name'>No Rank</span>
                </Checkbox>
            </div>
        );
    }

    function updateChoiceByRanking(irvChoice: IrvChoiceFormRanking) {
        function updateChoice(e: React.ChangeEvent<HTMLInputElement>) {
            const checkbox = e.target;

            updateBallotMarks({ choices: { [irvChoice.value]: checkbox.checked } });
        }

        return updateChoice;
    }

    function getRankings(candidateName: string, candidateDescription: string) {
        const rankingOptions = [];

        rankingOptions.push(
            <div className='rla-contest-choice-name'>
                <b>{candidateName}</b>
                {candidateDescription ? <br /> : null}
                {candidateDescription}
            </div>,
        );

        for (let i = 0; i < numberOfRankChoices; i++) {
            const rank = i + 1;
            const ranking: IrvChoiceFormRanking = {label: (rank).toString(), name: candidateName, rank,
                value: candidateName + `(${rank})`};
            const checked = marks.choices[ranking.value];

            rankingOptions.push(
                <div className='contest-choice-selection'>
                    <Checkbox
                        className='rla-contest-choice'
                        key={ranking.value}
                        value={ranking.value}
                        disabled={noConsensus}
                        checked={checked || false}
                        onChange={updateChoiceByRanking(ranking)}>
                        <span className='choice-name'>{ranking.label}</span>
                    </Checkbox>
                </div>,
            );
        }

        rankingOptions.push(getNoRankButton(candidateName));

        return rankingOptions;
    }

    return (
        <div className='irv-contest-choice-grid'
             style={{gridTemplateColumns: `repeat(${numberOfRankChoices + 2}, 1fr)`}}>
            <div style={{borderRight: '1px solid #ddd'}}>
                Candidate
            </div>
            <div style={{gridColumn: `2 / ${numberOfRankChoices + 3}`}}>
                Ranked Vote Choice
            </div>
            {_.map(choices, choice => {
                return (
                    getRankings(choice.name, choice.description)
                );
            })}
        </div>
    );
};

export default IrvChoiceForm;

declare namespace DOS {
    interface AppState {
        asm: DOS.ASMState;
        auditedContests: DOS.AuditedContests;
        auditTypes: ContestAuditTypes;
        contests: DOS.Contests;
        contestsIgnoringManifests: DOS.Contests;
        countyStatus: DOS.CountyStatuses;
        canonicalContests?: DOS.CanonicalContests;
        canonicalChoices?: DOS.CanonicalChoices;
        discrepancyCounts?: DOS.DiscrepancyCounts;
        estimatedBallotsToAudit?: DOS.EstimatedBallotsToAudit;
        election?: Election;
        publicMeetingDate?: Date;
        riskLimit?: number;
        seed?: string;
        settingAuditInfo?: boolean;
        standardizingContests?: boolean;
        generatingAssertions?: boolean;
        assertionsGenerated?: boolean;
        generateAssertionsSummaries: DOS.GenerateAssertionsSummaryWithCounty[];
        assertionGenerationStatuses?: DOS.AssertionGenerationStatuses;
        raireServiceStatus: string;
        type: 'DOS';
    }

    interface AssertionStatus {
        contestName: string;
        succeeded: boolean;
        retry: boolean;
    }

    interface AssertionGenerationStatuses extends Array<AssertionStatus> {}

    interface EstimatedBallotsToAudit {
        [contestId: number]: number;
    }

    interface DiscrepancyCount {
        audited: number;
        unaudited: number;
    }

    interface DiscrepancyCounts {
        [countyId: number]: DOS.DiscrepancyCount;
    }

    type DiscrepancyType = '-2' | '-1' | '0' | '1' | '2';

    interface DiscrepancyCount {
        // The index type is really limited to `DiscrepancyType`.
        [type: string]: number;
    }

    // Exactly matches the structure of the same name in the server.
    interface GenerateAssertionsSummary {
        id: number;
        contestName: string;
        winner: string;
        error: string;
        warning: string;
        message: string;
    }

    // Exactly matches the record of the same name in the server.
    interface GenerateAssertionsSummaryWithCounty {
        summary: GenerateAssertionsSummary;
        countyName: string;
    }

    interface CanonicalContests {
        [countyId: string]: string[];
    }

    interface CanonicalChoices {
        [contestName: string]: string[];
    }

    interface CountyStatuses {
        [countyId: number]: DOS.CountyStatus;
    }

    interface CountyStatus {
        asmState: County.ASMState;
        auditBoards: AuditBoards;
        auditBoardASMState: AuditBoardASMState;
        auditBoardCount?: number;
        auditedBallotCount: number;
        ballotManifest?: UploadedFile;
        ballotsRemainingInRound: number;
        currentRound: Round;
        cvrExport?: UploadedFile;
        disagreementCount: number;
        discrepancyCount: DiscrepancyCount;
        estimatedBallotsToAudit: number;
        id: number;
        manifestTimestamp: any;
        rounds: any;
        status: any;
    }

    interface Contests {
        [contestId: number]: Contest;
    }

    interface AuditedContests {
        [contestId: number]: AuditedContest;
    }

    type ASMState
        = 'DOS_INITIAL_STATE'
        | 'DOS_AUTHENTICATED'
        | 'RISK_LIMITS_SET'
        | 'CONTESTS_TO_AUDIT_IDENTIFIED'
        | 'DATA_TO_AUDIT_PUBLISHED'
        | 'RANDOM_SEED_PUBLISHED'
        | 'BALLOT_ORDER_DEFINED'
        | 'AUDIT_READY_TO_START'
        | 'DOS_AUDIT_ONGOING'
        | 'DOS_ROUND_COMPLETE'
        | 'DOS_AUDIT_COMPLETE'
        | 'AUDIT_RESULTS_PUBLISHED';

    interface AuditInfo {
        election: Election;
        publicMeetingDate: Date;
        riskLimit: number;
        uploadFiles?: string[];
    }

    namespace Form {
        namespace SelectContests {
            interface Ref {
                selectContestsForm?: DOS.Form.SelectContests.FormData;
            }

            type ReasonId = 'county_wide_contest' | 'state_wide_contest';

            interface Reason {
                id: ReasonId;
                text: string;
            }

            interface ContestStatus {
                audit: boolean;
                handCount: boolean;
                reason: Reason;
            }

            interface FormData {
                [contestId: number]: ContestStatus;
            }
        }

        namespace StandardizeChoices {
            interface FormData {
                [contestId: number]: FormRow[];
            }

            interface FormRow {
                existingChoice: string;
                newChoice: string;
            }

            interface Row {
                choiceName: string;
                choices: string[];
                contestId: number;
                contestName: string;
                countyName: string;
            }
        }

        namespace StandardizeContests {
            interface FormData {
                [contestId: number]: {
                    name: string;
                };
            }
        }
    }
}

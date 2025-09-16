declare namespace Action {
    interface SubmitData<S, R> {
        sent: S;
        received: R;
    }

    type App
        = BallotNotFoundFail
        | BallotNotFoundNetworkFail
        | BallotNotFoundOk
        | BallotNotFoundSend
        | CountyCVRImportFailNotice
        | CountyCVRImportOkNotice
        | CountyDashboardRefreshOk
        | CountyFetchContestsOk
        | CountyFetchCvrOk
        | CountyLoginOk
        | DOSDashboardRefreshOk
        | DOSDeleteFileOk
        | DOSFetchContestsOk
        | DOSFetchContestsIgnoreManifestsOk
        | DOSLoginOk
        | DeleteFileOK
        | FetchAuditBoardASMStateOk
        | FetchCountyASMStateOk
        | FetchCvrsToAuditOk
        | FetchDOSASMStateOk
        | FinalReviewComplete
        | ImportCvrExportOk
        | Login1FOk
        | ReAuditCvr
        | SelectContestsForAuditOk
        | SetAuditBoard
        | SetAuditInfo
        | SetAuditInfoFail
        | SetAuditInfoNetworkFail
        | SetAuditInfoOk
        | StandardizeContestsForAudit
        | StandardizeContestsForAuditFail
        | StandardizeContestsForAuditNetworkFail
        | StandardizeContestsForAuditOk
        | GenerateAssertions
        | GenerateAssertionsFail
        | GenerateAssertionsNetworkFail
        | GenerateAssertionsOk
        | UpdateAcvrForm
        | UploadBallotManifestOk
        | UploadAcvrFail
        | UploadAcvrNetworkFail
        | UploadAcvrOk
        | UploadAcvrSend
        | UploadCvrExportOk
        | UploadRandomSeedOk
        | UploadingBallotManifest
        | UploadingCvrExport;

    interface BallotNotFoundFail {
        type: 'BALLOT_NOT_FOUND_FAIL';
        data: any;
    }

    interface BallotNotFoundNetworkFail {
        type: 'BALLOT_NOT_FOUND_NETWORK_FAIL';
        data: any;
    }

    interface BallotNotFoundOk {
        type: 'BALLOT_NOT_FOUND_OK';
        data: any;
    }

    interface BallotNotFoundSend {
        type: 'BALLOT_NOT_FOUND_SEND';
        data: any;
    }

    interface CountyCVRImportFailNotice {
        type: 'COUNTY_CVR_IMPORT_FAIL_NOTICE';
        data: any;
    }

    interface CountyCVRImportOkNotice {
        type: 'COUNTY_CVR_IMPORT_OK_NOTICE';
        data: any;
    }

    interface CountyDashboardRefreshOk {
        type: 'COUNTY_DASHBOARD_REFRESH_OK';
        data: any;
    }

    interface CountyFetchContestsOk {
        type: 'COUNTY_FETCH_CONTESTS_OK';
        data: any;
    }

    interface CountyFetchCvrOk {
        type: 'COUNTY_FETCH_CVR_OK';
        data: any;
    }

    interface CountyLoginOk {
        type: 'COUNTY_LOGIN_OK';
        data: any;
    }

    interface DOSDashboardRefreshOk {
        type: 'DOS_DASHBOARD_REFRESH_OK';
        data: any;
    }

    interface DOSDeleteFileOk {
        type: 'DOS_DELETE_FILE_OK';
        data: any;
    }

    interface DOSFetchContestsOk {
        type: 'DOS_FETCH_CONTESTS_OK';
        data: any;
    }

    interface DOSFetchContestsIgnoreManifestsOk {
        type: 'DOS_FETCH_CONTESTS_IGNORE_MANIFESTS_OK';
        data: any;
    }

    interface DOSLoginOk {
        type: 'DOS_LOGIN_OK';
        data: any;
    }

    interface FetchAuditBoardASMStateOk {
        type: 'FETCH_AUDIT_BOARD_ASM_STATE_OK';
        data: any;
    }

    interface FetchCountyASMStateOk {
        type: 'FETCH_COUNTY_ASM_STATE_OK';
        data: JSON.FetchCountyASMStateOk;
    }

    interface FetchCvrsToAuditOk {
        type: 'FETCH_CVRS_TO_AUDIT_OK';
        data: any;
    }

    interface FetchDOSASMStateOk {
        type: 'FETCH_DOS_ASM_STATE_OK';
        data: any;
    }

    interface DeleteFileOK {
        type: 'DELETE_FILE_OK';
        data: any;
    }

    interface FinalReviewComplete {
        type: 'FINAL_REVIEW_COMPLETE';
        data: any;
    }

    interface ImportCvrExportOk {
        type: 'IMPORT_CVR_EXPORT_OK';
        data: any;
    }

    interface Login1FOk {
        type: 'LOGIN_1F_OK';
        data: any;
    }

    interface SelectContestsForAuditOk {
        type: 'SELECT_CONTESTS_FOR_AUDIT_OK';
        data: any;
    }

    interface SetAuditBoard {
        type: 'SET_AUDIT_BOARD';
        data: {
            auditBoardIndex: number;
        };
    }

    interface SetAuditInfo {
        type: 'SET_AUDIT_INFO';
        data: any;
    }

    interface SetAuditInfoFail {
        type: 'SET_AUDIT_INFO_FAIL';
        data: any;
    }

    interface SetAuditInfoNetworkFail {
        type: 'SET_AUDIT_INFO_NETWORK_FAIL';
        data: any;
    }

    interface SetAuditInfoOk {
        type: 'SET_AUDIT_INFO_OK';
        data: any;
    }

    interface StandardizeContestsForAudit {
        type: 'STANDARDIZE_CONTESTS_FOR_AUDIT';
        data: any;
    }

    interface StandardizeContestsForAuditFail {
        type: 'STANDARDIZE_CONTESTS_FOR_AUDIT_FAIL';
        data: any;
    }

    interface StandardizeContestsForAuditNetworkFail {
        type: 'STANDARDIZE_CONTESTS_FOR_AUDIT_NETWORK_FAIL';
        data: any;
    }

    interface StandardizeContestsForAuditOk {
        type: 'STANDARDIZE_CONTESTS_FOR_AUDIT_OK';
        data: any;
    }

    interface GenerateAssertions {
        type: 'GENERATE_ASSERTIONS';
        data: any;
    }

    interface GenerateAssertionsFail {
        type: 'GENERATE_ASSERTIONS_FAIL';
        data: any;
    }

    interface GenerateAssertionsNetworkFail {
        type: 'GENERATE_ASSERTIONS_NETWORK_FAIL';
        data: any;
    }

    interface GenerateAssertionsOk {
        type: 'GENERATE_ASSERTIONS_OK';
        data: any;
    }

    interface ReAuditCvr {
        type: 'RE_AUDIT_CVR';
        data: {
            comment: string;
            cvrId: number;
        };
    }

    interface UpdateAcvrForm {
        type: 'UPDATE_ACVR_FORM';
        data: any;
    }

    interface UploadBallotManifestOk {
        type: 'UPLOAD_BALLOT_MANIFEST_OK';
        data: any;
    }

    interface UploadAcvrFail {
        type: 'UPLOAD_ACVR_FAIL';
        data: any;
    }

    interface UploadAcvrNetworkFail {
        type: 'UPLOAD_ACVR_NETWORK_FAIL';
        data: any;
    }

    interface UploadAcvrOk {
        type: 'UPLOAD_ACVR_OK';
        data: any;
    }

    interface UploadAcvrSend {
        type: 'UPLOAD_ACVR_SEND';
        data: any;
    }

    interface UploadCvrExportOk {
        type: 'UPLOAD_CVR_EXPORT_OK';
        data: any;
    }

    interface UploadRandomSeedOk {
        type: 'UPLOAD_RANDOM_SEED_OK';
        data: any;
    }

    interface UploadingBallotManifest {
        type: 'UPLOADING_BALLOT_MANIFEST';
        data: any;
    }

    interface UploadingCvrExport {
        type: 'UPLOADING_CVR_EXPORT';
        data: any;
    }
}

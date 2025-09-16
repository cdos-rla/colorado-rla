/*
 * Free & Fair Colorado RLA System
 *
 * @title ColoradoRLA
 * @created Jul 27, 2017
 * @copyright 2017 Colorado Department of State
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * @creator Daniel M. Zimmerman <dmz@freeandfair.us>
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.endpoint;

import static us.freeandfair.corla.asm.ASMEvent.AuditBoardDashboardEvent.*;

import java.time.Instant;

import javax.persistence.PersistenceException;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.model.vote.IRVBallotInterpretation;
import au.org.democracydevelopers.corla.model.vote.IRVChoices;
import au.org.democracydevelopers.corla.model.vote.IRVParsingException;
import com.google.gson.JsonParseException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import spark.Request;
import spark.Response;

import us.freeandfair.corla.Main;
import us.freeandfair.corla.asm.ASMEvent;
import us.freeandfair.corla.controller.ComparisonAuditController;
import us.freeandfair.corla.json.SubmittedAuditCVR;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.model.CountyDashboard;
import us.freeandfair.corla.persistence.Persistence;

/**
 * The "audit CVR upload" endpoint.
 *
 * @author Daniel M. Zimmerman <dmz@freeandfair.us>
 * @version 1.0.0
 * Edits by Vanessa Teague to incorporate recording of IRV invalid ballot interpretations. These
 * are made only if the upload is OK but the IRV ballot is not a valid list of IRV preferences.
 * These are stored in the IRVBallotInterpretation table.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.CyclomaticComplexity"})
// TODO: consider rewriting along the same lines as CVRExportUpload
public class ACVRUpload extends AbstractAuditBoardDashboardEndpoint {
  /**
   * Class-wide logger
   */
  public static final Logger LOGGER =
    LogManager.getLogger(ACVRUpload.class);

  /**
   * The event we will return for the ASM.
   */
  private final ThreadLocal<ASMEvent> my_event = new ThreadLocal<ASMEvent>();

  /**
   * {@inheritDoc}
   */
  @Override
  public EndpointType endpointType() {
    return EndpointType.POST;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String endpointName() {
    return "/upload-audit-cvr";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ASMEvent endpointEvent() {
    return my_event.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {
    my_event.set(null);
  }

  /** build new acvr **/
  public CastVoteRecord buildNewAcvr(final SubmittedAuditCVR submission,
                                     final CastVoteRecord cvr,
                                     final CountyDashboard cdb) {
    final CastVoteRecord s = submission.auditCVR();
    final CastVoteRecord newAcvr =
      new CastVoteRecord(RecordType.AUDITOR_ENTERED,
                         Instant.now(),
                         s.countyID(), s.cvrNumber(), null, s.scannerID(),
                         s.batchID(), s.recordID(), s.imprintedID(),
                         s.ballotType(), s.contestInfo());
    newAcvr.setAuditBoardIndex(submission.getAuditBoardIndex());
    newAcvr.setCvrId(submission.cvrID());
    newAcvr.setRoundNumber(cdb.currentRound().number());
    newAcvr.setRand(cvr.getRand());

    return newAcvr;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings({"PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity"})
  @Override
  public String endpointBody(final Request the_request, final Response the_response) {
    final CastVoteRecord newAcvr;

    try {
      final SubmittedAuditCVR submission =
        Main.GSON.fromJson(the_request.body(), SubmittedAuditCVR.class);
      if (submission.auditCVR() == null || submission.cvrID() == null) {
        LOGGER.error("empty audit CVR upload");
        badDataContents(the_response, "empty audit CVR upload");
      } else {
        // FIXME extract-fn: handleACVR
        final CountyDashboard cdb =
            Persistence.getByID(Main.authentication().authenticatedCounty(the_request).id(),
                                CountyDashboard.class);
        if (cdb == null) {
          LOGGER.error("could not get audit board dashboard");
          serverError(the_response, "Could not save ACVR to dashboard");
        } else if (submission.isReaudit()) {

          final CastVoteRecord cvr = Persistence.getByID(submission.cvrID(),
                                                         CastVoteRecord.class);
          newAcvr = buildNewAcvr(submission, cvr, cdb);

          if (ComparisonAuditController.reaudit(cdb,cvr,newAcvr, submission.getComment())) {
            // Make a record of any invalid IRV ballot interpretations.
            recordIRVBallotInterpretations(submission.auditCVR(), RecordType.REAUDITED,
                submission.auditCVR().cvrNumber(), submission.auditCVR().imprintedID());
            // Return success response for reaudit.
            ok(the_response, "ACVR reaudited");
          } else {
            LOGGER.error("CVR has not previously been audited");
            invariantViolation(the_response, "CVR has not previously been audited");
          }

        } else if (cdb.ballotsRemainingInCurrentRound() > 0) {

          // Now we have a thing we can give our controller, maybe.
          final CastVoteRecord cvr = Persistence.getByID(submission.cvrID(),
                                                         CastVoteRecord.class);

          // FIXME extract-fn: setupACVR
          final CastVoteRecord acvr = submission.auditCVR();
          acvr.setID(null);

          newAcvr = buildNewAcvr(submission, cvr, cdb);

          Persistence.saveOrUpdate(newAcvr);
          LOGGER.info("Audit CVR for CVR id " + submission.cvrID() +
                           " parsed and stored as id " + newAcvr.id());

          if (cvr == null) {
            LOGGER.error("could not find original CVR");
            // FIXME throw and push HTTP response up.
            this.badDataContents(the_response, "could not find original CVR");
          } else {

            // The positive outcome is a little hard to notice in all the noise
            // FIXME return an appropriate value and push HTTP response up
            if (ComparisonAuditController.submitAuditCVR(cdb, cvr, newAcvr)) {
              LOGGER.debug("ACVR OK");

              // Make a record of any invalid IRV ballot interpretations.
              recordIRVBallotInterpretations(submission.auditCVR(), RecordType.AUDITOR_ENTERED,
                  submission.auditCVR().cvrNumber(), submission.auditCVR().imprintedID());
              Persistence.saveOrUpdate(cdb);
              ok(the_response, "ACVR submitted");
            } else {
              // FIXME throw and push HTTP response up
              LOGGER.error("invalid audit CVR uploaded");
              badDataContents(the_response, "invalid audit CVR uploaded");
            }
          }
        } else {
          // FIXME throw and push HTTP response up
          LOGGER.error("ballot submission with no remaining ballots in round");
          invariantViolation(the_response,
                             "ballot submission with no remaining ballots in round");
        }

        // don't advance state machine if reaudit
        if (!submission.isReaudit()) {
          if (cdb.ballotsRemainingInCurrentRound() == 0) {
            // TODO this has to happen before we can say RISK_LIMIT_ACHIEVED!
            LOGGER.debug("The round is over and set ROUND_COMPLETE_EVENT");
            my_event.set(ROUND_COMPLETE_EVENT);
          } else {
            LOGGER.debug("Some ballots remaining according to the CDB: REPORT_MARKING_EVENT");
            my_event.set(REPORT_MARKINGS_EVENT);
          }
        }
      } // extract-fn: handleACVR will have returned some value or thrown
    } catch (final JsonParseException | IRVParsingException e) {
      LOGGER.error("malformed audit CVR upload");
      badDataContents(the_response, "malformed audit CVR upload");
    } catch (final PersistenceException e) {
      LOGGER.error("could not save audit CVR");
      serverError(the_response, "Unable to save audit CVR");
    }
    return my_endpoint_result.get();
  }

  /**
   * Takes an uploaded audit cvr, iterates through all the IRV contests in it, and for each one,
   * examines whether the preferences are valid IRV choices (without skips, overvotes, repeated
   * preferences). If not, it persists a record of the valid interpretation.
   * The actual interpretation of the audit cvr, for audit computations, is made in
   * CVRContestInfoJsonAdapter - this function is just for record keeping.
   * @param cvr                  the uploaded audit cvr.
   * @param recordType           should be either AUDITOR_ENTERED or REAUDIT.
   * @param cvrNumber            the cvr number (as printed on the csv output).
   * @param imprintedID          the imprinted ID.
   * @throws IRVParsingException if the choices cannot be interpreted as IRV choices, i.e. of the
   *                             form name(r) for integer rank r. Note this is separate from whether
   *                             they are a valid list of IRV choices.
   */
  private void recordIRVBallotInterpretations(final CastVoteRecord cvr, final RecordType recordType,
                               final int cvrNumber, final String imprintedID) throws IRVParsingException {
    for(CVRContestInfo contestInfo : cvr.contestInfo()) {
      if(contestInfo.contest().description().equals(ContestType.IRV.toString())) {
        final IRVChoices irvChoices = new IRVChoices(contestInfo.rawChoices());
        if(!irvChoices.isValid()) {
          IRVBallotInterpretation interpretationRecord = new IRVBallotInterpretation(
              contestInfo.contest(),
              recordType,
              cvrNumber,
              imprintedID,
              contestInfo.rawChoices(),
              contestInfo.choices()
          );
          Persistence.save(interpretationRecord);
        }
      }
    }
  }
}

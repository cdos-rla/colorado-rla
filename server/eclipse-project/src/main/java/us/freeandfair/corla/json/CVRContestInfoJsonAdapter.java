/*
 * Free & Fair Colorado RLA System
 * 
 * @title ColoradoRLA
 * @created Jul 28, 2017
 * @copyright 2017 Colorado Department of State
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * @creator Daniel M. Zimmerman <dmz@freeandfair.us>
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.model.vote.IRVChoices;
import au.org.democracydevelopers.corla.model.vote.IRVParsingException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.apache.log4j.LogManager;
import us.freeandfair.corla.asm.AbstractStateMachine;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.Contest;
import us.freeandfair.corla.persistence.Persistence;

/**
 * JSON adapter for CVR contest information.
 * 
 * @author Daniel M. Zimmerman <dmz@freeandfair.us>
 * @version 1.0.0
 */
// the default constructor suffices for type adapters
@SuppressWarnings("PMD.AtLeastOneConstructor")
public final class CVRContestInfoJsonAdapter 
    extends TypeAdapter<CVRContestInfo> {

  /**
   * Class-wide logger
   */
  public static final org.apache.log4j.Logger LOGGER =
      LogManager.getLogger(CVRContestInfoJsonAdapter.class);

  /**
   * The "contest" string (for JSON serialization).
   */
  private static final String CONTEST = "contest";
  
  /**
   * The "choices" string (for JSON serialization).
   */
  private static final String CHOICES = "choices";
  
  /**
   * The "comment" string (for JSON serialization).
   */
  private static final String COMMENT = "comment";
  
  /**
   * THe "comments" string (for erroneous client JSON).
   */
  private static final String COMMENTS = "comments";
  
  /**
   * The "consensus" string (for JSON serialization).
   */
  private static final String CONSENSUS = "consensus";

  /**
   * Writes a CVR contest info object.
   * 
   * @param the_writer The JSON writer.
   * @param the_info The object to write.
   */ 
  @Override
  public void write(final JsonWriter the_writer, 
                    final CVRContestInfo the_info) 
      throws IOException {
    the_writer.beginObject();
    the_writer.name(CONTEST).value(the_info.contest().id());
    the_writer.name(COMMENT).value(the_info.comment());
    if (the_info.consensus() != null) {
      the_writer.name(CONSENSUS).value(the_info.consensus().toString());
    }
    the_writer.name(CHOICES);
    the_writer.beginArray();
    for (final String c : the_info.choices()) {
      the_writer.value(c);
    }
    the_writer.endArray();
    the_writer.endObject();
  }
  
  /**
   * Reads a set of choices.
   */
  private List<String> readChoices(final JsonReader the_reader)
      throws IOException {
    final List<String> result = new ArrayList<String>();
    the_reader.beginArray();
    while (the_reader.hasNext()) {
      result.add(the_reader.nextString());
    }
    the_reader.endArray();
    return result;
  }
  
  /**
   * Checks the sanity of a contest against a set of choices.
   * 
   * @param the_id The contest ID.
   * @param the_choices The choices.
   * @return the resulting contest, if the data is sane, or null if the
   * data is invalid.
   */
  private Contest contestSanityCheck(final Long the_id, 
                                     final List<String> the_choices) {
    final Contest result = Persistence.getByID(the_id, Contest.class);
    boolean error = the_choices == null;
    
    if (!error && result != null) {
      for (final String c : the_choices) {
        if (!result.isValidChoice(c)) {
          error = true;
        }
      }
    }
    
    if (error) {
      return null;
    } else {
      return result;
    }
  }
  
  /**
   * Reads a CVR contest info object.
   * 
   * @param the_reader The JSON reader.
   * @return the object.
   */
  @Override
  public CVRContestInfo read(final JsonReader the_reader) 
      throws IOException {
    final String prefix = "[read]";
    boolean error = false;
    List<String> choices = null;
    long contest_id = -1;
    String comment = null;
    ConsensusValue consensus = null;

    the_reader.beginObject();
    while (the_reader.hasNext()) {
      final String name = the_reader.nextName();
      switch (name) {
        case CONTEST:
          contest_id = the_reader.nextLong();
          break;

        case COMMENT:
        case COMMENTS:
          comment = the_reader.nextString();
          break;

        case CONSENSUS:
          try {
            consensus = ConsensusValue.valueOf(the_reader.nextString());
          } catch (final IllegalArgumentException e) {
            // assume undefined consensus, because enum value was invalid
          }
          break;

        case CHOICES:
          choices = readChoices(the_reader);
          break;

        default:
          error = true;
          break;
      }
    }
    the_reader.endObject();

    // check the sanity of these choices for this contest. This means checking whether they can be
    // parsed properly at all and, if so, whether the choices (candidate names or yes/no options)
    // are the ones expected for this contest. For plurality, we expect plain choices that exactly
    // match the valid choices; for IRV we expect something of the form "name(rank)" where "name"
    // is a valid choice.
    final Contest currentContest = Persistence.getByID(contest_id, Contest.class);

    // For IRV contests, first check whether the choices can be parsed as a list of IRV
    // preferences. Duplicates, overvotes and skipped ranks are OK, but strings that can't be
    // parsed as name(rank) will throw an exception.
    // Then conduct the sanity check (which checks whether the candidate names are the expected
    // names for this contest) on just the names (with preferences removed).
    //
    // Note: a more succinct, but actually incorrect, way to do this would have been to get the
    // IRV vote valid intent as
    // List<String> validInOrder = new IRVChoices(choices).getValidIntentAsOrderedList();
    // and then call contestSanityCheck on that. However, this would mean that invalid candidate
    // selections that had been omitted as part of IRV valid interpretation would not be flagged.
    // For example, a vote like [valid_candidate(1),invalid_candidate(3)] would first have the
    // 3rd preference omitted (because preference 2 was skipped), and then have only
    // [valid_candidate(1)] sanity-checked. The below implementation deliberately does this in the
    // opposite order, first getting _all_ the mentioned candidates (as choicesForSanityChecking),
    // sanity checking them, and then doing valid IRV interpretation afterwards, producing
    // interpretedChoices, which is returned in the CVRContestInfo.

    List<String> choicesForSanityChecking;
    List<String> interpretedChoices;
    try {
      // For IRV, the choices for sanity checking (which includes all mentioned candidates) are
      // different from the valid interpreted list (which omits everything after skipped or repeated
      // preferences).
      if (currentContest.description().equalsIgnoreCase(ContestType.IRV.toString())) {
        IRVChoices parsedChoices = new IRVChoices(choices);
        choicesForSanityChecking = parsedChoices.getCandidateNames();
        interpretedChoices = parsedChoices.getValidIntentAsOrderedList();

        // For plurality, just do the sanity check directly on the choices.
      } else {
        choicesForSanityChecking = choices;
        interpretedChoices = choices;
      }

      Contest contest = contestSanityCheck(contest_id, choicesForSanityChecking);

      if (error || contest == null) {
        final String msg = "invalid data detected in CVR contest info";
        LOGGER.error(String.format("%s %s", prefix, msg));
        throw new JsonSyntaxException(msg);
      }

      return new CVRContestInfo(contest, comment, consensus, interpretedChoices, choices);

    } catch (IRVParsingException e) {
      final String msg = "uploaded IRV vote could not be parsed.";
      LOGGER.error(String.format("%s %s", prefix, msg+" "+e.getMessage()));
      throw new IOException(msg+" "+e.getMessage());
    }
  }
}

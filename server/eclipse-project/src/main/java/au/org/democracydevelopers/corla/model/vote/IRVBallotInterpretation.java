package au.org.democracydevelopers.corla.model.vote;

import java.util.Collections;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.Contest;
import us.freeandfair.corla.persistence.PersistentEntity;
import us.freeandfair.corla.persistence.StringListConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A record of the interpretation of invalid IRV ballots. Used for reporting, not for the actual
 * auditing logic. Every time an invalid IRV vote is encountered, whether in the initial upload or
 * during the auditing steps, a record is made of the CVR ID, the county, contest name, raw vote,
 * and our interpretation of that vote as an ordered list of candidate names.
 * This is output as a report called IRVBallotInterpretationReport.
 */
@Entity
@Table(name = "irv_ballot_interpretation")
public class IRVBallotInterpretation implements PersistentEntity {

  /**
   * ID for interpretation record (for persistence).
   */
  @Id
  @Column(updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  /**
   * The version (for optimistic locking).
   */
  @Version
  private Long version;

  /**
   * The contest to which the interpreted vote belongs.
   */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Contest contest;

  /**
   * The record type, either UPLOADED or AUDITOR_ENTERED.
   */
  @Column(name = "record_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private CastVoteRecord.RecordType recordType;

  /**
   * ID of the CVR being audited, and whose paper ballot is being interpreted.
   * There may be multiple records with the same CVR ID, if one ballot contains multiple IRV contests.
   */
  @Column(name = "cvr_number", nullable = false)
  private int cvrNumber;

  /**
   * The imprinted ID, generally tabulator_id-batch_id-record_id.
   */
  @Column(name = "imprinted_id", nullable = false)
  private String imprintedID;

  /**
   * List of candidate names with ranks in parentheses representing a raw vote in an IRV contest.
   * Order is not important.
   */
  @Column(name = "raw_choices", columnDefinition = "character varying (1024)", nullable = false)
  @Convert(converter = StringListConverter.class)
  private List<String> rawChoices = new ArrayList<>();

  /**
   * List of candidates names, in order of rank, representing a valid vote in an IRV contest.
   */
  @Column(name = "interpretation", columnDefinition = "character varying (1024)", nullable = false)
  @Convert(converter = StringListConverter.class)
  private List<String> interpretation = new ArrayList<>();

  /**
   * Construct an empty IRVBallotInterpretation (for persistence).
   */
  public IRVBallotInterpretation() {
  }

  /**
   * Create a record of an IRV vote interpretation for a given contest and a given ballot
   * (identified by the CVR ID). This works for any IRV vote, but is expected to be used only
   * for invalid IRV votes.
   * @param contest        the Contest
   * @param recordType     the type, expected to be either UPLOADED, AUDITOR_ENTERED, or REAUDITED.
   * @param cvrNumber      the cvr Number, which appears in the csv file (not to be confused with
   *                       the cvr_id, which the database makes).
   * @param imprintedId    the imprinted ID, i.e. tabulator_id-batch_id-record_id.
   * @param rawChoices     the (invalid) raw IRV choices, e.g. [Bob(1),Alice(3),Chuan(4)].
   * @param orderedChoices the way colorado-rla interpreted the raw choices, as an order list of names.
   */
  public IRVBallotInterpretation(final Contest contest, final CastVoteRecord.RecordType recordType,
                                 final int cvrNumber, final String imprintedId,
                                 final List<String> rawChoices, final List<String> orderedChoices) {
    this.contest = contest;
    this.recordType = recordType;
    this.cvrNumber = cvrNumber;
    this.imprintedID = imprintedId;
    this.rawChoices = rawChoices;
    this.interpretation = orderedChoices;
  }

  /**
   * Returns the raw choices, prior to interpretation, as an immutable list.
   * @return The raw choices of this vote, prior to interpretation, as an immutable list.
   */
  public List<String> getRawChoices(){
    return Collections.unmodifiableList(rawChoices);
  }

  /**
   * Output details of the stored IRV vote interpretation as a String appropriate for a log message.
   * @return the data with headers incorporated.
   */
  public String logMessage(final String cvrNumberHeader, final String imprintedIDHeader) {
    return String.join(", ", (List.of(
        "County " + contest.county().name(),
        "Contest " + contest.name(),
        cvrNumberHeader + " " + cvrNumber,
        imprintedIDHeader + " " + imprintedID,
        "Record type " + recordType.name(),
        "Choices " + String.join(",", rawChoices),
        "Interpretation [" + String.join(",", interpretation) + "]"
    )));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long id() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setID(final Long the_id) {
    id = the_id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long version() {
    return version;
  }
}
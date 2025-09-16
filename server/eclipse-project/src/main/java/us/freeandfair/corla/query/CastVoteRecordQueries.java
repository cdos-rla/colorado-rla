/*
 * Free & Fair Colorado RLA System
 *
 * @title ColoradoRLA
 * 
 * @created Aug 8, 2017
 * 
 * @copyright 2017 Colorado Department of State
 * 
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * @creator Daniel M. Zimmerman <dmz@galois.com>
 * 
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.query;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.dialect.function.TemplateRenderer;
import org.hibernate.query.Query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import us.freeandfair.corla.Main;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.model.Tribute;

/**
 * Queries having to do with CastVoteRecord entities.
 *
 * @author Daniel M. Zimmerman <dmz@freeandfair.us>
 * @version 1.0.0
 */
@SuppressWarnings({"PMD.GodClass"})
public final class CastVoteRecordQueries {
  /**
   * <explanation>
   */
  //@ invariant I;
  private static final int _chunkOf1000 = 1000;

  /**
   * The "county ID" field.
   */
  private static final String COUNTY_ID = "my_county_id";

  /**
   * The "cvr number" field.
   */
  private static final String SEQUENCE_NUMBER = "my_sequence_number";

  /**
   * The "record type" field.
   */
  private static final String RECORD_TYPE = "my_record_type";

  /**
   * The "could not query database for CVRs error message.
   */
  private static final String COULD_NOT_QUERY_DATABASE = "could not query database for CVRs";

  private static final Gson GSON =
      new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

  /**
   * Private constructor to prevent instantiation.
   */
  private CastVoteRecordQueries() {
    // do nothing
  }

  /**
   * Obtain a stream of CastVoteRecord objects with the specified type.
   *
   * @param the_type The type.
   * @return the stream of CastVoteRecord objects, or null if one could not be
   *         acquired.
   * @exception IllegalStateException if this method is called outside a
   *              transaction.
   */
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public static Stream<CastVoteRecord> getMatching(final RecordType the_type) {
    if (!Persistence.isTransactionActive()) {
      throw new IllegalStateException("no running transaction");
    }

    Stream<CastVoteRecord> result = null;

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      cq.select(root).where(cb.equal(root.get(RECORD_TYPE), the_type));
      final Query<CastVoteRecord> query = s.createQuery(cq);
      result = query.stream();
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == null) {
      Main.LOGGER.debug("found no CVRs for type " + the_type);
    } else {
      Main.LOGGER.debug("query succeeded, returning CVR stream");
    }
    return result;
  }

  /**
   * Counts the CastVoteRecord objects with the specified type.
   *
   * @param the_type The type.
   * @return the count, empty if the query could not be completed successfully.
   */
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public static OptionalLong countMatching(final RecordType the_type) {
    if (!Persistence.isTransactionActive()) {
      throw new IllegalStateException("no running transaction");
    }

    OptionalLong result = OptionalLong.empty();

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      cq.select(cb.count(root)).where(cb.equal(root.get(RECORD_TYPE), the_type));
      final Query<Long> query = s.createQuery(cq);
      result = OptionalLong.of(query.getSingleResult());
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == OptionalLong.empty()) {
      Main.LOGGER.debug("found no CVRs for type " + the_type);
    } else {
      Main.LOGGER.debug("query succeeded, returning CVR stream");
    }
    return result;
  }

  /**
   * Obtain a stream of CastVoteRecord objects with the specified county and
   * type, ordered by their sequence number.
   *
   * @param the_county The county.
   * @param the_type The type.
   * @return the stream of CastVoteRecord objects, or null if one could not be
   *         acquired.
   * @exception IllegalStateException if this method is called outside a
   *              transaction.
   */
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public static Stream<CastVoteRecord> getMatching(final Long the_county,
                                                   final RecordType the_type) {
    Stream<CastVoteRecord> result = null;

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      final List<Predicate> conjuncts = new ArrayList<Predicate>();
      conjuncts.add(cb.equal(root.get(COUNTY_ID), the_county));
      conjuncts.add(cb.equal(root.get(RECORD_TYPE), the_type));
      cq.select(root).where(cb.and(conjuncts.toArray(new Predicate[conjuncts.size()])));
      cq.orderBy(cb.asc(root.get(SEQUENCE_NUMBER)));
      final Query<CastVoteRecord> query = s.createQuery(cq);
      result = query.stream();
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == null) {
      Main.LOGGER.debug("found no CVRs for county " + the_county + ", type " + the_type);
    } else {
      Main.LOGGER.debug("query succeeded, returning CVR stream");
    }
    return result;
  }

  /**
   * Counts the CastVoteRecord objects with the specified county and type.
   *
   * @param the_county The county.
   * @param the_type The type.
   * @return the count, empty if the query could not be completed successfully.
   */
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public static OptionalLong countMatching(final Long the_county, final RecordType the_type) {
    OptionalLong result = OptionalLong.empty();

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      final List<Predicate> conjuncts = new ArrayList<Predicate>();
      conjuncts.add(cb.equal(root.get(COUNTY_ID), the_county));
      conjuncts.add(cb.equal(root.get(RECORD_TYPE), the_type));
      cq.select(cb.count(root));
      cq.where(cb.and(conjuncts.toArray(new Predicate[conjuncts.size()])));
      final Query<Long> query = s.createQuery(cq);
      result = OptionalLong.of(query.getSingleResult());
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == OptionalLong.empty()) {
      Main.LOGGER.debug("found no CVRs for county " + the_county + ", type " + the_type);
    } else {
      Main.LOGGER.debug("query succeeded, returning CVR stream");
    }
    return result;
  }

  /**
   * change the votes from the export as if the cvr expost file headers had
   * contained the newChoice rather than the oldChoice
   **/
  public static int updateCVRContestInfos(final Long countyId, final Long contestId,
                                          final String oldChoice, String newChoice) {

    final Session s = Persistence.currentSession();

    newChoice = GSON.toJson(newChoice);
    newChoice = (newChoice.substring(0, newChoice.length() - 1));
    newChoice = (newChoice.substring(1, newChoice.length()));

    String escapedOldChoice = oldChoice.replaceAll("\"", Matcher.quoteReplacement("\\\\\"")).replace("?", "\\?");

       final Query q = s
        // this will only fix the first match, which is what we want, because
        // this
        // will make it possible to fix mistakes that create duplicates
        .createNativeQuery("update cvr_contest_info set choices = " +
                           "regexp_replace(choices, :oldChoice , :newChoice) " +
                           // "regexp_replace(choices, cast( :oldChoice as
                           // varchar), cast( :newChoice as varchar)) " +
                           " where county_id = :county_id " +
                           " and contest_id = :contest_id " +
                           " and choices like :oldChoiceLike")
        .setParameter("oldChoice", escapedOldChoice)
        .setParameter("oldChoiceLike", "%" + escapedOldChoice + "%")
        .setParameter("newChoice", newChoice)
        .setParameter("county_id", countyId)
        .setParameter("contest_id", contestId);

    return q.executeUpdate();
  }

  /**
   * CVRContestInfo has a required foreign key to CastVoteRecord so they must be
   * deleted first
   **/
  public static int deleteCVRContestInfos(final Long countyId) {
    final Session s = Persistence.currentSession();
    final Query q =
        s.createNativeQuery("delete from cvr_contest_info ci where ci.county_id = :county_id");
    q.setParameter("county_id", countyId);

    return q.executeUpdate();

  }

  /** delete all cvrs for a county, this supports the delete-file feature **/
  public static int deleteAll(final Long county_id) {
    final Session s = Persistence.currentSession();
    // CVRContestInfo has a required foreign key to CastVoteRecord so they must
    // be deleted first
    deleteCVRContestInfos(county_id);

    final Query query = s
        .createNativeQuery("delete from cast_vote_record cvr where cvr.county_id = :county_id");
    query.setParameter("county_id", county_id);

    return query.executeUpdate();
  }

  /**
   * Obtain the CastVoteRecord object with the specified county, type, and
   * sequence number.
   *
   * @param the_county_id The county.
   * @param the_type The type.
   * @param the_sequence_number The sequence number.
   * @return the matching CastVoteRecord object, or null if no objects match or
   *         the query fails.
   */
  // we are checking to see if exactly one result is in a list, and
  // PMD doesn't like it
  @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
  public static CastVoteRecord get(final Long the_county_id, final RecordType the_type,
                                   final Integer the_sequence_number) {
    CastVoteRecord result = null;

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      final List<Predicate> conjuncts = new ArrayList<Predicate>();
      conjuncts.add(cb.equal(root.get(COUNTY_ID), the_county_id));
      conjuncts.add(cb.equal(root.get(RECORD_TYPE), the_type));
      conjuncts.add(cb.equal(root.get(SEQUENCE_NUMBER), the_sequence_number));
      cq.select(root).where(cb.and(conjuncts.toArray(new Predicate[conjuncts.size()])));
      final TypedQuery<CastVoteRecord> query = s.createQuery(cq);
      final List<CastVoteRecord> query_results = query.getResultList();
      // if there's exactly one result, return that
      // TODO the else branch here shouldn't return null?
      if (query_results.size() == 1) {
        result = query_results.get(0);
      }
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == null) {
      Main.LOGGER.debug("found no CVR for county " + the_county_id + ", type " + the_type +
                        ", sequence " + the_sequence_number);
    } else {
      Main.LOGGER.debug("found CVR " + result);
    }

    return result;
  }

  /**
   * Obtain the CastVoteRecord objects with the specified county, type, and
   * sequence numbers.
   *
   * @param the_county_id The county.
   * @param the_type The type.
   * @param the_sequence_numbers The sequence numbers.
   * @return the matching CastVoteRecord objects, mapped by sequence number, an
   *         empty map if no records match, or null if the query fails.
   */
  public static Map<Integer, CastVoteRecord> get(final Long the_county_id,
                                                 final RecordType the_type,
                                                 final List<Integer> the_sequence_numbers) {

    // TODO: this doesn't handle the case where two CVRs in the same county have the same sequence number.
    // TODO: There should probably be a DB error or something if that happens.
    Map<Integer, CastVoteRecord> result = null;
    final Set<Integer> unique_numbers = new HashSet<>(the_sequence_numbers);

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      final List<Predicate> conjuncts = new ArrayList<>();
      conjuncts.add(cb.equal(root.get(COUNTY_ID), the_county_id));
      conjuncts.add(cb.equal(root.get(RECORD_TYPE), the_type));
      conjuncts.add(root.get("my_sequence_number").in(unique_numbers));
      cq.select(root).where(cb.and(conjuncts.toArray(new Predicate[conjuncts.size()])));
      final TypedQuery<CastVoteRecord> query = s.createQuery(cq);
      final List<CastVoteRecord> query_results = query.getResultList();
      result = new HashMap<>();
      for (final CastVoteRecord cvr : query_results) {
        result.put(cvr.sequenceNumber(), cvr);
      }
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result == null) {
      Main.LOGGER.debug("found no CVRs for county " + the_county_id + ", type " + the_type +
                        ", sequence " + unique_numbers);
    } else {
      Main.LOGGER.debug("found " + result.keySet().size() + "CVRs ");
    }

    return result;
  }

  /**
   * Obtain the CastVoteRecord objects with the specified IDs.
   *
   * @param the_ids The IDs.
   * @return the matching CastVoteRecord objects, an empty list if none are
   *         found, or null if the query fails.
   */
  public static List<CastVoteRecord> get(final List<Long> the_ids) {
    List<CastVoteRecord> result = new ArrayList<>();

    if (the_ids.isEmpty()) {
      return result;
    }

    try {
      final Session s = Persistence.currentSession();
      final CriteriaBuilder cb = s.getCriteriaBuilder();
      final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
      final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
      final List<Predicate> conjuncts = new ArrayList<>();
      conjuncts.add(root.get("my_id").in(the_ids));
      cq.select(root).where(cb.and(conjuncts.toArray(new Predicate[conjuncts.size()])));
      final TypedQuery<CastVoteRecord> query = s.createQuery(cq);
      result = query.getResultList();
    } catch (final PersistenceException e) {
      Main.LOGGER.error(COULD_NOT_QUERY_DATABASE);
    }
    if (result.isEmpty()) {
      Main.LOGGER.debug("found no CVRs with ids " + the_ids);
      return new ArrayList<>();
    } else {
      Main.LOGGER.debug("found " + result.size() + "CVRs ");
    }

    return result;
  }

  /**
   * Find a CVR by it's Ballot Manifest position
   * 
   * @parms tribute the ADT wrapping countyId, scannerId, batchId, and
   *        ballotPosition
   * @return a CastVoteRecord at some position in a manifest
   */
  public static CastVoteRecord atPosition(final Tribute tribute) {
    return atPosition(tribute.countyId, tribute.scannerId, tribute.batchId,
                      tribute.ballotPosition);
  }

  /** select cast_vote_record where uri in :uris **/
  public static List<CastVoteRecord> atPosition(final List<Tribute> tributes) {

    if (tributes.isEmpty()) {
      return new ArrayList<CastVoteRecord>();
    }

    final List<String> uris = tributes.stream().map(Persistence::persist).map(t -> t.getUri())
        .collect(Collectors.toList());

    final Session s = Persistence.currentSession();
    final Query<CastVoteRecord> q =
        s.createQuery("select cvr from CastVoteRecord cvr " + " where uri in (:uris) ");

    java.util.Spliterator<String> split = uris.stream().spliterator();
   
    final List<CastVoteRecord> results = new ArrayList<>(uris.size());
    
    while (true) {

      List<String> chunk = new ArrayList<>(_chunkOf1000);
      for (int i = 0; i < _chunkOf1000 && split.tryAdvance(chunk::add); i++)
        ;
      if (chunk.isEmpty())
        break;
      q.setParameter("uris", chunk);
      final List<CastVoteRecord> tempResults = q.getResultList();
      results.addAll(tempResults);
      Main.LOGGER.info(MessageFormat
          .format("Total URIs {0} chunk size {1} tempResults size {2} results size {3}",
                  uris.size(), chunk.size(), tempResults.size(), results.size()));
    }

    final Set<String> foundUris =
        results.stream().map(cvr -> (String) cvr.getUri()).collect(Collectors.toSet());

    final Set<CastVoteRecord> phantomRecords =
        tributes.stream().filter(distinctByKey((Tribute t) -> {
          return t.getUri();
        }))
            // is it faster to let the db do this with an except query?
            .filter(t -> !foundUris.contains(t.getUri())).map(CastVoteRecordQueries::phantomRecord)
            .map(Persistence::persist).collect(Collectors.toSet());

    results.addAll(phantomRecords);

    // this is a dummy list so we can add a cvr at a particular position(that of
    // the tributes uris)
    final List<CastVoteRecord> randomOrder =
        new ArrayList<CastVoteRecord>(Collections.nCopies(uris.size(), null));

    // line the cvrs back up into the random order
    for (final CastVoteRecord cvr : results) {
      int index = 0;
      for (final String uri : uris) {
        if (uri.equals(cvr.getUri())) {
          randomOrder.add(index, cvr);
        }
        index++;
      }
    }

    final List<CastVoteRecord> returnList =
        randomOrder.stream().filter(Objects::nonNull).collect(Collectors.toList());
    if (returnList.size() != uris.size()) {
      // TODO: I'm pretty sure this code is unreachable, since any time |URIs| < |return|, we
      // TODO: make phantoms until they equal. Maybe take this out?
      // we got a problem here
      Main.LOGGER
          .error("something went wrong with atPosition - returnList.size() != uris.size()");
    }

    return returnList;
  }

  /**
   * join query
   **/
  public static CastVoteRecord atPosition(final Long county_id, final Integer scanner_id,
                                          final String batch_id, final Integer position) {
    final Session s = Persistence.currentSession();
    final CriteriaBuilder cb = s.getCriteriaBuilder();
    final CriteriaQuery<CastVoteRecord> cq = cb.createQuery(CastVoteRecord.class);
    final Root<CastVoteRecord> root = cq.from(CastVoteRecord.class);
    cq.select(root)
        .where(cb.and(cb.equal(root.get("my_county_id"), county_id),
                      cb.equal(root.get("my_scanner_id"), scanner_id),
                      cb.equal(root.get("my_batch_id"), batch_id),
                      cb.equal(root.get("my_record_id"), position),
                      cb.or(cb.equal(root.get("my_record_type"), RecordType.UPLOADED),
                            // in case of duplicate selections on a phantom
                            // record
                            cb.equal(root.get("my_record_type"), RecordType.PHANTOM_RECORD))));

    final Query q = s.createQuery(cq);
    final Optional<CastVoteRecord> resultMaybe = q.uniqueResultOptional();

    if (resultMaybe.isPresent()) {
      return resultMaybe.get();
    } else {
      // hmm performance no good, prevents bulk queries
      return phantomRecord(county_id, scanner_id, batch_id, position);
    }
  }

  /** PHANTOM_RECORD conspiracy theory time **/
  public static CastVoteRecord phantomRecord(final Tribute tribute) {
    return phantomRecord(tribute.countyId, tribute.scannerId, tribute.batchId,
                         tribute.ballotPosition);
  }

  /** PHANTOM_RECORD conspiracy theory time **/
  public static CastVoteRecord phantomRecord(final Long county_id, final Integer scanner_id,
                                             final String batch_id, final Integer position) {
    final String imprintedID = String.format("%d-%s-%d", scanner_id, batch_id, position);
    final CastVoteRecord cvr =
        new CastVoteRecord(CastVoteRecord.RecordType.PHANTOM_RECORD, null, county_id, 0, // cvrNumber
                                                                                         // N/A
                           0, // sequenceNumber N/A
                           scanner_id, batch_id, position, imprintedID, "PHANTOM RECORD",
                           null);
    Persistence.save(cvr);
    return cvr;
  }

  /**
   * Find max revision looks for RCVRs that are old versions of a given CVR or
   * ACVR
   **/
  public static Long maxRevision(final CastVoteRecord cvr) {
    final Session s = Persistence.currentSession();
    final Query q =
        s.createQuery("select max(revision) from CastVoteRecord cvr " +
                      " where revision is not null" + " and my_county_id = :countyId" +
                      " and my_imprinted_id = :imprintedId");

    q.setLong("countyId", cvr.countyID());
    q.setString("imprintedId", cvr.imprintedID());

    try {
      // Make sure not to return null, which causes errors later. 0L is the correct return value if
      // there are no prior revisions in the database.
      // Some documentation says that getSingleResult() never returns null, but it definitely does.
      final Long result = (Long) q.getSingleResult();
      return result == null ? 0L : result;
    } catch (final PersistenceException e) {
      // the DB had a problem!
      // TODO: Technically this should probably be an error code?
      // TODO: Otherwise there's no way to discern this from a CVR with no revisions?
      // VT: Agree. Since 0L is used for "valid; no prior revisions", suggest using a different value
      // here (-1?) or throwing an exception.
      return 0L;
    }

  }

  /**
   * workaround. hibernate was ignoring the update of the object passed to the
   * method for some unknown reason
   **/
  public static Long forceUpdate(final CastVoteRecord cvr) {
    final Session s = Persistence.currentSession();
    final Query q =
        s.createNativeQuery("update cast_vote_record " + "set record_type = :recordType, " +
                            " revision = :revision, " + " uri = :uri " + " where id = :id ");
    q.setParameter("recordType", cvr.recordType().toString());
    q.setParameter("revision", cvr.getRevision());
    q.setParameter("uri", cvr.getUri());
    q.setParameter("id", cvr.id());
    final int result = q.executeUpdate();
    return Long.valueOf(result);
  }

  /**
   * select every acvr which has been submitted for the the given cvr ids,
   * including revisions(reaudits)
   **/
  public static List<CastVoteRecord> activityReport(final List<Long> contestCVRIds) {
    final Session s = Persistence.currentSession();
    final Query q =
        s.createQuery("select acvr from CastVoteRecord acvr " +
                      " where acvr.cvrId in (:cvrIds)" + " order by acvr.my_timestamp asc");

    Spliterator<Long> split = contestCVRIds.stream().spliterator();

    final List<CastVoteRecord> results = new ArrayList<>(contestCVRIds.size());

    while (true) {
      List<Long> chunk = new ArrayList<>(_chunkOf1000);
      for (int i = 0; i < _chunkOf1000 && split.tryAdvance(chunk::add); i++)
        ;
      if (chunk.isEmpty())
        break;
      q.setParameter("cvrIds", chunk);
      final List<CastVoteRecord> tempResults = q.getResultList();
      results.addAll(tempResults);
    }


    return results;
  }

  /**
   * select every acvr which has been submitted for the the given cvr ids,
   * excluding revisions(reaudits)
   **/
  public static List<CastVoteRecord> resultsReport(final List<Long> contestCVRIds) {
    final Session s = Persistence.currentSession();
    final Query q = s.createQuery("select acvr from CastVoteRecord acvr " +
                                  " where acvr.cvrId in (:cvrIds)" +
                                  " and acvr.my_record_type != 'REAUDITED' ");

    Spliterator<Long> split = contestCVRIds.stream().spliterator();

    final List<CastVoteRecord> results = new ArrayList<>(contestCVRIds.size());

    while (true) {
      List<Long> chunk = new ArrayList<>(_chunkOf1000);
      for (int i = 0; i < _chunkOf1000 && split.tryAdvance(chunk::add); i++)
        ;
      if (chunk.isEmpty())
        break;
      q.setParameter("cvrIds", chunk);
      final List<CastVoteRecord> tempResults = q.getResultList();
      results.addAll(tempResults);
    }

    return results;
  }

  /** Utility function **/
  public static <T> java.util.function.Predicate<T> distinctByKey(final Function<? super T, Object> keyExtractor) {
    final Map<Object, Boolean> map = new ConcurrentHashMap<>();
    return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}

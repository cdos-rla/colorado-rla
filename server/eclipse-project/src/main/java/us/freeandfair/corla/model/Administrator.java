/*
 * Free & Fair Colorado RLA System
 * 
 * @title ColoradoRLA
 * @created Aug 8, 2017
 * @copyright 2017 Colorado Department of State
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * @creator Daniel M. Zimmerman <dmz@galois.com>
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.model;

import static us.freeandfair.corla.util.EqualsHashcodeHelper.*;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;

import javax.persistence.*;

import us.freeandfair.corla.persistence.PersistentEntity;

/**
 * An administrator in the system. 
 * 
 * @author Daniel M. Zimmerman <dmz@freeandfair.us>
 * @author Joseph R. Kiniry <kiniry@freeandfair.us>
 * @version 1.0.0
 */
@Entity
@Cacheable(true)
@Table(name = "administrator",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"username"}) },
       indexes = { @Index(name = "idx_admin_username", 
                          columnList = "username", unique = true) })
// this class has many fields that would normally be declared final, but
// cannot be for compatibility with Hibernate and JPA.
@SuppressWarnings("PMD.ImmutableField")
public class Administrator implements PersistentEntity, Serializable {
  /**
   * The serialVersionUID.
   */
  private static final long serialVersionUID = 1; 
  
  /**
   * The ID number.
   */
  @Id
  @Column(updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long my_id;
  
  /**
   * The version (for optimistic locking).
   */
  @Version
  private Long my_version;
  
  /**
   * The username.
   */
  @Column(name = "username", unique = true, nullable = false, updatable = false)
  private String my_username;
  
  /**
   * The administrator type.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private AdministratorType my_type;
  
  /**
   * The user's full (display) name.
   */
  @Column(nullable = false, updatable = false)
  private String my_full_name;
  
  /**
   * The county.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn
  private County my_county;
  
  /**
   * The last login time.
   */
  private Instant my_last_login_time;
  
  /**
   * The last logout time.
   */
  private Instant my_last_logout_time;

  /**
   * A clock that is only used for testing
    */
  @Transient
  private Clock my_clock;
  
  /**
   * Constructs a new Administrator with default values.
   */
  public Administrator() {
    super();
  }
  
  /**
   * Constructs a new Administrator with the specified values, which has
   * never logged in or out.
   * 
   * @param the_username The username.
   * @param the_type The type.
   * @param the_full_name The full name.
   * @param the_county The county.
   */
  public Administrator(final String the_username,
                       final AdministratorType the_type,
                       final String the_full_name,
                       final County the_county) {
    super();
    my_username = the_username;
    my_type = the_type;
    my_full_name = the_full_name;
    my_county = the_county;
  }

  /**
   * Same as above, but injects a clock for use with testing.
   *
   * @param the_username
   * @param the_type
   * @param the_full_name
   * @param the_county
   * @param clock
   */
  public Administrator(final String the_username,
                       final AdministratorType the_type,
                       final String the_full_name,
                       final County the_county,
                       final Clock clock) {
    this(the_username, the_type, the_full_name, the_county);
    my_clock = clock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long id() {
    return my_id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setID(final Long the_id) {
    my_id = the_id;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public Long version() {
    return my_version;
  }
  
  /**
   * @return the username.
   */
  public String username() {
    return my_username;
  }
  
  /**
   * @return the type.
   */
  public AdministratorType type() {
    return my_type;
  }
  
  /**
   * @return the full name.
   */
  public String fullName() {
    return my_full_name;
  }
  
  /**
   * @return the county for the administrator, or null if it doesn't have one.
   */
  public County county() {
    return my_county;
  }
  
  /**
   * @return the last login time.
   */
  public Instant lastLoginTime() {
    return my_last_login_time;
  }
  
  /**
   * Updates the last login time to the current time.
   */
  public void updateLastLoginTime() {
    // If we're not testing, there will be no clock
    if (my_clock == null) {
      my_last_login_time = Instant.now();
    } else {
      my_last_login_time = Instant.now(my_clock);
    }
  }

  /**
   * @return the last logout time.
   */
  public Instant lastLogoutTime() {
    return my_last_logout_time;
  }
  
  /**
   * Updates the last logout time to the current time.
   */
  public void updateLastLogoutTime() {
    // If we're not testing, there will be no clock
    if (my_clock == null) {
      my_last_logout_time = Instant.now();
    } else {
      my_last_logout_time = Instant.now(my_clock);
    }
  }
  
  /**
   * @return a String representation of this contest.
   */
  @Override
  public String toString() {
    return "Administrator [username=" + my_username + ", type=" + 
           my_type + ", full_name=" + my_full_name + ", county=" + 
           my_county + ", last_login_time=" + my_last_login_time +
           ", last_logout_time=" + my_last_logout_time + "]";
  }

  /**
   * Compare this object with another for equivalence.
   * 
   * @param the_other The other object.
   * @return true if the objects are equivalent, false otherwise.
   */
  @Override
  public boolean equals(final Object the_other) {
    boolean result = true;
    if (the_other instanceof Administrator) {
      final Administrator other_admin = (Administrator) the_other;
      result &= nullableEquals(other_admin.username(), username());
    } else {
      result = false;
    }
    return result;
  }
  
  /**
   * @return a hash code for this object.
   */
  @Override
  public int hashCode() {
    return nullableHashCode(username());
  }

  /**
   * Types of administrator.
   */
  public enum AdministratorType {
    COUNTY, STATE;
  }
}

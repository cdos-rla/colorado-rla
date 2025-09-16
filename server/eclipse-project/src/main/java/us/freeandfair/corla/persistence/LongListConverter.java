/*
 * Free & Fair Colorado RLA System
 * 
 * @title ColoradoRLA
 * @created Aug 26, 2017
 * @copyright 2017 Colorado Department of State
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * @creator Daniel M. Zimmerman <dmz@freeandfair.us>
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.persistence;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * A converter between lists of Integers and JSON representations of such lists,
 * for database efficiency.
 * 
 * @author Daniel M. Zimmerman <dmz@freeandfair.us>
 * @version 1.0.0
 */
@Converter
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class LongListConverter implements AttributeConverter<List<Long>, String> {
  /**
   * The type information for a list of Long.
   */
  private static final Type LONG_LIST = new TypeToken<List<Long>>() { }.getType();
  
  /**
   * Our Gson instance, which does not do pretty-printing (unlike the global
   * one defined in Main).
   */
  private static final Gson GSON = 
      new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
  
  /**
   * Converts the specified list of Longs to a database column entry.
   * Do _not_ remove the apparently-redundant check for empty - this corrects an obscure
   * but important issue related to GSON and reflection
   * (see <a href="https://github.com/google/gson/issues/1875">...</a>)
   * which caused occasional failures in ballot generation.
   * @param the_list The list of Longs.
   */
  @Override
  public String convertToDatabaseColumn(final List<Long> the_list) {
    if (the_list != null && the_list.isEmpty()) {
      return GSON.toJson(new ArrayList<Long>());
    }
    return GSON.toJson(the_list); 
  }

  /**
   * Converts the specified database column entry to a list of strings.
   * 
   * @param the_column The column entry.
   */
  @Override
  public List<Long> convertToEntityAttribute(final String the_column) {
    return GSON.fromJson(the_column, LONG_LIST);
  }
}

package us.freeandfair.corla.csv;

/**
 * This exception is intended for when a marking for a contest is anything other than 0, 1, or an empty string
 */
public class CVRContestMarkingException extends Exception {
    public CVRContestMarkingException(String s) {
        super(s);
    }
}

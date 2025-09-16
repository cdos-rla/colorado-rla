package us.freeandfair.corla.query;

import org.testng.annotations.Test;
import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.AuditReason;
import us.freeandfair.corla.model.ComparisonAudit;
import us.freeandfair.corla.model.ContestResult;
import us.freeandfair.corla.persistence.Persistence;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class ComparisonAuditQueriesTest extends TestClassWithDatabase {

    @Test(enabled = false)
    public void testSortedList() {

        ContestResult contestResult = new ContestResult("Test Contest");
        BigDecimal riskLimit = new BigDecimal(0.05);
        BigDecimal dilutedMargin = new BigDecimal(.2);

        contestResult.setDilutedMargin(dilutedMargin);

        Persistence.saveOrUpdate(contestResult);
        ComparisonAudit ca = new ComparisonAudit(contestResult, riskLimit, dilutedMargin, Audit.GAMMA, AuditReason.STATE_WIDE_CONTEST);

        Persistence.saveOrUpdate(ca);
        assertEquals(new ArrayList<>(), ComparisonAuditQueries.sortedList());



    }
}

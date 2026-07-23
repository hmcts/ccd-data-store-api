package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Executes the clause produced by {@link SortOrderQueryBuilder} against the Testcontainers
 * Postgres instance already used by the integration tests.
 * <p>
 * Scope of what this proves: a quote-bearing case field id is now rejected by
 * CASE_FIELD_ID_PATTERN at build/validation time, so it never reaches the database. The
 * well-formed control case still executes cleanly end to end, proving the happy path works.
 */
public class SortOrderQueryBuilderIT extends WireMockBaseTest {

    private static final String CASE_TYPE_ID = "CaseTypeOne";
    private static final String JURISDICTION_ID = "JurisdictionOne";

    @Inject
    private SortOrderQueryBuilder sortOrderQueryBuilder;

    private JdbcTemplate template;

    @BeforeEach
    public void setUp() {
        template = new JdbcTemplate(db);
    }

    private String sortClauseFor(String caseFieldId) {
        MetaData metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId(caseFieldId)
            .metadata(false)
            .direction("ASC")
            .build());
        return sortOrderQueryBuilder.buildSortOrderClause(metaData);
    }

    @Test
    @DisplayName("A well-formed field id produces a clause Postgres can execute")
    void wellFormedFieldId_producesExecutableQuery() {
        String sql = "SELECT reference FROM case_data ORDER BY " + sortClauseFor("PersonFirstName");

        assertDoesNotThrow(() -> template.queryForList(sql));
    }

    @Test
    @DisplayName("A quote-bearing field id is rejected at validation and never reaches Postgres")
    void singleQuoteFieldId_rejectedAtValidation_neverReachesDatabase() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> sortClauseFor("foo'bar"));

        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }
}

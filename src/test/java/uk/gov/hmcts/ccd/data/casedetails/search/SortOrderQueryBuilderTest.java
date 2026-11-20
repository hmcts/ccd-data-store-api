package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * These tests pin the behaviour of {@link SortOrderQueryBuilder}'s CASE_FIELD_ID_PATTERN, now a
 * strict identifier allow-list: only letters, digits, underscore, dot and square brackets are
 * permitted. Single quotes, whitespace and hyphens - which could otherwise reach a JSONB path
 * literal that cannot be parameterised - are rejected with a BadRequestException.
 * The rejection cases below guard the tightened allow-list: they must keep failing closed. Do
 * not relax them to make a change pass.
 */
class SortOrderQueryBuilderTest {

    private static final String CASE_TYPE_ID = "CaseTypeOne";
    private static final String JURISDICTION_ID = "JurisdictionOne";

    private SortOrderQueryBuilder sortOrderQueryBuilder;

    @BeforeEach
    void setUp() {
        sortOrderQueryBuilder = new SortOrderQueryBuilder();
    }

    private String buildForNonMetadataField(String caseFieldId) {
        MetaData metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId(caseFieldId)
            .metadata(false)
            .direction("ASC")
            .build());
        return sortOrderQueryBuilder.buildSortOrderClause(metaData);
    }

    private String buildForMetadataField(String caseFieldId) {
        MetaData metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId(caseFieldId)
            .metadata(true)
            .direction("ASC")
            .build());
        return sortOrderQueryBuilder.buildSortOrderClause(metaData);
    }

    @Test
    @DisplayName("A single quote in a case field id is rejected")
    void pattern_rejectsSingleQuote() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo'bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("Whitespace in a case field id is rejected")
    void pattern_rejectsSpace() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("A hyphen in a case field id is rejected")
    void pattern_rejectsHyphen() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo-bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("Current limit: a semicolon in a case field id is rejected")
    void pattern_rejectsSemicolon() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo;bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("Current limit: an equals sign in a case field id is rejected")
    void pattern_rejectsEquals() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo=bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("A single quote is rejected at validation before any JSONB path is produced")
    void nonMetadataField_singleQuote_rejectedBeforeSqlIsProduced() {
        BadRequestException exception =
            assertThrows(BadRequestException.class, () -> buildForNonMetadataField("foo'bar"));
        assertThat(exception.getMessage(), containsString("Sort order field is invalid."));
    }

    @Test
    @DisplayName("A well-formed nested field id is converted to a JSONB path as expected")
    void nonMetadataField_dottedPath_isConvertedToJsonbPath() {
        assertThat(buildForNonMetadataField("Parent.Child"), containsString("data #>> '{Parent,Child}'"));
    }

    @Test
    @DisplayName("Metadata fields route through the CaseField enum and are never concatenated")
    void metadataField_routesThroughEnum_notConcatenated() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> buildForMetadataField("notAMetadataField")),
            () -> assertThrows(IllegalArgumentException.class, () -> buildForMetadataField("[notAMetadataField]")),
            () -> assertThat(buildForMetadataField("[STATE]"), containsString("state ASC")),
            () -> assertThat(buildForMetadataField("[STATE]"), not(containsString("data #>>")))
        );
    }
}

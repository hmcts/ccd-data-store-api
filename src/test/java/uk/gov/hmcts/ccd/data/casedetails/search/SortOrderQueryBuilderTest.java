package uk.gov.hmcts.ccd.data.casedetails.search;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SortOrderQueryBuilderTest {

    private static final String DESC = "DESC";
    private final String CASE_TYPE_ID = "CaseOne";
    private final String JURISDICTION_ID = "JurisdictionOne";

    private SortOrderQueryBuilder underTest;
    private MetaData metaData;

    @Before
    public void setup() {
        underTest = new SortOrderQueryBuilder();
        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
    }

    @Test
    public void shouldReturnQueryStringWhenNoSortOrderFields() {

        String sortOrderClause = underTest.buildSortOrderClause(metaData);

        assertThat(sortOrderClause, is("created_date ASC"));
    }

    @Test
    public void shouldReturnQueryStringWhenFieldIsMetaOne() {
        SortOrderField sortOrderField = SortOrderField.sortOrderWith()
            .metadata(true)
            .caseFieldId("[CASE_REFERENCE]")
            .direction(DESC)
            .build();
        metaData.addSortOrderField(sortOrderField);

        String sortOrderClause = underTest.buildSortOrderClause(metaData);

        assertThat(sortOrderClause, is("reference DESC, created_date ASC"));
    }

    @Test
    public void shouldReturnQueryStringWhenFieldIsNotAMetaOne() {
        SortOrderField sortOrderField = SortOrderField.sortOrderWith()
            .metadata(false)
            .caseFieldId("case_filed")
            .direction(DESC)
            .build();
        metaData.addSortOrderField(sortOrderField);

        String sortOrderClause = underTest.buildSortOrderClause(metaData);

        assertThat(sortOrderClause, is("data #>> '{case_filed}' DESC, created_date ASC"));
    }

    @Test
    public void shouldReturnQueryStringWhenFieldsAreMixedType() {
        SortOrderField sortOrderField1 = SortOrderField.sortOrderWith()
            .metadata(true)
            .caseFieldId("[CASE_REFERENCE]")
            .direction(DESC)
            .build();
        SortOrderField sortOrderField2 = SortOrderField.sortOrderWith()
            .metadata(false)
            .caseFieldId("data_field")
            .direction(DESC)
            .build();

        metaData.addSortOrderField(sortOrderField1);
        metaData.addSortOrderField(sortOrderField2);

        String sortOrderClause = underTest.buildSortOrderClause(metaData);

        assertThat(sortOrderClause, is("reference DESC, data #>> '{data_field}' DESC, created_date ASC"));
    }
}

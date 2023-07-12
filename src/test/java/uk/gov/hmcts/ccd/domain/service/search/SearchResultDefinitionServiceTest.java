package uk.gov.hmcts.ccd.domain.service.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.SEARCH;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.SearchResultBuilder.searchResult;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchResultUtil.buildSearchResultField;

class SearchResultDefinitionServiceTest {

    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String EVENT_ID = "An event";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_1_3_NESTED = "NESTED_FIELD_1";
    private static final CaseFieldDefinition CASE_FIELD_1_1 = CaseFieldDefinition.builder().id(CASE_FIELD_ID_1_1)
        .caseTypeId(CASE_TYPE_ID).label("Label1").metadata(true).build();
    private static final CaseFieldDefinition CASE_FIELD_1_2 = CaseFieldDefinition.builder().id(CASE_FIELD_ID_1_2)
        .caseTypeId(CASE_TYPE_ID).label("Label2").metadata(true).build();
    private static final CaseFieldDefinition CASE_FIELD_1_3 = CaseFieldDefinition.builder().id(CASE_FIELD_ID_1_3)
        .caseTypeId(CASE_TYPE_ID).label("Label3").metadata(false).fieldTypeDefinition(
            FieldTypeDefinition.builder().type("Complex").complexFields(List.of(
                CaseFieldDefinition.builder().id(CASE_FIELD_ID_1_3_NESTED).caseTypeId(CASE_TYPE_ID)
                    .label("NestedLabel3").build())
            ).build()
        ).build();
    private static final String ORG_CASES = "ORGCASES";

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @InjectMocks
    private SearchResultDefinitionService searchResultDefinitionService;

    private CaseTypeDefinition testCaseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testCaseTypeDefinition = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID)
            .caseFieldDefinitions(List.of(
                CASE_FIELD_1_1, CASE_FIELD_1_2, CASE_FIELD_1_3
            ))
            .events(List.of(CaseEventDefinition.builder().id(EVENT_ID).canSaveDraft(true).build()))
            .build();
    }

    @Test
    void shouldGetSearchResultDefinitionForWorkbasket() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, "",
                "")).build();
        when(uiDefinitionRepository.getWorkBasketResult(CASE_TYPE_ID)).thenReturn(searchResult);

        SearchResultDefinition result = searchResultDefinitionService.getSearchResultDefinition(testCaseTypeDefinition,
            WORKBASKET, Collections.emptyList());

        verify(uiDefinitionRepository).getWorkBasketResult(eq(CASE_TYPE_ID));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    void shouldGetSearchResultDefinitionForSearch() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, "",
                "")).build();
        when(uiDefinitionRepository.getSearchResult(CASE_TYPE_ID)).thenReturn(searchResult);

        SearchResultDefinition result = searchResultDefinitionService.getSearchResultDefinition(testCaseTypeDefinition,
            SEARCH, Collections.emptyList());

        verify(uiDefinitionRepository).getSearchResult(eq(CASE_TYPE_ID));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    void shouldGetSearchResultDefinitionForOrgCases() {
        SearchResultDefinition searchResult = searchResult()
            .withSearchResultFields(buildSearchResultField(CASE_TYPE_ID, CASE_FIELD_ID_1_1, "", CASE_FIELD_ID_1_1, "",
                "")).build();
        when(uiDefinitionRepository.getSearchCasesResult(CASE_TYPE_ID, ORG_CASES)).thenReturn(searchResult);

        SearchResultDefinition result = searchResultDefinitionService.getSearchResultDefinition(testCaseTypeDefinition,
            ORG_CASES, Collections.emptyList());

        verify(uiDefinitionRepository).getSearchCasesResult(eq(CASE_TYPE_ID), eq(ORG_CASES));
        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result, is(searchResult))
        );
    }

    @Test
    void shouldGetSearchResultDefinitionForDefaultUseCase() {
        CaseTypeDefinition caseTypeDefinition = CaseTypeDefinition.builder()
            .id(CASE_TYPE_ID)
            .caseFieldDefinitions(List.of(CASE_FIELD_1_1))
            .build();

        SearchResultDefinition result = searchResultDefinitionService.getSearchResultDefinition(caseTypeDefinition,
            "", Collections.emptyList());

        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result.getFields().length, is(1)),
            () -> assertThat(result.getFields()[0].getCaseFieldId(), is(CASE_FIELD_ID_1_1)),
            () -> assertThat(result.getFields()[0].getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(result.getFields()[0].getLabel(), is("Label1")),
            () -> assertThat(result.getFields()[0].isMetadata(), is(true))
        );
    }

    @Test
    void shouldGetSearchResultDefinitionForDefaultUseCaseWithRequestedFields() {
        SearchResultDefinition result = searchResultDefinitionService
            .getSearchResultDefinition(testCaseTypeDefinition, null, Arrays.asList(CASE_FIELD_ID_1_1,
                CASE_FIELD_ID_1_3 + "." + CASE_FIELD_ID_1_3_NESTED));

        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result.getFields().length, is(2)),
            () -> assertThat(result.getFields()[0].getCaseFieldId(), is(CASE_FIELD_ID_1_1)),
            () -> assertThat(result.getFields()[0].getCaseFieldPath(), is(nullValue())),
            () -> assertThat(result.getFields()[0].getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(result.getFields()[0].getLabel(), is("Label1")),
            () -> assertThat(result.getFields()[0].isMetadata(), is(true)),
            () -> assertThat(result.getFields()[1].getCaseFieldId(), is(CASE_FIELD_ID_1_3)),
            () -> assertThat(result.getFields()[1].getCaseFieldPath(), is(CASE_FIELD_ID_1_3_NESTED)),
            () -> assertThat(result.getFields()[1].getCaseTypeId(), is(CASE_TYPE_ID)),
            () -> assertThat(result.getFields()[1].getLabel(), is("NestedLabel3")),
            () -> assertThat(result.getFields()[1].isMetadata(), is(false))
        );
    }

    @Test
    void shouldIgnoreRequestedFieldsThatDoNotExist() {
        SearchResultDefinition result = searchResultDefinitionService
            .getSearchResultDefinition(testCaseTypeDefinition, null, Arrays.asList(CASE_FIELD_ID_1_1,
                "INVALID"));

        verifyNoMoreInteractions(uiDefinitionRepository);
        assertAll(
            () -> assertThat(result.getFields().length, is(1)),
            () -> assertThat(result.getFields()[0].getCaseFieldId(), is(CASE_FIELD_ID_1_1))
        );
    }
}

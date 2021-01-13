package uk.gov.hmcts.ccd.data.casedetails;

import com.google.common.collect.Maps;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Transactional
public class DefaultCaseDetailsRepositoryTest extends WireMockBaseTest {

    private static final long CASE_REFERENCE = 999999L;
    private static final String JURISDICTION_ID = "JeyOne";
    private static final String CASE_TYPE_ID = "CaseTypeOne";
    private static final String JURISDICTION = "PROBATE";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final Long REFERENCE = 1504259907353529L;
    private static final Long WRONG_REFERENCE = 9999999999999999L;

    private JdbcTemplate template;

    @MockBean
    private UserAuthorisation userAuthorisation;

    @MockBean
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Inject
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private ApplicationParams applicationParams;

    @Before
    public void setUp() {
        template = new JdbcTemplate(db);

        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.ALL);
    }

    @Test
    public void setShouldPersistCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        caseDetails.setState("CaseCreated");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        try {
            caseDetails.setData(JacksonUtils.convertValue(
                mapper.readTree("{\"Alliases\": [], \"HasOtherInfo\": \"Yes\"}")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final CaseDetails caseDetailsPersisted = caseDetailsRepository.set(caseDetails);
        assertThat(caseDetailsPersisted.getId(), is(notNullValue()));
        assertThat(caseDetailsPersisted.getReference(), is(caseDetails.getReference()));
        assertThat(caseDetailsPersisted.getJurisdiction(), is(caseDetails.getJurisdiction()));
        assertThat(caseDetailsPersisted.getCaseTypeId(), is(caseDetails.getCaseTypeId()));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByIdShouldReturnCorrectSingleRecord() {
        assumeDataInitialised();

        final CaseDetails byId = caseDetailsRepository.findById(1L);
        assertAll(
            () -> assertThat(byId.getId(), is("1")),
            () -> assertThat(byId.getJurisdiction(), is("PROBATE")),
            () -> assertThat(byId.getState(), is("CaseCreated")),
            () -> assertThat(byId.getCaseTypeId(), is("TestAddressBookCase")),
            () -> assertThat(byId.getSecurityClassification(), is(SecurityClassification.PUBLIC)),
            () -> assertThat(byId.getReference(), is(1504259907353529L)),
            () -> assertThat(byId.getData().get("PersonFirstName").asText(), is("Janet")),
            () -> assertThat(byId.getData().get("PersonLastName").asText(), is("Parker")),
            () -> assertThat(byId.getData().get("PersonAddress").get("AddressLine1").asText(), is("123")),
            () -> assertThat(byId.getData().get("PersonAddress").get("AddressLine2").asText(), is("Fake Street")),
            () -> assertThat(byId.getData().get("PersonAddress").get("AddressLine3").asText(), is("Hexton"))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReferenceShouldReturnCorrectSingleRecord() {
        assumeDataInitialised();

        final CaseDetails byReference = caseDetailsRepository.findByReference(1504259907353529L);
        assertAll(
            () -> assertThat(byReference.getId(), is("1")),
            () -> assertThat(byReference.getJurisdiction(), is("PROBATE")),
            () -> assertThat(byReference.getState(), is("CaseCreated")),
            () -> assertThat(byReference.getCaseTypeId(), is("TestAddressBookCase")),
            () -> assertThat(byReference.getSecurityClassification(), is(SecurityClassification.PUBLIC)),
            () -> assertThat(byReference.getReference(), is(1504259907353529L)),
            () -> assertThat(byReference.getData().get("PersonFirstName").asText(), is("Janet")),
            () -> assertThat(byReference.getData().get("PersonLastName").asText(), is("Parker")),
            () -> assertThat(byReference.getData().get("PersonAddress").get("AddressLine1").asText(), is("123")),
            () -> assertThat(byReference.getData().get("PersonAddress").get("AddressLine2").asText(),
                    is("Fake Street")),
            () -> assertThat(byReference.getData().get("PersonAddress").get("AddressLine3").asText(), is("Hexton"))
        );
    }


    @Test
    public void sanitisesInputsCountQuery() {
        String evil = "foo');insert into case users values(1,2,3);--";
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds("PROBATE",
                "TestAddressBookCase", CAN_READ))
            .thenReturn(asList(evil));

        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);
        when(userAuthorisation.getUserId()).thenReturn(evil);

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        final PaginatedSearchMetadata byMetaData =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata, Maps.newHashMap());

        // If any input is not correctly sanitized it will cause an exception since query result structure will not be
        // as hibernate expects.
        assertThat(byMetaData.getTotalResultsCount(), is(0));
    }

//CHECKSTYLE.OFF: CommentsIndentation
//  This test should be uncommented as part of future RDM-7408
//    @Test(expected = IllegalArgumentException.class)
//    public void validateInputsMainQuerySortOrder() {
//        String evil = "foo');insert into case users values(1,2,3);--";
//        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds("PROBATE", "TestAddressBookCase",
//        CAN_READ))
//            .thenReturn(asList(evil));
//
//        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);
//        when(userAuthorisation.getUserId()).thenReturn(evil);
//
//        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
//        metadata.addSortOrderField(SortOrderField.sortOrderWith()
//                                       .caseFieldId(evil)
//                                       .metadata(false)
//                                       .direction("DESC")
//                                       .build());
//
//        // If any input is not correctly validated it will pass the query to jdbc driver creating potential sql
//        injection vulnerability. caseDetailsRepository.findByMetaDataAndFieldData(metadata, Maps.newHashMap());
//    }
    //CHECKSTYLE.ON: CommentsIndentation


    @Test
    public void sanitiseInputMainQuerySortOrderForDirection() {
        String evil = "foo');insert into case users values(1,2,3);--";

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setSortDirection(Optional.of("Asc"));
        metadata.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId("[CASE_REFERENCE]")
            .metadata(true)
            .direction(evil)
            .build());

        final List<CaseDetails> byMetaData =
            caseDetailsRepository.findByMetaDataAndFieldData(metadata, Maps.newHashMap());

        // If any input is not correctly sanitized it will cause an exception since query result structure will not be
        // as hibernate expects.
        assertThat(byMetaData.size(), is(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputMainQueryMetaDataFieldId() {
        String notSoEvil = "[UNKNOWN_FIELD]";

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setSortDirection(Optional.of("Asc"));
        metadata.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId(notSoEvil)
            .metadata(true)
            .direction("DESC")
            .build());

        // If any input is not correctly validated it will pass the query to jdbc driver creating potential sql
        // injection vulnerability
        caseDetailsRepository.findByMetaDataAndFieldData(metadata, Maps.newHashMap());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByWildcardReturnCorrectRecords() {
        ReflectionTestUtils.setField(applicationParams, "wildcardSearchAllowed", true);
        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");

        Map<String, String> params = Maps.newHashMap();
        params.put("PersonFirstName", "%An%");
        final PaginatedSearchMetadata byMetaData = caseDetailsRepository.getPaginatedSearchMetadata(metadata, params);
        // See case types and citizen names in insert_cases.sql to understand this result.
        assertThat(byMetaData.getTotalResultsCount(), is(5));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getFindByMetadataReturnCorrectRecords() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        final PaginatedSearchMetadata byMetaData =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata, Maps.newHashMap());
        assertThat(byMetaData.getTotalResultsCount(), is(7));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getFindByMetadataAndFieldDataSortDescByMetaDataField() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setSortDirection(Optional.of("Asc"));
        SortOrderField sortOrderField = SortOrderField.sortOrderWith()
            .caseFieldId("[LAST_MODIFIED_DATE]")
            .metadata(true)
            .direction("DESC")
            .build();
        metadata.addSortOrderField(sortOrderField);

        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");
        final List<CaseDetails> byMetaDataAndFieldData = caseDetailsRepository.findByMetaDataAndFieldData(metadata,
            searchParams);

        // See the timestamps in insert_cases.sql.
        // Should be ordered by last modified desc, creation date asc.
        assertThat(byMetaDataAndFieldData.get(0).getId(), is("16"));
        assertThat(byMetaDataAndFieldData.get(1).getId(), is("1"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getFindByMetadataAndFieldDataSortByBothCaseAndMetadataFields() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setSortDirection(Optional.of("Asc"));
        metadata.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId("[LAST_MODIFIED_DATE]")
            .metadata(true)
            .direction("ASC")
            .build());
        metadata.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId("PersonLastName")
            .metadata(false)
            .direction("DESC")
            .build());

        final List<CaseDetails> byMetaDataAndFieldData = caseDetailsRepository.findByMetaDataAndFieldData(metadata,
            Maps.newHashMap());

        // See the timestamps in insert_cases.sql. (2 results based on pagination size = 2)
        // Should be ordered by last modified desc, person last name, creation date asc.
        assertThat(byMetaDataAndFieldData.size(), is(2));
        assertThat(byMetaDataAndFieldData.get(0).getId(), is("1"));
        assertThat(byMetaDataAndFieldData.get(1).getId(), is("2"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getFindByMetadataAndFieldDataReturnCorrectRecords() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");
        final List<CaseDetails> byMetaDataAndFieldData = caseDetailsRepository.findByMetaDataAndFieldData(metadata,
            searchParams);
        assertAll(
            () -> assertThat(byMetaDataAndFieldData.size(), is(2)),
            () -> assertThat(byMetaDataAndFieldData.get(0).getId(), is("1")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getJurisdiction(), is("PROBATE")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getState(), is("CaseCreated")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getCaseTypeId(), is("TestAddressBookCase")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getSecurityClassification(),
                is(SecurityClassification.PUBLIC)),
            () -> assertThat(byMetaDataAndFieldData.get(0).getReference(), is(1504259907353529L)),
            () -> assertThat(byMetaDataAndFieldData.get(0).getData().get("PersonFirstName").asText(), is("Janet")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getData().get("PersonLastName").asText(), is("Parker")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getData().get("PersonAddress").get("AddressLine1").asText(),
                is("123")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getData().get("PersonAddress").get("AddressLine2").asText(),
                is("Fake Street")),
            () -> assertThat(byMetaDataAndFieldData.get(0).getData().get("PersonAddress").get("AddressLine3").asText(),
                is("Hexton")),
            () -> verify(authorisedCaseDefinitionDataService).getUserAuthorisedCaseStateIds("PROBATE",
                "TestAddressBookCase", CAN_READ)
        );
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getPaginatedSearchMetadataShouldReturnPaginationInfoWhenSearchedWithMetadata() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setState(Optional.of("CaseCreated"));
        final PaginatedSearchMetadata paginatedSearchMetadata =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata,
            new HashMap<>());
        assertAll(
            () -> assertThat(paginatedSearchMetadata.getTotalResultsCount(), is(5)),
            () -> assertThat(paginatedSearchMetadata.getTotalPagesCount(), is(3))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getPaginatedSearchMetadataShouldReturnPaginationInfoWhenSearchedWithSearchParams() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");
        final PaginatedSearchMetadata paginatedSearchMetadata =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata, searchParams);
        assertAll(
            () -> assertThat(paginatedSearchMetadata.getTotalResultsCount(), is(2)),
            () -> assertThat(paginatedSearchMetadata.getTotalPagesCount(), is(1))
        );
    }

    private void assumeDataInitialised() {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void searchWithParams_withAccessLevelAll() {
        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        final List<CaseDetails> results = caseDetailsRepository.findByMetaDataAndFieldData(metadata, searchParams);

        assertAll(
            () -> assertThat(results, hasSize(2)),
            () -> assertThat(results, hasItem(allOf(
                hasProperty("id", equalTo("1")),
                hasProperty("reference", equalTo(1504259907353529L))
            ))),
            () -> assertThat(results, hasItem(allOf(
                hasProperty("id", equalTo("16")),
                hasProperty("reference", equalTo(1504254784737847L))
            )))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql"
    })
    public void searchWithParams_withAccessLevelGranted() {
        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);
        when(userAuthorisation.getUserId()).thenReturn("1");

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        final List<CaseDetails> results = caseDetailsRepository.findByMetaDataAndFieldData(metadata, searchParams);

        assertAll(
            () -> assertThat(results, hasSize(1)),
            () -> assertThat(results, hasItem(allOf(
                hasProperty("id", equalTo("16")),
                hasProperty("reference", equalTo(1504254784737847L))
            )))
        );
        assertThat(caseDetailsRepository.getPaginatedSearchMetadata(metadata, searchParams).getTotalResultsCount(),
            is(1));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_with_restricted_state.sql"
    })
    public void searchWithParams_restrictedStates() {
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds("PROBATE",
            "TestAddressBookCase", CAN_READ))
            .thenReturn(asList("CaseRestricted"));

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        final List<CaseDetails> results = caseDetailsRepository.findByMetaDataAndFieldData(metadata, Maps.newHashMap());

        assertThat(results.size(), is(1));
        assertThat(results.get(0).getReference(), is(1504254784737848L));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReference_withJurisdiction() {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(JURISDICTION, REFERENCE);

        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new AssertionError("No case found"));

        assertCaseDetails(caseDetails, "1", JURISDICTION, REFERENCE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReference_withJurisdiction_jurisdictionNotFound() {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(WRONG_JURISDICTION, REFERENCE);

        MatcherAssert.assertThat(maybeCase.isPresent(), is(false));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReference_withJurisdiction_referenceNotFound() {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(JURISDICTION, WRONG_REFERENCE);

        MatcherAssert.assertThat(maybeCase.isPresent(), is(false));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReferenceWithoutJurisdiction() {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(REFERENCE.toString());

        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new AssertionError("No case found"));

        assertCaseDetails(caseDetails, "1", JURISDICTION, REFERENCE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReferenceWithNoAccessControl() {
        final Optional<CaseDetails> maybeCase =
            caseDetailsRepository.findByReferenceWithNoAccessControl(REFERENCE.toString());

        final CaseDetails caseDetails = maybeCase.orElseThrow(() -> new AssertionError("No case found"));

        assertCaseDetails(caseDetails, "1", JURISDICTION, REFERENCE);
    }

    private void assertCaseDetails(CaseDetails caseDetails, String id, String jurisdictionId, Long caseReference) {
        MatcherAssert.assertThat(caseDetails.getId(), equalTo(id));
        MatcherAssert.assertThat(caseDetails.getJurisdiction(), equalTo(jurisdictionId));
        MatcherAssert.assertThat(caseDetails.getReference(), equalTo(caseReference));
    }

}

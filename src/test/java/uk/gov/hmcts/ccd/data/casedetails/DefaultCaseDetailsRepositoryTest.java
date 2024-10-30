package uk.gov.hmcts.ccd.data.casedetails;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextListener;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseTypeDefinition;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.GET_ROLE_ASSIGNMENTS_PREFIX;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentRecord;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentResponse;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.emptyRoleAssignmentResponseJson;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.roleToAccessProfileDefinition;

@Transactional
public class DefaultCaseDetailsRepositoryTest extends WireMockBaseTest {

    private static final long CASE_REFERENCE = 999999L;
    private static final String JURISDICTION_ID = "JeyOne";
    private static final String CASE_TYPE_ID = "CaseTypeOne";
    private static final String WRONG_CASE_TYPE_ID = "CaseTypeWrong";
    private static final String JURISDICTION = "PROBATE";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final Long REFERENCE = 1504259907353529L;
    private static final Long WRONG_REFERENCE = 9999999999999999L;
    private static final LocalDate RESOLVED_TTL = LocalDate.now();

    private JdbcTemplate template;
    private MockHttpServletRequest request;
    private RequestContextListener listener;
    private ServletContext context;

    @MockBean
    private UserAuthorisation userAuthorisation;

    @MockBean
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private PocApiClient pocApiClient;
    @Inject
    @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private ApplicationParams applicationParams;

    @Before
    public void setUp() {
        template = new JdbcTemplate(db);

        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.ALL);
        when(userAuthorisation.getUserId()).thenReturn("123");

        request = new MockHttpServletRequest();

        listener = new RequestContextListener();
        context = new MockServletContext();
        listener.requestInitialized(new ServletRequestEvent(context, request));

        wireMockServer.resetToDefaultMappings();
    }

    @After
    public void clearDown() {
        listener.requestDestroyed(new ServletRequestEvent(context, request));
    }

    @Test
    public void setShouldThrowCaseConcurrencyException() throws NoSuchFieldException, IllegalAccessException {
        EntityManager emMock = mock(EntityManager.class);
        CaseDetailsMapper caseDetailsMapper = mock(CaseDetailsMapper.class);

        DefaultCaseDetailsRepository defaultCaseDetailsRepository =
            new DefaultCaseDetailsRepository(caseDetailsMapper, null, null,
                applicationParams, pocApiClient);

        Field emField = DefaultCaseDetailsRepository.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(defaultCaseDetailsRepository, emMock);

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1L);

        CaseDetailsEntity caseDetailsEntity = new CaseDetailsEntity();
        caseDetailsEntity.setReference(1L);
        caseDetailsEntity.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        caseDetailsEntity.setVersion(1);

        when(caseDetailsMapper.modelToEntity(caseDetails)).thenReturn(caseDetailsEntity);
        when(emMock.merge(caseDetailsEntity)).thenReturn(caseDetailsEntity);
        doThrow(new OptimisticLockException()).when(emMock).flush();

        CaseConcurrencyException exception = assertThrows(CaseConcurrencyException.class,
            () -> defaultCaseDetailsRepository.set(caseDetails));

        assertThat(exception.getMessage(), is("Unfortunately we were unable to save your work to the case as "
            + "another action happened at the same time.\nPlease review the case and try again."));

        verify(emMock).merge(caseDetailsEntity);
        verify(emMock).flush();
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
        caseDetails.setResolvedTTL(RESOLVED_TTL);
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
        assertThat(caseDetailsPersisted.getResolvedTTL(), is(caseDetails.getResolvedTTL()));
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
        stubFor(WireMock.get(urlMatching(GET_ROLE_ASSIGNMENTS_PREFIX + ".*"))
            .willReturn(okJson(emptyRoleAssignmentResponseJson())
                .withStatus(200)));

        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds("PROBATE",
                "TestAddressBookCase", CAN_READ))
            .thenReturn(asList(evil));

        when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);
        when(userAuthorisation.getUserId()).thenReturn(evil);

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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

    @Test (expected = BadRequestException.class)
    public void sanitiseInputMainQuerySortOrderForCaseFieldID() {
        final String caseFieldId = "insert into case users values(1,2,3)";

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setSortDirection(Optional.of("Asc"));
        metadata.addSortOrderField(SortOrderField.sortOrderWith()
            .caseFieldId(caseFieldId)
            .metadata(true)
            .direction("DESC")
            .build());

        caseDetailsRepository.findByMetaDataAndFieldData(metadata, Maps.newHashMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateInputMainQueryMetaDataFieldId() {
        final String notSoEvil = "[UNKNOWN_FIELD]";

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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
        final MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

        Map<String, String> params = Maps.newHashMap();
        params.put("PersonFirstName", "%An%");
        final PaginatedSearchMetadata byMetaData = caseDetailsRepository.getPaginatedSearchMetadata(metadata, params);
        // See case types and citizen names in insert_cases.sql to understand this result.
        assertThat(byMetaData.getTotalResultsCount(), is(2));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getFindByMetadataReturnCorrectRecords() {
        assumeDataInitialised();

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        final PaginatedSearchMetadata byMetaData =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata, Maps.newHashMap());
        assertThat(byMetaData.getTotalResultsCount(), is(4));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getPaginatedSearchMetadataReturnEmptyResult() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        final PaginatedSearchMetadata byMetaData =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata, Maps.newHashMap());
        assertAll(
            () -> assertThat(byMetaData.getTotalResultsCount(), is(0)),
            () -> assertThat(byMetaData.getTotalPagesCount(), is(0))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByMetaDataAndFieldDataReturnEmtyCaseDetailsList() {
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

        assertEquals(0, byMetaDataAndFieldData.size());
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

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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

        final MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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
                is("Hexton"))
        );
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getPaginatedSearchMetadataShouldReturnPaginationInfoWhenSearchedWithMetadata() {
        assumeDataInitialised();

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        metadata.setState(Optional.of("CaseCreated"));

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

        final PaginatedSearchMetadata paginatedSearchMetadata =
            caseDetailsRepository.getPaginatedSearchMetadata(metadata,
                new HashMap<>());
        assertAll(
            () -> assertThat(paginatedSearchMetadata.getTotalResultsCount(), is(4)),
            () -> assertThat(paginatedSearchMetadata.getTotalPagesCount(), is(2))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getPaginatedSearchMetadataShouldReturnPaginationInfoWhenSearchedWithSearchParams() {
        assumeDataInitialised();

        final MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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
        final MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

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
    public void searchWithParams_withAccessLevelGranted() throws Exception {
        String userId = "123";
        CaseTypeDefinition caseTypeDefinition = loadCaseTypeDefinition("mappings/bookcase-definition.json");
        caseTypeDefinition.setRoleToAccessProfiles(asList(roleToAccessProfileDefinition("[CREATOR]")));

        stubFor(WireMock.get(urlMatching("/api/data/case-type/TestAddressBookCase"))
            .willReturn(okJson(defaultObjectMapper.writeValueAsString(caseTypeDefinition))
                .withStatus(200)));

        stubFor(WireMock.get(urlMatching(GET_ROLE_ASSIGNMENTS_PREFIX + userId))
            .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                createRoleAssignmentResponse(
                    singletonList(createRoleAssignmentRecord("assignment",
                        "1504254784737847",
                        "TestAddressBookCase",
                        JURISDICTION,
                        "[CREATOR]",
                        userId,
                        false)))))
                .withStatus(200)));

        MetaData metadata = new MetaData("TestAddressBookCase", "PROBATE");
        HashMap<String, String> searchParams = new HashMap<>();
        searchParams.put("PersonFirstName", "Janet");

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
            caseStateDefinition.setId("CaseCreated");

            when(accessControlService
                .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
                .thenReturn(asList(caseStateDefinition), asList());
        }

        final List<CaseDetails> results = caseDetailsRepository.findByMetaDataAndFieldData(metadata, searchParams);

        assertAll(
            () -> assertThat(results, hasSize(1)),
            () -> assertThat(results, hasItem(allOf(
                hasProperty("id", equalTo("16")),
                hasProperty("reference", equalTo(1504254784737847L))
            )))
        );

        CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
        caseStateDefinition.setId("CaseCreated");

        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(asList(caseStateDefinition), asList());

        assertThat(caseDetailsRepository.getPaginatedSearchMetadata(metadata, searchParams).getTotalResultsCount(),
            is(1));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_with_restricted_state.sql"
    })
    public void searchWithParams_restrictedStates() {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            CaseStateDefinition caseStateDefinition = new CaseStateDefinition();
            caseStateDefinition.setId("CaseRestricted");

            when(accessControlService
                .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
                .thenReturn(asList(caseStateDefinition));
        } else {
            when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds("PROBATE",
                "TestAddressBookCase", CAN_READ))
                .thenReturn(asList("CaseRestricted"));
        }

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

        assertThat(maybeCase.isPresent(), is(false));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByReference_withJurisdiction_referenceNotFound() {
        final Optional<CaseDetails> maybeCase = caseDetailsRepository.findByReference(JURISDICTION, WRONG_REFERENCE);

        assertThat(maybeCase.isPresent(), is(false));
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

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByParamsWithLimit_shouldFindSingleRecord() {

        // GIVEN
        MigrationParameters parameters = MigrationParameters.builder()
            .caseTypeId(CASE_01_TYPE)
            .jurisdictionId(JURISDICTION)
            .caseDataId(CASE_01_ID)
            .numRecords(1)
            .build();

        // WHEN
        final List<CaseDetails> results = caseDetailsRepository.findByParamsWithLimit(parameters);

        // THEN
        assertEquals(1, results.size());
        assertCaseDetails(results.get(0), CASE_01_ID.toString(), JURISDICTION, Long.parseLong(CASE_01_REFERENCE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByParamsWithLimit_shouldFindManyRecords() {

        // GIVEN
        MigrationParameters parameters = MigrationParameters.builder()
            .caseTypeId(CASE_01_TYPE)
            .jurisdictionId(JURISDICTION)
            .caseDataId(CASE_01_ID)
            .numRecords(5)
            .build();

        // WHEN
        final List<CaseDetails> results = caseDetailsRepository.findByParamsWithLimit(parameters);

        // THEN
        assertEquals(5, results.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByParamsWithLimit_shouldChangeStartingRecord() {

        // GIVEN
        MigrationParameters parameters = MigrationParameters.builder()
            .caseTypeId(CASE_01_TYPE)
            .jurisdictionId(JURISDICTION)
            .caseDataId(CASE_03_ID)
            .numRecords(1)
            .build();

        // WHEN
        final List<CaseDetails> results = caseDetailsRepository.findByParamsWithLimit(parameters);

        // THEN
        assertEquals(1, results.size());
        assertCaseDetails(results.get(0), CASE_03_ID.toString(), JURISDICTION, Long.parseLong(CASE_03_REFERENCE));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByParamsWithLimit_shouldFilterOnJurisdiction() {

        // GIVEN
        MigrationParameters parameters = MigrationParameters.builder()
            .caseTypeId(CASE_01_TYPE)
            .jurisdictionId(WRONG_JURISDICTION)
            .caseDataId(CASE_01_ID)
            .numRecords(1)
            .build();

        // WHEN
        final List<CaseDetails> results = caseDetailsRepository.findByParamsWithLimit(parameters);

        // THEN
        assertEquals(0, results.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void findByParamsWithLimit_shouldFilterOnCaseType() {

        // GIVEN
        MigrationParameters parameters = MigrationParameters.builder()
            .caseTypeId(WRONG_CASE_TYPE_ID)
            .jurisdictionId(JURISDICTION)
            .caseDataId(CASE_01_ID)
            .numRecords(1)
            .build();

        // WHEN
        final List<CaseDetails> results = caseDetailsRepository.findByParamsWithLimit(parameters);

        // THEN
        assertEquals(0, results.size());
    }

    private void assertCaseDetails(CaseDetails caseDetails, String id, String jurisdictionId, Long caseReference) {
        assertThat(caseDetails.getId(), equalTo(id));
        assertThat(caseDetails.getJurisdiction(), equalTo(jurisdictionId));
        assertThat(caseDetails.getReference(), equalTo(caseReference));
    }

}

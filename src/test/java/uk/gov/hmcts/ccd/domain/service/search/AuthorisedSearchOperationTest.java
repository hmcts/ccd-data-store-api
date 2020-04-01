package uk.gov.hmcts.ccd.domain.service.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class AuthorisedSearchOperationTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String JURISDICTION_ID = "Probate";
    private static final String STATE_ID = "Issued";
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final Set<String> USER_ROLES = Sets.newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);

    @Mock
    private SearchOperation nextOperationInChain;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;

    private AuthorisedSearchOperation authorisedSearchOperation;

    private MetaData metaData;
    private HashMap<String, String> criteria;
    private CaseType caseType;
    private final List<CaseField> caseFields = Lists.newArrayList();

    private JsonNode classifiedDataNode1;
    private JsonNode classifiedDataClassificationNode1;
    private JsonNode authorisedDataNode1;
    private JsonNode authorisedDataClassificationNode1;
    private CaseDetails classifiedCase1;

    private JsonNode classifiedDataNode2;
    private JsonNode classifiedDataClassificationNode2;
    private JsonNode authorisedDataNode2;
    private JsonNode authorisedDataClassificationNode2;
    private CaseDetails classifiedCase2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        criteria = new HashMap<>();
        caseType = new CaseType();
        caseType.setCaseFields(caseFields);

        when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(userRepository.getUserRoles()).thenReturn(USER_ROLES);

        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ);

        caseFields.addAll(getCaseFieldsWithIds("dataTestField11", "dataTestField12", "classificationTestField11", "classificationTestField12"));

        classifiedDataNode1 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedDataNode1).put("dataTestField11", "dataTestValue11");
        ((ObjectNode) classifiedDataNode1).put("dataTestField12", "dataTestValue12");
        classifiedDataClassificationNode1 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedDataClassificationNode1).put("classificationTestField11", "classificationTestValue11");
        ((ObjectNode) classifiedDataClassificationNode1).put("classificationTestField12", "classificationTestValue12");

        authorisedDataNode1 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedDataNode1).put("dataTestField11", "dataTestValue11");
        authorisedDataClassificationNode1 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedDataClassificationNode1).put("classificationTestField11", "classificationTestValue11");

        classifiedCase1 = new CaseDetails();
        classifiedCase1.setData(JacksonUtils.convertValue(classifiedDataNode1));
        classifiedCase1.setDataClassification(JacksonUtils.convertValue(classifiedDataClassificationNode1));

        caseFields.addAll(getCaseFieldsWithIds("dataTestField21", "dataTestField22", "classificationTestField21", "classificationTestField22"));

        classifiedDataNode2 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedDataNode2).put("dataTestField21", "dataTestValue21");
        ((ObjectNode) classifiedDataNode2).put("dataTestField22", "dataTestValue22");
        classifiedDataClassificationNode2 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) classifiedDataClassificationNode2).put("classificationTestField21", "classificationTestValue21");
        ((ObjectNode) classifiedDataClassificationNode2).put("classificationTestField22", "classificationTestValue22");

        authorisedDataNode2 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedDataNode2).put("dataTestField21", "dataTestValue21");
        authorisedDataClassificationNode2 = JSON_NODE_FACTORY.objectNode();
        ((ObjectNode) authorisedDataClassificationNode2).put("classificationTestField21", "classificationTestValue21");

        classifiedCase2 = new CaseDetails();
        classifiedCase2.setData(JacksonUtils.convertValue(classifiedDataNode2));
        classifiedCase2.setDataClassification(JacksonUtils.convertValue(classifiedDataClassificationNode2));

        doReturn(Arrays.asList(classifiedCase1, classifiedCase2)).when(nextOperationInChain).execute(metaData, criteria);
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(eq(classifiedCase1.getState()), eq(caseType), eq(USER_ROLES), eq(CAN_READ));
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(eq(classifiedCase2.getState()), eq(caseType), eq(USER_ROLES), eq(CAN_READ));

        doReturn(authorisedDataNode1).when(accessControlService).filterCaseFieldsByAccess(
            eq(classifiedDataNode1),
            eq(caseFields),
            eq(USER_ROLES),
            eq(CAN_READ),
            anyBoolean());
        doReturn(authorisedDataClassificationNode1).when(accessControlService).filterCaseFieldsByAccess(
            eq(classifiedDataClassificationNode1),
            eq(caseFields),
            eq(USER_ROLES),
            eq(CAN_READ),
            anyBoolean());

        doReturn(authorisedDataNode2).when(accessControlService).filterCaseFieldsByAccess(
            eq(classifiedDataNode2),
            eq(caseFields),
            eq(USER_ROLES),
            eq(CAN_READ),
            anyBoolean());
        doReturn(authorisedDataClassificationNode2).when(accessControlService).filterCaseFieldsByAccess(
            eq(classifiedDataClassificationNode2),
            eq(caseFields),
            eq(USER_ROLES),
            eq(CAN_READ),
            anyBoolean());

        authorisedSearchOperation = new AuthorisedSearchOperation(nextOperationInChain,
            caseDefinitionRepository, accessControlService, userRepository);
    }

    private List<CaseField> getCaseFieldsWithIds(String... dataTestFields) {
        return Stream.of(dataTestFields)
            .map(field -> {
                CaseField caseField = new CaseField();
                caseField.setId(field);
                return caseField;
            })
            .collect(Collectors.toList());
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {
        authorisedSearchOperation.execute(metaData, criteria);

        verify(nextOperationInChain).execute(metaData, criteria);
    }

    @Test
    @DisplayName("should return empty list when decorated returns null")
    void shouldReturnEmptyListWhenNullResult() {
        doReturn(null).when(nextOperationInChain).execute(metaData, criteria);

        final List<CaseDetails> output = authorisedSearchOperation.execute(metaData, criteria);

        assertAll(
            () -> assertThat(output, is(notNullValue())),
            () -> assertThat(output, hasSize(0))
        );
    }

    @Test
    @DisplayName("should return authorised search results")
    void shouldReturnAuthorisedSearchResults() {
        final List<CaseDetails> output = authorisedSearchOperation.execute(metaData, criteria);

        InOrder inOrder = inOrder(nextOperationInChain,
            caseDefinitionRepository,
            accessControlService,
            userRepository);

        assertAll(
            () -> assertThat(output, hasSize(2)),
            () -> assertThat(output, hasItems(classifiedCase1, classifiedCase2)),
            () -> assertThat(output.get(0).getData(),
                is(equalTo(JacksonUtils.convertValue(authorisedDataNode1)))),
            () -> assertThat(output.get(0).getDataClassification(),
                is(equalTo(JacksonUtils.convertValue(authorisedDataClassificationNode1)))),
            () -> assertThat(output.get(1).getData(),
                is(equalTo(JacksonUtils.convertValue(authorisedDataNode2)))),
            () -> assertThat(output.get(1).getDataClassification(),
                is(equalTo(JacksonUtils.convertValue(authorisedDataClassificationNode2)))),
            () -> inOrder.verify(nextOperationInChain).execute(metaData, criteria),
            () -> inOrder.verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID),
            () -> inOrder.verify(userRepository).getUserRoles(),
            () -> inOrder.verify(accessControlService).canAccessCaseTypeWithCriteria(eq(caseType),
                eq(USER_ROLES),
                eq(CAN_READ)),
            () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedDataNode1),
                eq(caseFields),
                eq(USER_ROLES),
                eq(CAN_READ),
                anyBoolean()),
            () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedDataClassificationNode1),
                eq(caseFields),
                eq(USER_ROLES),
                eq(CAN_READ),
                anyBoolean()),
            () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedDataNode2),
                eq(caseFields),
                eq(USER_ROLES),
                eq(CAN_READ),
                anyBoolean()),
            () -> inOrder.verify(accessControlService).filterCaseFieldsByAccess(eq(classifiedDataClassificationNode2),
                eq(caseFields),
                eq(USER_ROLES),
                eq(CAN_READ),
                anyBoolean())

        );
    }

    @Test
    @DisplayName("should fail if no case type found")
    void shouldFailIfNoCaseTypeFound() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedSearchOperation.execute(metaData, criteria));
    }

    @Test
    @DisplayName("should fail if null user roles found")
    void shouldFailIfNullUserRolesFound() {
        doReturn(null).when(userRepository).getUserRoles();

        assertThrows(ValidationException.class, () -> authorisedSearchOperation.execute(metaData, criteria));
    }

    @Test
    @DisplayName("should return no results when no case type read access")
    void shouldReturnEmptyResultsIfNoCaseTypeReadAccess() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(caseType, USER_ROLES, CAN_READ);

        final List<CaseDetails> output = authorisedSearchOperation.execute(metaData, criteria);

        assertAll(
            () -> assertThat(output, is(notNullValue())),
            () -> assertThat(output, hasSize(0)),
            () -> verify(accessControlService, never()).filterCaseFieldsByAccess(any(JsonNode.class), eq(caseFields), eq(USER_ROLES), eq(CAN_READ), anyBoolean())
        );
    }
}

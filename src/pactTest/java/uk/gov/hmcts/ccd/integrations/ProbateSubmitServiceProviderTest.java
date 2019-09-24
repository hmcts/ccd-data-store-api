package uk.gov.hmcts.ccd.integrations;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import au.com.dius.pact.provider.spring.SpringRestPactRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.SecurityConfiguration;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseTypeDefinitionVersion;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createevent.AuthorisedCreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.AuthorisedGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.AuthorisedStartEventOperation;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.service.ServiceRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;

@Provider("ccd")
@PactBroker(scheme = "${pact.broker.scheme}",host = "${pact.broker.baseUrl}", port = "${pact.broker.port}", tags={"${pact.broker.consumer.tag}"})
//@PactFolder(value = "probate")
@RunWith(SpringRestPactRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
    "server.port=8125", "spring.application.name=PACT_TEST"
})
@TestPropertySource(locations = "classpath:application-pact.properties")
@OverrideAutoConfiguration(enabled = true)
//@EnableAutoConfiguration(exclude = {SecurityConfiguration.class})
public class ProbateSubmitServiceProviderTest {

    @MockBean
    private CachedCaseDetailsRepository caseDetailsRepository;

    @MockBean
    private UIDService uidService;

    @TestTarget
    @SuppressWarnings(value = "VisibilityModifier")
    public final Target target = new HttpTarget("http", "localhost", 8125, "/");


    private static final String PRINCIPAL = "ccd_data";

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String AUTHORIZATION = "Authorization";

    @MockBean
    private SubjectResolver<Service> serviceResolver;

    @MockBean
    private UserRequestAuthorizer<User> userRequestAuthorizer;

    @MockBean
    ServiceRequestAuthorizer serviceRequestAuthorizer;

    @MockBean
    private HttpServletRequest request;

    @MockBean
    private DefaultUserRepository defaultUserRepository;

    @MockBean
    private CachedUserRepository cachedUserRepository;

    @MockBean
    private CaseUserRepository caseUserRepository;
    @MockBean
    private DefaultCaseDefinitionRepository defaultCaseDefinitionRepository;

    @MockBean
    private CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

    @MockBean
    private AuthorisedGetCaseOperation getCaseOperation;

    @MockBean
    private AuthorisedStartEventOperation authorisedStartEventOperation;

    @MockBean
    private AuthorisedCreateEventOperation authorisedCreateEventOperation;


    private Service service;

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    //
//    @Value("${pact.broker.version}")
//    private String providerVersion;
//
    @Before
    public void setUpTest() {

        service = new Service(PRINCIPAL);
        when(serviceResolver.getTokenDetails(anyString())).thenReturn(service);
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(service);
        Set<String> roles = new HashSet<>();
        User user = new User(PRINCIPAL,roles);
        when(userRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(user);
        // System.getProperties().setProperty("pact.verifier.publishResults", "true");
        //System.getProperties().setProperty("pact.provider.version", providerVersion);
    }

    @State("A start request for citizen is requested")
    public void toCheckStartRequestForCitizen() {
        StartEventTrigger startEventTrigger = new StartEventTrigger();
        startEventTrigger.setEventId("GOP_UPDATE_DRAFT");
        startEventTrigger.setToken("123234543456");
        when(authorisedStartEventOperation.triggerStartForCase("654321", "updateDraft", null)).thenReturn(startEventTrigger);

    }

    @State("A submit request for citizen is requested")
    public void toCheckSubmitRequestForCitizen() {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("deceasedMartialStatus", JSON_NODE_FACTORY.textNode("divorcedCivilPartnership"));
        data.put("primaryApplicantPhoneNumber", JSON_NODE_FACTORY.textNode("123455678"));
        data.put("declarationCheckbox", JSON_NODE_FACTORY.textNode("No"));
        data.put("childrenOverEighteenSurvived", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("primaryApplicantAdoptionInEnglandOrWales", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("uploadDocumentUrl", JSON_NODE_FACTORY.textNode("http://document-management/document/12345"));
        data.put("childrenDied", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedSpouseNotApplyingReason", JSON_NODE_FACTORY.textNode("mentallyIncapable"));
        data.put("ihtFormCompletedOnline", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtGrossValue", JSON_NODE_FACTORY.textNode("100000"));
        data.put("applicationType", JSON_NODE_FACTORY.textNode("Personal"));
        data.put("ihtNetValue", JSON_NODE_FACTORY.textNode("100000"));
        data.put("deceasedAnyOtherNames", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtReferenceNumber", JSON_NODE_FACTORY.textNode("GOT123456"));
        data.put("deceasedAnyChildren", JSON_NODE_FACTORY.textNode("No"));
        data.put("outsideUKGrantCopies", JSON_NODE_FACTORY.numberNode(6));
        data.put("primaryApplicantRelationshipToDeceased", JSON_NODE_FACTORY.textNode("adoptedChild"));
        data.put("extraCopiesOfGrant", JSON_NODE_FACTORY.numberNode(3));
        data.put("deceasedForenames", JSON_NODE_FACTORY.textNode("Ned"));
        data.put("primaryApplicantEmailAddress", JSON_NODE_FACTORY.textNode("someEmailAddress.com"));
        data.put("primaryApplicantSurname", JSON_NODE_FACTORY.textNode("Snow"));
        data.put("primaryApplicantForenames", JSON_NODE_FACTORY.textNode("Jon"));
        data.put("deceasedHasAssetsOutsideUK", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedDivorcedInEnglandOrWales", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedDateOfBirth", JSON_NODE_FACTORY.textNode("1930-01-01"));
        data.put("assetsOverseasNetValue", JSON_NODE_FACTORY.textNode("10050"));
        data.put("primaryApplicantAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedOtherChildren", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("grandChildrenSurvivedUnderEighteen", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtFormId", JSON_NODE_FACTORY.textNode("IHT205"));
        data.put("type", JSON_NODE_FACTORY.textNode("GrantOfRepresentation"));
        data.put("registryLocation", JSON_NODE_FACTORY.textNode("Oxford"));
        data.put("deceasedSurname", JSON_NODE_FACTORY.textNode("Stark"));
        data.put("deceasedDateOfDeath", JSON_NODE_FACTORY.textNode("2018-01-01"));
        ArrayNode deceasedAliasNameList = JSON_NODE_FACTORY.arrayNode();
        ObjectNode deceasedAliasNameObject = JSON_NODE_FACTORY.objectNode();
        ObjectNode deceasedAliasNameObjectContent = JSON_NODE_FACTORY.objectNode();
        deceasedAliasNameObjectContent.set("Forenames", JSON_NODE_FACTORY.textNode("King"));
        deceasedAliasNameObjectContent.set("LastName", JSON_NODE_FACTORY.textNode("North"));
        deceasedAliasNameObject.set("value", deceasedAliasNameObjectContent);
        ObjectNode deceasedAliasNameObject1 = JSON_NODE_FACTORY.objectNode();
        ObjectNode deceasedAliasNameObjectContent1 = JSON_NODE_FACTORY.objectNode();
        deceasedAliasNameObjectContent1.set("Forenames", JSON_NODE_FACTORY.textNode("King"));
        deceasedAliasNameObjectContent1.set("LastName", JSON_NODE_FACTORY.textNode("North"));
        deceasedAliasNameObject.set("value", deceasedAliasNameObjectContent);
        deceasedAliasNameObject1.set("value", deceasedAliasNameObjectContent1);
        deceasedAliasNameList.add(deceasedAliasNameObject);
        deceasedAliasNameList.add(deceasedAliasNameObject1);
        data.put("deceasedAliasNameList", deceasedAliasNameList);
        data.put("primaryApplicantForenames", JSON_NODE_FACTORY.textNode("Jon"));
        ObjectNode primaryApplicantAddress = JSON_NODE_FACTORY.objectNode();
        primaryApplicantAddress.set("AddressLine2", JSON_NODE_FACTORY.textNode("St. Georges Hospital"));
        primaryApplicantAddress.put("AddressLine1", JSON_NODE_FACTORY.textNode("Pret a Manger"));
        primaryApplicantAddress.put("AddressLine2", JSON_NODE_FACTORY.textNode("St. Georges Hospital"));
        primaryApplicantAddress.put("PostTown", JSON_NODE_FACTORY.textNode("London"));
        primaryApplicantAddress.put("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        data.put("primaryApplicantAddress", primaryApplicantAddress);
        ObjectNode deceasedAddress = JSON_NODE_FACTORY.objectNode();
        deceasedAddress.put("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        deceasedAddress.put("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        deceasedAddress.put("PostTown", JSON_NODE_FACTORY.textNode("London"));
        deceasedAddress.put("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        data.put("deceasedAddress", deceasedAddress);


        ArrayNode executorsApplyingList = JSON_NODE_FACTORY.arrayNode();
        ObjectNode executorsApplyingListObject = JSON_NODE_FACTORY.objectNode();
        ObjectNode executorsApplyingListObjectContent = JSON_NODE_FACTORY.objectNode();
        executorsApplyingListObjectContent.set("applyingExecutorPhoneNumber", JSON_NODE_FACTORY.textNode("07981898999"));
        executorsApplyingListObjectContent.set("applyingExecutorInvitationId", JSON_NODE_FACTORY.textNode("54321"));
        executorsApplyingListObjectContent.set("applyingExecutorAgreed", JSON_NODE_FACTORY.textNode("Yes"));
        executorsApplyingListObjectContent.set("applyingExecutorOtherNames", JSON_NODE_FACTORY.textNode("Graham Poll"));
        executorsApplyingListObjectContent.set("applyingExecutorEmail", JSON_NODE_FACTORY.textNode("address@email.com"));
        executorsApplyingListObjectContent.set("applyingExecutorLeadName", JSON_NODE_FACTORY.textNode("Graham Garderner"));
        executorsApplyingListObjectContent.set("applyingExecutorName", JSON_NODE_FACTORY.textNode("Jon Snow"));
        executorsApplyingListObjectContent.set("applyingExecutorOtherNamesReason", JSON_NODE_FACTORY.textNode("Divorce"));
        ObjectNode applyingExecutorAddress = JSON_NODE_FACTORY.objectNode();
        applyingExecutorAddress.set("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        applyingExecutorAddress.set("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        applyingExecutorAddress.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        applyingExecutorAddress.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        executorsApplyingListObjectContent.set("applyingExecutorAddress", applyingExecutorAddress);

        executorsApplyingListObject.set("value", executorsApplyingListObjectContent);

        ObjectNode executorsApplyingListObject1 = JSON_NODE_FACTORY.objectNode();
        ObjectNode executorsApplyingListObjectContent1 = JSON_NODE_FACTORY.objectNode();
        executorsApplyingListObjectContent1.set("applyingExecutorPhoneNumber", JSON_NODE_FACTORY.textNode("07981898999"));
        executorsApplyingListObjectContent1.set("applyingExecutorInvitationId", JSON_NODE_FACTORY.textNode("54321"));
        executorsApplyingListObjectContent1.set("applyingExecutorAgreed", JSON_NODE_FACTORY.textNode("Yes"));
        executorsApplyingListObjectContent1.set("applyingExecutorOtherNames", JSON_NODE_FACTORY.textNode("Graham Poll"));
        executorsApplyingListObjectContent1.set("applyingExecutorEmail", JSON_NODE_FACTORY.textNode("address@email.com"));
        executorsApplyingListObjectContent1.set("applyingExecutorLeadName", JSON_NODE_FACTORY.textNode("Graham Garderner"));
        executorsApplyingListObjectContent1.set("applyingExecutorName", JSON_NODE_FACTORY.textNode("Jon Snow"));
        executorsApplyingListObjectContent1.set("applyingExecutorOtherNamesReason", JSON_NODE_FACTORY.textNode("Divorce"));

        ObjectNode applyingExecutorAddress1 = JSON_NODE_FACTORY.objectNode();
        applyingExecutorAddress1.set("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        applyingExecutorAddress1.set("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        applyingExecutorAddress1.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        applyingExecutorAddress1.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        executorsApplyingListObjectContent1.set("applyingExecutorAddress", applyingExecutorAddress);
        executorsApplyingListObject1.set("value", executorsApplyingListObjectContent1);
        executorsApplyingList.add(executorsApplyingListObject);
        executorsApplyingList.add(executorsApplyingListObject1);
        data.put("executorsApplying", executorsApplyingList);


        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails()
            .withId("654321")
            .withReference(654321L)
            .withCaseTypeId("GRANT_OF_REPRESENTATION")
            .withData(data)
            .withState("Draft").build();
        when(authorisedCreateEventOperation.createCaseEvent(eq("654321"), any(CaseDataContent.class))).thenReturn(caseDetails);
    }

    @State("A GrantOfRepresentation case exists")
    public void toCheckGrantOfRepresentationCase654321Exists() {
        List<String> caseRoles = new ArrayList<>();
        when(caseUserRepository.findCaseRoles(anyLong(), anyString())).thenReturn(caseRoles);
        CaseDataService caseDataService = new CaseDataService();
        when(uidService.validateUID("654321")).thenReturn(true);
        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        when(defaultUserRepository.getUserRoles()).thenReturn(roles);
        Set<SecurityClassification> securityClassification = new HashSet<>();
        securityClassification.add(SecurityClassification.PUBLIC);
        when(cachedUserRepository.getUserClassifications("PROBATE")).thenReturn(securityClassification);
        CaseTypeDefinitionVersion caseTypeDefinition = new CaseTypeDefinitionVersion();
        caseTypeDefinition.setVersion(101);
        when(defaultCaseDefinitionRepository.getLatestVersion("GRANT_OF_REPRESENTATION")).thenReturn(caseTypeDefinition);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("outsideUKGrantCopies", JSON_NODE_FACTORY.numberNode(6));
        data.put("applicationType", JSON_NODE_FACTORY.textNode("Personal"));
        data.put("deceasedAnyOtherNames", JSON_NODE_FACTORY.textNode("No"));
        data.put("primaryApplicantAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedAnyChildren", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedMartialStatus", JSON_NODE_FACTORY.textNode("widowed"));
        data.put("primaryApplicantPhoneNumber", JSON_NODE_FACTORY.textNode("123455678"));
        data.put("declarationCheckbox", JSON_NODE_FACTORY.textNode("No"));
        data.put("childrenOverEighteenSurvived", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("primaryApplicantAdoptionInEnglandOrWales", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("uploadDocumentUrl", JSON_NODE_FACTORY.textNode("http://document-management/document/12345"));
        data.put("childrenDied", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtGrossValue", JSON_NODE_FACTORY.textNode("100000"));
        data.put("applicationType", JSON_NODE_FACTORY.textNode("Personal"));
        data.put("ihtNetValue", JSON_NODE_FACTORY.textNode("100000"));
        data.put("deceasedAnyOtherNames", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedAnyChildren", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtReferenceNumber", JSON_NODE_FACTORY.textNode("GOT123456"));
        data.put("outsideUKGrantCopies", JSON_NODE_FACTORY.numberNode(6));
        data.put("extraCopiesOfGrant", JSON_NODE_FACTORY.numberNode(3));
        data.put("primaryApplicantRelationshipToDeceased", JSON_NODE_FACTORY.textNode("adoptedChild"));
        data.put("deceasedSpouseNotApplyingReason", JSON_NODE_FACTORY.textNode("mentallyIncapable"));
        data.put("ihtFormCompletedOnline", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedForenames", JSON_NODE_FACTORY.textNode("Ned"));
        data.put("primaryApplicantEmailAddress", JSON_NODE_FACTORY.textNode("someEmailAddress.com"));
        data.put("primaryApplicantSurname", JSON_NODE_FACTORY.textNode("Snow"));
        data.put("primaryApplicantForenames", JSON_NODE_FACTORY.textNode("Jon"));
        data.put("deceasedHasAssetsOutsideUK", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedDivorcedInEnglandOrWales", JSON_NODE_FACTORY.textNode("No"));
        data.put("deceasedDateOfBirth", JSON_NODE_FACTORY.textNode("1930-01-01"));
        data.put("assetsOverseasNetValue", JSON_NODE_FACTORY.textNode("10050"));
        data.put("primaryApplicantAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("deceasedOtherChildren", JSON_NODE_FACTORY.textNode("Yes"));
        data.put("grandChildrenSurvivedUnderEighteen", JSON_NODE_FACTORY.textNode("No"));
        data.put("ihtFormId", JSON_NODE_FACTORY.textNode("IHT205"));
        data.put("type", JSON_NODE_FACTORY.textNode("GrantOfRepresentation"));
        data.put("registryLocation", JSON_NODE_FACTORY.textNode("Oxford"));
        data.put("deceasedSurname", JSON_NODE_FACTORY.textNode("Stark"));
        data.put("primaryApplicantAddressFound", JSON_NODE_FACTORY.textNode("Yes"));
        ObjectNode deceasedAddress = JSON_NODE_FACTORY.objectNode();
        deceasedAddress.set("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        deceasedAddress.set("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        deceasedAddress.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        deceasedAddress.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        data.put("deceasedAddress", deceasedAddress);
        ObjectNode primaryApplicantAddress =
            JSON_NODE_FACTORY.objectNode();
        primaryApplicantAddress.set("AddressLine1", JSON_NODE_FACTORY.textNode("Pret a Manger"));
        primaryApplicantAddress.set("AddressLine2", JSON_NODE_FACTORY.textNode("St. Georges Hospital"));
        primaryApplicantAddress.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        primaryApplicantAddress.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));

        data.put("primaryApplicantAddress", primaryApplicantAddress);
        data.put("deceasedDateOfDeath", JSON_NODE_FACTORY.textNode("2018-01-01"));
        ArrayNode deceasedAliasNameList = JSON_NODE_FACTORY.arrayNode();
        ObjectNode deceasedAliasNameList1 = JSON_NODE_FACTORY.objectNode();
        ObjectNode deceasedAliasNameList1Content = JSON_NODE_FACTORY.objectNode();
        deceasedAliasNameList1Content.set("Forenames", JSON_NODE_FACTORY.textNode("King"));
        deceasedAliasNameList1Content.set("LastName", JSON_NODE_FACTORY.textNode("North"));
        ObjectNode deceasedAliasNameList2 = JSON_NODE_FACTORY.objectNode();
        ObjectNode deceasedAliasNameList2Content = JSON_NODE_FACTORY.objectNode();
        deceasedAliasNameList2Content.set("Forenames", JSON_NODE_FACTORY.textNode("King"));
        deceasedAliasNameList2Content.set("LastName", JSON_NODE_FACTORY.textNode("North"));
        deceasedAliasNameList1.set("value", deceasedAliasNameList1Content);
        deceasedAliasNameList2.set("value", deceasedAliasNameList2Content);
        deceasedAliasNameList.add(deceasedAliasNameList1);
        deceasedAliasNameList.add(deceasedAliasNameList2);
        data.put("deceasedAliasNameList", deceasedAliasNameList);
        FieldType fieldType = TestBuildersUtil.FieldTypeBuilder.aFieldType().withType("Label").build();
        CaseField outsideUKGrantCopies = TestBuildersUtil.CaseFieldBuilder.newCaseField().withId("outsideUKGrantCopies").withSC(SecurityClassification.PUBLIC.name()).withFieldType(fieldType).build();
        CaseType caseType = TestBuildersUtil.CaseTypeBuilder.newCaseType().withId("GRANT_OF_REPRESENTATION")
            .withField(outsideUKGrantCopies)
            .withSecurityClassification(SecurityClassification.PUBLIC).build();
        when(cachedCaseDefinitionRepository.getCaseType("GRANT_OF_REPRESENTATION")).thenReturn(caseType);

        ArrayNode executorsApplying = JSON_NODE_FACTORY.arrayNode();
        ObjectNode executorsApplyingObject1 = JSON_NODE_FACTORY.objectNode();
        ObjectNode executorsApplyingObjectContent1 = JSON_NODE_FACTORY.objectNode();
        executorsApplyingObjectContent1.set("applyingExecutorLeadName", JSON_NODE_FACTORY.textNode("Graham Garderner"));
        executorsApplyingObjectContent1.set("applyingExecutorOtherNames", JSON_NODE_FACTORY.textNode("Graham Poll"));
        executorsApplyingObjectContent1.set("applyingExecutorPhoneNumber", JSON_NODE_FACTORY.textNode("07981898999"));
        executorsApplyingObjectContent1.set("applyingExecutorAgreed", JSON_NODE_FACTORY.textNode("Yes"));
        executorsApplyingObjectContent1.set("applyingExecutorName", JSON_NODE_FACTORY.textNode("Jon Snow"));
        executorsApplyingObjectContent1.set("applyingExecutorEmail", JSON_NODE_FACTORY.textNode("address@email.com"));
        executorsApplyingObjectContent1.set("applyingExecutorInvitationId", JSON_NODE_FACTORY.textNode("54321"));
        executorsApplyingObjectContent1.set("applyingExecutorOtherNamesReason", JSON_NODE_FACTORY.textNode("Divorce"));
        ObjectNode applyingExecutorAddress1 = JSON_NODE_FACTORY.objectNode();


        applyingExecutorAddress1.set("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        applyingExecutorAddress1.set("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        applyingExecutorAddress1.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        applyingExecutorAddress1.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        executorsApplyingObjectContent1.set("applyingExecutorAddress", applyingExecutorAddress1);
        executorsApplyingObject1.set("value", executorsApplyingObjectContent1);
        executorsApplying.add(executorsApplyingObject1);

        ObjectNode executorsApplyingObject2 = JSON_NODE_FACTORY.objectNode();
        ObjectNode executorsApplyingObjectContent2 = JSON_NODE_FACTORY.objectNode();
        executorsApplyingObjectContent2.set("applyingExecutorLeadName", JSON_NODE_FACTORY.textNode("Graham Garderner"));
        executorsApplyingObjectContent2.set("applyingExecutorOtherNames", JSON_NODE_FACTORY.textNode("Graham Poll"));
        executorsApplyingObjectContent2.set("applyingExecutorPhoneNumber", JSON_NODE_FACTORY.textNode("07981898999"));
        executorsApplyingObjectContent2.set("applyingExecutorAgreed", JSON_NODE_FACTORY.textNode("Yes"));
        executorsApplyingObjectContent2.set("applyingExecutorName", JSON_NODE_FACTORY.textNode("Jon Snow"));
        executorsApplyingObjectContent2.set("applyingExecutorEmail", JSON_NODE_FACTORY.textNode("address@email.com"));
        executorsApplyingObjectContent2.set("applyingExecutorInvitationId", JSON_NODE_FACTORY.textNode("54321"));
        executorsApplyingObjectContent2.set("applyingExecutorOtherNamesReason", JSON_NODE_FACTORY.textNode("Divorce"));
        ObjectNode applyingExecutorAddress2 = JSON_NODE_FACTORY.objectNode();

        applyingExecutorAddress2.set("AddressLine2", JSON_NODE_FACTORY.textNode("Westeros"));
        applyingExecutorAddress2.set("AddressLine1", JSON_NODE_FACTORY.textNode("Winterfell"));
        applyingExecutorAddress2.set("PostTown", JSON_NODE_FACTORY.textNode("London"));
        applyingExecutorAddress2.set("PostCode", JSON_NODE_FACTORY.textNode("SW17 0QT"));
        executorsApplyingObjectContent2.set("applyingExecutorAddress", applyingExecutorAddress2);

        executorsApplyingObject2.set("value", executorsApplyingObjectContent2);
        executorsApplying.add(executorsApplyingObject2);

        data.put("executorsApplying", executorsApplying);

        CaseDetails caseDetails = TestBuildersUtil.CaseDetailsBuilder.newCaseDetails().withId("654321")
            .withJurisdiction("PROBATE")
            .withCaseTypeId("GRANT_OF_REPRESENTATION")
            .withSecurityClassification(SecurityClassification.PUBLIC)
            .withData(data)
            .withReference(654321L)
            .withDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, data, Maps.newHashMap()))
            .build();
        caseDetails.setState("Draft");
        caseDetails.setCreatedDate(LocalDateTime.now());
        when(getCaseOperation.execute("654321")).thenReturn(Optional.of(caseDetails));

        when(caseDetailsRepository.findByReference(654321L)).thenReturn(caseDetails);
    }


}

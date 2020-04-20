package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CaseDocumentAttacherTest {

    @InjectMocks
    private CaseDocumentAttacher caseDocumentAttacher;

    HashMap<String, JsonNode> caseDetailsBefore;
    HashMap<String, JsonNode> caseDataContent;
    CaseDetails caseDetails;
    Map<String,String> beforeCallBack;
    CaseDocumentsMetadata  caseDocumentsMetadata;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetails();
        caseDetailsBefore = buildCaseData("case-detail-before-update.json");
        caseDataContent = buildCaseData("case-detail-after-update.json");
        caseDetails.setData(caseDetailsBefore);

    }


    @Test
    @DisplayName("should return document fields differences once updated ")
    void shouldReturnDeltaWhenDocumentFieldsUpdate() {
        Set<String> expectedOutput = new HashSet();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails,caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }
    @Test
    @DisplayName("should return document fields differences once document Fields inside Complex Field ")
    void shouldReturnDeltaWhenDocumentFieldsInsideComplexElement() throws IOException {
        HashMap<String, JsonNode> caseDataContent = buildCaseData("case-detail-after-with-complexFields-update.json");
        Set<String> expectedOutput = new HashSet();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails,caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }


    @Test
    @DisplayName("should return empty document set in case of CaseDataContent is empty")
    void shouldReturnEmptyDocumentSet() {
        caseDataContent=null;
        Set<String> expectedOutput = new HashSet();
        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails,caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }

    @Test
    @DisplayName("should  filter the Case Document Meta Data while  2 documents with hashcode  from request and  2 new documents without hash token from callback response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_1() {

        prepareInputs();

        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6",null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6",null);
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6",null);
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292",null);

        List<DocumentHashToken> expected = Arrays.asList(DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
                                                         DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build());

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack);

        List<DocumentHashToken> actual=caseDocumentsMetadata.getDocuments();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should  filter the Case Document Meta Data while  2 documents with hashcode  from request and  2 new documents with hash token from callback response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_2() {
        prepareInputs();
        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6","4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6","4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6",null);
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292",null);

        List<DocumentHashToken> expected = Arrays.asList(DocumentHashToken.builder().id("f5bd63a2-65c5-435e-a972-98ed658ad7d6")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d89a2d041b63").build(),
                                                         DocumentHashToken.builder().id("320233b8-fb61-4b58-8731-23c83638c9c6")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d89a2d041b53").build(),
                                                         DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
                                                         DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                                                                          .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
                                                        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack);

        List<DocumentHashToken> actual=caseDocumentsMetadata.getDocuments();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should  filter the Case Document Meta Data while  2 documents with hashcode  from request and  replace 1 documents without hash token from callback response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_3() {
        prepareInputs();

        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6",null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6",null);

        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                             .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build()

                                                        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack);

        List<DocumentHashToken> actual=caseDocumentsMetadata.getDocuments();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should  filter the Case Document Meta Data while  2 documents with hashcode from request and  replace 1 documents with hash token from callback response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_4() {
        prepareInputs();
        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6",null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6","4d49edc151423fb7b2e1f22d89a2d041b63");


        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("f5bd63a2-65c5-435e-a972-98ed658ad7d6")
                             .hashToken("4d49edc151423fb7b2e1f22d89a2d041b63").build(),
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                             .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build()
                                                        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack);

        List<DocumentHashToken> actual=caseDocumentsMetadata.getDocuments();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should  filter the Case Document Meta Data while  2 documents with hashcode from request and  no response from callback ")
    void shouldFilterCaseDocumentMetaData_With_Scenario_5() {
        prepareInputs();
        Map<String,String> afterCallBack = new HashMap<>();
        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                             .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                             .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
                                                        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack);

        List<DocumentHashToken> actual=caseDocumentsMetadata.getDocuments();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should  throw Service Exception with 500 While all hashToken tempered of user provided documents by Callback Service ")
    void shouldThrowExceptionWhileHashTokenTempered_Scenario_6() {
        prepareInputs();
        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6","4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6","4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6","4d49edc151423fb7b2e1f22d89a2d056234");
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292","4d49edc151423fb7b2e1f2230975328jk89");


        ServiceException exception=  Assertions.assertThrows(ServiceException.class,
                                                             () ->  caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack));
        Assertions.assertTrue(exception.getMessage().contains("call back attempted to change the hashToken of the following documents:[b6ee2bff-8244-431f-94ec-9d8ecace8dd6, e16f2ae0-d6ce-4bd0-a652-47b3c4d86292]"));
    }

    @Test
    @DisplayName("should  throw Service Exception with 500 While only one hashToken tempered of user provided documents by Callback Service ")
    void shouldThrowExceptionWhileHashTokenTempered_Scenario_7() {
        prepareInputs();
        Map<String,String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6","4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6","4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6","4d49edc151423fb7b2e1f22d89a2d056234");
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292",null);


        ServiceException exception=  Assertions.assertThrows(ServiceException.class,
                                                             () ->  caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata,beforeCallBack,afterCallBack));
        Assertions.assertTrue(exception.getMessage().contains("call back attempted to change the hashToken of the following documents:[b6ee2bff-8244-431f-94ec-9d8ecace8dd6]"));
    }




    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            CaseDocumentAttacherTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });
    }

    private void prepareInputs(){
        beforeCallBack = new HashMap<>();
        beforeCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6","4d49edc151423fb7b2e1f22d89a2d041b43");
        beforeCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292","4d49edc151423fb7b2e1f22d87b2d041b34");

        caseDocumentsMetadata = CaseDocumentsMetadata.builder()
                                                     .caseId("12345556")
                                                     .caseTypeId("BEFTA_CASETYPE_2")
                                                     .jurisdictionId("BEFTA_JURISDICTION_2")
                                                     .documents(new ArrayList<>())
                                                     .build();

    }
}


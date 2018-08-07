package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;

class CaseServiceTest {

    private static final String JURISDICTION = "SSCS";
    private static final String STATE = "CreatedState";
    private static final Long REFERENCE = 1234123412341236L;
    private static final String DATA_PERSON = "Person";
    private static final String DATA_NAMES = "Names";
    private static final String DATA_FNAME = "FirstName";
    private static final String PERSON_FNAME = "Jack";
    private static final String OTHER_NAME = "John";

    private CaseDataService caseDataService;
    private CaseService caseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDataService = new CaseDataService();
        caseService = new CaseService(caseDataService);
    }

    @Nested
    @DisplayName("clone()")
    class Clone {

        @Test
        @DisplayName("should clone case details")
        void shouldCloneCaseDetails() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            assertAll(
                () -> assertThat(clone, not(sameInstance(caseDetails))),
                () -> assertThat(clone.getJurisdiction(), equalTo(JURISDICTION)),
                () -> assertThat(clone.getReference(), equalTo(REFERENCE)),
                () -> assertThat(clone.getState(), equalTo(STATE))
            );
        }

        @Test
        @DisplayName("should deep clone case data")
        void shouldDeepCloneCaseData() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            final Map<String, JsonNode> data = caseDetails.getData();
            final Map<String, JsonNode> cloneData = clone.getData();

            final JsonNode person = data.get(DATA_PERSON);
            final JsonNode clonePerson = cloneData.get(DATA_PERSON);

            final JsonNode names = person.get(DATA_NAMES);
            final JsonNode cloneNames = clonePerson.get(DATA_NAMES);

            // Change name
            ((ObjectNode) names).set(DATA_FNAME, new TextNode(OTHER_NAME));

            final JsonNode fname = names.get(DATA_FNAME);
            final JsonNode cloneFname = cloneNames.get(DATA_FNAME);

            assertAll(
                () -> assertThat(cloneData, not(sameInstance(data))),
                () -> assertThat(clonePerson, not(sameInstance(person))),
                () -> assertThat(cloneNames, not(sameInstance(names))),
                () -> assertThat(cloneFname, not(sameInstance(fname))),
                () -> assertThat(cloneFname.asText(), equalTo(PERSON_FNAME)),
                () -> assertThat(fname.asText(), equalTo(OTHER_NAME))
            );
        }

        @Test
        @DisplayName("should deep clone case data classification")
        void shouldDeepCloneCaseDataClassification() {
            final CaseDetails caseDetails = buildCaseDetails();

            final CaseDetails clone = caseService.clone(caseDetails);

            final Map<String, JsonNode> classification = caseDetails.getDataClassification();
            final Map<String, JsonNode> cloneClassification = clone.getDataClassification();

            final JsonNode person = classification.get(DATA_PERSON);
            final JsonNode clonePerson = cloneClassification.get(DATA_PERSON);

            final JsonNode names = person.get(DATA_NAMES);
            final JsonNode cloneNames = clonePerson.get(DATA_NAMES);

            // Change name classification
            ((ObjectNode) names).set(DATA_FNAME, new TextNode("PRIVATE"));

            final JsonNode fname = names.get(DATA_FNAME);
            final JsonNode cloneFname = cloneNames.get(DATA_FNAME);

            assertAll(
                () -> assertThat(cloneClassification, not(sameInstance(classification))),
                () -> assertThat(clonePerson, not(sameInstance(person))),
                () -> assertThat(cloneNames, not(sameInstance(names))),
                () -> assertThat(cloneFname, not(sameInstance(fname))),
                () -> assertThat(cloneFname.asText(), equalTo("PUBLIC")),
                () -> assertThat(fname.asText(), equalTo("PRIVATE"))
            );
        }
    }

    private CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION);
        caseDetails.setReference(REFERENCE);
        caseDetails.setState(STATE);

        caseDetails.setData(buildCaseData());
        caseDetails.setDataClassification(buildCaseDataClassification());

        return caseDetails;
    }

    private Map<String, JsonNode> buildCaseDataStructure(String fnameValue) {
        final HashMap<String, JsonNode> data = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();

        final ObjectNode namesNode = objectMapper.createObjectNode();
        namesNode.set(DATA_FNAME, new TextNode(fnameValue));

        final ObjectNode personNode = objectMapper.createObjectNode();
        personNode.set(DATA_NAMES, namesNode);

        data.put(DATA_PERSON, personNode);

        return data;
    }

    private Map<String, JsonNode> buildCaseData() {
        return buildCaseDataStructure(PERSON_FNAME);
    }

    private Map<String, JsonNode> buildCaseDataClassification() {
        return buildCaseDataStructure("PUBLIC");
    }

}

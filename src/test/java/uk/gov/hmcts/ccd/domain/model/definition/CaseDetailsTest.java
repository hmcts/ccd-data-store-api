package uk.gov.hmcts.ccd.domain.model.definition;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseDetailsTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_DETAIL_FIELD = "dataTestField1";

    private CaseDetails caseDetails;

    @BeforeEach
    public void setup() {
        caseDetails = new CaseDetails();
        Map<String, JsonNode> dataMap = buildData(CASE_DETAIL_FIELD);
        caseDetails.setData(dataMap);
    }

    @Test
    void testExistsInDataIsAlwaysTrueForLabels() {
        CaseTypeTabField tabField = createCaseTypeTabField("someId", "Label");

        assertThat(caseDetails.existsInData(tabField), equalTo(true));
    }

    @Test
    void testExistsInDataIsAlwaysTrueForCasePaymentHistoryViewer() {
        CaseTypeTabField tabField = createCaseTypeTabField("someId", "CasePaymentHistoryViewer");

        assertThat(caseDetails.existsInData(tabField), equalTo(true));
    }

    @Test
    void testExistsInDataIsFalseIfTabFieldDoesNotBelongToCase() {
        CaseTypeTabField tabField = createCaseTypeTabField("someId2", "YesOrNo");

        assertThat(caseDetails.existsInData(tabField), equalTo(false));
    }

    @Test
    void testExistsInDataIsTrueIfTabFieldBelongsToCase() {
        CaseTypeTabField tabField = createCaseTypeTabField(CASE_DETAIL_FIELD, "YesOrNo");

        assertThat(caseDetails.existsInData(tabField), equalTo(true));
    }

    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        Lists.newArrayList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }

    private CaseTypeTabField createCaseTypeTabField(String id, String type) {
        CaseTypeTabField tabField = new CaseTypeTabField();
        CaseField caseField = new CaseField();
        caseField.setId(id);
        FieldType labelFieldType = new FieldType();
        labelFieldType.setType(type);
        caseField.setFieldType(labelFieldType);
        tabField.setCaseField(caseField);
        return tabField;
    }


}

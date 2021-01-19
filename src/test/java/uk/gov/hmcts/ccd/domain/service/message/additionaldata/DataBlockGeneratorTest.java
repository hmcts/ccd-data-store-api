package uk.gov.hmcts.ccd.domain.service.message.additionaldata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DataBlockGeneratorTest {

    @InjectMocks
    private DataBlockGenerator dataBlockGenerator;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String FIELD_ID = "FieldId";
    private static final String FIELD_ALIAS = "FieldAlias";
    private static final String TEXT_FIELD = "TextValue";
    private static final String NESTED_FIELD_1 = "NestedField1";
    private static final String NESTED_FIELD_2 = "NestedField2";
    private static final String SUB_NESTED_FIELD_1 = "SubNestedField1";
    private static final String SUB_NESTED_FIELD_2 = "SubNestedField2";
    private static final String COMPLEX_ID_1 = "ComplexType1";
    private static final String COMPLEX_ID_2 = "ComplexType2";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        caseEventDefinition = objectMapper.readValue(caseEventDefinitionString(), CaseEventDefinition.class);
        caseTypeDefinition = objectMapper.readValue(caseTypeDefinitionString(), CaseTypeDefinition.class);
        caseDetails = objectMapper.readValue(caseDetailsString(), CaseDetails.class);
    }

    @Ignore
    @Test
    void generateDefinitionForSimpleDataFields() throws JsonProcessingException {
        AdditionalDataContext context = new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        String stringResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        System.out.println(stringResult);

        assertEquals(result.toString(), "{AddressField={\"AddressLine1\":\"lin 1\",\"AddressLine2\":\"line 2\",\"AddressLine3\":\"line 3\",\"Country\":\"country\"}, AddressUKField={\"AddressLine1\":null,\"AddressLine2\":null,\"AddressLine3\":null,\"PostTown\":null,\"County\":null,\"PostCode\":null,\"Country\":null}, MoneyField=60000, DocumentField1=null, MarritalStatus=\"CIVIL_PARTNERSHIP\", NumberField=66, MultiSelectField=[\"MANCHESTER\",\"CARDIFF\"], EmailField=\"test@test.com\", YesNoField=false, PhoneField=\"07971238417\", TextField=\"Text field\", DateField=\"2000-01-01\", CollectionFieldMan=[], TextAreaField=\"text area\", CollectionField=[{\"value\":\"collextion field\",\"id\":\"d06f0976-4183-403d-955e-c9b54aad3bc0\"},{\"value\":\"collection field two\",\"id\":\"f2ab95f3-0eb9-43dc-8f5b-b9524ac8e518\"}]}");
    }

    @Test
    void shouldBuildDataForPublishableSimpleBooleanField() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("dontPublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build(),
                newCaseField()
                    .withId("dontPublished")
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("No", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ID), false),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleNumberField() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .build(),
                newCaseEventField()
                    .withCaseFieldId("dontPublished")
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.MONEY_GBP)
                        .withType(FieldTypeDefinition.MONEY_GBP)
                        .build())
                    .build(),
                newCaseField()
                    .withId("dontPublished")
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.YES_OR_NO)
                        .withType(FieldTypeDefinition.YES_OR_NO)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("60", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ID), 60),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleTextFieldAlias() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .withPublishAs(FIELD_ALIAS)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.TEXT)
                        .withType(FieldTypeDefinition.TEXT)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue(TEXT_FIELD, JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ALIAS), "TextValue"),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForPublishableSimpleAddressUkField() throws JsonProcessingException {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.MANDATORY)
                    .withPublish(true)
                    .withPublishAs(FIELD_ALIAS)
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(aFieldType()
                        .withId(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK)
                        .withType(FieldTypeDefinition.PREDEFINED_COMPLEX_ADDRESS_UK)
                        .build())
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
        data.put(FIELD_ID, mapper.convertValue("\"AddressField\" : {\n" +
            "    \"AddressLine1\" : \"lin 1\",\n" +
            "    \"AddressLine2\" : \"line 2\",\n" +
            "    \"AddressLine3\" : \"line 3\",\n" +
            "    \"Country\" : \"country\"\n" +
            "  }", JsonNode.class));

        caseDetails = newCaseDetails().withData(data).build();

        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertEquals(result.get(FIELD_ALIAS), "\"AddressField\" : {\n" +
                "    \"AddressLine1\" : \"lin 1\",\n" +
                "    \"AddressLine2\" : \"line 2\",\n" +
                "    \"AddressLine3\" : \"line 3\",\n" +
                "    \"Country\" : \"country\"\n" +
                "  }"),
            () -> MatcherAssert.assertThat(result.size(), Matchers.is(1))
        );
    }

    @Test
    void shouldBuildDataForComplexWithComplexOverrides() {
        caseEventDefinition = newCaseEvent()
            .withCaseFields(List.of(
                newCaseEventField()
                    .withCaseFieldId(FIELD_ID)
                    .withDisplayContext(DisplayContext.COMPLEX)
                    .withPublish(true)
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference(NESTED_FIELD_2)
                            .publish(true)
                            .build()
                    )
                    .addCaseEventFieldComplexDefinitions(
                        CaseEventFieldComplexDefinition.builder()
                            .reference("NestedField2.SubNestedField1")
                            .publish(true)
                            .build()
                    )
                    .build()
            ))
            .build();

        caseTypeDefinition = newCaseType()
            .withCaseFields(List.of(
                newCaseField()
                    .withId(FIELD_ID)
                    .withFieldType(
                        aFieldType()
                            .withId(COMPLEX_ID_1)
                            .withType(COMPLEX)
                            .withComplexField(complexField(NESTED_FIELD_1))
                            .withComplexField(
                                newCaseField()
                                    .withId(NESTED_FIELD_2)
                                    .withFieldType(
                                        aFieldType()
                                            .withId(COMPLEX_ID_2)
                                            .withType(COMPLEX)
                                            .withComplexField(complexField(SUB_NESTED_FIELD_1))
                                            .withComplexField(complexField(SUB_NESTED_FIELD_2))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> data = new HashMap<>();
//        data.put(FIELD_ID, mapper.convertValue("{\"FieldId\":{\"ComplexType1\":{\"NestedField1\":\"valueOne\","
//            + "\"NestedField2\":{\"SubNestedField1\":\"valueTwo\",\"SubNestedField2\":\"valueThree\"}}}}", JsonNode.class));

        data.put(FIELD_ID, mapper.convertValue("\"FieldId\": {\n" +
            "            \"ComplexType1\": {\n" +
            "                \"NestedField1\": \"valueOne\",\n" +
            "                \"NestedField2\": {\n" +
            "                    \"SubNestedField1\" : \"ValueTwo\",\n" +
            "                    \"SubNestedField2\" : \"ValueThree\"\n" +
            "                }\n" +
            "            }\n" +
            "        }", JsonNode.class));



        caseDetails = newCaseDetails().withData(data).build();


        AdditionalDataContext context =
            new AdditionalDataContext(caseEventDefinition, caseTypeDefinition, caseDetails);

        Map<String, Object> result = dataBlockGenerator.generateData(context);

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get(NESTED_FIELD_2), is(1))
        );
    }


    private String caseEventDefinitionString() {
        return "\n" +
            "{\"id\":\"createCase\",\"name\":\"Create a case\",\"description\":\"Create a case\",\"order\":1,\"case_fields\":[{\"case_field_id\":\"DocumentField1\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"AddressUKField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"CollectionField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"CollectionFieldMan\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MultiSelectField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"EmailField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MoneyField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MarritalStatus\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"YesNoField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"NumberField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"PhoneField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"AddressField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"DateField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextAreaField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]}],\"pre_states\":[],\"post_states\":[{\"enabling_condition\":null,\"priority\":99,\"post_state_reference\":\"CaseCreated\"}],\"callback_url_about_to_start_event\":null,\"retries_timeout_about_to_start_event\":[],\"callback_url_about_to_submit_event\":null,\"retries_timeout_url_about_to_submit_event\":[],\"callback_url_submitted_event\":null,\"retries_timeout_url_submitted_event\":[],\"security_classification\":\"PUBLIC\",\"show_summary\":true,\"show_event_notes\":null,\"end_button_label\":null,\"can_save_draft\":null,\"publish\":true,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]}\n" +
            "\n";
    }

    private String caseTypeDefinitionString() {
        return "{\"id\":\"FT_ConditionalPostState\",\"description\":\"CaseType for testing Conditional event post state\",\"version\":{\"number\":38,\"live_from\":1483228800000,\"live_until\":null},\"name\":\"FT-Conditional Post State\",\"events\":[{\"id\":\"updateCase2\",\"name\":\"Update a case 2\",\"description\":\"Update a case 2\",\"order\":3,\"case_fields\":[{\"case_field_id\":\"EmailField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]}],\"pre_states\":[\"CaseCreated\"],\"post_states\":[{\"enabling_condition\":\"TextField=\\\"keepstate\\\"\",\"priority\":3,\"post_state_reference\":\"*\"},{\"enabling_condition\":\"TextField=\\\"amended2\\\" OR EmailField=\\\"matched@test.com\\\"\",\"priority\":2,\"post_state_reference\":\"CaseAmended2\"},{\"enabling_condition\":null,\"priority\":99,\"post_state_reference\":\"CaseDeleted\"},{\"enabling_condition\":\"EmailField!=\\\"\\\"\",\"priority\":4,\"post_state_reference\":\"CaseRevoked2\"},{\"enabling_condition\":\"TextField=\\\"updated2\\\" AND EmailField=\\\"*\\\"\",\"priority\":1,\"post_state_reference\":\"CaseUpdated2\"}],\"callback_url_about_to_start_event\":null,\"retries_timeout_about_to_start_event\":[],\"callback_url_about_to_submit_event\":null,\"retries_timeout_url_about_to_submit_event\":[],\"callback_url_submitted_event\":null,\"retries_timeout_url_submitted_event\":[],\"security_classification\":\"PUBLIC\",\"show_summary\":true,\"show_event_notes\":null,\"end_button_label\":null,\"can_save_draft\":null,\"publish\":true,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"updateCase\",\"name\":\"Update a case\",\"description\":\"Update a case\",\"order\":2,\"case_fields\":[{\"case_field_id\":\"AddressField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"EmailField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]}],\"pre_states\":[\"CaseCreated\"],\"post_states\":[{\"enabling_condition\":\"TextField=\\\"amended\\\" AND EmailField=\\\"*\\\"\",\"priority\":2,\"post_state_reference\":\"CaseAmended\"},{\"enabling_condition\":null,\"priority\":99,\"post_state_reference\":\"CaseDeleted\"},{\"enabling_condition\":\"AddressField.AddressLine1=\\\"Some address\\\" AND EmailField=\\\"*\\\"\",\"priority\":3,\"post_state_reference\":\"CaseRevoked\"},{\"enabling_condition\":\"TextField=\\\"updated\\\" AND EmailField=\\\"*\\\"\",\"priority\":1,\"post_state_reference\":\"CaseUpdated\"}],\"callback_url_about_to_start_event\":null,\"retries_timeout_about_to_start_event\":[],\"callback_url_about_to_submit_event\":null,\"retries_timeout_url_about_to_submit_event\":[],\"callback_url_submitted_event\":null,\"retries_timeout_url_submitted_event\":[],\"security_classification\":\"PUBLIC\",\"show_summary\":true,\"show_event_notes\":null,\"end_button_label\":null,\"can_save_draft\":null,\"publish\":true,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"createCase\",\"name\":\"Create a case\",\"description\":\"Create a case\",\"order\":1,\"case_fields\":[{\"case_field_id\":\"DocumentField1\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"AddressUKField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"CollectionField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"CollectionFieldMan\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MultiSelectField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"EmailField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MoneyField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"MarritalStatus\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"YesNoField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"NumberField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"PhoneField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"AddressField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"DateField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextAreaField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]},{\"case_field_id\":\"TextField\",\"display_context\":\"OPTIONAL\",\"display_context_parameter\":null,\"show_condition\":null,\"show_summary_change_option\":true,\"show_summary_content_option\":null,\"label\":null,\"hint_text\":null,\"retain_hidden_value\":null,\"publish\":true,\"publish_as\":null,\"defaultValue\":null,\"case_fields_complex\":[]}],\"pre_states\":[],\"post_states\":[{\"enabling_condition\":null,\"priority\":99,\"post_state_reference\":\"CaseCreated\"}],\"callback_url_about_to_start_event\":null,\"retries_timeout_about_to_start_event\":[],\"callback_url_about_to_submit_event\":null,\"retries_timeout_url_about_to_submit_event\":[],\"callback_url_submitted_event\":null,\"retries_timeout_url_submitted_event\":[],\"security_classification\":\"PUBLIC\",\"show_summary\":true,\"show_event_notes\":null,\"end_button_label\":null,\"can_save_draft\":null,\"publish\":true,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]}],\"states\":[{\"id\":\"CaseCreated\",\"name\":\"Create case\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"CaseUpdated\",\"name\":\"Update case\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"CaseAmended\",\"name\":\"Amend case\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"CaseRevoked\",\"name\":\"Revoke case\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"CaseDeleted\",\"name\":\"Delete case\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]},{\"id\":\"CaseUpdated2\",\"name\":\"Update case 2\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[]},{\"id\":\"CaseAmended2\",\"name\":\"Amend case 2\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[]},{\"id\":\"CaseRevoked2\",\"name\":\"Revoke case 2\",\"description\":null,\"order\":1,\"title_display\":null,\"acls\":[]}],\"searchAliasFields\":[],\"jurisdiction\":{\"caseTypesIDs\":[],\"id\":\"BEFTA_MASTER\",\"name\":\"BEFTA Master\",\"description\":\"Content for the BEFTA Master Jurisdiction.\",\"live_from\":1483228800000,\"live_until\":null,\"case_types\":[]},\"security_classification\":\"PUBLIC\",\"case_fields\":[{\"id\":\"[STATE]\",\"label\":\"State\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"FixedList-FT_ConditionalPostState[STATE]\",\"type\":\"FixedList\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[{\"code\":\"CaseRevoked2\",\"label\":\"Revoke case 2\",\"order\":null},{\"code\":\"CaseAmended2\",\"label\":\"Amend case 2\",\"order\":null},{\"code\":\"CaseUpdated2\",\"label\":\"Update case 2\",\"order\":null},{\"code\":\"CaseDeleted\",\"label\":\"Delete case\",\"order\":null},{\"code\":\"CaseRevoked\",\"label\":\"Revoke case\",\"order\":null},{\"code\":\"CaseAmended\",\"label\":\"Amend case\",\"order\":null},{\"code\":\"CaseUpdated\",\"label\":\"Update case\",\"order\":null},{\"code\":\"CaseCreated\",\"label\":\"Create case\",\"order\":null}],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2021-01-13\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"DynamicList\",\"label\":\"Dynamic Lists\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"DynamicList\",\"type\":\"DynamicList\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"LabelField\",\"label\":\"LabelData: textField is ${TextField}\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Label\",\"type\":\"Label\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"CaseHistory\",\"label\":\"History\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"CaseHistoryViewer\",\"type\":\"CaseHistoryViewer\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"DocumentField1\",\"label\":\"Document Field 1\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Document\",\"type\":\"Document\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"DecreeNisiDocumentField\",\"label\":\"Decree Nisi Document Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Document\",\"type\":\"Document\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"DocumentField\",\"label\":\"Document Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Document\",\"type\":\"Document\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"AddressUKField\",\"label\":\"Enter PostCode\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"AddressUK\",\"type\":\"Complex\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[{\"id\":\"AddressLine1\",\"label\":\"Building and Street\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax150\",\"type\":\"Text\",\"min\":null,\"max\":150,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":false,\"publish_as\":null},{\"id\":\"AddressLine2\",\"label\":\"Address Line 2\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax50\",\"type\":\"Text\",\"min\":null,\"max\":50,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"AddressLine3\",\"label\":\"Address Line 3\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax50\",\"type\":\"Text\",\"min\":null,\"max\":50,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"PostTown\",\"label\":\"Town or City\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax50\",\"type\":\"Text\",\"min\":null,\"max\":50,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"County\",\"label\":\"County\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax50\",\"type\":\"Text\",\"min\":null,\"max\":50,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"PostCode\",\"label\":\"Postcode/Zipcode\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax14\",\"type\":\"Text\",\"min\":null,\"max\":14,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"Country\",\"label\":\"Country\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"TextMax50\",\"type\":\"Text\",\"min\":null,\"max\":50,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null}],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"CollectionField\",\"label\":\"Collection Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"CollectionField-ed8f4eca-390d-43b0-8257-cac57c403d1b\",\"type\":\"Collection\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null}},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"CollectionFieldMan\",\"label\":\"Collection Field MAN\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"CollectionFieldMan-07385176-a8b6-48f6-a857-7d3e3034434a\",\"type\":\"Collection\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null}},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"MultiSelectField\",\"label\":\"Multi Select Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"MultiSelectList-regionalCentreEnum\",\"type\":\"MultiSelectList\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[{\"code\":\"MANCHESTER\",\"label\":\"Manchester\",\"order\":\"2\"},{\"code\":\"CARDIFF\",\"label\":\"Cardiff\",\"order\":\"5\"},{\"code\":\"OXFORD\",\"label\":\"Oxford\",\"order\":null}],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"EmailField\",\"label\":\"Email Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Email\",\"type\":\"Email\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"MoneyField\",\"label\":\"Money Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"MoneyGBP\",\"type\":\"MoneyGBP\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"MarritalStatus\",\"label\":\"Fixed List\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"FixedList-marritalStatusEnum\",\"type\":\"FixedList\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[{\"code\":\"CIVIL_PARTNERSHIP\",\"label\":\"Civil Partnership\",\"order\":\"1\"},{\"code\":\"MARRIAGE\",\"label\":\"Marriage\",\"order\":\"2\"},{\"code\":\"WIDOW\",\"label\":\"Widow\",\"order\":\"3\"},{\"code\":\"SINGLE\",\"label\":\"Single\",\"order\":\"4\"}],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"YesNoField\",\"label\":\"Yes or No Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"YesOrNo\",\"type\":\"YesOrNo\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"NumberField\",\"label\":\"Number Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Number\",\"type\":\"Number\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"PhoneField\",\"label\":\"Phone Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"PhoneUK\",\"type\":\"PhoneUK\",\"min\":null,\"max\":null,\"regular_expression\":\"^(((\\\\+44\\\\s?\\\\d{4}|\\\\(?0\\\\d{4}\\\\)?)\\\\s?\\\\d{3}\\\\s?\\\\d{3})|((\\\\+44\\\\s?\\\\d{3}|\\\\(?0\\\\d{3}\\\\)?)\\\\s?\\\\d{3}\\\\s?\\\\d{4})|((\\\\+44\\\\s?\\\\d{2}|\\\\(?0\\\\d{2}\\\\)?)\\\\s?\\\\d{4}\\\\s?\\\\d{4}))(\\\\s?\\\\#(\\\\d{4}|\\\\d{3}))?$\",\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"AddressField\",\"label\":\"Address Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Address\",\"type\":\"Complex\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[{\"id\":\"AddressLine1\",\"label\":\"Address Line 1\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"AddressLine2\",\"label\":\"Address Line 2\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"AddressLine3\",\"label\":\"Address Line 3\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"Country\",\"label\":\"Country\",\"hidden\":null,\"order\":null,\"metadata\":false,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":null,\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null}],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"DateField\",\"label\":\"Date Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Date\",\"type\":\"Date\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"TextAreaField\",\"label\":\"Text Area\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"TextArea\",\"type\":\"TextArea\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"TextField\",\"label\":\"Text Field\",\"hidden\":false,\"order\":null,\"metadata\":false,\"case_type_id\":\"FT_ConditionalPostState\",\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2017-01-01\",\"live_until\":null,\"show_condition\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[JURISDICTION]\",\"label\":\"Jurisdiction\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[CASE_TYPE]\",\"label\":\"Case Type\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[SECURITY_CLASSIFICATION]\",\"label\":\"Security Classification\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Text\",\"type\":\"Text\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[CASE_REFERENCE]\",\"label\":\"Case Reference\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"Number\",\"type\":\"Number\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[CREATED_DATE]\",\"label\":\"Created Date\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"DateTime\",\"type\":\"DateTime\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[LAST_MODIFIED_DATE]\",\"label\":\"Last Modified Date\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"DateTime\",\"type\":\"DateTime\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null},{\"id\":\"[LAST_STATE_MODIFIED_DATE]\",\"label\":\"Last State Modified Date\",\"hidden\":false,\"order\":null,\"metadata\":true,\"case_type_id\":null,\"hint_text\":null,\"field_type\":{\"id\":\"DateTime\",\"type\":\"DateTime\",\"min\":null,\"max\":null,\"regular_expression\":null,\"fixed_list_items\":[],\"complex_fields\":[],\"collection_field_type\":null},\"security_classification\":\"PUBLIC\",\"live_from\":\"2020-11-26\",\"live_until\":null,\"show_condition\":null,\"acls\":[],\"complexACLs\":[],\"display_context\":null,\"display_context_parameter\":null,\"retain_hidden_value\":null,\"formatted_value\":null,\"default_value\":null,\"publish\":true,\"publish_as\":null}],\"printable_document_url\":null,\"acls\":[{\"role\":\"caseworker-befta_master\",\"create\":true,\"read\":true,\"update\":true,\"delete\":true}]}";
    }

    private String caseDetailsString() {
        return "{\"id\":1610536802776115,\"jurisdiction\":\"BEFTA_MASTER\",\"state\":\"CaseCreated\",\"version\":0,\"case_type_id\":\"FT_ConditionalPostState\",\"created_date\":null,\"last_modified\":null,\"last_state_modified_date\":null,\"security_classification\":\"PUBLIC\",\"case_data\":{\"AddressField\":{\"AddressLine1\":\"lin 1\",\"AddressLine2\":\"line 2\",\"AddressLine3\":\"line 3\",\"Country\":\"country\"},\"AddressUKField\":{\"AddressLine1\":null,\"AddressLine2\":null,\"AddressLine3\":null,\"PostTown\":null,\"County\":null,\"PostCode\":null,\"Country\":null},\"MoneyField\":\"60000\",\"MarritalStatus\":\"CIVIL_PARTNERSHIP\",\"NumberField\":\"66\",\"MultiSelectField\":[\"MANCHESTER\",\"CARDIFF\"],\"YesNoField\":\"No\",\"EmailField\":\"test@test.com\",\"TextField\":\"Text field\",\"PhoneField\":\"07971238417\",\"DateField\":\"2000-01-01\",\"TextAreaField\":\"text area\",\"CollectionFieldMan\":[],\"CollectionField\":[{\"value\":\"collextion field\",\"id\":\"d06f0976-4183-403d-955e-c9b54aad3bc0\"},{\"value\":\"collection field two\",\"id\":\"f2ab95f3-0eb9-43dc-8f5b-b9524ac8e518\"}]},\"data_classification\":{\"AddressField\":{\"classification\":\"PUBLIC\",\"value\":{\"AddressLine1\":\"PUBLIC\",\"AddressLine2\":\"PUBLIC\",\"AddressLine3\":\"PUBLIC\",\"Country\":\"PUBLIC\"}},\"AddressUKField\":{\"classification\":\"PUBLIC\",\"value\":{\"AddressLine1\":\"PUBLIC\",\"AddressLine2\":\"PUBLIC\",\"AddressLine3\":\"PUBLIC\",\"PostTown\":\"PUBLIC\",\"County\":\"PUBLIC\",\"PostCode\":\"PUBLIC\",\"Country\":\"PUBLIC\"}},\"MoneyField\":\"PUBLIC\",\"MarritalStatus\":\"PUBLIC\",\"NumberField\":\"PUBLIC\",\"MultiSelectField\":\"PUBLIC\",\"YesNoField\":\"PUBLIC\",\"EmailField\":\"PUBLIC\",\"TextField\":\"PUBLIC\",\"PhoneField\":\"PUBLIC\",\"DateField\":\"PUBLIC\",\"TextAreaField\":\"PUBLIC\",\"CollectionFieldMan\":{\"classification\":\"PUBLIC\",\"value\":[]},\"CollectionField\":{\"classification\":\"PUBLIC\",\"value\":[{\"id\":\"d06f0976-4183-403d-955e-c9b54aad3bc0\",\"classification\":\"PUBLIC\"},{\"id\":\"f2ab95f3-0eb9-43dc-8f5b-b9524ac8e518\",\"classification\":\"PUBLIC\"}]}},\"supplementary_data\":null,\"after_submit_callback_response\":null,\"callback_response_status_code\":null,\"callback_response_status\":null,\"delete_draft_response_status_code\":null,\"delete_draft_response_status\":null,\"security_classifications\":{\"AddressField\":{\"classification\":\"PUBLIC\",\"value\":{\"AddressLine1\":\"PUBLIC\",\"AddressLine2\":\"PUBLIC\",\"AddressLine3\":\"PUBLIC\",\"Country\":\"PUBLIC\"}},\"AddressUKField\":{\"classification\":\"PUBLIC\",\"value\":{\"AddressLine1\":\"PUBLIC\",\"AddressLine2\":\"PUBLIC\",\"AddressLine3\":\"PUBLIC\",\"PostTown\":\"PUBLIC\",\"County\":\"PUBLIC\",\"PostCode\":\"PUBLIC\",\"Country\":\"PUBLIC\"}},\"MoneyField\":\"PUBLIC\",\"MarritalStatus\":\"PUBLIC\",\"NumberField\":\"PUBLIC\",\"MultiSelectField\":\"PUBLIC\",\"YesNoField\":\"PUBLIC\",\"EmailField\":\"PUBLIC\",\"TextField\":\"PUBLIC\",\"PhoneField\":\"PUBLIC\",\"DateField\":\"PUBLIC\",\"TextAreaField\":\"PUBLIC\",\"CollectionFieldMan\":{\"classification\":\"PUBLIC\",\"value\":[]},\"CollectionField\":{\"classification\":\"PUBLIC\",\"value\":[{\"id\":\"d06f0976-4183-403d-955e-c9b54aad3bc0\",\"classification\":\"PUBLIC\"},{\"id\":\"f2ab95f3-0eb9-43dc-8f5b-b9524ac8e518\",\"classification\":\"PUBLIC\"}]}}}";
    }

    private CaseFieldDefinition complexField(String id, String type) {
        return newCaseField()
            .withId(id)
            .withFieldType(fieldType(type))
            .build();
    }

    private CaseFieldDefinition complexField(String id) {
        return newCaseField()
            .withId(id)
            .withFieldType(textField())
            .build();
    }

    private FieldTypeDefinition textField() {
        return fieldType(TEXT);
    }

    private FieldTypeDefinition fieldType(String type) {
        return aFieldType()
            .withId(type)
            .withType(type)
            .build();
    }
}

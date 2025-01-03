package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CategoryDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.DocumentValidator.DOCUMENT_URL;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class DocumentValidatorTest implements IVallidatorTest {

    private static final String DOCUMENT_FIELD_ID = "DOCUMENT_FIELD_ID";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String CATEGORY_ID = "category_id";
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";
    private static final String CASE_TYPE_ID = "FT_CaseAccessCategories";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n"
            + "  \"id\": \"DOCUMENT_FIELD_ID\",\n"
            + "  \"field_type\": {\n"
            + "    \"type\": \"Document\"\n"
            + "    }\n"
            + "}";
    private static final String VALID_DOCUMENT_URL = "https://dm.reform.hmcts.net/documents/a1-2Z-3-x";
    private static final String VALID_EM_HRS_API_DOCUMENT_URL_1 =
        "https://em-hrs-api-aat.service.core-compute-aat.internal/hearing-recordings/"
            + "b10ea9c4-a116-11eb-bcbc-0242ac130002/segments/3";
    private static final String VALID_EM_HRS_API_DOCUMENT_URL_2 =
        "https://em-hrs-api-pr-155.service.core-compute-preview.internal/hearing-recordings/"
            + "e5b96e94-4010-43b3-9196-44d8539f32b8/segments/0";
    private static final String VALID_EM_HRS_API_DOCUMENT_URL_3 =
        "https://em-hrs-api-aat.service.core-compute-aat.internal/hearing-recordings/"
            + "b10ea9c4-a116-11eb-bcbc-0242ac130002/segments/330";
    private static final String INVALID_RECORD_ID_EM_HRS_API_DOCUMENT_URL =
        "https://em-hrs-api.service.core-compute-aat.internal/hearing-recordings/"
            + "123456789012/segments/3";
    private static final String INVALID_EM_HRS_API_DOCUMENT_URL =
        "https://em-hrs-api.service.core-compute-aat.internal/documents/a1-2Z-3-x";
    private static final String MISSING_DOCUMENT_PATH_URL = "https://dm.reform.hmcts.net/docs/a1-2Z-3-x";
    private static final String UNKNOWN_DOCUMENT_DOMAIN_URL = "https://example.com/documents/a1-2Z-3-x";
    private static final String DOCUMENT_URL_WITH_PORT = "https://ng.reform.hmcts.net:6789/documents/a1-2Z-3-x-ngitb";
    private static final String UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL =
        "https://dm.reform.hmcts.net.example.com/documents/a1-2Z-3-x";
    private static final String VALID_CATEGORY_ID = "mainEvidence";
    private static final String NON_EXISTENT_CATEGORY_ID = "notInCategoriesList";
    private static final String VALID_UPLOAD_TIMESTAMP = "2012-12-10T00:00:00";
    private DocumentValidator validator;
    private CaseFieldDefinition caseFieldDefinition;
    private ObjectNode data;
    private List<ValidationResult> validDocumentUrlResult;
    private DateTimeValidator dateTimeValidator;
    private CaseDefinitionRepository caseDefinitionRepository;

    @Test
    public void shouldValidateIfPortsAreSpecifiedAndMatch() {
        final DocumentValidator validatorWithPort = buildDocumentValidator("https://ng.reform.hmcts.net:6789");

        validDocumentUrlResult = validatorWithPort.validate(DOCUMENT_FIELD_ID,
            data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, empty());
    }

    @Test
    public void shouldNotValidateIfPortsAreSpecifiedAndNotMatch() {
        final DocumentValidator validatorWithPort = buildDocumentValidator("https://ng.reform.hmcts.net:7789");

        data = createDoc(DOCUMENT_URL_WITH_PORT);
        validDocumentUrlResult = validatorWithPort.validate(DOCUMENT_FIELD_ID,
            data,
            caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(1));
        assertThat(validDocumentUrlResult.get(0).getErrorMessage(),
            is(DOCUMENT_URL_WITH_PORT + " does not match Document Management domain or expected URL path"));
    }

    private DocumentValidator buildDocumentValidator(final String urlBase) {
        final ApplicationParams ap = mock(ApplicationParams.class);
        when(ap.getDocumentURLPattern()).thenReturn(urlBase + "/documents/[A-Za-z0-9-]+(?:/binary)?");

        return new DocumentValidator(ap,dateTimeValidator, caseDefinitionRepository);
    }

    @Before
    public void setUp() throws Exception {
        final FieldTypeDefinition documentFieldTypeDefinition = mock(FieldTypeDefinition.class);
        final FieldTypeDefinition dateTimeFieldTypeDefinition = mock(FieldTypeDefinition.class);
        when(documentFieldTypeDefinition.getType()).thenReturn("Document");
        when(dateTimeFieldTypeDefinition.getType()).thenReturn("DateTime");

        caseDefinitionRepository = mock(CaseDefinitionRepository.class);
        doReturn(List.of(documentFieldTypeDefinition, dateTimeFieldTypeDefinition))
            .when(caseDefinitionRepository).getBaseTypes();

        BaseType.setCaseDefinitionRepository(caseDefinitionRepository);

        final ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getDocumentURLPattern()).thenReturn("https://dm.reform.hmcts.net/documents/[A-Za-z0-9-]+(?:/binary)?");

        dateTimeValidator = new DateTimeValidator();

        validator = new DocumentValidator(
            applicationParams, dateTimeValidator, caseDefinitionRepository);
        caseFieldDefinition = MAPPER.readValue(CASE_FIELD_STRING, CaseFieldDefinition.class);
    }

    @Test
    public void shouldNotValidateIfDocumentUrlIsNotText() {
        data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, true);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateIfDocumentUrlDoesNotExist() {
        data = MAPPER.createObjectNode();
        data.put("some_data", "some_value");
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateIfDocumentUrlIsNull() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, null);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithValidUrlAndDomain() {
        data = createDoc(VALID_DOCUMENT_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithValidEmHrsUrlAndDomain() {
        validator = setUpEmHrsApiValidator();

        data = createDoc(VALID_EM_HRS_API_DOCUMENT_URL_1);
        validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
        data = createDoc(VALID_EM_HRS_API_DOCUMENT_URL_2);
        validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithMultipleDigitEmHrsUrlAndDomain() {
        validator = setUpEmHrsApiValidator();

        data = createDoc(VALID_EM_HRS_API_DOCUMENT_URL_3);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithInValidEmHrsRecordId() {
        validator = setUpEmHrsApiValidator();

        data = createDoc(INVALID_RECORD_ID_EM_HRS_API_DOCUMENT_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithInValidEmHrsUrl() {
        validator = setUpEmHrsApiValidator();

        data = createDoc(INVALID_EM_HRS_API_DOCUMENT_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithMissingDocumentPath() {
        data = createDoc(MISSING_DOCUMENT_PATH_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownDomain() {
        data = createDoc(UNKNOWN_DOCUMENT_DOMAIN_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownParentDomain() {
        data = createDoc(UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithValidBinaryUrlAndDomain() {
        data = createDoc(DOCUMENT_BINARY_URL,VALID_DOCUMENT_URL + "/binary");
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithNullBinaryUrlAndDomain() {
        data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, VALID_DOCUMENT_URL);
        data.set("document_binary_url", null);
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithMissingDocumentPathToBinary() {
        data = createDoc(DOCUMENT_BINARY_URL, MISSING_DOCUMENT_PATH_URL + "/binary");
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownDomainForBinary() {
        data = createDoc(DOCUMENT_BINARY_URL, UNKNOWN_DOCUMENT_DOMAIN_URL + "/binary");
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownParentDomainForBinary() {
        data = createDoc(DOCUMENT_BINARY_URL, UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL + "/binary");
        validDocumentUrlResult =
            validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void getType() {
        assertEquals("Type is incorrect", validator.getType(), BaseType.get("DOCUMENT"));
    }

    @Test
    public void nullValue() {
        assertEquals("Did not catch NULL", 0, validator.validate(DOCUMENT_FIELD_ID, null, caseFieldDefinition).size());
    }

    @Test
    public void nullObjectValue() {
        assertEquals("Did not catch NULL", 0, validator.validate(DOCUMENT_FIELD_ID, new ObjectNode(null),
            caseFieldDefinition).size());
    }

    @Test
    public void emptyObjectNode() {
        data = MAPPER.createObjectNode();
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, empty());
    }

    @Test
    public void invalidObjectNode() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put("ngitb", "most definitely");
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("object does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingBinaryNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.binaryNode("Ngitb".getBytes()), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("binary does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.arrayNode(), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("array does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.booleanNode(true), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("boolean does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingTextNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.textNode("IATB"), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("string does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingNumberNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.numberNode(678), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("number does not have document_url key specified"));
    }

    @Test
    public void shouldFailWhenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate("TEST_FIELD_ID", NODE_FACTORY.pojoNode(1), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("pojo does not have document_url key specified"));
    }

    @Test
    public void shouldFail_whenValidatingBooleanDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.booleanNode(true));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingObjectDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.objectNode());
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingArrayDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.arrayNode());
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingNumberDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.numberNode(1));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingPojoDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.pojoNode("text"));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingBinaryDocumentUrl() {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, NODE_FACTORY.binaryNode("n".getBytes()));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldPass_whenValidatingNullCategoryId() {
        data = createDoc(CATEGORY_ID,null);
        validDocumentUrlResult = validator.validate(CATEGORY_ID, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(0));
    }

    @Test
    public void shouldFail_whenValidatingBooleanCategoryId() {
        data = createDoc(VALID_DOCUMENT_URL);
        data.put(CATEGORY_ID, true);

        validDocumentUrlResult = validator.validate(CATEGORY_ID, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(1));
        assertThat(validDocumentUrlResult.get(0).getErrorMessage(), is("boolean is not a string : " + CATEGORY_ID));
    }

    @Test
    public void shouldValidateCategoryId() {

        setupWithCategories();
        data = createDoc(CATEGORY_ID, VALID_CATEGORY_ID);
        caseFieldDefinition.setCaseTypeId(CASE_TYPE_ID);

        validDocumentUrlResult = validator.validate(CATEGORY_ID, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, empty());
    }

    @Test
    public void shouldValidateNonExistentCategoryId() {

        setupWithCategories();
        data = createDoc(CATEGORY_ID, NON_EXISTENT_CATEGORY_ID);
        caseFieldDefinition.setCaseTypeId(CASE_TYPE_ID);

        validDocumentUrlResult = validator.validate(CATEGORY_ID, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, empty());
    }

    @Test
    public void shouldFail_whenValidatingNullUploadTimeStamp() {
        data = createDoc(UPLOAD_TIMESTAMP,null);
        validDocumentUrlResult = validator.validate(UPLOAD_TIMESTAMP, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(0));
    }

    @Test
    public void shouldFail_whenValidatingInvalidUploadTimeStampDate() {
        data = createDoc(UPLOAD_TIMESTAMP, "2001-12-10T00:00:0O");
        validDocumentUrlResult = validator.validate(UPLOAD_TIMESTAMP, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(1));
        assertThat(validDocumentUrlResult.get(0).getErrorMessage(),
            is("Date or Time entered is not valid : " + UPLOAD_TIMESTAMP));
    }

    @Test
    public void shouldValidateUploadTimeStamp() {
        data = createDoc(UPLOAD_TIMESTAMP,VALID_UPLOAD_TIMESTAMP);
        validDocumentUrlResult = validator.validate(UPLOAD_TIMESTAMP, data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, empty());
    }

    private ObjectNode createDoc(String documentUrl) {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, new TextNode(documentUrl));
        return data;
    }

    private ObjectNode createDoc(String key, String value) {
        data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, new TextNode(VALID_DOCUMENT_URL));
        data.set(key, new TextNode(value));
        return data;
    }

    private DocumentValidator setUpEmHrsApiValidator() {
        final ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getDocumentURLPattern()).thenReturn("https://(em-hrs-api-aat.service.core-compute-aat|"
            + "em-hrs-api-(pr-[0-9]+|preview).service.core-compute-preview).internal(?::d+)?/"
            + "hearing-recordings/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
            + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/segments/[0-9]+");
        validator = new DocumentValidator(applicationParams, null, null);
        return validator;
    }

    private void setupWithCategories() {
        final CaseTypeDefinition caseTypeDefinition = mock(CaseTypeDefinition.class);
        CategoryDefinition category = new CategoryDefinition();
        category.setCategoryId(VALID_CATEGORY_ID);
        List<CategoryDefinition> categories = List.of(category);
        doReturn(caseTypeDefinition)
            .when(caseDefinitionRepository).getCaseType(anyString());
        doReturn(categories)
            .when(caseTypeDefinition).getCategories();
    }

}

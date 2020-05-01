package uk.gov.hmcts.ccd.domain.types;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.DocumentValidator.DOCUMENT_URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Collections;
import java.util.List;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public class DocumentValidatorTest implements IVallidatorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CASE_FIELD_STRING =
        "{\n"
            + "  \"id\": \"DOCUMENT_FIELD_ID\",\n"
            + "  \"field_type\": {\n"
            + "    \"type\": \"Document\"\n"
            + "    }\n"
            + "}";
    private static final String VALID_DOCUMENT_URL = "https://dm.reform.hmcts.net/documents/a1-2Z-3-x";
    private static final String MISSING_DOCUMENT_PATH_URL = "https://dm.reform.hmcts.net/docs/a1-2Z-3-x";
    private static final String UNKNOWN_DOCUMENT_DOMAIN_URL = "https://example.com/documents/a1-2Z-3-x";
    private static final String DOCUMENT_URL_WITH_PORT = "https://ng.reform.hmcts.net:6789/documents/a1-2Z-3-x-ngitb";
    private static final String UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL = "https://dm.reform.hmcts.net.example.com/documents/a1-2Z-3-x";
    public static final String DOCUMENT_FIELD_ID = "DOCUMENT_FIELD_ID";

    private DocumentValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeClass
    public static void setUpClass() {
        final FieldTypeDefinition documentFieldTypeDefinition = mock(FieldTypeDefinition.class);
        when(documentFieldTypeDefinition.getType()).thenReturn("Document");
        final CaseDefinitionRepository definitionRepository = mock(CaseDefinitionRepository.class);
        doReturn(Collections.singletonList(documentFieldTypeDefinition)).when(definitionRepository).getBaseTypes();

        BaseType.setCaseDefinitionRepository(definitionRepository);
    }

    @Test
    public void shouldValidateIfPortsAreSpecifiedAndMatch() {
        final DocumentValidator validatorWithPort = buildDocumentValidator("https://ng.reform.hmcts.net:6789");

        final ObjectNode data = createDoc(DOCUMENT_URL_WITH_PORT);
        final List<ValidationResult> validDocumentUrlResult = validatorWithPort.validate(DOCUMENT_FIELD_ID,
            data, caseFieldDefinition);
        assertThat(validDocumentUrlResult, empty());
    }

    @Test
    public void shouldNotValidateIfPortsAreSpecifiedAndNotMatch() {
        final DocumentValidator validatorWithPort = buildDocumentValidator("https://ng.reform.hmcts.net:7789");

        final ObjectNode data = createDoc(DOCUMENT_URL_WITH_PORT);
        final List<ValidationResult> validDocumentUrlResult = validatorWithPort.validate(DOCUMENT_FIELD_ID,
            data,
                caseFieldDefinition);
        assertThat(validDocumentUrlResult, hasSize(1));
        assertThat(validDocumentUrlResult.get(0).getErrorMessage(),
            is(DOCUMENT_URL_WITH_PORT + " does not match Document Management domain or expected URL path"));
    }

    private DocumentValidator buildDocumentValidator(final String url) {
        final ApplicationParams ap = mock(ApplicationParams.class);
        when(ap.getValidDMDomain()).thenReturn(url);
        return new DocumentValidator(ap);
    }

    @Before
    public void setUp() throws Exception {
        final ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getValidDMDomain()).thenReturn("https://dm.reform.hmcts.net");
        validator = new DocumentValidator(applicationParams);
        caseFieldDefinition = MAPPER.readValue(CASE_FIELD_STRING, CaseFieldDefinition.class);
    }

    @Test
    public void shouldNotValidateIfDocumentUrlIsNotText() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, true);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateIfDocumentUrlDoesNotExist() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put("some_data", "some_value");
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateIfDocumentUrlIsNull() {
        ObjectNode data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, null);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithValidUrlAndDomain() {
        ObjectNode data = createDoc(VALID_DOCUMENT_URL);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithMissingDocumentPath() {
        ObjectNode data = createDoc(MISSING_DOCUMENT_PATH_URL);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownDomain() {
        ObjectNode data = createDoc(UNKNOWN_DOCUMENT_DOMAIN_URL);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownParentDomain() {
        ObjectNode data = createDoc(UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithValidBinaryUrlAndDomain() {
        ObjectNode data = createDoc(VALID_DOCUMENT_URL, VALID_DOCUMENT_URL + "/binary");
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 0, validDocumentUrlResult.size());
    }

    @Test
    public void shouldValidateDocumentWithNullBinaryUrlAndDomain() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, VALID_DOCUMENT_URL);
        data.set("document_binary_url", null);
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithMissingDocumentPathToBinary() {
        ObjectNode data = createDoc(VALID_DOCUMENT_URL, MISSING_DOCUMENT_PATH_URL + "/binary");
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownDomainForBinary() {
        ObjectNode data = createDoc(VALID_DOCUMENT_URL, UNKNOWN_DOCUMENT_DOMAIN_URL + "/binary");
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertEquals(validDocumentUrlResult.toString(), 1, validDocumentUrlResult.size());
    }

    @Test
    public void shouldNotValidateDocumentWithUnknownParentDomainForBinary() {
        ObjectNode data = createDoc(VALID_DOCUMENT_URL, UNKNOWN_DOCUMENT_PARENT_DOMAIN_URL + "/binary");
        final List<ValidationResult> validDocumentUrlResult = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
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
        assertEquals("Did not catch NULL", 0, validator.validate(DOCUMENT_FIELD_ID, new ObjectNode(null), caseFieldDefinition).size());
    }

    @Test
    public void emptyObjectNode() {
        ObjectNode data = MAPPER.createObjectNode();
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
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.booleanNode(true));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingObjectDocumentUrl() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.objectNode());
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingArrayDocumentUrl() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.arrayNode());
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingNumberDocumentUrl() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.numberNode(1));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingPojoDocumentUrl() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.pojoNode("text"));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    @Test
    public void shouldFail_whenValidatingBinaryDocumentUrl() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put(DOCUMENT_URL, NODE_FACTORY.binaryNode("n".getBytes()));
        final List<ValidationResult> result = validator.validate(DOCUMENT_FIELD_ID, data, caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("document_url is not a text value or is null"));
    }

    private ObjectNode createDoc(String documentUrl) {
        ObjectNode data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, new TextNode(documentUrl));
        return data;
    }

    private ObjectNode createDoc(String documentUrl, String documentBinaryUrl) {
        ObjectNode data = MAPPER.createObjectNode();
        data.set(DOCUMENT_URL, new TextNode(documentUrl));
        data.set("document_binary_url", new TextNode(documentBinaryUrl));
        return data;
    }
}

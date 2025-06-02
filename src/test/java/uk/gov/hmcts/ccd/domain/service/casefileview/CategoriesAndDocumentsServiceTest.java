package uk.gov.hmcts.ccd.domain.service.casefileview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple2;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.domain.model.casefileview.CategoriesAndDocuments;
import uk.gov.hmcts.ccd.domain.model.casefileview.Category;
import uk.gov.hmcts.ccd.domain.model.casefileview.Document;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CategoryDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataExtractor;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CategoriesAndDocumentsServiceTest extends TestFixtures {
    @Mock
    private CaseDataExtractor caseDataExtractor;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private FileViewDocumentService fileViewDocumentService;

    @InjectMocks
    private CategoriesAndDocumentsService underTest;

    @Test
    @SuppressWarnings("ConstantConditions")
    void testShouldRaiseExceptionWhenNullParameterIsProvided() {
        assertThatNullPointerException().isThrownBy(() -> underTest.findRootCategoryDefinitions(null));
    }

    @Test
    void testShouldReturnEmptyList() {
        final List<CategoryDefinition> result = underTest.findRootCategoryDefinitions(emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    void testShouldFindRootCategoryDefinitions() {
        final List<CategoryDefinition> rootCategoryDefinitions = provideRootCategoryDefinitions();
        final List<CategoryDefinition> categoryDefinitions = provideCategoryDefinitions();

        final List<CategoryDefinition> rootCategories = underTest.findRootCategoryDefinitions(categoryDefinitions);

        assertThat(rootCategories)
            .isNotEmpty()
            .hasSize(2)
            .hasSameElementsAs(rootCategoryDefinitions);
    }

    @Test
    void testShouldTransformOne() {
        final CategoryDefinition categoryDefinition =
            new CategoryDefinition("Cat-1", "Cat-1", null, null, null, 1, "");
        final List<CategoryDefinition> categoryDefinitions = provideCategoryDefinitions();
        final Category expectedCategory = getCategory1();

        final Category result = underTest.transform(categoryDefinition, categoryDefinitions, emptyMap());

        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedCategory);
    }

    @Test
    void testShouldTransformAll() {
        final List<CategoryDefinition> rootCategoryDefinitions = provideRootCategoryDefinitions();
        final List<CategoryDefinition> categoryDefinitions = provideCategoryDefinitions();
        final Category category1 = getCategory1();
        final Category category4 = getCategory4();

        final List<Category> result =
            underTest.transformCategories(rootCategoryDefinitions, categoryDefinitions, emptyMap());

        assertThat(result)
            .isNotEmpty()
            .hasSize(2)
            .containsExactly(category1, category4);
    }

    @ParameterizedTest
    @MethodSource("provideCategoryIdParameters")
    void testShouldResolveDocumentCategory(final String categoryOnDocument,
                                           final String categoryOnFieldDefinition,
                                           final String expectedCategory) {
        CategoryDefinition categoryDefinition =  new CategoryDefinition("document-cat",
            "document-cat", "", null, null, 1, "");
        final String result = underTest.resolveDocumentCategory(categoryOnDocument, categoryOnFieldDefinition,
            asList(categoryDefinition));

        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedCategory);
    }

    private static Stream<Arguments> provideCategoryIdParameters() {
        return Stream.of(
            Arguments.of(null, null, "uncategorised_documents"),
            Arguments.of(null, "def-cat", "def-cat"),
            Arguments.of("document-cat", null, "document-cat"),
            Arguments.of("document-cat", "def-cat", "document-cat"),
            Arguments.of("document-NonCat", null, "uncategorised_documents"),
            Arguments.of("document-NonCat", "def-cat", "uncategorised_documents")
        );
    }

    @ParameterizedTest
    @MethodSource("provideCaseFieldExtractParameters")
    void testShouldTransformDocument(final CaseFieldMetadata caseFieldExtract,
               final Tuple2<String, Map<String, String>> documentNode,
               final Document expectedDocument) throws Exception {
        CategoryDefinition categoryDefinition =  new CategoryDefinition("document-cat",
            "document-cat", "", null, null, 1, "");
        final Map<String, JsonNode> caseData =
            loadCaseDataFromJson(String.format("tests/%s", "CaseDataExtractorDocumentData.json"));
        doReturn(documentNode).when(fileViewDocumentService).getDocumentNode(caseFieldExtract.getPath(), caseData);

        final Tuple2<String, Optional<Document>> actualResult = underTest.transformDocument(caseFieldExtract, caseData,
            asList(categoryDefinition));

        assertThat(actualResult)
            .isNotNull()
            .satisfies(x -> assertThat(x._2)
                .isPresent()
                .map(document -> document)
                .hasValue(expectedDocument));
    }

    @Test
    void testShouldTransformNullDocumentToNull() throws Exception {
        final CaseFieldMetadata caseFieldExtract = new CaseFieldMetadata("draftOrderDoc", null);
        final Tuple2<String, Map<String, String>> documentNode = new Tuple2<>(
            "draftOrderDoc",
            null
        );

        final Map<String, JsonNode> caseData =
            loadCaseDataFromJson(String.format("tests/%s", "CaseDataExtractorDocumentData.json"));
        doReturn(documentNode).when(fileViewDocumentService).getDocumentNode(caseFieldExtract.getPath(), caseData);
        CategoryDefinition categoryDefinition =  new CategoryDefinition("document-cat",
            "document-cat", "", null, null, 1, "");
        final Tuple2<String, Optional<Document>> actualResult = underTest.transformDocument(caseFieldExtract, caseData,
            asList(categoryDefinition));
        assertThat(actualResult).isNull();
    }

    @Test
    void testBuildCategorisedDocumentDictionary() throws Exception {
        final List<Document> expectedDocuments = getExpectedDocumentsForDocumentDictionary();
        final String documentType = "Document";
        final List<CaseFieldMetadata> caseFieldExtracts = List.of(
            new CaseFieldMetadata("state.0.partyDetail.0.type", null),
            new CaseFieldMetadata("state.0.partyDetail.1.type", null),
            new CaseFieldMetadata("state.1.partyDetail.0.type", null),
            new CaseFieldMetadata("state.1.partyDetail.1.type", null),
            new CaseFieldMetadata("state.2.partyDetail.0.type", null),
            new CaseFieldMetadata("state.2.partyDetail.1.type", null),
            new CaseFieldMetadata("state.2.partyDetail.2.type", null)
        );
        final Map<String, JsonNode> caseData = loadCaseDataFromJson("tests/CaseDataExtractorDocumentData.json");
        final List<CaseFieldDefinition> caseFieldDefinitions =
            getCaseFieldsFromJson("tests/DocumentCaseTypeDefinitions_state.json");

        doReturn(caseFieldExtracts).when(caseDataExtractor)
            .extractFieldTypePaths(caseData, caseFieldDefinitions, documentType);
        primeFileViewDocumentServiceForDocumentDictionary(caseData);

        CategoryDefinition categoryDefinition =  new CategoryDefinition("document-cat",
            "document-cat", "", null, null, 1, "");
        final Map<String, List<Document>> results =
            underTest.buildCategorisedDocumentDictionary(caseData, caseFieldDefinitions, asList(categoryDefinition));

        assertThat(results)
            .isNotEmpty()
            .satisfies(x -> x.forEach((key, value) -> assertThat(value)
                .isNotEmpty()
                .hasSameElementsAs(expectedDocuments)));
    }

    @Test
    void testShouldGetCategoriesAndDocuments() throws Exception {
        final CategoriesAndDocuments expectedResult = getExpectedCategoriesAndDocumentsForGetCategoriesAndDocuments();
        final String documentType = "Document";
        final String caseType = "FT_CaseFileView_2";
        final CaseTypeDefinition caseTypeDefinition =
            loadCaseTypeDefinitionFromJson("tests/FT_CaseFileView_2_Definition-file.json");
        final Map<String, JsonNode> caseData = loadCaseDataFromJson("tests/case-data-for-FT_CaseFileView_2.json");

        final List<CaseFieldMetadata> caseFieldExtracts = List.of(
            new CaseFieldMetadata("evidenceDocuments.0", null),
            new CaseFieldMetadata("nationalityProof.documentEvidence.0", null),
            new CaseFieldMetadata("miscellaneousDocuments.0", null),
            new CaseFieldMetadata("applicationDocuments.0.document", null),
            new CaseFieldMetadata("applicationDocuments.1.document", null)
        );

        doReturn(caseTypeDefinition).when(caseTypeService).getCaseType(caseType);
        doReturn(caseFieldExtracts).when(caseDataExtractor).extractFieldTypePaths(
            caseData,
            caseTypeDefinition.getCaseFieldDefinitions(),
            documentType
        );
        primeFileViewDocumentServiceForGetCategoriesAndDocuments(caseData);

        final CategoriesAndDocuments result = underTest.getCategoriesAndDocuments(VERSION_NUMBER, caseType, caseData);

        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedResult);
    }

    @Test
    void testShouldReturnNullWhenParameterIsNull() {
        final LocalDateTime result = underTest.parseUploadTimestamp(null);

        assertThat(result)
            .isNull();
    }

    @Test
    void testShouldRaiseExceptionWhenInputIsNotNullButInvalidTimestamp() {
        final Throwable thrown = catchThrowable(() -> underTest.parseUploadTimestamp("abc"));

        assertThat(thrown)
            .isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @MethodSource("provideTimestampParameters")
    void testShouldParseTimestamps(String timestamp, LocalDateTime expectedTimestamp) {
        final LocalDateTime result = underTest.parseUploadTimestamp(timestamp);

        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedTimestamp);
    }

    private static Stream<Arguments> provideTimestampParameters() {
        return Stream.of(
            Arguments.of(
                "2022-04-06T16:44:52.000000000Z", LocalDateTime.of(2022, 4, 6, 16, 44, 52)
            ),
            Arguments.of(
                "2022-04-06T16:44:52.000000000", LocalDateTime.of(2022, 4, 6, 16, 44, 52)
            ),
            Arguments.of(
                "2022-04-06T16:44:52.000", LocalDateTime.of(2022, 4, 6, 16, 44, 52)
            ),
            Arguments.of(
                "2022-04-06T16:44:52.123", LocalDateTime.of(2022, 4, 6, 16, 44, 52, 123000000)
            ),
            Arguments.of(
                "2022-04-06T16:44:52", LocalDateTime.of(2022, 4, 6, 16, 44, 52)
            )
        );
    }

    private static Stream<Arguments> provideCaseFieldExtractParameters() {
        return Stream.of(
            Arguments.of(new CaseFieldMetadata("draftOrderDoc", null),
                new Tuple2<>(
                    "draftOrderDoc",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8",
                        "document_filename", "A_Simple Document.docx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8",
                    "A_Simple Document.docx",
                    "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8/binary",
                    "draftOrderDoc",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("evidence.type", null),
                new Tuple2<>(
                    "evidence.type",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",
                        "document_filename", "B_Document Inside Complex Type.docx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",
                    "B_Document Inside Complex Type.docx",
                    "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea/binary",
                    "evidence.type",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("extraDocUploadList.0", null),
                new Tuple2<>(
                    "extraDocUploadList[90a2df83-f256-43ec-aaa0-48e127a44402].value",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb",
                        "document_filename", "C_1_Collection of Documents.docx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb",
                    "C_1_Collection of Documents.docx",
                    "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb/binary",
                    "extraDocUploadList[90a2df83-f256-43ec-aaa0-48e127a44402].value",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("timeline.1.type", null),
                new Tuple2<>(
                    "timeline[12f2e482-b8e3-48cf-8c14-feaefdb09018].value.type",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404",
                        "document_filename", "D_2_Collection of Complex Type with Documents.docx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404",
                    "D_2_Collection of Complex Type with Documents.docx",
                    "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404/binary",
                    "timeline[12f2e482-b8e3-48cf-8c14-feaefdb09018].value.type",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.0.partyDetail.0.type", null),
                new Tuple2<>(
                    "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                        + ".partyDetail[bf0c087a-cee6-4e6e-b91c-b06a5b4c2e1f].type",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
                        "document_filename", "E_1_AA_Claimant details.xlsx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
                    "E_1_AA_Claimant details.xlsx",
                    "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary",
                    "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                        + ".partyDetail[bf0c087a-cee6-4e6e-b91c-b06a5b4c2e1f].type",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.1.partyDetail.1.type", null),
                new Tuple2<>(
                    "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                        + ".partyDetail[81998007-6fe6-4e1e-97ab-cd8e8fb05090].value.type",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
                        "document_filename", "E_2_BB_Claimant details.xlsx",
                        "document_binary_url",
                        "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
                    "E_2_BB_Claimant details.xlsx",
                    "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary",
                    "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                        + ".partyDetail[81998007-6fe6-4e1e-97ab-cd8e8fb05090].value.type",
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.2.partyDetail.2.type", null),
                new Tuple2<>(
                    "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                        + ".partyDetail[930d2fe8-c0fa-4cde-9e81-6fa39ddf4441].value.type",
                    Map.of(
                        "document_url", "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
                        "document_filename", "E_3_CC_Claimant details.pdf",
                        "document_binary_url",
                        "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary"
                    )
                ),
                new Document(
                    "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
                    "E_3_CC_Claimant details.pdf",
                    "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary",
                    "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                        + ".partyDetail[930d2fe8-c0fa-4cde-9e81-6fa39ddf4441].value.type",
                    null
                ))
        );
    }

    private List<CategoryDefinition> provideRootCategoryDefinitions() {
        final CategoryDefinition cat1 = new CategoryDefinition("Cat-1", "Cat-1", null, null, null, 1, "");
        final CategoryDefinition cat4 = new CategoryDefinition("Cat-4", "Cat-4", null, null, null, 1, "");

        return List.of(cat1, cat4);
    }

    private List<CategoryDefinition> provideCategoryDefinitions() {
        final List<CategoryDefinition> rootCategoryDefinitions = provideRootCategoryDefinitions();
        final CategoryDefinition cat2 = new CategoryDefinition("Cat-2", "Cat-2", "Cat-1", null, null, 1, "");
        final CategoryDefinition cat3 = new CategoryDefinition("Cat-3", "Cat-3", "Cat-2", null, null, 1, "");
        final CategoryDefinition cat5 = new CategoryDefinition("Cat-5", "Cat-5", "Cat-4", null, null, 1, "");
        final CategoryDefinition cat6 = new CategoryDefinition("Cat-6", "Cat-6", "Cat-4", null, null, 1, "");
        final CategoryDefinition cat7 = new CategoryDefinition("Cat-7", "Cat-7", "Cat-6", null, null, 1, "");
        final CategoryDefinition cat8 = new CategoryDefinition("Cat-8", "Cat-8", "Cat-6", null, null, 1, "");

        return Stream.of(rootCategoryDefinitions, List.of(cat2, cat3, cat5, cat6, cat7, cat8))
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList());
    }

    private Category getCategory1() {
        final Category category3 = new Category("Cat-3", "Cat-3", 1, emptyList(), emptyList());
        final Category category2 = new Category("Cat-2", "Cat-2", 1, emptyList(), List.of(category3));

        return new Category("Cat-1", "Cat-1", 1, emptyList(), List.of(category2));
    }

    private Category getCategory4() {
        final Category category8 = new Category("Cat-8", "Cat-8", 1, emptyList(), emptyList());
        final Category category7 = new Category("Cat-7", "Cat-7", 1, emptyList(), emptyList());
        final Category category6 = new Category("Cat-6", "Cat-6", 1, emptyList(), List.of(category7, category8));
        final Category category5 = new Category("Cat-5", "Cat-5", 1, emptyList(), emptyList());

        return new Category("Cat-4", "Cat-4", 1, emptyList(), List.of(category5, category6));
    }

    private void primeFileViewDocumentServiceForDocumentDictionary(final Map<String, JsonNode> caseData) {
        final Tuple2<String, Map<String, String>> node00 = new Tuple2<>(
            "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                + ".partyDetail[bf0c087a-cee6-4e6e-b91c-b06a5b4c2e1f].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
                "document_filename", "E_1_AA_Claimant details.xlsx",
                "document_binary_url", "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node01 = new Tuple2<>(
            "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                + ".partyDetail[6dc61824-49f8-413e-b61f-a399b9c8436b].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca",
                "document_filename", "E_1_BBClaimant details.xlsx",
                "document_binary_url", "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node10 = new Tuple2<>(
            "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                + ".partyDetail[f63ebcbc-9abf-40f9-9546-8da5375de65c].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c",
                "document_filename", "E_2_AA_Claimant details.xlsx",
                "document_binary_url", "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node11 = new Tuple2<>(
            "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                + ".partyDetail[81998007-6fe6-4e1e-97ab-cd8e8fb05090].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
                "document_filename", "E_2_BB_Claimant details.xlsx",
                "document_binary_url", "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node20 = new Tuple2<>(
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[45c8633f-6298-4f4f-97df-016e16a18d35].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09",
                "document_filename", "E_3_AA_Claimant details.pdf",
                "document_binary_url", "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node21 = new Tuple2<>(
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[958df3a9-9a57-4717-99ed-63a7cfc3e0e2].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00",
                "document_filename", "E_3_BB_Claimant details.pdf",
                "document_binary_url", "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node22 = new Tuple2<>(
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[930d2fe8-c0fa-4cde-9e81-6fa39ddf4441].value.type",
            Map.of(
                "document_url", "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
                "document_filename", "E_3_CC_Claimant details.pdf",
                "document_binary_url", "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary"
            )
        );

        doReturn(node00, node01, node10, node11, node20, node21, node22)
            .when(fileViewDocumentService).getDocumentNode(anyString(), eq(caseData));
    }

    private List<Document> getExpectedDocumentsForDocumentDictionary() {
        final Document document10 = new Document(
            "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
            "E_1_AA_Claimant details.xlsx",
            "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary",
            "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                + ".partyDetail[bf0c087a-cee6-4e6e-b91c-b06a5b4c2e1f].value.type",
            null
        );

        final Document document11 = new Document(
            "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca",
            "E_1_BBClaimant details.xlsx",
            "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca/binary",
            "state[01827252-e41c-476a-aec0-29ef33fdb8f9].value"
                + ".partyDetail[6dc61824-49f8-413e-b61f-a399b9c8436b].value.type",
            null
        );

        final Document document20 = new Document(
            "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c",
            "E_2_AA_Claimant details.xlsx",
            "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c/binary",
            "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                + ".partyDetail[f63ebcbc-9abf-40f9-9546-8da5375de65c].value.type",
            null
        );

        final Document document21 = new Document(
            "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
            "E_2_BB_Claimant details.xlsx",
            "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary",
            "state[c7327e34-48b1-4b5f-8663-5bad84964fb9].value"
                + ".partyDetail[81998007-6fe6-4e1e-97ab-cd8e8fb05090].value.type",
            null
        );

        final Document document30 = new Document(
            "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09",
            "E_3_AA_Claimant details.pdf",
            "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09/binary",
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[45c8633f-6298-4f4f-97df-016e16a18d35].value.type",
            null
        );

        final Document document31 = new Document(
            "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00",
            "E_3_BB_Claimant details.pdf",
            "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00/binary",
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[958df3a9-9a57-4717-99ed-63a7cfc3e0e2].value.type",
            null
        );

        final Document document32 = new Document(
            "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
            "E_3_CC_Claimant details.pdf",
            "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary",
            "state[afa735d8-686d-40ed-b43d-0bd875b771b1].value"
                + ".partyDetail[930d2fe8-c0fa-4cde-9e81-6fa39ddf4441].value.type",
            null
        );

        return List.of(document10, document11, document20, document21, document30, document31, document32);
    }

    private void primeFileViewDocumentServiceForGetCategoriesAndDocuments(final Map<String, JsonNode> caseData) {
        final Tuple2<String, Map<String, String>> node00 = new Tuple2<>(
            "evidenceDocuments[a0e14419-c0b0-46ea-9e41-9ed8446c9b15].value",
            Map.of(
                "document_url", "http://dm-store:8080/documents/afef337a-a6d8-4c76-b74f-133395705371",
                "document_filename", "1_0019WG_CAMB_MADINGLEY_JPG.jpeg",
                "document_binary_url", "http://dm-store:8080/documents/afef337a-a6d8-4c76-b74f-133395705371/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node10 = new Tuple2<>(
            "nationalityProof.documentEvidence[cf2635fa-fd39-4a35-8e6c-0a6b00331842].value",
            Map.of(
                "document_url", "http://dm-store:8080/documents/cda314f4-3987-4a58-bcbd-6768b998f440",
                "document_filename", "805_calverton.jpeg",
                "document_binary_url", "http://dm-store:8080/documents/cda314f4-3987-4a58-bcbd-6768b998f440/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node20 = new Tuple2<>(
            "miscellaneousDocuments[6763aedb-1873-4dda-a870-711d8f23c02d].value",
            Map.of(
                "document_url", "http://dm-store:8080/documents/f508b7a4-7347-4705-997e-56356b410929",
                "document_filename", "1200px-American_military_cemetery_2003.jpeg",
                "document_binary_url", "http://dm-store:8080/documents/f508b7a4-7347-4705-997e-56356b410929/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node30 = new Tuple2<>(
            "applicationDocuments[bc99820f-218f-4710-b6a2-72e4cce01951].value.document",
            Map.of(
                "document_url", "http://dm-store:8080/documents/6874b267-7db6-4862-9e1f-e25dd982799a",
                "document_filename", "060523-F-0000J-101.jpeg",
                "document_binary_url", "http://dm-store:8080/documents/6874b267-7db6-4862-9e1f-e25dd982799a/binary"
            )
        );
        final Tuple2<String, Map<String, String>> node31 = new Tuple2<>(
            "applicationDocuments[8d7624a8-c77d-4d27-985d-14820f0a6347].value.document",
            Map.of(
                "document_url", "http://dm-store:8080/documents/6d7aff9f-197b-4181-a2e5-3f13a72d5e9a",
                "document_filename", "aa.jpeg",
                "document_binary_url", "http://dm-store:8080/documents/6d7aff9f-197b-4181-a2e5-3f13a72d5e9a/binary"
            )
        );

        doReturn(node00, node10, node20, node30, node31)
            .when(fileViewDocumentService).getDocumentNode(anyString(), eq(caseData));
    }

    private CategoriesAndDocuments getExpectedCategoriesAndDocumentsForGetCategoriesAndDocuments() {
        final Document document00 = new Document(
            "http://dm-store:8080/documents/afef337a-a6d8-4c76-b74f-133395705371",
            "1_0019WG_CAMB_MADINGLEY_JPG.jpeg",
            "http://dm-store:8080/documents/afef337a-a6d8-4c76-b74f-133395705371/binary",
            "evidenceDocuments[a0e14419-c0b0-46ea-9e41-9ed8446c9b15].value",
            null
        );

        final Document document10 = new Document(
            "http://dm-store:8080/documents/cda314f4-3987-4a58-bcbd-6768b998f440",
            "805_calverton.jpeg",
            "http://dm-store:8080/documents/cda314f4-3987-4a58-bcbd-6768b998f440/binary",
            "nationalityProof.documentEvidence[cf2635fa-fd39-4a35-8e6c-0a6b00331842].value",
            null
        );

        final Document document20 = new Document(
            "http://dm-store:8080/documents/f508b7a4-7347-4705-997e-56356b410929",
            "1200px-American_military_cemetery_2003.jpeg",
            "http://dm-store:8080/documents/f508b7a4-7347-4705-997e-56356b410929/binary",
            "miscellaneousDocuments[6763aedb-1873-4dda-a870-711d8f23c02d].value",
            null
        );

        final Document document30 = new Document(
            "http://dm-store:8080/documents/6874b267-7db6-4862-9e1f-e25dd982799a",
            "060523-F-0000J-101.jpeg",
            "http://dm-store:8080/documents/6874b267-7db6-4862-9e1f-e25dd982799a/binary",
            "applicationDocuments[bc99820f-218f-4710-b6a2-72e4cce01951].value.document",
            null
        );

        final Document document31 = new Document(
            "http://dm-store:8080/documents/6d7aff9f-197b-4181-a2e5-3f13a72d5e9a",
            "aa.jpeg",
            "http://dm-store:8080/documents/6d7aff9f-197b-4181-a2e5-3f13a72d5e9a/binary",
            "applicationDocuments[8d7624a8-c77d-4d27-985d-14820f0a6347].value.document",
            null
        );

        return new CategoriesAndDocuments(
            VERSION_NUMBER,
            emptyList(),
            List.of(document00, document10, document20, document30, document31)
        );
    }
}

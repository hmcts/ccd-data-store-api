package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.casefileview.CategoriesAndDocuments;
import uk.gov.hmcts.ccd.domain.model.casefileview.Category;
import uk.gov.hmcts.ccd.domain.model.casefileview.Document;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CategoryDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataExtractor;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

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
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.TestFixtures.getCaseFieldsFromJson;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseDataFromJson;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseDetails;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseTypeDefinition;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseTypeDefinitionFromJson;

@ExtendWith(MockitoExtension.class)
class GetCaseCategoriesAndDocumentsTest {
    @Mock
    private CaseDataExtractor caseDataExtractor;
    @Mock
    private CaseTypeService caseTypeService;

    @InjectMocks
    private GetCaseCategoriesAndDocuments underTest;

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
            new CategoryDefinition("Cat-1", "Cat-1", null, null, null, "1", "");
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
    void test2(final String categoryOnDocument, final String categoryOnFieldDefinition, final String expectedCategory) {
        final String result = underTest.resolveDocumentCategory(categoryOnDocument, categoryOnFieldDefinition);

        assertThat(result)
            .isNotNull()
            .isEqualTo(expectedCategory);
    }

    private static Stream<Arguments> provideCategoryIdParameters() {
        return Stream.of(
            Arguments.of(null, null, "uncategorised_document"),
            Arguments.of(null, "def-cat", "def-cat"),
            Arguments.of("document-cat", null, "document-cat"),
            Arguments.of("document-cat", "def-cat", "document-cat")
        );
    }

    @ParameterizedTest
    @MethodSource("provideCaseFieldExtractParameters")
    void test1(final CaseFieldMetadata caseFieldExtract, final Document expectedDocument) throws Exception {
        final Map<String, JsonNode> caseData =
            loadCaseDataFromJson(String.format("tests/%s", "CaseDataExtractorDocumentData.json"));

        final Tuple2<String, Optional<Document>> actualResult = underTest.transformDocument(caseFieldExtract, caseData);

        assertThat(actualResult.v2)
            .isPresent()
            .map(document -> document)
            .hasValue(expectedDocument);
    }

    @Test
    void testBuildCategorisedDocumentDictionary() throws Exception {
        final List<Document> expectedDocuments = getExpectedDocuments();
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

        final Map<String, List<Document>> results =
            underTest.buildCategorisedDocumentDictionary(caseData, caseFieldDefinitions);

        assertThat(results)
            .isNotEmpty()
            .satisfies(x -> x.forEach((key, value) -> assertThat(value)
                .isNotEmpty()
                .hasSameElementsAs(expectedDocuments)));
    }

    @Test
    void test() throws Exception {
        final CaseTypeDefinition caseTypeDefinition = loadCaseTypeDefinitionFromJson("def-file-complex.json");
        //final CaseDetails caseDetails = loadCaseDetails("def-file-complex.json");
        doReturn(caseTypeDefinition).when(caseTypeService).getCaseType("FT_CaseFileView_2");

        final Map<String, JsonNode> caseData = loadCaseDataFromJson("cd-complex1.json");
        final CategoriesAndDocuments result = underTest.getCategoriesAndDocuments("FT_CaseFileView_2", caseData);

        assertThat(result)
            .isNotNull();
    }

    private static Stream<Arguments> provideCaseFieldExtractParameters() {
        return Stream.of(
            Arguments.of(new CaseFieldMetadata("draftOrderDoc", null),
                new Document(
                    "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8",
                    "A_Simple Document.docx",
                    "http://dm-store:8080/documents/f1f9a2c2-c309-4060-8f77-1800be0c85a8/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("evidence.type", null),
                new Document(
                    "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea",
                    "B_Document Inside Complex Type.docx",
                    "http://dm-store:8080/documents/84f04693-56ae-4aad-97e8-d1fc7592acea/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("extraDocUploadList.0", null),
                new Document(
                    "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb",
                    "C_1_Collection of Documents.docx",
                    "http://dm-store:8080/documents/7654b1e0-5df6-47c4-a5a0-3ec79fc78cfb/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("timeline.1.type", null),
                new Document(
                    "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404",
                    "D_2_Collection of Complex Type with Documents.docx",
                    "http://dm-store:8080/documents/cdab9cf7-1b38-4245-9c91-e098d28f6404/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.0.partyDetail.0.type", null),
                new Document(
                    "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
                    "E_1_AA_Claimant details.xlsx",
                    "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.1.partyDetail.1.type", null),
                new Document(
                    "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
                    "E_2_BB_Claimant details.xlsx",
                    "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary",
                    null,
                    null
                )),
            Arguments.of(new CaseFieldMetadata("state.2.partyDetail.2.type", null),
                new Document(
                    "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
                    "E_3_CC_Claimant details.pdf",
                    "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary",
                    null,
                    null
                ))
        );
    }

    private List<CategoryDefinition> provideRootCategoryDefinitions() {
        final CategoryDefinition cat1 = new CategoryDefinition("Cat-1", "Cat-1", null, null, null, "1", "");
        final CategoryDefinition cat4 = new CategoryDefinition("Cat-4", "Cat-4", null, null, null, "1", "");

        return List.of(cat1, cat4);
    }

    private List<CategoryDefinition> provideCategoryDefinitions() {
        final List<CategoryDefinition> rootCategoryDefinitions = provideRootCategoryDefinitions();
        final CategoryDefinition cat2 = new CategoryDefinition("Cat-2", "Cat-2", "Cat-1", null, null, "1", "");
        final CategoryDefinition cat3 = new CategoryDefinition("Cat-3", "Cat-3", "Cat-2", null, null, "1", "");
        final CategoryDefinition cat5 = new CategoryDefinition("Cat-5", "Cat-5", "Cat-4", null, null, "1", "");
        final CategoryDefinition cat6 = new CategoryDefinition("Cat-6", "Cat-6", "Cat-4", null, null, "1", "");
        final CategoryDefinition cat7 = new CategoryDefinition("Cat-7", "Cat-7", "Cat-6", null, null, "1", "");
        final CategoryDefinition cat8 = new CategoryDefinition("Cat-8", "Cat-8", "Cat-6", null, null, "1", "");

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

    private List<Document> getExpectedDocuments() {
        final Document document10 = new Document(
            "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979",
            "E_1_AA_Claimant details.xlsx",
            "http://dm-store:8080/documents/5a16b8ed-c62f-41b3-b3c9-1df20b6a9979/binary",
            null,
            null
        );

        final Document document11 = new Document(
            "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca",
            "E_1_BBClaimant details.xlsx",
            "http://dm-store:8080/documents/19de0db3-37c6-4191-a81d-c31a1379a9ca/binary",
            null,
            null
        );

        final Document document20 = new Document(
            "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c",
            "E_2_AA_Claimant details.xlsx",
            "http://dm-store:8080/documents/f51456a2-7b25-4855-844a-81c9763bc02c/binary",
            null,
            null
        );

        final Document document21 = new Document(
            "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec",
            "E_2_BB_Claimant details.xlsx",
            "http://dm-store:8080/documents/2b01ebbc-d6e5-4ee5-9a80-58b28cb623ec/binary",
            null,
            null
        );

        final Document document30 = new Document(
            "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09",
            "E_3_AA_Claimant details.pdf",
            "http://dm-store:8080/documents/c6924316-4146-441d-a66d-6e181c48cb09/binary",
            null,
            null
        );

        final Document document31 = new Document(
            "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00",
            "E_3_BB_Claimant details.pdf",
            "http://dm-store:8080/documents/77ad6295-59ce-4167-8f1e-4aa711ba2c00/binary",
            null,
            null
        );

        final Document document32 = new Document(
            "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363",
            "E_3_CC_Claimant details.pdf",
            "http://dm-store:8080/documents/80e9471e-0f67-42ef-8739-170aa1942363/binary",
            null,
            null
        );

        return List.of(document10, document11, document20, document21, document30, document31, document32);
    }
}

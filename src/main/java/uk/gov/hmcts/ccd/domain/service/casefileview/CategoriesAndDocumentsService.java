package uk.gov.hmcts.ccd.domain.service.casefileview;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple2;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.casefileview.CategoriesAndDocuments;
import uk.gov.hmcts.ccd.domain.model.casefileview.Category;
import uk.gov.hmcts.ccd.domain.model.casefileview.Document;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;

@Named
public class CategoriesAndDocumentsService {
    private static final String DOCUMENT_TYPE = "Document";
    private static final String UNCATEGORISED_KEY = "uncategorised_documents";

    private static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String DOCUMENT_FILENAME = "document_filename";
    private static final String CATEGORY_ID = "category_id";
    private static final String ATTRIBUTE_PATH = null;
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    private final CaseDataExtractor caseDataExtractor;
    private final CaseTypeService caseTypeService;
    private final FileViewDocumentService fileViewDocumentService;

    @Inject
    public CategoriesAndDocumentsService(final CaseDataExtractor caseDataExtractor,
                                         final CaseTypeService caseTypeService,
                                         final FileViewDocumentService fileViewDocumentService) {
        this.caseDataExtractor = caseDataExtractor;
        this.caseTypeService = caseTypeService;
        this.fileViewDocumentService = fileViewDocumentService;
    }

    public CategoriesAndDocuments getCategoriesAndDocuments(@NonNull final String caseType,
                                                            @NonNull final Map<String, JsonNode> caseData) {
        final CaseTypeDefinition caseTypeDefinition = caseTypeService.getCaseType(caseType);

        final Map<String, List<Document>> documentDictionary = buildCategorisedDocumentDictionary(
            caseData,
            caseTypeDefinition.getCaseFieldDefinitions()
        );

        final List<Category> categories = buildCategories(caseTypeDefinition.getCategories(), documentDictionary);

        return new CategoriesAndDocuments(
            caseTypeDefinition.getVersion().getNumber().longValue(),
            categories,
            Optional.ofNullable(documentDictionary.get(UNCATEGORISED_KEY)).orElse(emptyList())
        );
    }

    private List<Category> buildCategories(final List<CategoryDefinition> categoryDefinitions,
                                           final Map<String, List<Document>> documentDictionary) {
        final List<CategoryDefinition> rootCategoryDefinitions = findRootCategoryDefinitions(categoryDefinitions);

        return transformCategories(
            rootCategoryDefinitions,
            categoryDefinitions,
            documentDictionary
        );
    }

    List<CategoryDefinition> findRootCategoryDefinitions(@NonNull final List<CategoryDefinition> categoryDefinitions) {
        return categoryDefinitions.stream()
            .filter(category -> null == category.getParentCategoryId())
            .collect(Collectors.toUnmodifiableList());
    }

    Function<CategoryDefinition, Function<List<CategoryDefinition>, Function<Map<String, List<Document>>, Category>>>
        categoryHierarchyFunction = subject -> categoryDefinitions -> documentDictionary ->
        transform(subject, categoryDefinitions, documentDictionary);

    Category transform(@NonNull final CategoryDefinition categoryDefinition,
                       @NonNull final List<CategoryDefinition> categoryDefinitions,
                       @NonNull final Map<String, List<Document>> documentDictionary) {
        final String categoryId = categoryDefinition.getCategoryId();
        final List<Category> children = categoryDefinitions.stream()
            .filter(categoryDef1 -> categoryId.equals(categoryDef1.getParentCategoryId()))
            .map(categoryDef2 -> categoryHierarchyFunction.apply(categoryDef2)
                .apply(categoryDefinitions)
                .apply(documentDictionary))
            .collect(Collectors.toUnmodifiableList());

        return new Category(categoryId,
            categoryDefinition.getCategoryLabel(),
            Integer.valueOf(categoryDefinition.getDisplayOrder()),
            Optional.ofNullable(documentDictionary.get(categoryId)).orElse(emptyList()),
            children
        );
    }

    List<Category> transformCategories(@NonNull final List<CategoryDefinition> rootCategoryDefinitions,
                                       @NonNull final List<CategoryDefinition> categoryDefinitions,
                                       @NonNull final Map<String, List<Document>> documentDictionary) {
        return rootCategoryDefinitions.stream()
            .map(v -> transform(v, categoryDefinitions, documentDictionary))
            .collect(Collectors.toUnmodifiableList());
    }

    private static final Function<List<Tuple2<String, Optional<Document>>>, List<Document>> DOCUMENTS_FUNCTION =
        list -> list.stream()
            .map(tuple -> tuple._2.map(List::of).orElse(emptyList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList());

    Map<String, List<Document>> buildCategorisedDocumentDictionary(
        final Map<String, JsonNode> caseData,
        final List<CaseFieldDefinition> caseFieldDefinitions
    ) {
        final List<CaseFieldMetadata> caseFieldExtracts = caseDataExtractor.extractFieldTypePaths(
            caseData,
            caseFieldDefinitions,
            DOCUMENT_TYPE
        );

        return caseFieldExtracts.stream()
            .map(caseFieldExtract -> transformDocument(caseFieldExtract, caseData))
            .collect(Collectors.groupingBy(tuple -> tuple._1,
                collectingAndThen(toUnmodifiableList(), DOCUMENTS_FUNCTION)));
    }

    Tuple2<String, Optional<Document>> transformDocument(@NonNull final CaseFieldMetadata caseFieldMetadata,
                                                         @NonNull final Map<String, JsonNode> caseData) {
        final Tuple2<String, Map<String, String>> documentNode =
            fileViewDocumentService.getDocumentNode(caseFieldMetadata.getPath(), caseData);

        final Document document = buildDocument(documentNode._1, documentNode._2);

        final String resolvedCategory = resolveDocumentCategory(
            documentNode._2.get(CATEGORY_ID),
            caseFieldMetadata.getCategoryId()
        );

        return new Tuple2<>(resolvedCategory, Optional.of(document));
    }

    private Document buildDocument(final String attributePath, @NonNull final Map<String, String> documentNode) {
        return new Document(
            documentNode.get(DOCUMENT_URL),
            documentNode.get(DOCUMENT_FILENAME),
            documentNode.get(DOCUMENT_BINARY_URL),
            attributePath,
            null
        );
    }

    String resolveDocumentCategory(final String categoryOnDocument,
                                   final String categoryOnFieldDefinition) {
        return Optional.ofNullable(categoryOnDocument)
            .orElseGet(() -> resolveDocumentCategory(categoryOnFieldDefinition));
    }

    private String resolveDocumentCategory(final String categoryOnFieldDefinition) {
        return null == categoryOnFieldDefinition ? UNCATEGORISED_KEY : categoryOnFieldDefinition;
    }
}

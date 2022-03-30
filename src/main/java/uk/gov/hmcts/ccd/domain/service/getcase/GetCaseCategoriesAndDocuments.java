package uk.gov.hmcts.ccd.domain.service.getcase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import org.jooq.lambda.tuple.Tuple2;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

@Named
public class GetCaseCategoriesAndDocuments {
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

    @Inject
    public GetCaseCategoriesAndDocuments(final CaseDataExtractor caseDataExtractor,
                                         final CaseTypeService caseTypeService) {
        this.caseDataExtractor = caseDataExtractor;
        this.caseTypeService = caseTypeService;
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
        func2 = subject -> categoryDefinitions -> documentDictionary ->
        transform(subject, categoryDefinitions, documentDictionary);

    Category transform(@NonNull final CategoryDefinition categoryDefinition,
                       @NonNull final List<CategoryDefinition> categoryDefinitions,
                       @NonNull final Map<String, List<Document>> documentDictionary) {
        final String categoryId = categoryDefinition.getCategoryId();
        final List<Category> children = categoryDefinitions.stream()
            .filter(x -> categoryId.equals(x.getParentCategoryId()))
            .map(y -> func2.apply(y).apply(categoryDefinitions).apply(documentDictionary))
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
            .map(tuple -> tuple.v2.map(List::of).orElse(emptyList()))
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
            .collect(Collectors.groupingBy(tuple -> tuple.v1,
                collectingAndThen(toUnmodifiableList(), DOCUMENTS_FUNCTION)));
    }

    Tuple2<String, Optional<Document>> transformDocument(@NonNull final CaseFieldMetadata caseFieldMetadata,
                                                         @NonNull final Map<String, JsonNode> caseData) {
        final Optional<Map<String, String>> documentNode =
            findDocumentNode(caseFieldMetadata.getPathAsJsonPath(), caseData);
        return documentNode.map(node -> {
            final Document document = buildDocument(node, caseFieldMetadata.getPathAsAttributePath());
            final String resolvedCategory = resolveDocumentCategory(
                node.get(CATEGORY_ID),
                caseFieldMetadata.getCategoryId()
            );

            return new Tuple2<>(resolvedCategory, Optional.of(document));
        }).orElseGet(() -> {
            final String resolvedCategory = resolveDocumentCategory(caseFieldMetadata.getCategoryId());
            return new Tuple2<>(resolvedCategory, Optional.empty());
        });
    }

    private Optional<Map<String, String>> findDocumentNode(@NonNull final String documentPath,
                                                           @NonNull final Map<String, JsonNode> caseData) {
        try {
            final String jsonString = MAPPER.writeValueAsString(caseData);
            final Map<String, String> documentNode = JsonPath.parse(jsonString)
                .read(documentPath);
            return Optional.ofNullable(documentNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Document buildDocument(@NonNull final Map<String, String> documentNode, final String attributePath) {
        return new Document(
            documentNode.get(DOCUMENT_URL),
            documentNode.get(DOCUMENT_FILENAME),
            documentNode.get(DOCUMENT_BINARY_URL),
            attributePath,
            LocalDateTime.now()
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

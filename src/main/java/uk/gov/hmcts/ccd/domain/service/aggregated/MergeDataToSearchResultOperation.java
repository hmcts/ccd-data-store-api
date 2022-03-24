package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeSearchResultProcessor;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static java.lang.String.format;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;

@Named
@Singleton
public class MergeDataToSearchResultOperation {

    private final DateTimeSearchResultProcessor dateTimeSearchResultProcessor;
    private CaseDataAccessControl caseDataAccessControl;

    public MergeDataToSearchResultOperation(final DateTimeSearchResultProcessor dateTimeSearchResultProcessor,
                                            CaseDataAccessControl caseDataAccessControl) {
        this.dateTimeSearchResultProcessor = dateTimeSearchResultProcessor;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public SearchResultView execute(final CaseTypeDefinition caseTypeDefinition,
                                    final SearchResultDefinition searchResult,
                                    final List<CaseDetails> caseDetails,
                                    final String resultError) {

        final List<SearchResultViewColumn> viewColumns = buildSearchResultViewColumn(caseTypeDefinition, searchResult);

        final List<SearchResultViewItem> viewItems = caseDetails.stream()
            .map(caseData -> buildSearchResultViewItem(caseData, caseTypeDefinition, searchResult))
            .collect(Collectors.toList());

        return new SearchResultView(
            viewColumns,
            dateTimeSearchResultProcessor.execute(viewColumns, viewItems),
            resultError
        );
    }

    private List<SearchResultViewColumn> buildSearchResultViewColumn(CaseTypeDefinition caseTypeDefinition,
                                                                    SearchResultDefinition searchResult) {
        final HashSet<String> addedFields = new HashSet<>();

        return Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseTypeDefinition.getCaseFieldDefinitions().stream()
                    .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                    .filter(caseField -> filterDistinctFieldsByAccessProfile(addedFields, searchResultField))
                    .map(caseField -> createSearchResultViewColumn(searchResultField, caseField))
                    )
            .collect(Collectors.toList());
    }

    private SearchResultViewColumn createSearchResultViewColumn(final SearchResultField searchResultField,
                                                                final CaseFieldDefinition caseFieldDefinition) {
        CommonField commonField = commonField(searchResultField, caseFieldDefinition);
        return new SearchResultViewColumn(
            searchResultField.buildCaseFieldId(),
            commonField.getFieldTypeDefinition(),
            searchResultField.getLabel(),
            searchResultField.getDisplayOrder(),
            searchResultField.isMetadata(),
            displayContextParameter(searchResultField, commonField));
    }

    private boolean filterDistinctFieldsByAccessProfile(final HashSet<String> addedFields,
                                                        final SearchResultField resultField) {
        String id = resultField.buildCaseFieldId();
        if (addedFields.contains(id)) {
            return false;
        } else {

            if (StringUtils.isEmpty(resultField.getRole())
                || caseDataAccessControl.anyAccessProfileEqualsTo(resultField.getCaseTypeId(),
                resultField.getRole())) {
                addedFields.add(id);
                return true;
            } else {
                return false;
            }
        }
    }

    private CommonField commonField(SearchResultField searchResultField, CaseFieldDefinition caseFieldDefinition) {
        return caseFieldDefinition.getComplexFieldNestedField(searchResultField.getCaseFieldPath())
            .orElseThrow(() ->
                new BadRequestException(format("CaseField %s has no nested elements with code %s.",
                    caseFieldDefinition.getId(), searchResultField.getCaseFieldPath())));
    }

    private String displayContextParameter(SearchResultField searchResultField, CommonField commonField) {
        return searchResultField.getDisplayContextParameter() == null
            ? commonField.getDisplayContextParameter()
            : searchResultField.getDisplayContextParameter();
    }

    private SearchResultViewItem buildSearchResultViewItem(final CaseDetails caseDetails,
                                                           final CaseTypeDefinition caseTypeDefinition,
                                                           final SearchResultDefinition searchResult) {
        Map<String, JsonNode> caseData = new HashMap<>(caseDetails.getData());
        Map<String, Object> caseMetadata = new HashMap<>(caseDetails.getMetadata());
        convertReferenceToString(caseMetadata);
        Map<String, TextNode> labels = caseTypeDefinition.getLabelsFromCaseFields();
        Map<String, Object> caseFields = prepareData(searchResult, caseData, caseMetadata, labels);
        String caseId = caseDetails.hasCaseReference() ? caseDetails.getReferenceAsString() : caseDetails.getId();
        return new SearchResultViewItem(caseId, caseFields, new HashMap<>(caseFields));
    }


    private Map<String, Object> convertReferenceToString(Map<String, Object> caseMetadata) {
        final String convertedCaseReference = caseMetadata.get(CASE_REFERENCE.getReference()).toString();
        caseMetadata.put(CASE_REFERENCE.getReference(), convertedCaseReference);
        return caseMetadata;
    }

    private Map<String, Object> prepareData(SearchResultDefinition searchResult,
                                            Map<String, JsonNode> caseData,
                                            Map<String, Object> metadata,
                                            Map<String, TextNode> labels) {

        Map<String, Object> newResults = new HashMap<>();

        searchResult.getFieldsWithPaths().forEach(searchResultField -> {
            JsonNode topLevelCaseFieldNode = caseData.get(searchResultField.getCaseFieldId());
            if (topLevelCaseFieldNode != null) {
                newResults.put(searchResultField.buildCaseFieldId(),
                    getNestedCaseFieldByPath(topLevelCaseFieldNode, searchResultField.getCaseFieldPath()));
            }
        });

        newResults.putAll(caseData);
        newResults.putAll(labels);
        newResults.putAll(metadata);

        return newResults;
    }
}

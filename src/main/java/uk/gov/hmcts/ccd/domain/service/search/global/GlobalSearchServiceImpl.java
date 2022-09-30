package uk.gov.hmcts.ccd.domain.service.search.global;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchIndex;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.search.global.LocationCollector.toLocationLookup;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;
    private final ReferenceDataRepository referenceDataRepository;
    private final GlobalSearchResponseTransformer globalSearchResponseTransformer;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;
    private final GlobalSearchQueryBuilder globalSearchQueryBuilder;
    private final CaseDefinitionRepository caseDefinitionRepository;

    static final Function<List<BuildingLocation>, LocationLookup> LOCATION_LOOKUP_FUNCTION =
        locations -> locations.stream().collect(toLocationLookup());

    static final Function<List<ServiceReferenceData>, ServiceLookup> SERVICE_LOOKUP_FUNCTION =
        services -> {
            final Map<String, String> servicesMap = services.stream()
                .collect(toUnmodifiableMap(ServiceReferenceData::getServiceCode,
                    ServiceReferenceData::getServiceShortDescription));
            return new ServiceLookup(servicesMap);
        };

    @Autowired
    public GlobalSearchServiceImpl(final ApplicationParams applicationParams,
                                   final ObjectMapperService objectMapperService,
                                   final ReferenceDataRepository referenceDataRepository,
                                   final GlobalSearchResponseTransformer globalSearchResponseTransformer,
                                   final ElasticsearchQueryHelper elasticsearchQueryHelper,
                                   final GlobalSearchQueryBuilder globalSearchQueryBuilder,
                                   @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                       final CaseDefinitionRepository caseDefinitionRepository) {
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
        this.referenceDataRepository = referenceDataRepository;
        this.globalSearchResponseTransformer = globalSearchResponseTransformer;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
        this.globalSearchQueryBuilder = globalSearchQueryBuilder;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public CrossCaseTypeSearchRequest assembleSearchQuery(GlobalSearchRequestPayload request) {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (request == null || request.getSearchCriteria() == null) {
            return null;
        }

        // if no CaseType filter applied :: load all case types available for jurisdictions
        if (CollectionUtils.isEmpty(request.getSearchCriteria().getCcdCaseTypeIds())) {
            // NB: population of CaseTypeIds is needed to ensure the case type filters are applied to the search
            request.getSearchCriteria().setCcdCaseTypeIds(
                caseDefinitionRepository.getCaseTypesIDsByJurisdictions(
                    request.getSearchCriteria().getCcdJurisdictionIds()
                )
            );
        }

        // generate ES query builder
        searchSourceBuilder.query(globalSearchQueryBuilder.globalSearchQuery(request));

        // add sort(s)
        globalSearchQueryBuilder.globalSearchSort(request).forEach(searchSourceBuilder::sort);

        // add pagination
        if (request.getMaxReturnRecordCount() != null && request.getMaxReturnRecordCount() > 0) {
            searchSourceBuilder.size(request.getMaxReturnRecordCount());
        }
        if (request.getStartRecordNumber() != null && request.getStartRecordNumber() > 0) {
            // NB: `GS.StartRecordNumber` is not zero indexed but `ES.from` is
            searchSourceBuilder.from(request.getStartRecordNumber() - 1);
        }

        // construct search JSON
        ObjectNode jsonSearchRequest =
            objectMapperService.convertStringToObject(searchSourceBuilder.toString(), ObjectNode.class);

        // :: configure data fields to return
        jsonSearchRequest.set(ElasticsearchRequest.SOURCE, globalSearchQueryBuilder.globalSearchSourceFields());

        // convert to CCD ES request
        ElasticsearchRequest elasticsearchRequest =
            elasticsearchQueryHelper.validateAndConvertRequest(jsonSearchRequest.toString());

        // :: configure SupplementaryData fields to return
        elasticsearchRequest.setRequestedSupplementaryData(
            globalSearchQueryBuilder.globalSearchSupplementaryDataFields()
        );

        // point to global search index
        SearchIndex searchIndex = new SearchIndex(
            applicationParams.getGlobalSearchIndexName(),
            applicationParams.getGlobalSearchIndexType()
        );

        return new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(request.getSearchCriteria().getCcdCaseTypeIds())
            .withSearchRequest(elasticsearchRequest)
            .withSearchIndex(searchIndex)
            .build();
    }

    public GlobalSearchResponsePayload transformResponse(final GlobalSearchRequestPayload requestPayload,
                                                         Long caseSearchResultTotal,
                                                         final List<CaseDetails> filteredCaseList) {
        final List<ServiceReferenceData> services = referenceDataRepository.getServices();
        final List<BuildingLocation> buildingLocations = referenceDataRepository.getBuildingLocations();
        final ServiceLookup serviceLookup = SERVICE_LOOKUP_FUNCTION.apply(services);
        final LocationLookup locationLookup = LOCATION_LOOKUP_FUNCTION.apply(buildingLocations);

        final List<GlobalSearchResponsePayload.Result> results = filteredCaseList.stream()
            .map(caseDetails ->
                globalSearchResponseTransformer.transformResult(caseDetails, serviceLookup, locationLookup))
            .collect(Collectors.toUnmodifiableList());

        final GlobalSearchResponsePayload.ResultInfo resultInfo = globalSearchResponseTransformer.transformResultInfo(
            requestPayload.getMaxReturnRecordCount(),
            requestPayload.getStartRecordNumber(),
            caseSearchResultTotal,
            results.size()
        );

        return GlobalSearchResponsePayload.builder()
            .resultInfo(resultInfo)
            .results(results)
            .build();
    }

}

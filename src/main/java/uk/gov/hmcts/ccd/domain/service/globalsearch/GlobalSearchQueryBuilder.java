package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CASE_TYPE;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.BASE_LOCATION;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_ID;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_NAME;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_LOCATION;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.CASE_NAME_HMCTS_INTERNAL;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.OTHER_REFERENCE;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.REGION;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTIES;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_DATE_OF_BIRTH;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_EMAIL_ADDRESS;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_NAME;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_POSTCODE;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.JURISDICTION;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.REFERENCE;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.STATE;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchFields.SupplementaryDataFields.SERVICE_ID;

@Named
public class GlobalSearchQueryBuilder {

    static final String STANDARD_ANALYZER = "standard";

    public QueryBuilder globalSearchQuery(GlobalSearchRequestPayload request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (request != null) {
            SearchCriteria searchCriteria = request.getSearchCriteria();
            if (searchCriteria != null) {
                // add terms queries for properties that must match 1 from many
                addTermsQuery(boolQueryBuilder, REFERENCE, searchCriteria.getCaseReferences());
                addTermsQuery(boolQueryBuilder, JURISDICTION, searchCriteria.getCcdJurisdictionIds());
                addTermsQuery(boolQueryBuilder, CASE_TYPE, searchCriteria.getCcdCaseTypeIds());
                addTermsQuery(boolQueryBuilder, STATE, searchCriteria.getStateIds());
                addTermsQuery(boolQueryBuilder, REGION, searchCriteria.getCaseManagementRegionIds());
                addTermsQuery(boolQueryBuilder, BASE_LOCATION, searchCriteria.getCaseManagementBaseLocationIds());
                addTermsQuery(boolQueryBuilder, OTHER_REFERENCE, searchCriteria.getOtherReferences());
                // add parties query for all party values
                addPartiesQuery(boolQueryBuilder, searchCriteria.getParties());
            }
        }

        return boolQueryBuilder;
    }

    public List<FieldSortBuilder> globalSearchSort(GlobalSearchRequestPayload request) {
        List<FieldSortBuilder> sortBuilders = Lists.newArrayList();

        if (request != null && request.getSortCriteria() != null) {
            request.getSortCriteria().forEach(sortCriteria -> createSort(sortCriteria).ifPresent(sortBuilders::add));
        }

        return sortBuilders;
    }

    /**
     * Get list of all case data fields to return from search.
     */
    public ArrayNode globalSearchSourceFields() {
        return JacksonUtils.MAPPER.createArrayNode()
            // root level case data fields
            .add(CASE_MANAGEMENT_CATEGORY_ID)
            .add(CASE_MANAGEMENT_CATEGORY_NAME)
            .add(CASE_MANAGEMENT_LOCATION)
            .add(CASE_NAME_HMCTS_INTERNAL)
            // remaining case data fields
            .add(OTHER_REFERENCE);
    }

    /**
     * Get list of all SupplementaryData fields to return from search.
     */
    public ArrayNode globalSearchSupplementaryDataFields() {
        return JacksonUtils.MAPPER.createArrayNode()
            .add(SERVICE_ID);
    }

    private void addTermsQuery(BoolQueryBuilder boolQueryBuilder, String term, List<String> values) {
        if (CollectionUtils.isNotEmpty(values)) {
            boolQueryBuilder.must(
                // NB: switch to lower case for terms query
                QueryBuilders.termsQuery(term, values.stream().map(String::toLowerCase).collect(Collectors.toList()))
            );
        }
    }

    private void addPartiesQuery(BoolQueryBuilder boolQueryBuilder, List<Party> parties) {
        if (CollectionUtils.isNotEmpty(parties)) {
            // NB: the generated ShouldQuery, which will contain all the party specific queries, will be wrapped in a
            //     single BoolQuery and added to the list of 'must' queries (i.e. alongside the other term based
            //     queries) rather than added directly to the main query.  This will allow any future additional
            //     SearchCriteria object based queries to be added in a similar fashion without impacting this query.
            BoolQueryBuilder partiesQueryBuilder = QueryBuilders.boolQuery();

            parties.forEach(party -> createPartyQuery(party).ifPresent(partiesQueryBuilder::should));

            // if found any party queries:: complete the parties query and attach to main query.
            if (!partiesQueryBuilder.should().isEmpty()) {
                partiesQueryBuilder.minimumShouldMatch(1);
                boolQueryBuilder.must(partiesQueryBuilder);
            }
        }
    }

    private Optional<QueryBuilder> createPartyQuery(Party party) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (party != null) {
            if (StringUtils.isNotBlank(party.getPartyName())) {
                boolQueryBuilder.must(
                    QueryBuilders.matchPhraseQuery(SEARCH_PARTY_NAME, party.getPartyName())
                        .analyzer(STANDARD_ANALYZER)
                );
            }
            if (StringUtils.isNotBlank(party.getEmailAddress())) {
                boolQueryBuilder.must(
                    QueryBuilders.matchPhraseQuery(SEARCH_PARTY_EMAIL_ADDRESS, party.getEmailAddress())
                        .analyzer(STANDARD_ANALYZER)
                );
            }
            if (StringUtils.isNotBlank(party.getAddressLine1())) {
                boolQueryBuilder.must(
                    QueryBuilders.matchPhraseQuery(SEARCH_PARTY_ADDRESS_LINE_1, party.getAddressLine1())
                        .analyzer(STANDARD_ANALYZER)
                );
            }
            if (StringUtils.isNotBlank(party.getPostCode())) {
                boolQueryBuilder.must(
                    QueryBuilders.matchPhraseQuery(SEARCH_PARTY_POSTCODE, party.getPostCode())
                        .analyzer(STANDARD_ANALYZER)
                );
            }
            if (StringUtils.isNotBlank(party.getDateOfBirth())) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery(SEARCH_PARTY_DATE_OF_BIRTH)
                    .gte(party.getDateOfBirth())
                    .lte(party.getDateOfBirth())
                );
            }
        }

        if (boolQueryBuilder.must().isEmpty()) {
            return Optional.empty();
        }

        // NB: uses nested query so multiple property search is against a single party object
        return Optional.of(QueryBuilders.nestedQuery(SEARCH_PARTIES, boolQueryBuilder, ScoreMode.Total));
    }

    private Optional<FieldSortBuilder> createSort(SortCriteria sortCriteria) {

        if (sortCriteria != null && StringUtils.isNotBlank(sortCriteria.getSortBy())) {
            GlobalSearchSortByCategory sortByCategory = GlobalSearchSortByCategory.getEnum(sortCriteria.getSortBy());

            if (sortByCategory != null) {
                SortOrder sortOrder =
                    GlobalSearchSortDirection.DESCENDING.name().equalsIgnoreCase(sortCriteria.getSortDirection())
                        ? SortOrder.DESC : SortOrder.ASC;

                return Optional.of(SortBuilders
                    .fieldSort(sortByCategory.getField())
                    .order(sortOrder));
            }
        }

        return Optional.empty();
    }

}

package uk.gov.hmcts.ccd.domain.service.search.global;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOptionsBuilders;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;


import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CASE_TYPE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.BASE_LOCATION;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.CASE_ACCESS_CATEGORY;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_ID;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_NAME;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_LOCATION;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.CASE_NAME_HMCTS_INTERNAL;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.OTHER_REFERENCE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.OTHER_REFERENCE_VALUE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.REGION;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTIES;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_DATE_OF_BIRTH;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_DATE_OF_DEATH;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_EMAIL_ADDRESS;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_NAME;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataPaths.SEARCH_PARTY_POSTCODE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.JURISDICTION;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.REFERENCE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.STATE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SupplementaryDataFields.SERVICE_ID;

@Named
public class GlobalSearchQueryBuilder {

    static final String KEYWORD = ".keyword";

    public Query globalSearchQuery(GlobalSearchRequestPayload request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        int numberOfShouldFields = 0;

        if (request != null) {
            SearchCriteria searchCriteria = request.getSearchCriteria();
            if (searchCriteria != null) {
                numberOfShouldFields += checkForWildcardValues(boolBuilder, REFERENCE,
                    searchCriteria.getCaseReferences());
                addTermsQuery(boolBuilder, CASE_TYPE, searchCriteria.getCcdCaseTypeIds());
                addTermsQuery(boolBuilder, JURISDICTION, searchCriteria.getCcdJurisdictionIds());
                addTermsQuery(boolBuilder, STATE, searchCriteria.getStateIds());
                addTermsQuery(boolBuilder, REGION, searchCriteria.getCaseManagementRegionIds());
                addTermsQuery(boolBuilder, BASE_LOCATION, searchCriteria.getCaseManagementBaseLocationIds());
                numberOfShouldFields += checkForWildcardValues(boolBuilder, OTHER_REFERENCE_VALUE,
                    searchCriteria.getOtherReferences());

                addPartiesQuery(boolBuilder, searchCriteria.getParties());

                if (numberOfShouldFields > 0) {
                    boolBuilder.minimumShouldMatch(String.valueOf(numberOfShouldFields));
                }
            }
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    private Optional<Query> createPartyQuery(Party party) {
        if (party == null) {
            return Optional.empty();
        }

        BoolQuery.Builder innerBool = new BoolQuery.Builder();

        addNameQueryIfPresent(party, innerBool);
        addEmailQueryIfPresent(party, innerBool);
        addAddressQueryIfPresent(party, innerBool);
        addPostCodeQueryIfPresent(party, innerBool);
        addDateOfBirthQueryIfPresent(party, innerBool);
        addDateOfDeathQueryIfPresent(party, innerBool);

        if (innerBool.build().must().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(createNestedPartyQuery(innerBool));
    }

    public List<SortOptions> globalSearchSort(GlobalSearchRequestPayload request) {
        List<SortOptions> sortOptions = new ArrayList<>();

        if (request != null && request.getSortCriteria() != null) {
            for (SortCriteria sortCriteria : request.getSortCriteria()) {
                Optional<SortOptions> sort = createSort(sortCriteria);
                sort.ifPresent(sortOptions::add);
            }
        }

        return sortOptions;
    }

    private int checkForWildcardValues(BoolQuery.Builder builder, String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            for (String val : values) {
                builder.should(Query.of(q -> q.wildcard(w -> w.field(field + KEYWORD)
                    .value(val))));
            }
            return 1;
        }
        return 0;
    }

    private void addTermsQuery(BoolQuery.Builder builder, String field, List<String> values) {
        if (CollectionUtils.isNotEmpty(values)) {
            builder.must(Query.of(q -> q.terms(t -> t
                .field(field)
                .terms(v -> v.value(values.stream().map(String::toLowerCase).map(FieldValue::of)
                    .collect(Collectors.toList())))
            )));
        }
    }

    private void addPartiesQuery(BoolQuery.Builder builder, List<Party> parties) {
        if (CollectionUtils.isNotEmpty(parties)) {
            BoolQuery.Builder partyBoolBuilder = new BoolQuery.Builder();

            for (Party party : parties) {
                createPartyQuery(party).ifPresent(partyBoolBuilder::should);
            }

            if (!partyBoolBuilder.build().should().isEmpty()) {
                partyBoolBuilder.minimumShouldMatch("1");
                builder.must(Query.of(q -> q.bool(partyBoolBuilder.build())));
            }
        }
    }

    private void addNameQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getPartyName())) {
            builder.must(Query.of(q -> q.wildcard(w -> w
                .field(SEARCH_PARTY_NAME + KEYWORD)
                .value(party.getPartyName()))));
        }
    }

    private void addEmailQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getEmailAddress())) {
            builder.must(Query.of(q -> q.matchPhrase(m -> m
                .field(SEARCH_PARTY_EMAIL_ADDRESS)
                .query(party.getEmailAddress()))));
        }
    }

    private void addAddressQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getAddressLine1())) {
            builder.must(Query.of(q -> q.wildcard(w -> w
                .field(SEARCH_PARTY_ADDRESS_LINE_1 + KEYWORD)
                .value(party.getAddressLine1()))));
        }
    }

    private void addPostCodeQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getPostCode())) {
            builder.must(Query.of(q -> q.matchPhrase(m -> m
                .field(SEARCH_PARTY_POSTCODE)
                .query(party.getPostCode()))));
        }
    }

    private void addDateOfBirthQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getDateOfBirth())) {
            builder.must(createDateRangeQuery(SEARCH_PARTY_DATE_OF_BIRTH, party.getDateOfBirth()));
        }
    }

    private void addDateOfDeathQueryIfPresent(Party party, BoolQuery.Builder builder) {
        if (StringUtils.isNotBlank(party.getDateOfDeath())) {
            builder.must(createDateRangeQuery(SEARCH_PARTY_DATE_OF_DEATH, party.getDateOfDeath()));
        }
    }

    private Query createDateRangeQuery(String fieldName, String dateToDo) {

        if (StringUtils.isNotBlank(dateToDo)) {
            return Query.of(q -> q.range(r -> r
                .date(d -> d
                    .field(fieldName)
                    .gte(dateToDo)
                    .lte(dateToDo)
                )
            ));

        }
        return null; // TODO: Handle null case appropriately
    }

    private Query createNestedPartyQuery(BoolQuery.Builder innerBool) {
        return Query.of(q -> q.nested(n -> n
            .path(SEARCH_PARTIES)
            .query(Query.of(b -> b.bool(innerBool.build())))
            .scoreMode(ChildScoreMode.Sum)));
    }


    private Optional<SortOptions> createSort(SortCriteria sortCriteria) {
        if (sortCriteria != null && StringUtils.isNotBlank(sortCriteria.getSortBy())) {
            GlobalSearchSortByCategory sortByCategory = GlobalSearchSortByCategory.getEnum(sortCriteria.getSortBy());

            if (sortByCategory != null) {
                SortOrder sortOrder =
                    GlobalSearchSortDirection.DESCENDING.name().equalsIgnoreCase(sortCriteria.getSortDirection())
                        ? SortOrder.Desc : SortOrder.Asc;

                return Optional.of(SortOptionsBuilders.field(f -> f
                    .field(sortByCategory.getField())
                    .order(sortOrder)));
            }
        }
        return Optional.empty();
    }


    public ArrayNode globalSearchSourceFields() {
        return JacksonUtils.MAPPER.createArrayNode()
            .add(CASE_ACCESS_CATEGORY)
            .add(CASE_MANAGEMENT_CATEGORY_ID)
            .add(CASE_MANAGEMENT_CATEGORY_NAME)
            .add(CASE_MANAGEMENT_LOCATION)
            .add(CASE_NAME_HMCTS_INTERNAL)
            .add(OTHER_REFERENCE);
    }

    public ArrayNode globalSearchSupplementaryDataFields() {
        return JacksonUtils.MAPPER.createArrayNode()
            .add(SERVICE_ID);
    }
}

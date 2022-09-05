package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.DefaultJurisdictionsResolver;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.JurisdictionsResolver;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SUPPLEMENTARY_DATA;

@Service
@Slf4j
public class ElasticsearchQueryHelper {

    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UserRepository userRepository;
    private final JurisdictionsResolver jurisdictionsResolver;

    @Autowired
    public ElasticsearchQueryHelper(ApplicationParams applicationParams,
                                    ObjectMapperService objectMapperService,
                                    @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                        CaseDefinitionRepository caseDefinitionRepository,
                                    @Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository,
                                    @Qualifier(DefaultJurisdictionsResolver.QUALIFIER)
                                        JurisdictionsResolver jurisdictionsResolver) {
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.userRepository = userRepository;
        this.jurisdictionsResolver = jurisdictionsResolver;
    }

    public List<String> getGlobalSearchCaseTypes(GlobalSearchRequestPayload payload) {
        List<String> jurisdictions = payload.getSearchCriteria().getCcdJurisdictionIds();
        List<String> caseTypes = payload.getSearchCriteria().getCcdCaseTypeIds();

        if (CollectionUtils.isEmpty(jurisdictions) && CollectionUtils.isEmpty(caseTypes)) {
            throw new BadSearchRequest("At least one jurisdiction or case type must be provided");
        }

        if (!CollectionUtils.isEmpty(caseTypes)) {
            return caseTypes;
        }

        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
    }

    public List<String> getCaseTypesAvailableToUser() {
        if (userRepository.anyRoleEqualsAnyOf(applicationParams.getCcdAccessControlCrossJurisdictionRoles())) {
            return caseDefinitionRepository.getAllCaseTypesIDs();
        } else {
            return getCaseTypesFromUserJurisdictions();
        }
    }

    private List<String> getCaseTypesFromUserJurisdictions() {
        List<String> jurisdictions = jurisdictionsResolver.getJurisdictions();
        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
    }

    public ElasticsearchRequest validateAndConvertRequest(String jsonSearchRequest) {
        rejectBlackListedQuery(jsonSearchRequest);
        JsonNode searchRequestNode;
        try {
            searchRequestNode = objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class);
        } catch (ServiceException ex) {
            throw new BadRequestException("Request requires correctly formatted JSON, " + ex.getMessage());
        }
        validateSupplementaryData(searchRequestNode);
        return new ElasticsearchRequest(searchRequestNode);
    }

    private void rejectBlackListedQuery(String jsonSearchRequest) {
        List<String> blackListedQueries = applicationParams.getSearchBlackList();
        Optional<String> blackListedQueryOpt = blackListedQueries
            .stream()
            .filter(blacklisted -> {
                Pattern p = Pattern.compile("\\b" + blacklisted + "\\b");
                Matcher m = p.matcher(jsonSearchRequest);
                return m.find();
            })
            .findFirst();
        blackListedQueryOpt.ifPresent(blacklisted -> {
            throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
        });
    }

    private void validateSupplementaryData(JsonNode searchRequest) {
        JsonNode supplementaryDataNode = searchRequest.get(SUPPLEMENTARY_DATA);
        if (supplementaryDataNode != null && !isArrayOfTextFields(supplementaryDataNode)) {
            throw new BadSearchRequest("Requested supplementary_data must be an array of text fields.");
        }
    }

    private boolean isArrayOfTextFields(JsonNode node) {
        return node.isArray() && arrayContainsOnlyText((ArrayNode) node);
    }

    private boolean arrayContainsOnlyText(ArrayNode node) {
        return StreamSupport.stream(node.spliterator(), false)
            .allMatch(JsonNode::isTextual);
    }
}

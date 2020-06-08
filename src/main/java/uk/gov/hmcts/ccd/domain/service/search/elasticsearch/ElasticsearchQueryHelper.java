package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ElasticsearchQueryHelper {

    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public ElasticsearchQueryHelper(ApplicationParams applicationParams,
                                    ObjectMapperService objectMapperService) {
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
    }

    public ElasticsearchRequest validateAndConvertRequest(String jsonSearchRequest) {
        rejectBlackListedQuery(jsonSearchRequest);
        return new ElasticsearchRequest(objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class));
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
}

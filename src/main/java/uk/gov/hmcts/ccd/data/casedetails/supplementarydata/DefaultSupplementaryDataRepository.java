package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.DefaultObjectMapperService;

@Service
@Qualifier("default")
@Singleton
@Transactional
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSupplementaryDataRepository.class);

    @PersistenceContext
    private EntityManager em;

    private List<SupplementaryDataQueryBuilder> queryBuilders;
    private DefaultObjectMapperService defaultObjectMapperService;

    @Autowired
    public DefaultSupplementaryDataRepository(final List<SupplementaryDataQueryBuilder> queryBuilders,
                                              DefaultObjectMapperService defaultObjectMapperService) {
        this.queryBuilders = queryBuilders;
        this.defaultObjectMapperService = defaultObjectMapperService;
    }

    @Override
    public void setSupplementaryData(final String caseReference, final SupplementaryDataUpdateRequest updateRequest) {
        LOG.debug("Set supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.SET).buildQueryForEachSupplementaryDataProperty(em, caseReference, updateRequest);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public void incrementSupplementaryData(final String caseReference, SupplementaryDataUpdateRequest updateRequest) {
        LOG.debug("Insert supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.INC).buildQueryForEachSupplementaryDataProperty(em, caseReference, updateRequest);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference, SupplementaryDataUpdateRequest updateRequest) {
        LOG.debug("Find supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.FIND).buildQueryForEachSupplementaryDataProperty(em, caseReference, updateRequest);
        JsonNode responseNode = (JsonNode) queryList.get(0).getSingleResult();
        return getSupplementaryData(updateRequest, responseNode);
    }

    private SupplementaryData getSupplementaryData(SupplementaryDataUpdateRequest updateRequest, JsonNode responseNode) {
        if (updateRequest != null) {
            DocumentContext context = JsonPath.parse(defaultObjectMapperService.convertObjectToString(responseNode));
            Map<String, Object> updatedResponse = new HashMap<>();
            updateRequest.getRequestDataKeys().stream().forEach(key -> {
                Object value = context.read("$." + key, Object.class);
                updatedResponse.put(key, value);
            });

            return new SupplementaryData(updatedResponse);
        }
        return new SupplementaryData(responseNode);
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName() + " Not Supported"));
    }
}

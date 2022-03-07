package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
@Qualifier("default")
@Singleton
@Transactional
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSupplementaryDataRepository.class);

    @PersistenceContext
    private EntityManager em;

    private List<SupplementaryDataQueryBuilder> queryBuilders;

    @Autowired
    public DefaultSupplementaryDataRepository(final List<SupplementaryDataQueryBuilder> queryBuilders) {
        this.queryBuilders = queryBuilders;
    }

    @Override
    public void setSupplementaryData(final String caseReference,
                                     String fieldPath,
                                     Object fieldValue) {
        LOG.info("In setSupplementaryData caseReference {}, fieldPath {}, fieldValue {}", caseReference, fieldPath,
            fieldValue);
        Query query = queryBuilder(SupplementaryDataOperation.SET).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        query.executeUpdate();
    }

    @Override
    public void incrementSupplementaryData(final String caseReference,
                                           String fieldPath,
                                           Object fieldValue) {
        LOG.info("In incrementSupplementaryData caseReference {}, fieldPath {}, fieldValue {}, supplementary data {}",
            caseReference, fieldPath, fieldValue, findSupplementaryData(caseReference));

        Query query = queryBuilder(SupplementaryDataOperation.INC).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        int count = query.executeUpdate();

        LOG.info("Number of records updated {}, supplementary data {} ", count, findSupplementaryData(caseReference));
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference, Set<String> requestedProperties) {
        JsonNode responseNode = findSupplementaryDataNode(caseReference);
        return new SupplementaryData(responseNode, requestedProperties);
    }

    @Override
    public String findSupplementaryData(final String caseReference) {
        JsonNode responseNode = findSupplementaryDataNode(caseReference);
        return jsonNodeToString(responseNode);
    }

    @Override
    public Map<String, JsonNode> findSupplementaryDataMap(final String caseReference) {
        JsonNode responseNode = findSupplementaryDataNode(caseReference);
        return JacksonUtils.convertValue(responseNode);
    }

    public JsonNode findSupplementaryDataNode(final String caseReference) {
        Query query = queryBuilder(SupplementaryDataOperation.FIND).build(em,
            caseReference,
            null,
            null);
        return (JsonNode) query.getSingleResult();
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName()
                + " Not Supported"));
    }

    private String jsonNodeToString(Object data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Unable to map object to JSON string", e);
        }
    }
}

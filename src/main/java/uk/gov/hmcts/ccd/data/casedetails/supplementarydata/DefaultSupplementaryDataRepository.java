package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
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
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

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
        LOG.error(String.format("Case reference => %s, Set supplementary data", caseReference));
        Query query = queryBuilder(SupplementaryDataOperation.SET).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        LOG.error(String.format("Case reference => %s, Executing set query", caseReference));
        query.executeUpdate();
    }

    @Override
    public void incrementSupplementaryData(final String caseReference,
                                           String fieldPath,
                                           Object fieldValue) {
        LOG.error(String.format("Case reference => %s, Insert supplementary data", caseReference));
        Query query = queryBuilder(SupplementaryDataOperation.INC).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        LOG.error(String.format("Case reference => %s, Executing update query", caseReference));
        query.executeUpdate();
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference, Set<String> requestedProperties) {
        LOG.error(String.format("Case reference => %s, Find supplementary data", caseReference));
        Query query = queryBuilder(SupplementaryDataOperation.FIND).build(em,
            caseReference,
            null,
            null);
        JsonNode responseNode = (JsonNode) query.getSingleResult();
        ObjectMapper mapper = new ObjectMapper();
        try {
            LOG.error(String.format("Case reference => %s, supplementary data => %s", caseReference,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseNode)));
        } catch (JsonProcessingException jpe) {
            LOG.error(jpe.getMessage());
        }
        return new SupplementaryData(caseReference, responseNode, requestedProperties);
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName()
                + " Not Supported"));
    }
}

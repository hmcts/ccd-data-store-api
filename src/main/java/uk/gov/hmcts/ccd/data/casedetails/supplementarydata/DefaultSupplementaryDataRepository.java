package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.ccd.config.JacksonUtils;
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
    public void setSupplementaryData(final String caseReference, final Map<String, Object> supplementaryData) {
        LOG.debug("Set supplementary data");
        List<Query> queryList = getQueryBuilder(Operation.SET).buildQueries(em, caseReference, supplementaryData);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public void incrementSupplementaryData(final String caseReference, final Map<String, Object> supplementaryData) {
        LOG.debug("Insert supplementary data");
        List<Query> queryList = getQueryBuilder(Operation.INC).buildQueries(em, caseReference, supplementaryData);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference) {
        LOG.debug("Find supplementary data");
        List<Query> queryList = getQueryBuilder(Operation.FIND).buildQueries(em, caseReference, null);
        JsonNode result = (JsonNode) queryList.get(0).getSingleResult();
        return new SupplementaryData(JacksonUtils.convertJsonNode(result));
    }

    private SupplementaryDataQueryBuilder getQueryBuilder(final Operation operation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == operation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + operation.getOperationName() + " Not Supported"));
    }
}

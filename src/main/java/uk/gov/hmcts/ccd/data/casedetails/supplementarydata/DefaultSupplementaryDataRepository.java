package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
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
        LOG.debug("Set supplementary data");
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
        LOG.debug("Insert supplementary data");
        Query query = queryBuilder(SupplementaryDataOperation.INC).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        query.executeUpdate();
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference, Set<String> requestedProperties) {
        LOG.debug("Find supplementary data");
        Query query = queryBuilder(SupplementaryDataOperation.FIND).build(em,
            caseReference,
            null,
            null);
        JsonNode responseNode = (JsonNode) query.getSingleResult();
        return new SupplementaryData(responseNode, requestedProperties);
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName() + " Not Supported"));
    }
}

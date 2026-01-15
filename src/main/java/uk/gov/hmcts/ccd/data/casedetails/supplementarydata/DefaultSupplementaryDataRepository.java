package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
        Query query = queryBuilder(SupplementaryDataOperation.INC).build(em,
            caseReference,
            fieldPath,
            fieldValue);
        query.executeUpdate();
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference, Set<String> requestedProperties) {
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
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName()
                + " Not Supported"));
    }
}

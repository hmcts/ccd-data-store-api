package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Qualifier("default")
@Singleton
@Transactional
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    @PersistenceContext
    private EntityManager em;

    private List<SupplementaryDataQueryBuilder> queryBuilders;

    private FindCasesWithSupplementaryDataQueryBuilder findCasesWithSupplementaryDataQueryBuilder;

    @Autowired
    public DefaultSupplementaryDataRepository(final List<SupplementaryDataQueryBuilder> queryBuilders,
                                              final FindCasesWithSupplementaryDataQueryBuilder findCasesQueryBuilder) {
        this.queryBuilders = queryBuilders;
        this.findCasesWithSupplementaryDataQueryBuilder = findCasesQueryBuilder;
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

    @Override
    public List<String> findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(String caseType, LocalDateTime from,
                                                                                           Optional<LocalDateTime> to,
                                                                                           Integer limit) {
        Query query = findCasesWithSupplementaryDataQueryBuilder.build(em, caseType, from, to, limit);
        return query.getResultList();
    }


    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName()
                + " Not Supported"));
    }
}

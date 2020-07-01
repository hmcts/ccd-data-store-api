package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.List;
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
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

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
    public void setSupplementaryData(final String caseReference, final SupplementaryDataUpdateRequest updateRequest) {
        LOG.debug("Set supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.SET).buildQueryForEachSupplementaryDataProperty(em, caseReference, updateRequest);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public void incrementSupplementaryData(final String caseReference,SupplementaryDataUpdateRequest updateRequest) {
        LOG.debug("Insert supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.INC).buildQueryForEachSupplementaryDataProperty(em, caseReference, updateRequest);
        queryList.stream().forEach(query -> query.executeUpdate());
    }

    @Override
    public SupplementaryData findSupplementaryData(final String caseReference) {
        LOG.debug("Find supplementary data");
        List<Query> queryList = queryBuilder(SupplementaryDataOperation.FIND).buildQueryForEachSupplementaryDataProperty(em, caseReference, null);
        return new SupplementaryData(JacksonUtils.convertJsonNode(queryList.get(0).getSingleResult()));
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName() + " Not Supported"));
    }
}

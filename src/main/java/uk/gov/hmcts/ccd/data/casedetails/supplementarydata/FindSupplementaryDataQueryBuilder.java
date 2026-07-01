package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("find")
public class FindSupplementaryDataQueryBuilder implements SupplementaryDataQueryBuilder {

    private static final String SUPPLEMENTARY_DATA_QUERY = "SELECT cd.supplementary_data FROM case_data cd WHERE "
                                                         + "cd.reference = :reference";

    @Override
    public Query build(EntityManager entityManager,
                       String caseReference,
                       String fieldPath,
                       Object fieldValue) {
        Query selectQuery = entityManager.createNativeQuery(SUPPLEMENTARY_DATA_QUERY);
        selectQuery.setParameter("reference", caseReference);
        selectQuery.unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("supplementary_data", SupplementaryDataUserType.CUSTOM_TYPE);
        return selectQuery;
    }

    @Override
    public SupplementaryDataOperation operationType() {
        return SupplementaryDataOperation.FIND;
    }
}

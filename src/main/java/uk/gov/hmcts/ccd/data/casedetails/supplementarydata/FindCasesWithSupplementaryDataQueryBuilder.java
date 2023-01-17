package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;

@Component
@Qualifier("findcases")
public class FindCasesWithSupplementaryDataQueryBuilder {
    private static final String CASES_DATA_QUERY = "SELECT cd.reference FROM case_data cd WHERE "
        + "jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'HMCTSServiceId') IS NOT NULL "
        + "AND jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'orgs_assigned_users') IS NULL";

    public Query build(EntityManager entityManager) {
        javax.persistence.Query selectQuery = entityManager.createNativeQuery(CASES_DATA_QUERY);
        selectQuery.unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("reference", StandardBasicTypes.STRING);
        return selectQuery;
    }

}

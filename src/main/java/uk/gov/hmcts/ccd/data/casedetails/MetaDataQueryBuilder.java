package uk.gov.hmcts.ccd.data.casedetails;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Named
@Singleton
public class MetaDataQueryBuilder {

    @PersistenceContext
    private EntityManager em;

    public  Query  build(final String jurisdiction,final String caseType,String state) {
        final Query query = em.createNamedQuery(CaseDetailsEntity.FIND_BY_METADATA);
        query.setParameter(CaseDetailsEntity.JURISDICTION_ID_PARAM, jurisdiction == null ? "%" : jurisdiction);
        query.setParameter(CaseDetailsEntity.CASE_TYPE_PARAM, caseType == null ? "%" : caseType);
        query.setParameter(CaseDetailsEntity.STATE_PARAM, state == null ? "%" : state);
        return query;
    }
}

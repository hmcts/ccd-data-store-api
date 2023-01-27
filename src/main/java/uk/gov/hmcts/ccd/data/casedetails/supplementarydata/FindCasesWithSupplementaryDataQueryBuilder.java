package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class FindCasesWithSupplementaryDataQueryBuilder {
    private static final StringBuilder ORG_NODE_DATA = new StringBuilder();
    private static final String CASES_DATA_QUERY = "SELECT cd.reference, supplementary_data, case_type_id, jurisdiction,"
        + " concat (" + ORG_NODE_DATA + ")"
        + " FROM case_data cd WHERE "
        + "jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'HMCTSServiceId') IS NOT NULL "
        + "AND jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'orgs_assigned_users') IS NULL "
        + "AND (cd.last_modified BETWEEN :date_from AND :date_to) ORDER BY last_modified "
        + "AND CASE_TYPE_ID = :case_type_id "
        + " LIMIT :limit";

    private static final String CASE_TYPES_QUERY = "SELECT CF.REFERENCE FROM CASE_FIELD CF, CASE_TYPE CT WHERE "
        + "CASE_TYPE_ID IN (SELECT MAX(ID) FROM CASE_TYPE WHERE UPPER(REFERENCE) IN (:case_type_id) "
        + "GROUP BY REFERENCE) AND CF.FIELD_TYPE_ID = ("
        + "SELECT ID FROM FIELD_TYPE WHERE UPPER(REFERENCE) = 'ORGANISATIONPOLICY') "
        + "AND CF.CASE_TYPE_ID = CT.ID";


    public Query build(EntityManager entityManager, String caseType, LocalDateTime from, Optional<LocalDateTime> to, Integer limit) {
        List<String> orgNodes = findOrgNodes(entityManager, caseType);
        ORG_NODE_DATA.append( this.generateOrgNodeIdString(orgNodes));
        Query selectQuery;
        selectQuery = entityManager.createNativeQuery(CASES_DATA_QUERY);
        selectQuery.setParameter("date_from", Timestamp.valueOf(from), TemporalType.TIMESTAMP);
        LocalDateTime dateTo = to.isEmpty() ? LocalDateTime.now() : to.get();
        selectQuery.setParameter("date_to", Timestamp.valueOf(dateTo), TemporalType.TIMESTAMP);
        selectQuery.setParameter("limit", limit);
        selectQuery.setParameter("case_type_id", caseType);


        selectQuery.unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("reference", StandardBasicTypes.STRING);
        return selectQuery;
    }

    private String generateOrgNodeIdString(List<String> orgNodes) {
        StringBuilder orgNodeStr = new StringBuilder();
          orgNodes.forEach(orgNode ->
              orgNodeStr.append( "'"+  "jsonb_extract_path_text(COALESCE(data, '{}'), '"+ orgNode +"')" + "'|"));
          return orgNodeStr.toString();
    }

    public List<String> findOrgNodes(EntityManager entityManager, String caseType) {
        Query selectQuery;
      /*  selectQuery = entityManager.createNativeQuery(CASE_TYPES_QUERY);
        selectQuery.setParameter("case_type_id", caseType);


        selectQuery.unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("reference", StandardBasicTypes.STRING);

        return selectQuery.getResultList();*/

        return Arrays.asList("respondent2OrganisationPolicy",
            "respondent1OrganisationPolicy",
            "applicant2OrganisationPolicy",
            "applicant1OrganisationPolicy");

    }

}

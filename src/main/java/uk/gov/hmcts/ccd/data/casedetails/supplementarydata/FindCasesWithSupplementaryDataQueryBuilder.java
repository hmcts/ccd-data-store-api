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
        + " concat ('''',  jsonb_extract_path_text(data, 'applicant1OrganisationPolicy', 'Organisation', 'OrganisationID'), '''', '|',"
        + "'''',  jsonb_extract_path_text(data, 'applicant2OrganisationPolicy', 'Organisation', 'OrganisationID'), '''', '|',"
        + "'''', jsonb_extract_path_text(data, 'respondent1OrganisationPolicy', 'Organisation', 'OrganisationID'), '''', '|',"
        + " '''', jsonb_extract_path_text(data, 'respondent2OrganisationPolicy', 'Organisation', 'OrganisationID'), '''' )"
        + " FROM case_data cd WHERE "
        + "jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'HMCTSServiceId') IS NOT NULL "
        + "AND jsonb_extract_path_text(COALESCE(supplementary_data, '{}'), 'orgs_assigned_users') IS NULL "
        + "AND (cd.last_modified BETWEEN :date_from AND :date_to) "
        + "AND CASE_TYPE_ID = :case_type_id ORDER BY last_modified "
        + " LIMIT :limit";

    public Query build(EntityManager entityManager, String caseType, LocalDateTime from, Optional<LocalDateTime> to, Integer limit) {
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



}

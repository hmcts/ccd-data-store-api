package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import static uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeSqlQueryBuilder.AND_NOT;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeSqlQueryBuilder.EMPTY;
import static uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeSqlQueryBuilder.OR;

@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class AccessControlGrantTypeQueryBuilder {

    private static final String QUERY = "( %s )";
    private static final String FINAL_QUERY = " AND ( %s )";

    private final BasicGrantTypeQueryBuilder basicGrantTypeQueryBuilder;
    private final SpecificGrantTypeQueryBuilder specificGrantTypeQueryBuilder;
    private final StandardGrantTypeQueryBuilder standardGrantTypeQueryBuilder;
    private final ChallengedGrantTypeQueryBuilder challengedGrantTypeQueryBuilder;
    private final ExcludedGrantTypeQueryBuilder excludedGrantTypeQueryBuilder;

    @Autowired
    public AccessControlGrantTypeQueryBuilder(BasicGrantTypeQueryBuilder basicGrantTypeQueryBuilder,
                                              SpecificGrantTypeQueryBuilder specificGrantTypeQueryBuilder,
                                              StandardGrantTypeQueryBuilder standardGrantTypeQueryBuilder,
                                              ChallengedGrantTypeQueryBuilder challengedGrantTypeQueryBuilder,
                                              ExcludedGrantTypeQueryBuilder excludedGrantTypeQueryBuilder) {
        this.basicGrantTypeQueryBuilder = basicGrantTypeQueryBuilder;
        this.specificGrantTypeQueryBuilder = specificGrantTypeQueryBuilder;
        this.standardGrantTypeQueryBuilder = standardGrantTypeQueryBuilder;
        this.challengedGrantTypeQueryBuilder = challengedGrantTypeQueryBuilder;
        this.excludedGrantTypeQueryBuilder = excludedGrantTypeQueryBuilder;
    }

    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              CaseTypeDefinition caseTypeDefinition) {
        String basicQuery = basicGrantTypeQueryBuilder
            .createQuery(roleAssignments, params, caseTypeDefinition);

        String specificQuery = specificGrantTypeQueryBuilder
            .createQuery(roleAssignments, params, caseTypeDefinition);

        String standardQuery = standardGrantTypeQueryBuilder
            .createQuery(roleAssignments, params, caseTypeDefinition);

        String challengedQuery = challengedGrantTypeQueryBuilder
            .createQuery(roleAssignments, params, caseTypeDefinition);

        String orgQuery = mergeQuery(standardQuery, challengedQuery, OR);
        String excludedQuery = excludedGrantTypeQueryBuilder
            .createQuery(roleAssignments, params, caseTypeDefinition);

        String nonOrgQuery = mergeQuery(basicQuery, specificQuery, OR);

        if (StringUtils.isBlank(nonOrgQuery)
            && StringUtils.isBlank(orgQuery)
            && StringUtils.isNotBlank(excludedQuery)) {
            return AND_NOT + excludedQuery;
        }

        if (StringUtils.isBlank(nonOrgQuery)
            && StringUtils.isNotBlank(orgQuery)
            && StringUtils.isBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, orgQuery);
        }

        if (StringUtils.isNotBlank(nonOrgQuery)
            && StringUtils.isBlank(orgQuery)
            && StringUtils.isBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, nonOrgQuery);
        }

        if (StringUtils.isBlank(nonOrgQuery)
            && StringUtils.isNotBlank(orgQuery)
            && StringUtils.isNotBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, orgQuery
                + getOperator(orgQuery, AND_NOT)
                + excludedQuery);
        }

        if (StringUtils.isNotBlank(nonOrgQuery)
            && StringUtils.isBlank(orgQuery)
            && StringUtils.isNotBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, nonOrgQuery
                + getOperator(nonOrgQuery, AND_NOT)
                + excludedQuery);
        }


        if (StringUtils.isNotBlank(nonOrgQuery)
            && StringUtils.isNotBlank(orgQuery)
            && StringUtils.isBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, nonOrgQuery
                + getOperator(nonOrgQuery, OR)
                + orgQuery);
        }

        if (StringUtils.isNotBlank(nonOrgQuery)
            && StringUtils.isNotBlank(orgQuery)
            && StringUtils.isNotBlank(excludedQuery)) {
            return String.format(FINAL_QUERY, nonOrgQuery
                + OR + String.format(QUERY, orgQuery + AND_NOT + excludedQuery));
        }

        return EMPTY;
    }

    private String mergeQuery(String queryOne,
                              String queryTwo,
                              String operator) {
        String tmpQuery = queryOne;

        if (StringUtils.isNotBlank(queryTwo)) {
            return String.format(QUERY, tmpQuery + getOperator(tmpQuery, operator) + queryTwo);
        }
        return tmpQuery;
    }

    private String getOperator(String query, String operator) {
        if (StringUtils.isNotBlank(query)) {
            return operator;
        }
        return "";
    }
}

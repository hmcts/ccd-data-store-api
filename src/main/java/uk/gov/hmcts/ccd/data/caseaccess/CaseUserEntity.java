package uk.gov.hmcts.ccd.data.caseaccess;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "case_users")
@NamedQueries({
    @NamedQuery(name = CaseUserEntity.GET_ALL_CASES_USER_HAS_ACCESS_TO,
        query = "SELECT casePrimaryKey.caseDataId from CaseUserEntity where casePrimaryKey.userId = :userId"),
    @NamedQuery(name = CaseUserEntity.GET_ALL_CASE_ROLES_USER_HAS_ACCESS_FOR_A_CASE,
        query = "SELECT casePrimaryKey.caseRole from CaseUserEntity where casePrimaryKey.userId = :userId and casePrimaryKey.caseDataId = :caseDataId")
})
public class CaseUserEntity implements Serializable {

    protected static final String GET_ALL_CASES_USER_HAS_ACCESS_TO = "GET_ALL_CASES";
    protected static final String GET_ALL_CASE_ROLES_USER_HAS_ACCESS_FOR_A_CASE = "GET_ALL_CASES_ROLES_FOR_A_CASE";

    public static class CasePrimaryKey implements Serializable {
        @Column(name = "case_data_id")
        private Long caseDataId;
        @Column(name = "user_id")
        private String userId;
        @Column(name = "case_role")
        private String caseRole;

        public Long getCaseDataId() {
            return caseDataId;
        }

        public void setCaseDataId(Long caseDataId) {
            this.caseDataId = caseDataId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getCaseRole() {
            return caseRole;
        }

        public void setCaseRole(String caseRole) {
            this.caseRole = caseRole;
        }
    }

    @EmbeddedId
    private CasePrimaryKey casePrimaryKey;

    public CaseUserEntity() {
        // needed for hibernate
    }

    CaseUserEntity(Long caseDataId, String userId) {
        this(caseDataId, userId, GlobalCaseRole.CREATOR.getRole());
    }

    CaseUserEntity(Long caseDataId, String userId, String caseRole) {
        CasePrimaryKey casePrimaryKey = new CasePrimaryKey();
        casePrimaryKey.caseDataId = caseDataId;
        casePrimaryKey.userId = userId;
        casePrimaryKey.caseRole = caseRole;

        this.casePrimaryKey = casePrimaryKey;
    }

    public CasePrimaryKey getCasePrimaryKey() {
        return casePrimaryKey;
    }

    public void setCasePrimaryKey(CasePrimaryKey casePrimaryKey) {
        this.casePrimaryKey = casePrimaryKey;
    }

}

package uk.gov.hmcts.ccd.data.caseaccess;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "case_users")
@NamedQueries({@NamedQuery(name = CaseUserEntity.GET_ALL_CASES_USER_HAS_ACCESS_TO,
    query = "SELECT casePrimaryKey.caseDataId from CaseUserEntity where casePrimaryKey.userId = :userId")})
public class CaseUserEntity implements Serializable {

    static final String GET_ALL_CASES_USER_HAS_ACCESS_TO = "GET_ALL_CASES";

    public static class CasePrimaryKey implements Serializable {
        @Column(name = "case_data_id")
        private Long caseDataId;
        @Column(name = "user_id")
        private String userId;

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
    }

    @EmbeddedId
    private CasePrimaryKey casePrimaryKey;

    public CaseUserEntity() {
        // needed for hibernate
    }

    CaseUserEntity(Long caseDataId, String userId) {
        CasePrimaryKey casePrimaryKey = new CasePrimaryKey();
        casePrimaryKey.caseDataId = caseDataId;
        casePrimaryKey.userId = userId;

        this.casePrimaryKey = casePrimaryKey;
    }

    public CasePrimaryKey getCasePrimaryKey() {
        return casePrimaryKey;
    }

    public void setCasePrimaryKey(CasePrimaryKey casePrimaryKey) {
        this.casePrimaryKey = casePrimaryKey;
    }

}

package uk.gov.hmcts.ccd.data.caseaccess;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

@Entity
@Table(name = "case_users_audit")
public class CaseUserAuditEntity {

    public enum Action {
        GRANT, REVOKE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
        generator = "case_users_audit_id_seq_generator")
    @SequenceGenerator(name = "case_users_audit_id_seq_generator", 
        sequenceName = "case_users_audit_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "case_data_id")
    private long caseDataId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "case_role")
    private String caseRole;
    @Column(name = "changed_by_id")
    private String changedById;
    @Column(name = "changed_at")
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime changedAt;
    @Enumerated(EnumType.STRING)
    private Action action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getCaseDataId() {
        return caseDataId;
    }

    public void setCaseDataId(long caseDataId) {
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

    public String getChangedById() {
        return changedById;
    }

    public void setChangedById(String changedById) {
        this.changedById = changedById;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}

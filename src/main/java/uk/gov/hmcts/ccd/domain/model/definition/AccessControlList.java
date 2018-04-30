package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.ToString;

import java.io.Serializable;

@ToString
public class AccessControlList implements Serializable {

    private String role;
    private boolean create;
    private boolean read;
    private boolean update;
    private boolean delete;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

}

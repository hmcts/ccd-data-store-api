package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ComplexACL extends AccessControlList {
    private String listElementCode;

    public String getListElementCode() {
        return listElementCode;
    }

    public void setListElementCode(String listElementCode) {
        this.listElementCode = listElementCode;
    }

    @JsonIgnore
    public ComplexACL deepCopy() {
        ComplexACL copy = new ComplexACL();
        copy.setListElementCode(this.getListElementCode());
        copy.setAccessProfile(this.getAccessProfile());
        copy.setCreate(this.isCreate());
        copy.setRead(this.isRead());
        copy.setUpdate(this.isUpdate());
        copy.setDelete(this.isDelete());
        return copy;
    }
}

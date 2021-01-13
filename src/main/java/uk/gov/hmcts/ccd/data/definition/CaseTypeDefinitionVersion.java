package uk.gov.hmcts.ccd.data.definition;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class CaseTypeDefinitionVersion implements Serializable {

    private static final long serialVersionUID = 3792842101045258030L;

    private Integer version;

    public CaseTypeDefinitionVersion() {
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("version", version)
            .toString();
    }
}

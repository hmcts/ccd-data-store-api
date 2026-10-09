package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class ComplexACL extends AccessControlList {
    private final String listElementCode;

    @JsonCreator
    public ComplexACL(
        @JsonProperty("accessProfile") @JsonAlias("role") String accessProfile,
        @JsonProperty("create") boolean create,
        @JsonProperty("read") boolean read,
        @JsonProperty("update") boolean update,
        @JsonProperty("delete") boolean delete,
        @JsonProperty("listElementCode") String listElementCode
    ) {
        super(accessProfile, create, read, update, delete);
        this.listElementCode = listElementCode;
    }

    public String getListElementCode() {
        return listElementCode;
    }

    @Override
    public String toString() {
        return super.toString() + ", listElementCode='" + listElementCode + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        if (!(o instanceof ComplexACL that)) {
            return false;
        }

        return Objects.equals(listElementCode, that.listElementCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), listElementCode);
    }
}

package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;

import java.io.Serializable;
import java.util.Objects;

public class AccessControlList implements Serializable {

    private final String accessProfile;
    private final boolean create;
    private final boolean read;
    private final boolean update;
    private final boolean delete;

    @JsonCreator
    @Builder
    public AccessControlList(
        @JsonProperty("accessProfile") @JsonAlias("role") String accessProfile,
        @JsonProperty("create") boolean create,
        @JsonProperty("read") boolean read,
        @JsonProperty("update") boolean update,
        @JsonProperty("delete") boolean delete
    ) {
        this.accessProfile = accessProfile;
        this.create = create;
        this.read = read;
        this.update = update;
        this.delete = delete;
    }

    @JsonGetter("role")
    public String getAccessProfile() {
        return accessProfile;
    }

    public boolean isCreate() {
        return create;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isUpdate() {
        return update;
    }

    public boolean isDelete() {
        return delete;
    }

    @Override
    public String toString() {
        return "ACL{"
            + "accessProfile='" + accessProfile + '\''
            + ", crud=" + (isCreate() ? "C" : "") + (isRead() ? "R" : "")
            + (isUpdate() ? "U" : "") + (isDelete() ? "D" : "")
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        // Exact-class comparison, not instanceof: ComplexACL carries an extra listElementCode, so an instanceof
        // check would make plainAcl.equals(complexAcl) true while complexAcl.equals(plainAcl) is false. That breaks
        // the symmetry contract, and List.remove(Object) - used on ACL lists in CaseFieldDefinition - relies on it.
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AccessControlList that = (AccessControlList) o;
        return create == that.create
            && read == that.read
            && update == that.update
            && delete == that.delete
            && Objects.equals(accessProfile, that.accessProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessProfile, create, read, update, delete);
    }

    @JsonIgnore
    public AccessControlList duplicate() {
        return AccessControlList.builder()
            .accessProfile(this.accessProfile)
            .create(this.create)
            .read(this.read)
            .update(this.update)
            .delete(this.delete)
            .build();
    }
}

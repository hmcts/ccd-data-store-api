package uk.gov.hmcts.ccd.data.casedetails.search;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

public class MetaData {

    public static final String JURISDICTION_METADATA = "jurisdiction";
    public static final String CASE_TYPE_METADATA = "case_type";
    public static final String STATE_METADATA = "state";
    public static final String CASE_REFERENCE_METADATA = "case_reference";
    public static final String CREATED_DATE_METADATA = "created_date";
    public static final String LAST_MODIFIED_METADATA = "last_modified_date";
    public static final String SECURITY_CLASSIFICATION_METADATA = "security_classification";
    public static final String PAGE_METADATA = "page";
    public static final String SORT_DIRECTION_METADATA = "sortDirection";
    private static final List<String> METADATA_QUERY_PARAMETERS = newArrayList(STATE_METADATA, CASE_REFERENCE_METADATA,
                                                                               CREATED_DATE_METADATA,
                                                                               LAST_MODIFIED_METADATA,
                                                                               SECURITY_CLASSIFICATION_METADATA,
                                                                               PAGE_METADATA, SORT_DIRECTION_METADATA);

    private final String caseTypeId;
    private final String jurisdiction;
    private Optional<String> state = Optional.empty();
    private Optional<String> caseReference = Optional.empty();
    private Optional<String> createdDate = Optional.empty();
    private Optional<String> lastModified = Optional.empty();
    private Optional<String> securityClassification = Optional.empty();
    private Optional<String> page = Optional.empty();
    private Optional<String> sortDirection = Optional.empty();

    public MetaData(String caseTypeId, String jurisdiction) {
        this.caseTypeId = caseTypeId;
        this.jurisdiction = jurisdiction;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public Optional<String> getState() {
        return state;
    }

    public void setCaseReference(Optional<String> caseReference) {
        this.caseReference = caseReference;
    }

    public Optional<String> getCaseReference() {
        return caseReference;
    }

    public void setState(Optional<String> state) {
        this.state = state;
    }

    public Optional<String> getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Optional<String> createdDate) {
        this.createdDate = createdDate;
    }

    public void setLastModified(Optional<String> lastModified) {
        this.lastModified = lastModified;
    }

    public Optional<String> getLastModified() {
        return lastModified;
    }

    public void setSecurityClassification(Optional<String> securityClassification) {
        this.securityClassification = securityClassification.map(this::toTrimmedLowerCase);
    }

    public Optional<String> getSecurityClassification() {
        return securityClassification;
    }

    public Optional<String> getPage() {
        return page;
    }

    public void setPage(Optional<String> page) {
        this.page = page;
    }

    public Optional<String> getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(Optional<String> sortDirection) {
        this.sortDirection = sortDirection;
    }

    public static List<String> unknownMetadata(List<String> parameters) {
        return parameters.stream().filter(p -> !METADATA_QUERY_PARAMETERS.contains(p)).collect(toList());
    }

    private String toTrimmedLowerCase(String s) {
        return s.trim().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaData metaData = (MetaData) o;
        return Objects.equals(caseTypeId, metaData.caseTypeId) &&
            Objects.equals(jurisdiction, metaData.jurisdiction) &&
            Objects.equals(state, metaData.state) &&
            Objects.equals(caseReference, metaData.caseReference) &&
            Objects.equals(createdDate, metaData.createdDate) &&
            Objects.equals(lastModified, metaData.lastModified) &&
            Objects.equals(securityClassification, metaData.securityClassification) &&
            Objects.equals(page, metaData.page) &&
            Objects.equals(sortDirection, metaData.sortDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseTypeId,
                            jurisdiction,
                            state,
                            caseReference,
                            createdDate,
                            lastModified,
                            securityClassification,
                            page,
                            sortDirection);
    }
}

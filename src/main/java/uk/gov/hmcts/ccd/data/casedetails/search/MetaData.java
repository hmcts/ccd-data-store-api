package uk.gov.hmcts.ccd.data.casedetails.search;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;

public class MetaData {

    public static final String PAGE_PARAM = "page";
    public static final String SORT_PARAM = "sortDirection";
    private static final List<String> METADATA_QUERY_PARAMETERS = newArrayList(STATE.getParameterName(),
                                                                               CASE_REFERENCE.getParameterName(),
                                                                               CREATED_DATE.getParameterName(),
                                                                               LAST_MODIFIED_DATE.getParameterName(),
                                                                               SECURITY_CLASSIFICATION.getParameterName(),
                                                                               PAGE_PARAM, SORT_PARAM);

    // Metadata case fields
    public enum CaseField {
        JURISDICTION("jurisdiction"),
        CASE_TYPE("case_type"),
        STATE("state"),
        CASE_REFERENCE("case_reference"),
        CREATED_DATE("created_date"),
        LAST_MODIFIED_DATE("last_modified_date"),
        SECURITY_CLASSIFICATION("security_classification");

        private final String parameterName;

        CaseField(String parameterName) {
            this.parameterName = parameterName;
        }

        public String getParameterName() {
            return parameterName;
        }

        public String getReference() {
            return String.join(getParameterName().toUpperCase(), "[", "]");
        }
    }

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

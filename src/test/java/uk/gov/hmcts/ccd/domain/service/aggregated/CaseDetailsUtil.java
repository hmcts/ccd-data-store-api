package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;

import java.time.LocalDateTime;
import java.util.Map;

class CaseDetailsUtil extends CaseDetails {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    public static class CaseDetailsBuilder {
        private final CaseDetails caseDetails;

        private CaseDetailsBuilder() {
            this.caseDetails = new CaseDetails();
        }

        static CaseDetailsBuilder caseDetails() {
            return new CaseDetailsBuilder();
        }

        public CaseDetailsBuilder withReference(Long reference) {
            caseDetails.setReference(reference);
            return this;
        }

        public CaseDetailsBuilder withCaseTypeId(String caseTypeId) {
            caseDetails.setCaseTypeId(caseTypeId);
            return this;
        }

        public CaseDetailsBuilder withState(String state) {
            caseDetails.setState(state);
            return this;
        }

        public CaseDetailsBuilder withJurisdiction(String jurisdiction) {
            caseDetails.setJurisdiction(jurisdiction);
            return this;
        }

        public CaseDetailsBuilder withData(Map<String, JsonNode> data) {
            caseDetails.setData(data);
            return this;
        }

        public CaseDetailsBuilder withLastStateModified(LocalDateTime date) {
            caseDetails.setLastStateModifiedDate(date);
            return this;
        }

        public CaseDetailsBuilder withLastModified(LocalDateTime date) {
            caseDetails.setLastModified(date);
            return this;
        }

        public CaseDetailsBuilder withCreated(LocalDateTime date) {
            caseDetails.setCreatedDate(date);
            return this;
        }

        public CaseDetailsBuilder withSecurityClassification(SecurityClassification securityClassification) {
            caseDetails.setSecurityClassification(securityClassification);
            return this;
        }

        public CaseDetails build() {
            return caseDetails;
        }
    }
//
//    static SearchResultField buildSearchResultField(String caseTypedId,
//                                                    String caseFieldId,
//                                                    String caseFieldPath,
//                                                    String label,
//                                                    String displayContextParameter) {
//        SearchResultField searchResultField = new SearchResultField();
//        searchResultField.setCaseFieldId(caseFieldId);
//        searchResultField.setCaseFieldPath(caseFieldPath);
//        searchResultField.setCaseTypeId(caseTypedId);
//        searchResultField.setLabel(label);
//        searchResultField.setDisplayOrder(1);
//        searchResultField.setDisplayContextParameter(displayContextParameter);
//        return searchResultField;
//    }
//
}

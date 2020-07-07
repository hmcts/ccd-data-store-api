package uk.gov.hmcts.ccd.datastore.tests.fixture;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition of case type AUTOTEST1/AAT as code.
 */
public interface AATCaseType {
    String JURISDICTION = "AUTOTEST1";
    String JURISDICTION_AUTOTEST2 = "AUTOTEST2";
    String CASE_TYPE = "AAT";
    String AAT_PRIVATE_CASE_TYPE = "AAT_PRIVATE";
    String AAT_PRIVATE2_CASE_TYPE = "AAT_PRIVATE2";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class CaseData {
        @JsonProperty("TextField")
        private String textField;

        @JsonProperty("NumberField")
        private String numberField;

        @JsonProperty("YesOrNoField")
        private String yesOrNoField;

        @JsonProperty("PhoneUKField")
        private String phoneUKField;

        @JsonProperty("EmailField")
        private String emailField;

        @JsonProperty("MoneyGBPField")
        private String moneyGBPField;

        @JsonProperty("DateField")
        private String dateField;

        @JsonProperty("DateTimeField")
        private String dateTimeField;

        @JsonProperty("TextAreaField")
        private String textAreaField;

        @JsonProperty("FixedListField")
        private String fixedListField;

        @JsonProperty("MultiSelectListField")
        private String[] multiSelectListField;

        @JsonProperty("CollectionField")
        private CollectionItem[] collectionField;

        @JsonProperty("ComplexField")
        private ComplexType complexField;

        @JsonProperty("AddressUKField")
        private AddressUKField addressUKField;

        @JsonProperty("DocumentField")
        private DocumentField documentField;
    }

    interface State {
        String TODO = "TODO";
        String IN_PROGRESS = "IN_PROGRESS";
        String DONE = "DONE";
    }

    interface Event {
        String CREATE = "CREATE";
        String START_PROGRESS = "START_PROGRESS";
        String STOP_PROGRESS = "STOP_PROGRESS";
        String COMPLETE = "COMPLETE";
        String UPDATE = "UPDATE";
        String REVIEW = "REVIEW";

        static CCDEventBuilder create() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, CREATE);
        }

        static CCDEventBuilder create(String caseType) {
            return new CCDEventBuilder(JURISDICTION, caseType, CREATE);
        }

        static CCDEventBuilder create(String jurisdiction, String caseType) {
            return new CCDEventBuilder(jurisdiction, caseType, CREATE);
        }

        static CCDEventBuilder startProgress(Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, caseReference, START_PROGRESS);
        }

        static CCDEventBuilder startProgress(String caseType, Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, caseType, caseReference, START_PROGRESS);
        }

        static CCDEventBuilder stopProgress(Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, caseReference, STOP_PROGRESS);
        }

        static CCDEventBuilder complete(Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, caseReference, COMPLETE);
        }

        static CCDEventBuilder update(Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, caseReference, UPDATE);
        }

        static CCDEventBuilder review(Long caseReference) {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, caseReference, REVIEW);
        }
    }

    enum Tab {
        FIRST("FirstTab", "First tab", 1),
        SECOND("SecondTab", "Second tab", 2),
        THIRD("ThirdTab", "Third tab", 3),
        FOURTH("HistoryTab", "History Tab", 4);

        public final String id;
        public final String name;
        public final Integer order;

        Tab(String id, String name, Integer order) {
            this.id = id;
            this.name = name;
            this.order = order;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CollectionItem {
        @JsonProperty("id")
        private String id;

        @JsonProperty("value")
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ComplexType {
        @JsonProperty("ComplexTextField")
        private String complexTextField;

        @JsonProperty("ComplexFixedListField")
        private String complexFixedListField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AddressUKField {
        @JsonProperty("AddressLine1")
        private String addressLine1;

        @JsonProperty("AddressLine2")
        private String addressLine2;

        @JsonProperty("AddressLine3")
        private String addressLine3;

        @JsonProperty("PostTown")
        private String postTown;

        @JsonProperty("County")
        private String county;

        @JsonProperty("PostCode")
        private String postCode;

        @JsonProperty("Country")
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class DocumentField {
        @JsonProperty("document_binary_url")
        private String binaryUrl;

        @JsonProperty("document_filename")
        private String filename;

        @JsonProperty("document_url")
        private String url;
    }
}

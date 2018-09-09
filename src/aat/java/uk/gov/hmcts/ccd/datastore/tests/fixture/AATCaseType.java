package uk.gov.hmcts.ccd.datastore.tests.fixture;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Definition of case type AUTOTEST1/AAT as code.
 */
public interface AATCaseType {
    String JURISDICTION = "AUTOTEST1";
    String CASE_TYPE = "AAT";

    @Data
    @Builder
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

        static CCDEventBuilder startProgress() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, START_PROGRESS);
        }

        static CCDEventBuilder stopProgress() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, STOP_PROGRESS);
        }

        static CCDEventBuilder complete() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, COMPLETE);
        }

        static CCDEventBuilder update() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, UPDATE);
        }

        static CCDEventBuilder review() {
            return new CCDEventBuilder(JURISDICTION, CASE_TYPE, REVIEW);
        }
    }

    @Data
    @AllArgsConstructor
    class CollectionItem {
        @JsonProperty("id")
        private String id;

        @JsonProperty("value")
        private String value;
    }

    @Data
    @AllArgsConstructor
    class ComplexType {
        @JsonProperty("ComplexTextField")
        private String complexTextField;

        @JsonProperty("ComplexFixedListField")
        private String complexFixedListField;
    }

    @Data
    @Builder
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
}

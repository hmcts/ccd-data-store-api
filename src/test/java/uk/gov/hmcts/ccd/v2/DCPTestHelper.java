package uk.gov.hmcts.ccd.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DCPTestHelper {

    public static final String TEXT_FIELD = "TextField";
    public static final String DATE_FIELD = "DateField";
    public static final String DATE_TIME_FIELD = "DateTimeField";
    public static final String COLLECTION_FIELD = "CollectionField";
    public static final String NESTED_NUMBER_FIELD = "NestedNumberField";
    public static final String COMPLEX_NESTED_FIELD = "ComplexNestedField";
    public static final String COLLECTION_COMPLEX_DATE_TIME = "CollectionComplexDateTime";
    public static final String COMPLEX_DATE_TIME_FIELD = "ComplexDateTimeField";
    public static final String NESTED_COMPLEX = "NestedComplex";
    public static final String STANDARD_DATE = "StandardDate";
    public static final String STANDARD_DATE_TIME = "StandardDateTime";
    public static final String COMPLEX_FIELD = "ComplexField";

    private DCPTestHelper() {
        // Utility class
    }

    public static String jsonPath(String... elements) {
        return "/" + String.join("/", elements);
    }

    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Object> mapOf(Object object) {
        return (LinkedHashMap<String, Object>) object;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<LinkedHashMap<String, Object>> arrayOf(Object object) {
        return (ArrayList<LinkedHashMap<String, Object>>) object;
    }

    public static String validateContent() {
        return "{\n"
            + "    \"data\": {\n"
            + "      \"TextField\": \"Case 1 Text\",\n"
            + "      \"CollectionComplexDateTime\": [\n"
            + "        {\n"
            + "          \"id\": \"ID\",\n"
            + "          \"value\": {\n"
            + "            \"DateField\": \"07-05-1963\",\n"
            + "            \"DateTimeField\": \"2008-04-02T16:37\",\n"
            + "            \"StandardDate\": \"1999-08-19\",\n"
            + "            \"StandardDateTime\": \"2010-06-17T19:20:00.000\",\n"
            + "            \"NestedComplex\": {\n"
            + "              \"DateField\": \"02-1981\",\n"
            + "              \"DateTimeField\": \"2002-03-04\",\n"
            + "              \"StandardDate\": \"2020-02-19\",\n"
            + "              \"StandardDateTime\": \"2007-07-17T07:07:00.000\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      ]\n"
            + "    },\n"
            + "    \"event\": {\n"
            + "      \"id\": \"UPDATE\",\n"
            + "      \"summary\": \"\",\n"
            + "      \"description\": \"\"\n"
            + "    }\n"
            + "}";
    }

    public static String invalidValidateContent() {
        return "{\n"
            + "    \"data\": {\n"
            + "      \"TextField\": \"Case 1 Text\",\n"
            + "      \"CollectionComplexDateTime\": [\n"
            + "        {\n"
            + "          \"id\": \"ID\",\n"
            + "          \"value\": {\n"
            + "            \"NestedComplex\": {\n"
            + "              \"DateField\": \"2000\"\n"
            + "            }\n"
            + "          }\n"
            + "        }\n"
            + "      ]\n"
            + "    },\n"
            + "    \"event\": {\n"
            + "      \"id\": \"UPDATE\",\n"
            + "      \"summary\": \"\",\n"
            + "      \"description\": \"\"\n"
            + "    }\n"
            + "}";
    }

    public static String createCaseRequestContent(String eventToken) {
        return String.format(
            "{\n"
                + "    \"data\": {\n"
                + "        \"TextField\": \"Case 1 Text\",\n"
                + "        \"DateField\": \"2003\",\n"
                + "        \"DateTimeField\": \"0708\",\n"
                + "        \"ComplexField\": {\n"
                + "            \"ComplexDateTimeField\": \"07-2004\",\n"
                + "            \"ComplexNestedField\": {\n"
                + "                \"NestedNumberField\": \"1234\",\n"
                + "                \"NestedCollectionTextField\": []\n"
                + "            }\n"
                + "        },\n"
                + "        \"CollectionField\": [\n"
                + "            {\n"
                + "                \"id\": null,\n"
                + "                \"value\": \"2017-12-16T18:19:20.000\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"id\": null,\n"
                + "                \"value\": \"2020-12-21T22:23:24.000\"\n"
                + "            }\n"
                + "        ]\n"
                + "    },\n"
                + "    \"event\": {\n"
                + "        \"id\": \"CREATE\",\n"
                + "        \"summary\": \"\",\n"
                + "        \"description\": \"\"\n"
                + "    },\n"
                + "    \"event_token\": \"%s\",\n"
                + "    \"ignore_warning\": false,\n"
                + "    \"draft_id\": null\n"
                + "}", eventToken
        );
    }

    public static String updateEventRequestContent(String eventToken) {
        return String.format(
            "{\n"
                + "    \"data\": {\n"
                + "        \"TextField\": \"Case 1 Text\",\n"
                + "        \"CollectionComplexDateTime\": [\n"
                + "            {\n"
                + "                \"id\": \"1c811aa0-116c-45ad-a315-ecd94801a42f\",\n"
                + "                \"value\": {\n"
                + "                    \"DateField\": \"07-05-1963\",\n"
                + "                    \"DateTimeField\": \"2008-04-02T16:37\",\n"
                + "                    \"StandardDate\": \"1999-08-19\",\n"
                + "                    \"StandardDateTime\": \"2010-06-17T19:20:00.000\",\n"
                + "                    \"NestedComplex\": {\n"
                + "                        \"DateField\": \"02-1981\",\n"
                + "                        \"DateTimeField\": \"2002-03-04\",\n"
                + "                        \"StandardDate\": \"2020-02-19\",\n"
                + "                        \"StandardDateTime\": \"2007-07-17T07:07:00.000\"\n"
                + "                    }\n"
                + "                }\n"
                + "            }\n"
                + "        ],\n"
                + "        \"DateField\": \"2000-10-20\",\n"
                + "        \"DateTimeField\": \"1987-15\",\n"
                + "        \"ComplexField\": {\n"
                + "            \"ComplexDateTimeField\": \"03-2005\",\n"
                + "            \"ComplexNestedField\": {\n"
                + "                \"NestedNumberField\": \"1730\",\n"
                + "                \"NestedCollectionTextField\": []\n"
                + "            }\n"
                + "        },\n"
                + "        \"CollectionField\": [\n"
                + "            {\n"
                + "                \"id\": \"4497a96c-3ab2-4b87-a57f-0ff379fa49fb\",\n"
                + "                \"value\": \"20040506\"\n"
                + "            },\n"
                + "            {\n"
                + "                \"id\": \"7e015142-5806-4446-ad67-c97a3af5ef6d\",\n"
                + "                \"value\": \"20101112\"\n"
                + "            }\n"
                + "        ]\n"
                + "    },\n"
                + "    \"event\": {\n"
                + "        \"id\": \"UPDATE\",\n"
                + "        \"summary\": \"\",\n"
                + "        \"description\": \"\"\n"
                + "    },\n"
                + "    \"event_token\": \"%s\",\n"
                + "    \"ignore_warning\": false\n"
                + "}", eventToken
        );
    }
}

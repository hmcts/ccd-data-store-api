package uk.gov.hmcts.ccd.fta.util;

public class StringUtils {

    public static String getTitleCaseFor(String original) {
        if (original == null || original.length() == 0)
            return original;
        String initial = original.substring(0, 1);
        String rest = original.substring(1);
        return initial.toUpperCase() + rest.toLowerCase();
    }

    public static String firstLetterToUpperCase(String original) {
        if (original == null || original.length() == 0)
            return original;
        String initial = original.substring(0, 1);
        String rest = original.substring(1);
        return initial.toUpperCase() + rest;
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a != null && b != null)
            return a.equalsIgnoreCase(b);
        return a == b;
    }

    public static String extractNumericStringWithPlus(String originalNumber) {
        return "+" + extractNumericStringWithoutPlus(originalNumber);
    }

    public static String extractNumericStringWithoutPlus(String originalNumber) {
        String refinedNumber = originalNumber.replaceAll(" ", "");
        refinedNumber = refinedNumber.replaceAll("\\+", "");
        refinedNumber = refinedNumber.replaceAll("\\'", "");
        refinedNumber = refinedNumber.replaceAll("\\\"", "");
        refinedNumber = refinedNumber.replaceAll("\t", "");
        refinedNumber = refinedNumber.replaceAll("-", "");
        refinedNumber = refinedNumber.replaceAll("_", "");
        refinedNumber = refinedNumber.replaceAll("\\.", "");
        refinedNumber = refinedNumber.replaceAll("\\(", "");
        refinedNumber = refinedNumber.replaceAll("\\)", "");
        return refinedNumber;
    }

}

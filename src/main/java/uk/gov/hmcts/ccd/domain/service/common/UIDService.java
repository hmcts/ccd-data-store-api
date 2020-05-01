package uk.gov.hmcts.ccd.domain.service.common;

import javax.inject.Named;
import javax.inject.Singleton;
import java.security.SecureRandom;

import static java.lang.Character.getNumericValue;

@Named
@Singleton
public class UIDService {

    private SecureRandom random = new SecureRandom();

    /**
     * Generates a random valid UID.
     *
     * @return A randomly generated, valid, UID.
     */
    public String generateUID() {
        String currentTime10OfSeconds = String.valueOf(System.currentTimeMillis()).substring(0, 11);
        StringBuilder builder = new StringBuilder(currentTime10OfSeconds);
        for (int i = 0; i < 4; i++) {
            int digit = random.nextInt(10);
            builder.append(digit);
        }
        // Do the Luhn algorithm to generate the check digit.
        int checkDigit = checkSum(builder.toString(), true);
        builder.append(checkDigit);

        return builder.toString();
    }

    /**
     * Validate a number string using Luhn algorithm.
     *
     * @param numberString number string to process
     * @return
     */
    public boolean validateUID(String numberString) {
        if (numberString == null || numberString.length() != 16) {
            return false;
        }
        try {
            Long.parseLong(numberString);
        } catch (NumberFormatException nfe) {
            return false;
        }

        int calculatedCheckDigit = checkSum(numberString);
        int inputCheckDigit = getNumericValue(numberString.charAt(numberString.length() - 1));
        return calculatedCheckDigit == inputCheckDigit;
    }

    /**
     * Generate check digit for a number string. Assumes check digit or a place
     * holder is already appended at end of the string.
     *
     * @param numberString number string to process
     * @return
     */
    public int checkSum(String numberString) {
        return checkSum(numberString, false);
    }

    /**
     * Generate check digit for a number string.
     *
     * @param numberString number string to process
     * @param noCheckDigit Whether check digit is present or not. True if no check Digit
     *                     is appended.
     * @return
     */
    public int checkSum(String numberString, boolean noCheckDigit) {
        int sum = 0;
        int checkDigit = 0;

        if (!noCheckDigit) {
            numberString = numberString.substring(0, numberString.length() - 1);
        }

        boolean isDouble = true;
        for (int i = numberString.length() - 1; i >= 0; i--) {
            int k = Integer.parseInt(String.valueOf(numberString.charAt(i)));
            sum += sumToSingleDigit((k * (isDouble ? 2 : 1)));
            isDouble = !isDouble;
        }

        if ((sum % 10) > 0) {
            checkDigit = (10 - (sum % 10));
        }

        return checkDigit;
    }

    private int sumToSingleDigit(int k) {
        if (k < 10) {
            return k;
        }

        return sumToSingleDigit(k / 10) + (k % 10);
    }
}

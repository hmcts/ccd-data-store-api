package uk.gov.hmcts.ccd.infrastructure;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.math.BigInteger;
import java.security.SecureRandom;

@Named
@Singleton
public class RandomKeyGenerator {
    private SecureRandom random = new SecureRandom();

    public String generate() {
        return new BigInteger(130, random).toString(32);
    }
}

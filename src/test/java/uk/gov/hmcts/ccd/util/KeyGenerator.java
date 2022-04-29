package uk.gov.hmcts.ccd.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

public class KeyGenerator {

    private static RSAKey rsaJWK;
    private static final String KEY_ID = "23456789";

    private KeyGenerator() {
    }

    public static RSAKey getRsaJWK() throws JOSEException {
        if (rsaJWK == null) {
            rsaJWK = new RSAKeyGenerator(2048)
                .keyID(KEY_ID)
                .generate();
        }
        return rsaJWK;
    }

}

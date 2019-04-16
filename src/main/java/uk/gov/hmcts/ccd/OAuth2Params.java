package uk.gov.hmcts.ccd;

public interface OAuth2Params {

    String getIdamBaseURL();

    String getOauth2RedirectUrl();

    String getOauth2ClientId();

    String getOauth2ClientSecret();

}

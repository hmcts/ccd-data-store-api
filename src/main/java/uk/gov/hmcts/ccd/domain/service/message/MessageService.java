package uk.gov.hmcts.ccd.domain.service.message;

public interface MessageService {

    void handleMessage(MessageContext messageContext);
}

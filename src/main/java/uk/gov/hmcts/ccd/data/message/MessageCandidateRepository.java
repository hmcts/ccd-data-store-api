package uk.gov.hmcts.ccd.data.message;

import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Named
@Singleton
public class MessageCandidateRepository {
    private final MessageCandidateMapper messageCandidateMapper;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public MessageCandidateRepository(final MessageCandidateMapper messageCandidateMapper) {
        this.messageCandidateMapper = messageCandidateMapper;
    }

    public MessageQueueCandidate save(final MessageQueueCandidate messageQueueCandidate) {
        final MessageQueueCandidateEntity newMessageCandidateEntity =
            messageCandidateMapper.modelToEntity(messageQueueCandidate);
        em.persist(newMessageCandidateEntity);
        return messageCandidateMapper.entityToModel(newMessageCandidateEntity);
    }

}

package uk.gov.hmcts.ccd.data.message;

import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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

package uk.gov.hmcts.ccd.domain.service.caselinking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseLinkService {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseLinkRepository caseLinkRepository;
    private final CaseLinkMapper caseLinkMapper;
    private final CaseLinkExtractor caseLinkExtractor;
    private final JdbcTemplate jdbcTemplate;

    @Inject
    public CaseLinkService(CaseLinkRepository caseLinkRepository,
                           CaseLinkMapper caseLinkMapper,
                           @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                               CaseDetailsRepository caseDetailsRepository,
                           CaseLinkExtractor caseLinkExtractor,
                           JdbcTemplate jdbcTemplate) {
        this.caseLinkRepository = caseLinkRepository;
        this.caseLinkMapper = caseLinkMapper;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseLinkExtractor = caseLinkExtractor;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void updateCaseLinks(CaseDetails caseDetails, List<CaseFieldDefinition> caseFieldDefinitions) {
        final Long caseReference = Objects.requireNonNull(
            caseDetails.getReference(), "case reference must not be null");
        final List<CaseLink> caseLinksWithReferences =
            caseLinkExtractor.getCaseLinksFromData(caseDetails, caseFieldDefinitions);
        lockCaseDataRows(caseReference, caseLinksWithReferences);

        // NB: delete all and re-add as this will update any links that have a changed StandardFlag value
        caseLinkRepository.deleteAllByCaseReference(caseReference);
        createCaseLinks(caseReference, caseLinksWithReferences);
    }

    private void createCaseLinks(Long caseReference, List<CaseLink> caseLinksWithReferences) {
        caseLinksWithReferences.stream()
            .filter(caseLink -> caseLink != null && caseLink.getLinkedCaseReference() != null)
            .forEach(caseLink -> {
                caseLinkRepository.insertUsingCaseReferences(
                    caseReference,
                    caseLink.getLinkedCaseReference(),
                    caseLink.getStandardLink());
                log.debug(
                    "inserted case link with id {}, linkedCaseId {} and StandardLink {}",
                    caseReference, caseLink.getLinkedCaseReference(), caseLink.getStandardLink());
            });
    }

    public List<CaseLink> findCaseLinks(String caseReference) {
        List<CaseLinkEntity> allByCaseReference =
            caseLinkRepository.findAllByCaseReference(Long.parseLong(caseReference));
        List<CaseLink> allLinkedCases =
            caseLinkMapper.entitiesToModels(allByCaseReference);

        return allLinkedCases.stream()
            .map(caseLink -> setCaseLinkReferences(Long.parseLong(caseReference), caseLink))
            .collect(Collectors.toList());
    }

    private CaseLink setCaseLinkReferences(Long caseReference, CaseLink caseLink) {

        caseLink.setCaseReference(caseReference);
        caseDetailsRepository.findById(null, caseLink.getLinkedCaseId())
            .ifPresent(caseDetails -> caseLink.setLinkedCaseReference(caseDetails.getReference()));

        return caseLink;
    }

    private void lockCaseDataRows(Long caseReference, List<CaseLink> caseLinksWithReferences) {
        List<Long> referencesToLock = new ArrayList<>();
        referencesToLock.add(caseReference);
        if (caseLinksWithReferences != null) {
            referencesToLock.addAll(caseLinksWithReferences.stream()
                .filter(Objects::nonNull)
                .map(CaseLink::getLinkedCaseReference)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        }
        if (referencesToLock.isEmpty()) {
            return;
        }

        referencesToLock = referencesToLock.stream().sorted().toList();
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("references", referencesToLock);
        namedTemplate.queryForList(
            "select id from case_data "
                + "where reference in (:references) "
                + "order by reference for update",
            params);
    }

}

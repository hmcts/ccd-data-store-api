package uk.gov.hmcts.ccd.domain.service.caselinking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.NON_STANDARD_LINK;
import static uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity.STANDARD_LINK;


class CaseLinkMapperTest {

    private static final String CASE_ID = "15";
    private static final String LINKED_CASE_ID = "25";
    private static final String CASE_TYPE_ID = "CaseType";

    private CaseLink caseLinkModel;
    private CaseLinkEntity caseLinkEntity;
    private CaseLinkMapper caseLinkMapper;

    @BeforeEach
    void setUp() {
        caseLinkModel = CaseLink.builder()
            .caseId(CASE_ID)
            .linkedCaseId(LINKED_CASE_ID)
            .caseTypeId(CASE_TYPE_ID)
            .standardLink(NON_STANDARD_LINK)
            .build();
        caseLinkEntity = new CaseLinkEntity(CASE_ID, LINKED_CASE_ID, CASE_TYPE_ID, NON_STANDARD_LINK);
        caseLinkMapper = new CaseLinkMapper();
    }

    @Test
    void testModelToEntity() {
        CaseLinkEntity mappedCaseLinkEntity = caseLinkMapper.modelToEntity(caseLinkModel);
        assertAll(() -> {
            assertEquals(CASE_ID, mappedCaseLinkEntity.getCaseLinkPrimaryKey().getCaseId());
            assertEquals(LINKED_CASE_ID, mappedCaseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId());
            assertEquals(CASE_TYPE_ID, mappedCaseLinkEntity.getCaseTypeId());
            assertEquals(NON_STANDARD_LINK, mappedCaseLinkEntity.getStandardLink());
        });
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testModelToEntity_NPE() {
        assertThrows(NullPointerException.class, () ->
            caseLinkMapper.modelToEntity(null)
        );
    }

    @Test
    void testEntityToModel() {
        CaseLink mappedCaseLinkModel = caseLinkMapper.entityToModel(caseLinkEntity);
        assertAll(() -> {
            assertEquals(CASE_ID, mappedCaseLinkModel.getCaseId());
            assertEquals(LINKED_CASE_ID, mappedCaseLinkModel.getLinkedCaseId());
            assertEquals(CASE_TYPE_ID, mappedCaseLinkModel.getCaseTypeId());
            assertEquals(NON_STANDARD_LINK, mappedCaseLinkModel.getStandardLink());
        });
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testEntityToModel_NPE() {
        assertThrows(NullPointerException.class, () ->
            caseLinkMapper.entityToModel(null)
        );
    }

    @Test
    void testEntitiesToModel() {

        List<CaseLinkEntity> caseLinkEntities =
            List.of(new CaseLinkEntity("10", "20", CASE_TYPE_ID, NON_STANDARD_LINK),
                new CaseLinkEntity("15", "25", CASE_TYPE_ID, STANDARD_LINK),
                new CaseLinkEntity("20", "30", CASE_TYPE_ID, NON_STANDARD_LINK));

        List<CaseLink> expectedCaseLinkModels = List.of(CaseLink.builder()
                                                            .caseId("10")
                                                            .linkedCaseId("20")
                                                            .caseTypeId(CASE_TYPE_ID)
                                                            .standardLink(NON_STANDARD_LINK)
                                                            .build(),
                                                        CaseLink.builder()
                                                            .caseId("15")
                                                            .linkedCaseId("25")
                                                            .caseTypeId(CASE_TYPE_ID)
                                                            .standardLink(STANDARD_LINK)
                                                            .build(),
                                                        CaseLink.builder()
                                                            .caseId("20")
                                                            .linkedCaseId("30")
                                                            .caseTypeId(CASE_TYPE_ID)
                                                            .standardLink(NON_STANDARD_LINK)
                                                            .build());

        final List<CaseLink> caseLinks = caseLinkMapper.entitiesToModels(caseLinkEntities);
        assertEquals(caseLinkEntities.size(), caseLinks.size());
        assertTrue(expectedCaseLinkModels.containsAll(caseLinks));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testEntitiesToModel_NPE() {
        assertThrows(NullPointerException.class, () ->
            caseLinkMapper.entitiesToModels(null)
        );
    }

}

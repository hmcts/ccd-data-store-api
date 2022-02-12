package uk.gov.hmcts.ccd.domain.service.casedeletion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.data.caseaccess.CaseLinkEntity;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CaseLinkMapperTest {

    private static final Long CASE_ID = 15L;
    private static final Long LINKED_CASE_ID = 25L;
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
            .build();
        caseLinkEntity = new CaseLinkEntity(CASE_ID, LINKED_CASE_ID, CASE_TYPE_ID);
        caseLinkMapper = new CaseLinkMapper();
    }

    @Test
    void testModelToEntity() {
        CaseLinkEntity mappedCaseLinkEntity = caseLinkMapper.modelToEntity(caseLinkModel);
        assertAll(() -> {
            assertEquals(CASE_ID, mappedCaseLinkEntity.getCaseLinkPrimaryKey().getCaseId());
            assertEquals(LINKED_CASE_ID, mappedCaseLinkEntity.getCaseLinkPrimaryKey().getLinkedCaseId());
            assertEquals(CASE_TYPE_ID, mappedCaseLinkEntity.getCaseTypeId());
        });
    }

    @Test
    void testNullModelToEntity() {
        assertNull(caseLinkMapper.modelToEntity(null));
    }

    @Test
    void testEntityToModel() {
        CaseLink mappedCaseLinkModel = caseLinkMapper.entityToModel(caseLinkEntity);
        assertAll(() -> {
            assertEquals(CASE_ID, mappedCaseLinkModel.getCaseId());
            assertEquals(LINKED_CASE_ID, mappedCaseLinkModel.getLinkedCaseId());
            assertEquals(CASE_TYPE_ID, mappedCaseLinkModel.getCaseTypeId());
        });
    }

    @Test
    void testEntitiesToModel() {

        List<CaseLinkEntity> caseLinkEntities = List.of(new CaseLinkEntity(10L, 20L, CASE_TYPE_ID),
                                                        new CaseLinkEntity(15L, 25L, CASE_TYPE_ID),
                                                        new CaseLinkEntity(20L, 30L, CASE_TYPE_ID));

        List<CaseLink> expectedCaseLinkModels = List.of(CaseLink.builder().caseId(10L).linkedCaseId(20L).caseTypeId(CASE_TYPE_ID).build(),
                                                        CaseLink.builder().caseId(15L).linkedCaseId(25L).caseTypeId(CASE_TYPE_ID).build(),
                                                        CaseLink.builder().caseId(20L).linkedCaseId(30L).caseTypeId(CASE_TYPE_ID).build());

        final List<CaseLink> caseLinks = caseLinkMapper.entitiesToModels(caseLinkEntities);
        assertEquals(caseLinkEntities.size(), caseLinks.size());
        assertTrue(expectedCaseLinkModels.containsAll(caseLinks));
    }

    @Test
    void testNullEntityToModel() {
        CaseLinkEntity caseLinkEntity = null;
        assertNull(caseLinkMapper.entityToModel(caseLinkEntity));
    }
}

package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Repository
public interface CaseLinkRepository extends CrudRepository<CaseLinkEntity, CaseLinkEntity.CaseLinkPrimaryKey> {

    @Modifying
    @Query("delete from CaseLinkEntity cle where cle.caseLinkPrimaryKey.caseId = "
        + "(select cd.id from CaseDetailsEntity cd where cd.reference=:caseReference) "
        + "and cle.caseLinkPrimaryKey.linkedCaseId = (select cd.id from CaseDetailsEntity cd where "
        + "cd.reference=:linkedCaseReference)")
    int deleteByCaseReferenceAndLinkedCaseReference(@Param("caseReference") Long caseReference,
                                                    @Param("linkedCaseReference") Long linkedCaseReference);

    @Modifying
    @Query(value = "insert into case_link (case_id, linked_case_id, case_type_id) values ("
        + "(select id from case_data cd where cd.reference=:caseReference),"
        + "(select id from case_data cd where cd.reference=:linkedCaseReference), "
        + ":caseTypeId)", nativeQuery = true)
    void insertUsingCaseReferenceLinkedCaseReferenceAndCaseTypeId(@Param("caseReference") Long caseReference,
                                                                  @Param("linkedCaseReference")
                                                                      Long linkedCaseReference,
                                                                  @Param("caseTypeId") String caseTypeId);

    @Query(value = "select cle from CaseLinkEntity cle where cle.caseLinkPrimaryKey.caseId = "
        + "(select cd.id from CaseDetailsEntity cd where cd.reference=:caseReference)")
    List<CaseLinkEntity> findAllByCaseReference(@Param("caseReference") Long caseReference);
}

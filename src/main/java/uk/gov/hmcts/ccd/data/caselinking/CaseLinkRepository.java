package uk.gov.hmcts.ccd.data.caselinking;

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

    /**
     * Delete all CaseLinks records belonging to the supplied Case Reference.
     *
     * @param caseReference parent case reference to delete by
     * @return number of records deleted
     */
    @Modifying
    @Query("delete from CaseLinkEntity cle where cle.caseLinkPrimaryKey.caseId = "
        + "(select cd.id from CaseDetailsEntity cd where cd.reference=:caseReference)")
    int deleteAllByCaseReference(@Param("caseReference") Long caseReference);

    @Modifying
    @Query(value = "insert into case_link (case_id, linked_case_id, case_type_id, standard_link) values ("
        + "(select id from case_data cd where cd.reference=:caseReference), "
        + "(select id from case_data cd where cd.reference=:linkedCaseReference), "
        + "(select case_type_id from case_data cd where cd.reference=:linkedCaseReference), "
        + ":standardLink)", nativeQuery = true)
    void insertUsingCaseReferences(@Param("caseReference") Long caseReference,
                                   @Param("linkedCaseReference") Long linkedCaseReference,
                                   @Param("standardLink") Boolean standardLink);

    @Query(value = "select cle from CaseLinkEntity cle where cle.caseLinkPrimaryKey.caseId = "
        + "(select cd.id from CaseDetailsEntity cd where cd.reference=:caseReference)")
    List<CaseLinkEntity> findAllByCaseReference(@Param("caseReference") Long caseReference);


    @Query(value = "select cd.reference from CaseDetailsEntity cd where cd.id in "
        + "   (select cle.caseLinkPrimaryKey.caseId "
        + "      from CaseLinkEntity cle "
        + "     where cle.caseLinkPrimaryKey.linkedCaseId = (select lcd.id "
        + "                                                    from CaseDetailsEntity lcd "
        + "                                                   where lcd.reference=:linkedCaseReference)"
        + "       and cle.standardLink=:standardLink)"
        + " order by cd.createdDate asc"
    )
    List<Long> findCaseReferencesByLinkedCaseReferenceAndStandardLink(
        @Param("linkedCaseReference") Long linkedCaseReference,
        @Param("standardLink") Boolean standardLink
    );

}

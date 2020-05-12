<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">


    <changeSet id="rdm-8463" author="suman.sree@hmcts.net">
        <sql dbms="postgresql"
             endDelimiter="\nGO"
             splitStatements="true"
             stripComments="true">
            CREATE INDEX  idx_case_data_appeal_appellant_identity_nino ON public.case_data USING BTREE ((TRIM(UPPER(data#>>'{appeal,appellant,identity,nino}'))));
            CREATE INDEX  idx_case_data_deceaseddateofbirth ON public.case_data USING BTREE ((TRIM(UPPER(data#>>'{deceasedDateofBirth}'))));
            CREATE INDEX  idx_case_data_createdingapsfrom_dwpstate ON public.case_data USING BTREE ((TRIM(UPPER(data#>>'{createdingapsfrom,dwpstate}'))));
            CREATE INDEX  idx_case_data_d8petitionerlastname ON public.case_data USING BTREE ((TRIM(UPPER(data#>>’{D8PetitionerLastname}')))); 
        </sql>
    </changeSet>
</databaseChangeLog>

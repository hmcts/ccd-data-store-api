-- Script to delete ccd-data data by case-data.jurisdiction
-- Please note: A valid jurisdiction needs to be provided in the DECLARE block i.e ??? = EMPLOYMENT
DO $$
DECLARE
  jurisdiction_ref constant varchar := '???';
BEGIN

CREATE TEMP TABLE tmp_case_data_ids ON COMMIT DROP AS
    SELECT id FROM case_data WHERE jurisdiction = jurisdiction_ref;

DELETE FROM case_users_audit WHERE case_data_id IN (
    (SELECT id FROM tmp_case_data_ids)
    );

DELETE FROM case_users WHERE case_data_id IN (
    SELECT id FROM tmp_case_data_ids
    );

DELETE FROM case_event_significant_items WHERE case_event_id IN (
    SELECT id FROM case_event WHERE case_data_id IN (
        SELECT id FROM tmp_case_data_ids
        )
    );
DELETE FROM case_event WHERE case_data_id IN (
    SSELECT id FROM tmp_case_data_ids
    );

DELETE FROM case_data WHERE jurisdiction = jurisdiction_ref

-- Check count is 0
SELECT COUNT(*) FROM case_data WHERE jurisdiction = jurisdiction_ref
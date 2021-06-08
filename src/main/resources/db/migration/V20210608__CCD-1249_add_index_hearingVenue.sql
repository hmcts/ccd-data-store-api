CREATE UNIQUE INDEX uidx_case_data_hearing_venue ON public.case_data USING btree (btrim(upper((data #>> '{hearingVenue}'::text[]))));

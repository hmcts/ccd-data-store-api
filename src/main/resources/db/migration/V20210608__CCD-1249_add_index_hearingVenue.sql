CREATE UNIQUE INDEX uidx_case_data_external_id ON public.case_data USING btree (btrim(upper((data #>> '{hearingVenue}'::text[]))));

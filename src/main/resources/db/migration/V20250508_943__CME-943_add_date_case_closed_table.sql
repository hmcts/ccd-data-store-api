CREATE TABLE public.date_case_closed (
    id bigint NOT NULL,
    ccd_case_number bigint NOT NULL,
    state character varying(255),
    state_category character varying(500),
    state_changed_date timestamp,
    PRIMARY KEY(id)
);

CREATE SEQUENCE public.date_case_closed_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.date_case_closed_id_seq OWNED BY public.date_case_closed.id;

ALTER TABLE ONLY public.date_case_closed
    ALTER COLUMN id SET DEFAULT nextval('public.date_case_closed_id_seq'::regclass);

ALTER TABLE public.date_case_closed
    ADD CONSTRAINT fk_date_case_closed_ccd_case_number_case_data
    FOREIGN KEY (ccd_case_number) REFERENCES public.case_data(reference);

CREATE INDEX idx_date_case_closed_state_changed_date
    ON public.date_case_closed (state_changed_date);

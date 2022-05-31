CREATE TABLE public.case_link (
    case_id bigint NOT NULL,
    linked_case_id bigint NOT NULL,
    case_type_id character varying(255) NOT NULL,
    PRIMARY KEY(case_id, linked_case_id)
);

ALTER TABLE public.case_link
    ADD CONSTRAINT fk_case_link_case_id_case_data FOREIGN KEY (case_id) REFERENCES public.case_data(id);

ALTER TABLE public.case_link
    ADD CONSTRAINT fk_case_link_linked_case_id_case_data FOREIGN KEY (linked_case_id) REFERENCES public.case_data(id);

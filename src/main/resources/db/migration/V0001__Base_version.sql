--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.19
-- Dumped by pg_dump version 13.1


--
-- Name: case_users_audit_action; Type: TYPE; Schema: public; Owner: ccd
--

CREATE TYPE public.case_users_audit_action AS ENUM (
    'GRANT',
    'REVOKE'
);


--
-- Name: securityclassification; Type: TYPE; Schema: public; Owner: ccd
--

CREATE TYPE public.securityclassification AS ENUM (
    'PUBLIC',
    'PRIVATE',
    'RESTRICTED'
);


--
-- Name: significant_item_type; Type: TYPE; Schema: public; Owner: ccd
--

CREATE TYPE public.significant_item_type AS ENUM (
    'DOCUMENT'
);


--
-- Name: set_case_data_marked_by_logstash(); Type: FUNCTION; Schema: public; Owner: ccd
--

CREATE FUNCTION public.set_case_data_marked_by_logstash() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
            BEGIN
                NEW.marked_by_logstash := false;
                RETURN NEW;
            END
            $$;



SET default_tablespace = '';

--
-- Name: case_data; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_data (
    id bigint NOT NULL,
    created_date timestamp without time zone DEFAULT now() NOT NULL,
    last_modified timestamp without time zone,
    jurisdiction character varying(255) NOT NULL,
    case_type_id character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    data jsonb NOT NULL,
    data_classification jsonb,
    reference bigint NOT NULL,
    security_classification public.securityclassification NOT NULL,
    version integer DEFAULT 1,
    last_state_modified_date timestamp without time zone,
    supplementary_data jsonb,
    marked_by_logstash boolean DEFAULT false
);


--
-- Name: case_data_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_data_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_data_id_seq OWNED BY public.case_data.id;


--
-- Name: case_event; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_event (
    id bigint NOT NULL,
    created_date timestamp without time zone DEFAULT now() NOT NULL,
    event_id character varying(70) NOT NULL,
    summary character varying(1024),
    description character varying(65536),
    user_id character varying(64) NOT NULL,
    case_data_id bigint NOT NULL,
    case_type_id character varying(255) NOT NULL,
    case_type_version integer NOT NULL,
    state_id character varying(255) NOT NULL,
    data jsonb NOT NULL,
    user_first_name character varying(255) DEFAULT NULL::character varying NOT NULL,
    user_last_name character varying(255) DEFAULT NULL::character varying NOT NULL,
    event_name character varying(30) DEFAULT NULL::character varying NOT NULL,
    state_name character varying(255) DEFAULT ''::character varying NOT NULL,
    data_classification jsonb,
    security_classification public.securityclassification NOT NULL,
    proxied_by character varying(64),
    proxied_by_first_name character varying(255),
    proxied_by_last_name character varying(255)
);


--
-- Name: case_event_case_data_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_event_case_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_event_case_data_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_event_case_data_id_seq OWNED BY public.case_event.case_data_id;


--
-- Name: case_event_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_event_id_seq OWNED BY public.case_event.id;


--
-- Name: case_event_significant_items; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_event_significant_items (
    id integer NOT NULL,
    description character varying(64) NOT NULL,
    type public.significant_item_type NOT NULL,
    url text,
    case_event_id integer NOT NULL
);


--
-- Name: case_event_significant_items_case_event_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_event_significant_items_case_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_event_significant_items_case_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_event_significant_items_case_event_id_seq OWNED BY public.case_event_significant_items.case_event_id;


--
-- Name: case_event_significant_items_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_event_significant_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_event_significant_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_event_significant_items_id_seq OWNED BY public.case_event_significant_items.id;


--
-- Name: case_users; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_users (
    case_data_id bigint NOT NULL,
    user_id character varying(64) NOT NULL,
    case_role character varying(40) DEFAULT '[CREATOR]'::character varying NOT NULL
);


--
-- Name: case_users_audit; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_users_audit (
    id bigint NOT NULL,
    case_data_id bigint NOT NULL,
    user_id character varying(64) NOT NULL,
    changed_by_id character varying(64) NOT NULL,
    changed_at timestamp without time zone DEFAULT now() NOT NULL,
    action public.case_users_audit_action NOT NULL,
    case_role character varying(40) DEFAULT '[CREATOR]'::character varying NOT NULL
);


--
-- Name: case_users_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.case_users_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: case_users_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.case_users_audit_id_seq OWNED BY public.case_users_audit.id;

-- Name: message_queue_candidates; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.message_queue_candidates (
    id bigint NOT NULL,
    message_type character varying(70) NOT NULL,
    time_stamp timestamp without time zone DEFAULT now() NOT NULL,
    published timestamp without time zone,
    message_information jsonb NOT NULL
);


--
-- Name: message_queue_candidates_id_seq; Type: SEQUENCE; Schema: public; Owner: ccd
--

CREATE SEQUENCE public.message_queue_candidates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: message_queue_candidates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: ccd
--

ALTER SEQUENCE public.message_queue_candidates_id_seq OWNED BY public.message_queue_candidates.id;


--
-- Name: case_data id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_data ALTER COLUMN id SET DEFAULT nextval('public.case_data_id_seq'::regclass);


--
-- Name: case_event id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event ALTER COLUMN id SET DEFAULT nextval('public.case_event_id_seq'::regclass);


--
-- Name: case_event case_data_id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event ALTER COLUMN case_data_id SET DEFAULT nextval('public.case_event_case_data_id_seq'::regclass);


--
-- Name: case_event_significant_items id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event_significant_items ALTER COLUMN id SET DEFAULT nextval('public.case_event_significant_items_id_seq'::regclass);


--
-- Name: case_event_significant_items case_event_id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event_significant_items ALTER COLUMN case_event_id SET DEFAULT nextval('public.case_event_significant_items_case_event_id_seq'::regclass);


--
-- Name: case_users_audit id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_users_audit ALTER COLUMN id SET DEFAULT nextval('public.case_users_audit_id_seq'::regclass);


--
-- Name: message_queue_candidates id; Type: DEFAULT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.message_queue_candidates ALTER COLUMN id SET DEFAULT nextval('public.message_queue_candidates_id_seq'::regclass);


--
-- Name: case_data case_data_pkey; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_data
    ADD CONSTRAINT case_data_pkey PRIMARY KEY (id);


--
-- Name: case_data case_data_reference_key; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_data
    ADD CONSTRAINT case_data_reference_key UNIQUE (reference);


--
-- Name: case_event case_event_pkey; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event
    ADD CONSTRAINT case_event_pkey PRIMARY KEY (id);


--
-- Name: case_users_audit case_users_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_users_audit
    ADD CONSTRAINT case_users_audit_pkey PRIMARY KEY (id);


--
-- Name: case_users case_users_pkey; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_users
    ADD CONSTRAINT case_users_pkey PRIMARY KEY (case_data_id, user_id, case_role);




--
-- Name: message_queue_candidates message_queue_candidates_pkey; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.message_queue_candidates
    ADD CONSTRAINT message_queue_candidates_pkey PRIMARY KEY (id);


--
-- Name: case_event_significant_items pk_case_event_significant_items; Type: CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event_significant_items
    ADD CONSTRAINT pk_case_event_significant_items PRIMARY KEY (id);


--
-- Name: idx_case_data__deceased_surname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__deceased_surname ON public.case_data USING btree (btrim(upper((data #>> '{deceasedSurname}'::text[]))));


--
-- Name: idx_case_data__evidence_handled; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__evidence_handled ON public.case_data USING btree (btrim(upper((data #>> '{evidenceHandled}'::text[]))));


--
-- Name: idx_case_data__grant_issued_date; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__grant_issued_date ON public.case_data USING btree (btrim(upper((data #>> '{grantIssuedDate}'::text[]))));


--
-- Name: idx_case_data__registry_location; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__registry_location ON public.case_data USING btree (btrim(upper((data #>> '{registryLocation}'::text[]))));


--
-- Name: idx_case_data__subscription_appointeesubscription_tya; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__subscription_appointeesubscription_tya ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,appointeeSubscription,tya}'::text[]))));


--
-- Name: idx_case_data__subscription_representativesubscription_tya; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data__subscription_representativesubscription_tya ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,representativeSubscription,tya}'::text[]))));


--
-- Name: idx_case_data_appeal_appellant_address_postcode; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_appeal_appellant_address_postcode ON public.case_data USING btree (btrim(upper((data #>> '{appeal,appellant,address,postcode}'::text[]))));


--
-- Name: idx_case_data_appeal_appellant_identity_lastname_region; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_appeal_appellant_identity_lastname_region ON public.case_data USING btree (btrim(upper((data #>> '{appeal,appellant,name,lastName}'::text[]))), btrim(upper((data #>> '{region}'::text[]))));


--
-- Name: idx_case_data_appeal_appellant_identity_nino; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_appeal_appellant_identity_nino ON public.case_data USING btree (btrim(upper((data #>> '{appeal,appellant,identity,nino}'::text[]))));


--
-- Name: idx_case_data_applicantlname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_applicantlname ON public.case_data USING btree (btrim(upper((data #>> '{applicantLName}'::text[]))));


--
-- Name: idx_case_data_application_type; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_application_type ON public.case_data USING btree (btrim(upper((data #>> '{applicationType}'::text[]))));


--
-- Name: idx_case_data_applicationtype_evidencehandled_registrylocation_; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_applicationtype_evidencehandled_registrylocation_ ON public.case_data USING btree (btrim(upper((data #>> '{applicationType}'::text[]))), btrim(upper((data #>> '{evidenceHandled}'::text[]))), btrim(upper((data #>> '{registryLocation}'::text[]))), btrim(upper((data #>> '{caseType}'::text[]))));


--
-- Name: idx_case_data_assignedto; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_assignedto ON public.case_data USING btree (btrim(upper((data #>> '{assignedTo}'::text[]))));


--
-- Name: idx_case_data_case_external_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_case_external_id ON public.case_data USING btree (btrim(upper((data #>> '{externalId}'::text[]))));


--
-- Name: idx_case_data_case_reference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_case_reference ON public.case_data USING btree (btrim(upper((data #>> '{caseReference}'::text[]))));


--
-- Name: idx_case_data_caselocalauthority; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_caselocalauthority ON public.case_data USING btree (btrim(upper((data #>> '{caseLocalAuthority}'::text[]))));


--
-- Name: idx_case_data_caveatorsurname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_caveatorsurname ON public.case_data USING btree (btrim(upper((data #>> '{caveatorSurname}'::text[]))));


--
-- Name: idx_case_data_cmc_previousservicecasereference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_cmc_previousservicecasereference ON public.case_data USING btree (jurisdiction, case_type_id, btrim(upper((data #>> '{previousServiceCaseReference}'::text[])))) WHERE (((jurisdiction)::text = 'CMC'::text) AND ((case_type_id)::text = 'MoneyClaimCase'::text));


--
-- Name: idx_case_data_cmc_submitteremail; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_cmc_submitteremail ON public.case_data USING btree (btrim(upper((data #>> '{submitterEmail}'::text[]))));


--
-- Name: idx_case_data_containspayments; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_containspayments ON public.case_data USING btree (btrim(upper((data #>> '{containsPayments}'::text[]))));


--
-- Name: idx_case_data_created_date; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_created_date ON public.case_data USING btree (created_date);


--
-- Name: idx_case_data_createdingapsfrom_dwpregionalcentre; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_createdingapsfrom_dwpregionalcentre ON public.case_data USING btree (btrim(upper((data #>> '{createdInGapsFrom}'::text[]))), btrim(upper((data #>> '{dwpRegionalCentre}'::text[]))));


--
-- Name: idx_case_data_createdingapsfrom_dwpstate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_createdingapsfrom_dwpstate ON public.case_data USING btree (btrim(upper((data #>> '{createdInGapsFrom}'::text[]))), btrim(upper((data #>> '{dwpState}'::text[]))));


--
-- Name: idx_case_data_d8_case_reference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8_case_reference ON public.case_data USING btree (btrim(upper((data #>> '{D8caseReference}'::text[]))));


--
-- Name: idx_case_data_d8_divorce_unit; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8_divorce_unit ON public.case_data USING btree (btrim(upper((data #>> '{D8DivorceUnit}'::text[]))));


--
-- Name: idx_case_data_d8_petitioner_email; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8_petitioner_email ON public.case_data USING btree (btrim(upper((data #>> '{D8PetitionerEmail}'::text[]))));


--
-- Name: idx_case_data_d8marriagedate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8marriagedate ON public.case_data USING btree (btrim(upper((data #>> '{D8MarriageDate}'::text[]))));


--
-- Name: idx_case_data_d8petitionerfirstname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8petitionerfirstname ON public.case_data USING btree (btrim(upper((data #>> '{D8PetitionerFirstName}'::text[]))));


--
-- Name: idx_case_data_d8petitionerlastname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_d8petitionerlastname ON public.case_data USING btree (btrim(upper((data #>> '{D8PetitionerLastName}'::text[]))));


--
-- Name: idx_case_data_deceased_forenames; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_deceased_forenames ON public.case_data USING btree (btrim(upper((data #>> '{deceasedForenames}'::text[]))));


--
-- Name: idx_case_data_deceaseddateofbirth; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_deceaseddateofbirth ON public.case_data USING btree (btrim(upper((data #>> '{deceasedDateOfBirth}'::text[]))));


--
-- Name: idx_case_data_deceaseddateofdeath; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_deceaseddateofdeath ON public.case_data USING btree (btrim(upper((data #>> '{deceasedDateOfDeath}'::text[]))));


--
-- Name: idx_case_data_deliverydate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_deliverydate ON public.case_data USING btree (btrim(upper((data #>> '{deliveryDate}'::text[]))));


--
-- Name: idx_case_data_divorce_applyfordecreeabsolute; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_applyfordecreeabsolute ON public.case_data USING btree (btrim(upper((data #>> '{ApplyForDecreeAbsolute}'::text[]))));


--
-- Name: idx_case_data_divorce_d8helpwithfeesneedhelp; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_d8helpwithfeesneedhelp ON public.case_data USING btree (btrim(upper((data #>> '{D8HelpWithFeesNeedHelp}'::text[]))));


--
-- Name: idx_case_data_divorce_d8marriagepetitionername; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_d8marriagepetitionername ON public.case_data USING btree (btrim(upper((data #>> '{D8MarriagePetitionerName}'::text[]))));


--
-- Name: idx_case_data_divorce_d8marriagerespondentname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_d8marriagerespondentname ON public.case_data USING btree (btrim(upper((data #>> '{D8MarriageRespondentName}'::text[]))));


--
-- Name: idx_case_data_divorce_d8respondentfirstname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_d8respondentfirstname ON public.case_data USING btree (btrim(upper((data #>> '{D8RespondentFirstName}'::text[]))));


--
-- Name: idx_case_data_divorce_d8respondentlastname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_d8respondentlastname ON public.case_data USING btree (btrim(upper((data #>> '{D8RespondentLastName}'::text[]))));


--
-- Name: idx_case_data_divorce_divorcecasenumber; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_divorcecasenumber ON public.case_data USING btree (btrim(upper((data #>> '{divorceCaseNumber}'::text[]))));


--
-- Name: idx_case_data_divorce_dnoutcomecase; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_dnoutcomecase ON public.case_data USING btree (btrim(upper((data #>> '{DnOutcomeCase}'::text[]))));


--
-- Name: idx_case_data_divorce_midlandsfrclist; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_midlandsfrclist ON public.case_data USING btree (btrim(upper((data #>> '{midlandsFRCList}'::text[]))));


--
-- Name: idx_case_data_divorce_regionlist; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_regionlist ON public.case_data USING btree (btrim(upper((data #>> '{regionList}'::text[]))));


--
-- Name: idx_case_data_divorce_solicitorreference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_solicitorreference ON public.case_data USING btree (btrim(upper((data #>> '{solicitorReference}'::text[]))));


--
-- Name: idx_case_data_divorce_solpaymenthowtopay; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_divorce_solpaymenthowtopay ON public.case_data USING btree (btrim(upper((data #>> '{SolPaymentHowToPay}'::text[]))));


--
-- Name: idx_case_data_documentsenttodwp; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_documentsenttodwp ON public.case_data USING btree (btrim(upper((data #>> '{documentSentToDwp}'::text[]))));


--
-- Name: idx_case_data_dwpfurtherevidencestates; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_dwpfurtherevidencestates ON public.case_data USING btree (btrim(upper((data #>> '{dwpFurtherEvidenceStates}'::text[]))));


--
-- Name: idx_case_data_dwpregionalcentre; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_dwpregionalcentre ON public.case_data USING btree (btrim(upper((data #>> '{dwpRegionalCentre}'::text[]))));


--
-- Name: idx_case_data_dwpstate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_dwpstate ON public.case_data USING btree (btrim(upper((data #>> '{dwpState}'::text[]))));


--
-- Name: idx_case_data_generated_nino; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_generated_nino ON public.case_data USING btree (btrim(upper((data #>> '{generatedNino}'::text[]))));


--
-- Name: idx_case_data_generated_surname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_generated_surname ON public.case_data USING btree (btrim(upper((data #>> '{generatedSurname}'::text[]))));


--
-- Name: idx_case_data_hearingcentre; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_hearingcentre ON public.case_data USING btree (btrim(upper((data #>> '{hearingCentre}'::text[]))));


--
-- Name: idx_case_data_hmctsdwpstate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_hmctsdwpstate ON public.case_data USING btree (btrim(upper((data #>> '{hmctsDwpState}'::text[]))));


--
-- Name: idx_case_data_ia_appealreferencenumber; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_appealreferencenumber ON public.case_data USING btree (btrim(upper((data #>> '{appealReferenceNumber}'::text[]))));


--
-- Name: idx_case_data_ia_appellantdateofbirth; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_appellantdateofbirth ON public.case_data USING btree (btrim(upper((data #>> '{appellantDateOfBirth}'::text[]))));


--
-- Name: idx_case_data_ia_appellantnamefordisplay; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_appellantnamefordisplay ON public.case_data USING btree (btrim(upper((data #>> '{appellantNameForDisplay}'::text[]))));


--
-- Name: idx_case_data_ia_homeofficereferencenumber; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_homeofficereferencenumber ON public.case_data USING btree (btrim(upper((data #>> '{homeOfficeReferenceNumber}'::text[]))));


--
-- Name: idx_case_data_ia_legalrepreferencenumber; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_legalrepreferencenumber ON public.case_data USING btree (btrim(upper((data #>> '{legalRepReferenceNumber}'::text[]))));


--
-- Name: idx_case_data_ia_searchpostcode; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_ia_searchpostcode ON public.case_data USING btree (btrim(upper((data #>> '{searchPostcode}'::text[]))));


--
-- Name: idx_case_data_interlocreferraldate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_interlocreferraldate ON public.case_data USING btree (btrim(upper((data #>> '{interlocReferralDate}'::text[]))));


--
-- Name: idx_case_data_interlocreviewstate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_interlocreviewstate ON public.case_data USING btree (btrim(upper((data #>> '{interlocReviewState}'::text[]))));


--
-- Name: idx_case_data_isscottishcase; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_isscottishcase ON public.case_data USING btree (btrim(upper((data #>> '{isScottishCase}'::text[]))));


--
-- Name: idx_case_data_isscottishcase_documentsenttodwp; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_isscottishcase_documentsenttodwp ON public.case_data USING btree (btrim(upper((data #>> '{isScottishCase}'::text[]))), btrim(upper((data #>> '{documentSentToDwp}'::text[]))));


--
-- Name: idx_case_data_issuedon; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_issuedon ON public.case_data USING btree (btrim(upper((data #>> '{issuedOn}'::text[]))));


--
-- Name: idx_case_data_jur_case_type_state; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_jur_case_type_state ON public.case_data USING btree (jurisdiction, case_type_id, state);


--
-- Name: idx_case_data_languagepreferencewelsh; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_languagepreferencewelsh ON public.case_data USING btree (btrim(upper((data #>> '{languagePreferenceWelsh}'::text[]))));


--
-- Name: idx_case_data_last_state_modified_date; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_last_state_modified_date ON public.case_data USING btree (last_state_modified_date);


--
-- Name: idx_case_data_latestgrantreissuedate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_latestgrantreissuedate ON public.case_data USING btree (btrim(upper((data #>> '{latestGrantReissueDate}'::text[]))));


--
-- Name: idx_case_data_marked_by_logstash; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_marked_by_logstash ON public.case_data USING btree (marked_by_logstash);


--
-- Name: idx_case_data_openingdate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_openingdate ON public.case_data USING btree (btrim(upper((data #>> '{openingDate}'::text[]))));


--
-- Name: idx_case_data_paymentreference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_paymentreference ON public.case_data USING btree (btrim(upper((data #>> '{paymentReference}'::text[]))));


--
-- Name: idx_case_data_pl_dateandtimesubmitted; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pl_dateandtimesubmitted ON public.case_data USING btree (btrim(upper((data #>> '{dateAndTimeSubmitted}'::text[]))));


--
-- Name: idx_case_data_pl_familymancasenumber; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pl_familymancasenumber ON public.case_data USING btree (btrim(upper((data #>> '{familyManCaseNumber}'::text[]))));


--
-- Name: idx_case_data_pobox; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pobox ON public.case_data USING btree (btrim(upper((data #>> '{poBox}'::text[]))));


--
-- Name: idx_case_data_poboxjurisdiction; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_poboxjurisdiction ON public.case_data USING btree (btrim(upper((data #>> '{poBoxJurisdiction}'::text[]))));


--
-- Name: idx_case_data_pr_caseprinted; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pr_caseprinted ON public.case_data USING btree (btrim(upper((data #>> '{casePrinted}'::text[]))));


--
-- Name: idx_case_data_pr_casetype; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pr_casetype ON public.case_data USING btree (btrim(upper((data #>> '{caseType}'::text[]))));


--
-- Name: idx_case_data_pr_expirydate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pr_expirydate ON public.case_data USING btree (btrim(upper((data #>> '{expiryDate}'::text[]))));


--
-- Name: idx_case_data_pr_journeyclassification; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_pr_journeyclassification ON public.case_data USING btree (btrim(upper((data #>> '{journeyClassification}'::text[]))));


--
-- Name: idx_case_data_previous_service_case_reference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE UNIQUE INDEX idx_case_data_previous_service_case_reference ON public.case_data USING btree (btrim(upper((data #>> '{previousServiceCaseReference}'::text[])))) WHERE ((jurisdiction)::text = 'CMC'::text);


--
-- Name: idx_case_data_probate_applicationsubmitteddate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_probate_applicationsubmitteddate ON public.case_data USING btree (btrim(upper((data #>> '{applicationSubmittedDate}'::text[]))));


--
-- Name: idx_case_data_probate_formtype; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_probate_formtype ON public.case_data USING btree (jurisdiction, btrim(upper((data #>> '{formType}'::text[])))) WHERE ((jurisdiction)::text = 'PROBATE'::text);


--
-- Name: idx_case_data_region; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_region ON public.case_data USING btree (btrim(upper((data #>> '{region}'::text[]))));


--
-- Name: idx_case_data_southeastfrclist; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_southeastfrclist ON public.case_data USING btree (btrim(upper((data #>> '{southEastFRCList}'::text[]))));


--
-- Name: idx_case_data_sscs_appeal_hearingtype; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_appeal_hearingtype ON public.case_data USING btree (btrim(upper((data #>> '{appeal,hearingType}'::text[]))));


--
-- Name: idx_case_data_sscs_appellant_dob; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_appellant_dob ON public.case_data USING btree (btrim(upper((data #>> '{appeal,appellant,identity,dob}'::text[]))));


--
-- Name: idx_case_data_sscs_attachtocasereference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_attachtocasereference ON public.case_data USING btree (btrim(upper((data #>> '{attachToCaseReference}'::text[]))));


--
-- Name: idx_case_data_sscs_createdingapsfrom; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_createdingapsfrom ON public.case_data USING btree (btrim(upper((data #>> '{createdInGapsFrom}'::text[]))));


--
-- Name: idx_case_data_sscs_datesenttodwp; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_datesenttodwp ON public.case_data USING btree (btrim(upper((data #>> '{dateSentToDwp}'::text[]))));


--
-- Name: idx_case_data_sscs_directionduedate; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_directionduedate ON public.case_data USING btree (btrim(upper((data #>> '{directionDueDate}'::text[]))));


--
-- Name: idx_case_data_sscs_formtype; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_formtype ON public.case_data USING btree (jurisdiction, btrim(upper((data #>> '{formType}'::text[])))) WHERE ((jurisdiction)::text = 'SSCS'::text);


--
-- Name: idx_case_data_sscs_subscriptionsjp_tya; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_subscriptionsjp_tya ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,jointPartySubscription,tya}'::text[]))));


--
-- Name: idx_case_data_sscs_surname; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_surname ON public.case_data USING btree (btrim(upper((data #>> '{surname}'::text[]))));


--
-- Name: idx_case_data_sscs_urgentcase; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_sscs_urgentcase ON public.case_data USING btree (btrim(upper((data #>> '{urgentCase}'::text[]))));


--
-- Name: idx_case_data_subscription_appellantsubscription_email; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_subscription_appellantsubscription_email ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,appellantSubscription,email}'::text[]))));


--
-- Name: idx_case_data_subscription_appellantsubscription_tya; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_subscription_appellantsubscription_tya ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,appellantSubscription,tya}'::text[]))));


--
-- Name: idx_case_data_subscriptions_appellantsubscription_mobile; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_subscriptions_appellantsubscription_mobile ON public.case_data USING btree (btrim(upper((data #>> '{subscriptions,appellantSubscription,mobile}'::text[]))));


--
-- Name: idx_case_data_translationworkoutstanding; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_translationworkoutstanding ON public.case_data USING btree (btrim(upper((data #>> '{translationWorkOutstanding}'::text[]))));


--
-- Name: idx_case_data_welsh_dnout_d8_sol_apply; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_data_welsh_dnout_d8_sol_apply ON public.case_data USING btree (btrim(upper((data #>> '{{LanguagePreferenceWelsh}}'::text[]))), btrim(upper((data #>> '{DnOutcomeCase}'::text[]))), btrim(upper((data #>> '{D8DivorceUnit}'::text[]))), btrim(upper((data #>> '{SolPaymentHowToPay}'::text[]))), btrim(upper((data #>> '{ApplyForDecreeAbsolute}'::text[]))));


--
-- Name: idx_case_details; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_details ON public.case_event USING btree (case_data_id);


--
-- Name: idx_case_event_case_type_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_event_case_type_id ON public.case_event USING btree (case_type_id);


--
-- Name: idx_case_event_created_date; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_event_created_date ON public.case_event USING btree (created_date);


--
-- Name: idx_case_event_event_id_state_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_event_event_id_state_id ON public.case_event USING btree (event_id, state_id);


--
-- Name: idx_case_event_significant_items_case_event_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_event_significant_items_case_event_id ON public.case_event_significant_items USING btree (case_event_id);


--
-- Name: idx_case_user_user_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_user_user_id ON public.case_users USING btree (user_id);


--
-- Name: idx_case_users_case_data_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_case_users_case_data_id ON public.case_users USING btree (case_data_id);


--
-- Name: idx_last_modified; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_last_modified ON public.case_data USING btree (last_modified);


--
-- Name: idx_message_queue_candidates_time_stamp; Type: INDEX; Schema: public; Owner: ccd
--

CREATE INDEX idx_message_queue_candidates_time_stamp ON public.message_queue_candidates USING btree (time_stamp);


--
-- Name: uidx_case_data_ethoscasereference; Type: INDEX; Schema: public; Owner: ccd
--

CREATE UNIQUE INDEX uidx_case_data_ethoscasereference ON public.case_data USING btree (case_type_id, btrim(upper((data #>> '{ethosCaseReference}'::text[])))) WHERE ((jurisdiction)::text = 'EMPLOYMENT'::text);


--
-- Name: uidx_case_data_external_id; Type: INDEX; Schema: public; Owner: ccd
--

CREATE UNIQUE INDEX uidx_case_data_external_id ON public.case_data USING btree (btrim(upper((data #>> '{externalId}'::text[])))) WHERE ((jurisdiction)::text = 'CMC'::text);


--
-- Name: case_data trg_case_data_updated; Type: TRIGGER; Schema: public; Owner: ccd
--

CREATE TRIGGER trg_case_data_updated BEFORE INSERT OR UPDATE OF data, data_classification, last_modified, last_state_modified_date, security_classification, state, supplementary_data ON public.case_data FOR EACH ROW EXECUTE PROCEDURE public.set_case_data_marked_by_logstash();


--
-- Name: case_event fk_case_event_case_data; Type: FK CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event
    ADD CONSTRAINT fk_case_event_case_data FOREIGN KEY (case_data_id) REFERENCES public.case_data(id);


--
-- Name: case_event_significant_items fk_case_event_items_case_event_id; Type: FK CONSTRAINT; Schema: public; Owner: ccd
--

ALTER TABLE ONLY public.case_event_significant_items
    ADD CONSTRAINT fk_case_event_items_case_event_id FOREIGN KEY (case_event_id) REFERENCES public.case_event(id);


--
-- PostgreSQL database dump complete
--


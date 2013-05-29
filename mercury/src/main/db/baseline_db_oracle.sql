
    drop table athena.billable_item cascade constraints;

    drop table athena.billable_item_aud cascade constraints;

    drop table athena.price_item cascade constraints;

    drop table athena.price_item_aud cascade constraints;

    drop table athena.product cascade constraints;

    drop table athena.product_add_ons cascade constraints;

    drop table athena.product_add_ons_aud cascade constraints;

    drop table athena.product_aud cascade constraints;

    drop table athena.product_family cascade constraints;

    drop table athena.product_family_aud cascade constraints;

    drop table athena.product_order cascade constraints;

    drop table athena.product_order_aud cascade constraints;

    drop table athena.product_order_sample cascade constraints;

    drop table athena.product_order_sample_aud cascade constraints;

    drop table athena.product_price_items cascade constraints;

    drop table athena.product_price_items_aud cascade constraints;

    drop table athena.project_person cascade constraints;

    drop table athena.project_person_aud cascade constraints;

    drop table athena.research_project cascade constraints;

    drop table athena.research_project_aud cascade constraints;

    drop table athena.research_project_cohort cascade constraints;

    drop table athena.research_project_cohort_aud cascade constraints;

    drop table athena.research_project_funding cascade constraints;

    drop table athena.research_project_funding_aud cascade constraints;

    drop table athena.research_projectirb cascade constraints;

    drop table athena.research_projectirb_aud cascade constraints;

    drop table athena.product_order_add_on_aud cascade constraints;

    drop table athena.product_order_add_on cascade constraints;



    drop table mercury.jira_ticket cascade constraints;

    drop table mercury.jira_ticket_aud cascade constraints;

    drop table mercury.lab_batch cascade constraints;

    drop table mercury.lab_batch_aud cascade constraints;

--     drop table mercury.lab_batch_starting_samples cascade constraints;
--
--     drop table mercury.lab_batch_starting_samples_aud cascade constraints;

    drop table mercury.lab_event cascade constraints;

    drop table mercury.lab_event_aud cascade constraints;

    drop table mercury.lab_event_reagents cascade constraints;

    drop table mercury.lab_event_reagents_aud cascade constraints;

    drop table mercury.lab_vessel cascade constraints;

    drop table mercury.lab_vessel_aud cascade constraints;

    drop table mercury.lab_vessel_containers cascade constraints;

    drop table mercury.lab_vessel_containers_aud cascade constraints;

    drop table mercury.lab_vessel_lab_batches cascade constraints;

    drop table mercury.lab_vessel_lab_batches_aud cascade constraints;

    drop table mercury.lab_vessel_notes cascade constraints;

    drop table mercury.lab_vessel_notes_aud cascade constraints;

    drop table mercury.lab_vessel_tickets_created cascade constraints;

    drop table mercury.lab_vessel_tickets_created_aud cascade constraints;

--     drop table mercury.lab_work_queue cascade constraints;
--
--     drop table mercury.lab_work_queue_aud cascade constraints;

    drop table mercury.lb_starting_lab_vessels cascade constraints;

    drop table mercury.lb_starting_lab_vessels_aud cascade constraints;

    drop table mercury.lv_map_position_to_vessel cascade constraints;

    drop table mercury.lv_map_position_to_vessel_aud cascade constraints;

    drop table mercury.lv_reagent_contents cascade constraints;

    drop table mercury.lv_reagent_contents_aud cascade constraints;

--     drop table mercury.molecular_envelope cascade constraints;
--
--     drop table mercury.molecular_envelope_aud cascade constraints;

    drop table mercury.molecular_index cascade constraints;

    drop table mercury.molecular_index_aud cascade constraints;

    drop table mercury.molecular_index_position cascade constraints;

    drop table mercury.molecular_index_position_aud cascade constraints;

    drop table mercury.molecular_indexing_scheme cascade constraints;

    drop table mercury.molecular_indexing_scheme_aud cascade constraints;

--     drop table mercury.molecular_state cascade constraints;
--
--     drop table mercury.molecular_state_aud cascade constraints;

--     drop table mercury.molecular_state_template cascade constraints;
--
--     drop table mercury.molecular_state_template_aud cascade constraints;

    drop table mercury.person cascade constraints;

    drop table mercury.person_aud cascade constraints;
/*
    drop table mercury.pp_map_start_smpl_to_aliqt cascade constraints;

    drop table mercury.pp_map_start_smpl_to_aliqt_aud cascade constraints;

    drop table mercury.pp_map_start_vssl_to_aliqt cascade constraints;

    drop table mercury.pp_map_start_vssl_to_aliqt_aud cascade constraints;

    drop table mercury.pp_starting_lab_vessels cascade constraints;

    drop table mercury.pp_starting_lab_vessels_aud cascade constraints;

    drop table mercury.pp_starting_samples cascade constraints;

    drop table mercury.pp_starting_samples_aud cascade constraints;

    drop table mercury.project cascade constraints;

    drop table mercury.project_aud cascade constraints;

    drop table mercury.project_plan cascade constraints;

    drop table mercury.project_plan_aud cascade constraints;
*/
    drop table mercury.quote cascade constraints;

    drop table mercury.quote_aud cascade constraints;

    drop table mercury.reagent cascade constraints;

    drop table mercury.reagent_aud cascade constraints;

    drop table mercury.reagent_containers cascade constraints;

    drop table mercury.reagent_containers_aud cascade constraints;

    drop table mercury.rev_info cascade constraints;

    drop table mercury.seq_run_run_cartridges cascade constraints;

    drop table mercury.seq_run_run_cartridges_aud cascade constraints;

    drop table mercury.sequencing_run cascade constraints;

    drop table mercury.sequencing_run_aud cascade constraints;

--     drop table mercury.starting_sample cascade constraints;
--
--     drop table mercury.starting_sample_aud cascade constraints;

--     drop table mercury.state_change cascade constraints;
--
--     drop table mercury.state_change_aud cascade constraints;

    drop table mercury.status_note cascade constraints;

    drop table mercury.status_note_aud cascade constraints;

    drop table mercury.vessel_transfer cascade constraints;

    drop table mercury.vessel_transfer_aud cascade constraints;

--     drop table mercury.workflow_description cascade constraints;
--
--     drop table mercury.workflow_description_aud cascade constraints;

    drop table mercury.lab_vessel_mercury_samples_aud cascade constraints;

    drop table mercury.lab_vessel_mercury_samples cascade constraints;

    drop table mercury.mercury_sample cascade constraints;

    drop table mercury.mercury_sample_aud cascade constraints;

    drop table mercury.bucket_entry cascade constraints;

    drop table mercury.bucket_entry_aud cascade constraints;

    drop table mercury.bucket cascade constraints;

    drop table mercury.bucket_aud cascade constraints;


/*
    drop table project_available_quotes cascade constraints;

    drop table project_available_quotes_aud cascade constraints;

    drop table project_available_work_qs cascade constraints;

    drop table project_available_work_qs_aud cascade constraints;

    drop table project_plan_jira_tickets cascade constraints;

    drop table project_plan_jira_tickets_aud cascade constraints;

    drop table project_project_plans cascade constraints;

    drop table project_project_plans_aud cascade constraints;
*/
    drop sequence athena.SEQ_BILLABLE_ITEM;

    drop sequence athena.SEQ_ORDER_SAMPLE;

    drop sequence athena.SEQ_PRICE_ITEM;

    drop sequence athena.SEQ_PRODUCT;

    drop sequence athena.SEQ_PRODUCT_FAMILY;

    drop sequence athena.SEQ_PRODUCT_ORDER;

    drop sequence athena.seq_project_person_index;

    drop sequence athena.seq_research_project_index;

    drop sequence athena.seq_rp_cohort_index;

    drop sequence athena.seq_rp_funding_index;

    drop sequence athena.seq_rp_irb_index;

    drop sequence athena.seq_order_add_on;

    drop sequence mercury.SEQ_LAB_BATCH;

    drop sequence mercury.SEQ_LAB_EVENT;

    drop sequence mercury.SEQ_LAB_VESSEL;

--    drop sequence mercury.SEQ_LAB_WORK_QUEUE;

--     drop sequence mercury.SEQ_MOLECULAR_ENVELOPE;
--
--     drop sequence mercury.SEQ_MOLECULAR_STATE;

--     drop sequence mercury.SEQ_MOLECULAR_STATE_TEMPLATE;

    drop sequence mercury.SEQ_PERSON;
/*
    drop sequence mercury.SEQ_PROJECT;

    drop sequence mercury.SEQ_PROJECT_PLAN;
*/
    drop sequence mercury.SEQ_REAGENT;

    drop sequence mercury.SEQ_REV_INFO;

    drop sequence mercury.SEQ_SEQUENCING_RUN;

--     drop sequence mercury.SEQ_STARTING_SAMPLE;

--     drop sequence mercury.SEQ_STATE_CHANGE;

    drop sequence mercury.SEQ_VESSEL_TRANSFER;

--     drop sequence mercury.SEQ_WORKFLOW_DESCRIPTION;

    drop sequence mercury.seq_molecular_index;

    drop sequence mercury.seq_molecular_indexing_scheme;

    drop sequence mercury.seq_mercury_sample;

    drop sequence mercury.seq_bucket;

    drop sequence mercury.seq_bucket_entry;


    create table athena.billable_item (
        billable_item_id number(19,0) not null,
        count number(19,2),
        price_item number(19,0),
        product_order_sample number(19,0),
        primary key (billable_item_id)
    );

    create table athena.billable_item_aud (
        billable_item_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        count number(19,2),
        price_item number(19,0),
        product_order_sample number(19,0),
        primary key (billable_item_id, rev)
    );

    create table athena.price_item (
        price_item_id number(19,0) not null,
        category varchar2(255 char),
        name varchar2(255 char),
        platform varchar2(255 char),
        quote_server_id varchar2(255 char),
        primary key (price_item_id),
        unique (platform, category, name)
    );

    create table athena.price_item_aud (
        price_item_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        category varchar2(255 char),
        name varchar2(255 char),
        platform varchar2(255 char),
        quote_server_id varchar2(255 char),
        primary key (price_item_id, rev)
    );

    create table athena.product (
        product_id number(19,0) not null,
        availability_date timestamp,
        deliverables varchar2(2000 char),
        description varchar2(2000 char),
        discontinued_date timestamp,
        expected_cycle_time_seconds number(10,0),
        guaranteed_cycle_time_seconds number(10,0),
        input_requirements varchar2(2000 char),
        part_number varchar2(255 char),
        product_name varchar2(255 char),
        samples_per_week number(10,0),
        top_level_product number(1,0) not null,
        workflow_name varchar2(255 char),
        default_price_item number(19,0),
        product_family number(19,0),
        primary key (product_id),
        unique (part_number)
    );

    create table athena.product_add_ons (
        product number(19,0) not null,
        add_ons number(19,0) not null,
        primary key (product, add_ons)
    );

    create table athena.product_add_ons_aud (
        rev number(19,0) not null,
        product number(19,0) not null,
        add_ons number(19,0) not null,
        revtype number(3,0),
        primary key (rev, product, add_ons)
    );

    create table athena.product_aud (
        product_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        availability_date timestamp,
        deliverables varchar2(2000 char),
        description varchar2(2000 char),
        discontinued_date timestamp,
        expected_cycle_time_seconds number(10,0),
        guaranteed_cycle_time_seconds number(10,0),
        input_requirements varchar2(2000 char),
        part_number varchar2(255 char),
        product_name varchar2(255 char),
        samples_per_week number(10,0),
        top_level_product number(1,0),
        workflow_name varchar2(255 char),
        default_price_item number(19,0),
        product_family number(19,0),
        primary key (product_id, rev)
    );

    create table athena.product_family (
        product_family_id number(19,0) not null,
        name varchar2(255 char),
        primary key (product_family_id),
        unique (name)
    );

    create table athena.product_family_aud (
        product_family_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        name varchar2(255 char),
        primary key (product_family_id, rev)
    );

    create table athena.product_order (
        product_order_id number(19,0) not null,
        comments varchar2(2000 char),
        jira_ticket_key varchar2(255 char),
        order_status number(10,0),
        quote_id varchar2(255 char),
        title varchar2(255 char) unique,
        product number(19,0),
        research_project number(19,0),
        primary key (product_order_id)
    );

    create table athena.product_order_aud (
        product_order_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        comments varchar2(2000 char),
        jira_ticket_key varchar2(255 char),
        order_status number(10,0),
        quote_id varchar2(255 char),
        title varchar2(255 char),
        product number(19,0),
        research_project number(19,0),
        primary key (product_order_id, rev)
    );

    create table athena.product_order_sample (
        product_order_sample_id number(19,0) not null,
        billing_status number(10,0),
        sample_comment varchar2(255 char),
        sample_name varchar2(255 char),
        product_order number(19,0),
        primary key (product_order_sample_id)
    );

    create table athena.product_order_sample_aud (
        product_order_sample_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        billing_status number(10,0),
        sample_comment varchar2(255 char),
        sample_name varchar2(255 char),
        product_order number(19,0),
        primary key (product_order_sample_id, rev)
    );

    create table athena.product_price_items (
        product number(19,0) not null,
        price_items number(19,0) not null,
        primary key (product, price_items)
    );

    create table athena.product_price_items_aud (
        rev number(19,0) not null,
        product number(19,0) not null,
        price_items number(19,0) not null,
        revtype number(3,0),
        primary key (rev, product, price_items)
    );

    create table athena.project_person (
        project_person_id number(19,0) not null,
        person_id number(19,0),
        role number(10,0),
        research_project number(19,0),
        primary key (project_person_id)
    );

    create table athena.project_person_aud (
        project_person_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        person_id number(19,0),
        role number(10,0),
        research_project number(19,0),
        primary key (project_person_id, rev)
    );

    create table athena.research_project (
        research_project_id number(19,0) not null,
        created_by number(19,0),
        created_date timestamp,
        irb_notes varchar2(255 char),
        jira_ticket_key varchar2(255 char),
        modified_by number(19,0),
        modified_date timestamp,
        status number(10,0),
        synopsis varchar2(255 char),
        title varchar2(255 char) unique,
        primary key (research_project_id)
    );

    create table athena.research_project_aud (
        research_project_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        created_by number(19,0),
        created_date timestamp,
        irb_notes varchar2(255 char),
        jira_ticket_key varchar2(255 char),
        modified_by number(19,0),
        modified_date timestamp,
        status number(10,0),
        synopsis varchar2(255 char),
        title varchar2(255 char),
        primary key (research_project_id, rev)
    );

    create table athena.research_project_cohort (
        research_project_cohort_id number(19,0) not null,
        cohort_id varchar2(255 char),
        research_project number(19,0),
        primary key (research_project_cohort_id)
    );

    create table athena.research_project_cohort_aud (
        research_project_cohort_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        cohort_id varchar2(255 char),
        research_project number(19,0),
        primary key (research_project_cohort_id, rev)
    );

    create table athena.research_project_funding (
        research_project_funding_id number(19,0) not null,
        funding_id varchar2(255 char),
        research_project number(19,0),
        primary key (research_project_funding_id)
    );

    create table athena.research_project_funding_aud (
        research_project_funding_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        funding_id varchar2(255 char),
        research_project number(19,0),
        primary key (research_project_funding_id, rev)
    );

    create table athena.research_projectirb (
        research_projectirbid number(19,0) not null,
        irb varchar2(255 char),
        irb_type number(10,0),
        research_project number(19,0),
        primary key (research_projectirbid)
    );

    create table athena.research_projectirb_aud (
        research_projectirbid number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        irb varchar2(255 char),
        irb_type number(10,0),
        research_project number(19,0),
        primary key (research_projectirbid, rev)
    );

    create table mercury.jira_ticket (
        ticket_id varchar2(255 char) not null,
        browser_url varchar2(255 char),
        ticket_name varchar2(255 char),
        lab_batch number(19,0),
        primary key (ticket_id)
    );

    create table mercury.jira_ticket_aud (
        ticket_id varchar2(255 char) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        browser_url varchar2(255 char),
        ticket_name varchar2(255 char),
        lab_batch number(19,0),
        primary key (ticket_id, rev)
    );

    create table mercury.lab_batch (
        lab_batch_id number(19,0) not null,
        batch_name varchar2(255 char),
        is_active number(1,0) not null,
        jira_ticket varchar2(255 char),
--        project_plan number(19,0),
        primary key (lab_batch_id),
        unique (batch_name)
    );

    create table mercury.lab_batch_aud (
        lab_batch_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        batch_name varchar2(255 char),
        is_active number(1,0),
        jira_ticket varchar2(255 char),
--        project_plan number(19,0),
        primary key (lab_batch_id, rev)
    );

--     create table mercury.lab_batch_starting_samples (
--         lab_batch number(19,0) not null,
--         starting_samples number(19,0) not null,
--         primary key (lab_batch, starting_samples)
--     );
--
--     create table mercury.lab_batch_starting_samples_aud (
--         rev number(19,0) not null,
--         lab_batch number(19,0) not null,
--         starting_samples number(19,0) not null,
--         revtype number(3,0),
--         primary key (rev, lab_batch, starting_samples)
--     );

    create table mercury.lab_event (
        dtype varchar2(31 char) not null,
        lab_event_id number(19,0) not null,
        disambiguator number(19,0),
        event_date timestamp,
        event_location varchar2(255 char),
        quote_server_batch_id varchar2(255 char),
        lab_event_type varchar2(255 char),
        event_operator number(19,0),
        in_place_lab_vessel number(19,0),
--        project_plan_override number(19,0),
        lab_batch number(19,0),
        primary key (lab_event_id),
        unique (event_location, event_date, disambiguator)
    );

    create table mercury.lab_event_aud (
        dtype varchar2(31 char) not null,
        lab_event_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        disambiguator number(19,0),
        event_date timestamp,
        event_location varchar2(255 char),
        quote_server_batch_id varchar2(255 char),
        event_operator number(19,0),
        in_place_lab_vessel number(19,0),
--        project_plan_override number(19,0),
        lab_event_type varchar2(255 char),
        lab_batch number(19,0),
        primary key (lab_event_id, rev)
    );

    create table mercury.lab_event_reagents (
        lab_event number(19,0) not null,
        reagents number(19,0) not null,
        primary key (lab_event, reagents)
    );

    create table mercury.lab_event_reagents_aud (
        rev number(19,0) not null,
        lab_event number(19,0) not null,
        reagents number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_event, reagents)
    );

    create table mercury.lab_vessel (
        dtype varchar2(31 char) not null,
        lab_vessel_id number(19,0) not null,
        created_on timestamp,
        label varchar2(255 char),
        plate_type varchar2(255 char),
        flowcell_barcode varchar2(255 char),
        flowcell_type varchar2(255 char),
        vessel_position varchar2(255 char),
        digest varchar2(255 char),
        rack_type varchar2(255 char),
--         molecular_state number(19,0),
--        project number(19,0),
--        project_authority number(19,0),
--         read_bucket_authority number(19,0),
--         aliquot number(19,0),
        plate number(19,0),
        primary key (lab_vessel_id),
        unique (label)
    );

    create table mercury.lab_vessel_aud (
        dtype varchar2(31 char) not null,
        lab_vessel_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        created_on timestamp,
        label varchar2(255 char),
--         molecular_state number(19,0),
--        project number(19,0),
--        project_authority number(19,0),
--         read_bucket_authority number(19,0),
        digest varchar2(255 char),
        rack_type varchar2(255 char),
--         aliquot number(19,0),
        vessel_position varchar2(255 char),
        plate number(19,0),
        plate_type varchar2(255 char),
        flowcell_barcode varchar2(255 char),
        flowcell_type varchar2(255 char),
        primary key (lab_vessel_id, rev)
    );

    create table mercury.lab_vessel_containers (
        lab_vessel number(19,0) not null,
        containers number(19,0) not null,
        primary key (lab_vessel, containers)
    );

    create table mercury.lab_vessel_containers_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        containers number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, containers)
    );

    create table mercury.lab_vessel_lab_batches (
        lab_vessel number(19,0) not null,
        lab_batches number(19,0) not null,
        primary key (lab_vessel, lab_batches)
    );

    create table mercury.lab_vessel_lab_batches_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        lab_batches number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, lab_batches)
    );

    create table mercury.lab_vessel_notes (
        lab_vessel number(19,0) not null,
        notes number(19,0) not null,
        unique (notes)
    );

    create table mercury.lab_vessel_notes_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        notes number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, notes)
    );

    create table mercury.lab_vessel_tickets_created (
        lab_vessel number(19,0) not null,
        tickets_created varchar2(255 char) not null,
        primary key (lab_vessel, tickets_created),
        unique (tickets_created)
    );

    create table mercury.lab_vessel_tickets_created_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        tickets_created varchar2(255 char) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, tickets_created)
    );

--     create table mercury.lab_work_queue (
--         lab_work_queue_id number(19,0) not null,
--         primary key (lab_work_queue_id)
--     );
--
--     create table mercury.lab_work_queue_aud (
--         lab_work_queue_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         primary key (lab_work_queue_id, rev)
--     );

    create table mercury.lb_starting_lab_vessels (
        lab_batch number(19,0) not null,
        starting_lab_vessels number(19,0) not null,
        primary key (lab_batch, starting_lab_vessels)
    );

    create table mercury.lb_starting_lab_vessels_aud (
        rev number(19,0) not null,
        lab_batch number(19,0) not null,
        starting_lab_vessels number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_batch, starting_lab_vessels)
    );

    create table mercury.lv_map_position_to_vessel (
        lab_vessel number(19,0) not null,
        map_position_to_vessel number(19,0) not null,
        mapkey varchar2(255 char) not null,
        primary key (lab_vessel, mapkey)
    );

    create table mercury.lv_map_position_to_vessel_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        map_position_to_vessel number(19,0) not null,
        mapkey varchar2(255 char) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, map_position_to_vessel, mapkey)
    );

    create table mercury.lv_reagent_contents (
        lab_vessel number(19,0) not null,
        reagent_contents number(19,0) not null,
        primary key (lab_vessel, reagent_contents)
    );

    create table mercury.lv_reagent_contents_aud (
        rev number(19,0) not null,
        lab_vessel number(19,0) not null,
        reagent_contents number(19,0) not null,
        revtype number(3,0),
        primary key (rev, lab_vessel, reagent_contents)
    );

--     create table mercury.molecular_envelope (
--         dtype varchar2(31 char) not null,
--         molecular_envelope_id number(19,0) not null,
--         five_prime_seq varchar2(255 char),
--         name varchar2(255 char),
--         three_prime_seq varchar2(255 char),
--         primary key (molecular_envelope_id)
--     );
--
--     create table mercury.molecular_envelope_aud (
--         dtype varchar2(31 char) not null,
--         molecular_envelope_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         five_prime_seq varchar2(255 char),
--         name varchar2(255 char),
--         three_prime_seq varchar2(255 char),
--         primary key (molecular_envelope_id, rev)
--     );

    create table mercury.molecular_index (
        molecular_index_id number(19,0) not null,
        sequence varchar2(255 char),
        primary key (molecular_index_id)
    );

    create table mercury.molecular_index_aud (
        molecular_index_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        sequence varchar2(255 char),
        primary key (molecular_index_id, rev)
    );

    create table mercury.molecular_index_position (
        scheme_id number(19,0) not null,
        index_id number(19,0) not null,
        mapkey varchar2(255 char) not null,
        primary key (scheme_id, mapkey)
    );

    create table mercury.molecular_index_position_aud (
        rev number(19,0) not null,
        scheme_id number(19,0) not null,
        index_id number(19,0) not null,
        mapkey varchar2(255 char) not null,
        revtype number(3,0),
        primary key (rev, scheme_id, index_id, mapkey)
    );

    create table mercury.molecular_indexing_scheme (
        molecular_indexing_scheme_id number(19,0) not null,
        name varchar2(255 char),
        primary key (molecular_indexing_scheme_id)
    );

    create table mercury.molecular_indexing_scheme_aud (
        molecular_indexing_scheme_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        name varchar2(255 char),
        primary key (molecular_indexing_scheme_id, rev)
    );

--     create table mercury.molecular_state (
--         molecular_state_id number(19,0) not null,
--         nucleic_acid_state varchar2(255 char),
--         strand varchar2(255 char),
--         molecular_envelope number(19,0),
--         molecular_state_template number(19,0),
--         primary key (molecular_state_id)
--     );
--
--     create table mercury.molecular_state_aud (
--         molecular_state_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         nucleic_acid_state varchar2(255 char),
--         strand varchar2(255 char),
--         molecular_envelope number(19,0),
--         molecular_state_template number(19,0),
--         primary key (molecular_state_id, rev)
--     );

--     create table mercury.molecular_state_template (
--         molecular_state_template_id number(19,0) not null,
--         primary key (molecular_state_template_id)
--     );
--
--     create table mercury.molecular_state_template_aud (
--         molecular_state_template_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         primary key (molecular_state_template_id, rev)
--     );

    create table mercury.person (
        person_id number(19,0) not null,
        first_name varchar2(255 char),
        last_name varchar2(255 char),
        username varchar2(255 char),
        primary key (person_id)
    );

    create table mercury.person_aud (
        person_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        first_name varchar2(255 char),
        last_name varchar2(255 char),
        username varchar2(255 char),
        primary key (person_id, rev)
    );
/*
    create table mercury.pp_map_start_smpl_to_aliqt (
        project_plan number(19,0) not null,
        map_starting_sample_to_aliquot number(19,0) not null,
        mapkey number(19,0) not null,
        primary key (project_plan, mapkey)
    );

    create table mercury.pp_map_start_smpl_to_aliqt_aud (
        rev number(19,0) not null,
        project_plan number(19,0) not null,
        map_starting_sample_to_aliquot number(19,0) not null,
        mapkey number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project_plan, map_starting_sample_to_aliquot, mapkey)
    );

    create table mercury.pp_map_start_vssl_to_aliqt (
        project_plan number(19,0) not null,
        map_starting_vessel_to_aliquot number(19,0) not null,
        mapkey number(19,0) not null,
        primary key (project_plan, mapkey)
    );

    create table mercury.pp_map_start_vssl_to_aliqt_aud (
        rev number(19,0) not null,
        project_plan number(19,0) not null,
        map_starting_vessel_to_aliquot number(19,0) not null,
        mapkey number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project_plan, map_starting_vessel_to_aliquot, mapkey)
    );

    create table mercury.pp_starting_lab_vessels (
        project_plan number(19,0) not null,
        starting_lab_vessels number(19,0) not null,
        primary key (project_plan, starting_lab_vessels)
    );

    create table mercury.pp_starting_lab_vessels_aud (
        rev number(19,0) not null,
        project_plan number(19,0) not null,
        starting_lab_vessels number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project_plan, starting_lab_vessels)
    );

    create table mercury.pp_starting_samples (
        project_plan number(19,0) not null,
        starting_samples number(19,0) not null,
        primary key (project_plan, starting_samples)
    );

    create table mercury.pp_starting_samples_aud (
        rev number(19,0) not null,
        project_plan number(19,0) not null,
        starting_samples number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project_plan, starting_samples)
    );

    create table mercury.project (
        dtype varchar2(31 char) not null,
        project_id number(19,0) not null,
        active number(1,0) not null,
        project_name varchar2(255 char),
        jira_ticket varchar2(255 char),
        platform_owner number(19,0),
        primary key (project_id)
    );

    create table mercury.project_aud (
        dtype varchar2(31 char) not null,
        project_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        active number(1,0),
        project_name varchar2(255 char),
        jira_ticket varchar2(255 char),
        platform_owner number(19,0),
        primary key (project_id, rev)
    );

    create table mercury.project_plan (
        dtype varchar2(31 char) not null,
        project_plan_id number(19,0) not null,
        notes varchar2(255 char),
        plan_name varchar2(255 char),
        project number(19,0),
        workflow_description number(19,0),
        quote varchar2(255 char),
        primary key (project_plan_id)
    );

    create table mercury.project_plan_aud (
        dtype varchar2(31 char) not null,
        project_plan_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        project number(19,0),
        workflow_description number(19,0),
        notes varchar2(255 char),
        plan_name varchar2(255 char),
        quote varchar2(255 char),
        primary key (project_plan_id, rev)
    );
*/
    create table mercury.quote (
        alphanumeric_id varchar2(255 char) not null,
        primary key (alphanumeric_id)
    );

    create table mercury.quote_aud (
        alphanumeric_id varchar2(255 char) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        primary key (alphanumeric_id, rev)
    );

    create table mercury.reagent (
        dtype varchar2(31 char) not null,
        reagent_id number(19,0) not null,
        lot varchar2(255 char),
        reagent_name varchar2(255 char),
--         molecular_envelope number(19,0),
        molecular_indexing_scheme number(19,0),
        primary key (reagent_id)
    );

    create table mercury.reagent_aud (
        dtype varchar2(31 char) not null,
        reagent_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        lot varchar2(255 char),
        reagent_name varchar2(255 char),
--         molecular_envelope number(19,0),
        molecular_indexing_scheme number(19,0),
        primary key (reagent_id, rev)
    );

    create table mercury.reagent_containers (
        reagent number(19,0) not null,
        containers number(19,0) not null,
        primary key (reagent, containers)
    );

    create table mercury.reagent_containers_aud (
        rev number(19,0) not null,
        reagent number(19,0) not null,
        containers number(19,0) not null,
        revtype number(3,0),
        primary key (rev, reagent, containers)
    );

    create table mercury.rev_info (
        rev_info_id number(19,0) not null,
        rev_date timestamp,
        username varchar2(255 char),
        primary key (rev_info_id)
    );

    create table mercury.seq_run_run_cartridges (
        sequencing_run number(19,0) not null,
        run_cartridges number(19,0) not null,
        primary key (sequencing_run, run_cartridges),
        unique (run_cartridges)
    );

    create table mercury.seq_run_run_cartridges_aud (
        rev number(19,0) not null,
        sequencing_run number(19,0) not null,
        run_cartridges number(19,0) not null,
        revtype number(3,0),
        primary key (rev, sequencing_run, run_cartridges)
    );

    create table mercury.sequencing_run (
        dtype varchar2(31 char) not null,
        sequencing_run_id number(19,0) not null,
        machine_name varchar2(255 char),
        run_barcode varchar2(255 char),
        run_date timestamp,
        run_name varchar2(255 char),
        test_run number(1,0),
        operator number(19,0),
        primary key (sequencing_run_id)
    );

    create table mercury.sequencing_run_aud (
        dtype varchar2(31 char) not null,
        sequencing_run_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        machine_name varchar2(255 char),
        run_barcode varchar2(255 char),
        run_date timestamp,
        run_name varchar2(255 char),
        test_run number(1,0),
        operator number(19,0),
        primary key (sequencing_run_id, rev)
    );

--     create table mercury.starting_sample (
--         dtype varchar2(31 char) not null,
--         sample_id number(19,0) not null,
--         sample_name varchar2(255 char),
--         bsp_sample_authority_twodtube number(19,0),
-- --        project_plan number(19,0),
--         primary key (sample_id)
--     );
--
--     create table mercury.starting_sample_aud (
--         dtype varchar2(31 char) not null,
--         sample_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         sample_name varchar2(255 char),
--         bsp_sample_authority_twodtube number(19,0),
-- --        project_plan number(19,0),
--         primary key (sample_id, rev)
--     );

--     create table mercury.state_change (
--         state_change_id number(19,0) not null,
--         primary key (state_change_id)
--     );
--
--     create table mercury.state_change_aud (
--         state_change_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         primary key (state_change_id, rev)
--     );

    create table mercury.status_note (
        status_note_id number(19,0) not null,
        event_name number(10,0),
        note_date timestamp,
        primary key (status_note_id)
    );

    create table mercury.status_note_aud (
        status_note_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        event_name number(10,0),
        note_date timestamp,
        primary key (status_note_id, rev)
    );

    create table mercury.vessel_transfer (
        dtype varchar2(31 char) not null,
        vessel_transfer_id number(19,0) not null,
        source_section varchar2(255 char),
        target_section varchar2(255 char),
        source_position varchar2(255 char),
        target_position varchar2(255 char),
        lab_event number(19,0),
        source_vessel number(19,0),
        target_vessel number(19,0),
        target_lab_vessel number(19,0),
        primary key (vessel_transfer_id)
    );

    create table mercury.vessel_transfer_aud (
        dtype varchar2(31 char) not null,
        vessel_transfer_id number(19,0) not null,
        rev number(19,0) not null,
        revtype number(3,0),
        source_position varchar2(255 char),
        target_position varchar2(255 char),
        lab_event number(19,0),
        source_vessel number(19,0),
        target_vessel number(19,0),
        target_section varchar2(255 char),
        source_section varchar2(255 char),
        target_lab_vessel number(19,0),
        primary key (vessel_transfer_id, rev)
    );

--     create table mercury.workflow_description (
--         workflow_description_id number(19,0) not null,
--         issue_type number(10,0),
--         workflow_name varchar2(255 char),
--         primary key (workflow_description_id)
--     );
--
--     create table mercury.workflow_description_aud (
--         workflow_description_id number(19,0) not null,
--         rev number(19,0) not null,
--         revtype number(3,0),
--         issue_type number(10,0),
--         workflow_name varchar2(255 char),
--         primary key (workflow_description_id, rev)
--     );
/*
    create table project_available_quotes (
        project number(19,0) not null,
        available_quotes varchar2(255 char) not null,
        unique (available_quotes)
    );

    create table project_available_quotes_aud (
        rev number(19,0) not null,
        project number(19,0) not null,
        available_quotes varchar2(255 char) not null,
        revtype number(3,0),
        primary key (rev, project, available_quotes)
    );

    create table project_available_work_qs (
        project number(19,0) not null,
        available_work_queues number(19,0) not null,
        unique (available_work_queues)
    );

    create table project_available_work_qs_aud (
        rev number(19,0) not null,
        project number(19,0) not null,
        available_work_queues number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project, available_work_queues)
    );

    create table project_plan_jira_tickets (
        project_plan number(19,0) not null,
        jira_tickets varchar2(255 char) not null,
        unique (jira_tickets)
    );

    create table project_plan_jira_tickets_aud (
        rev number(19,0) not null,
        project_plan number(19,0) not null,
        jira_tickets varchar2(255 char) not null,
        revtype number(3,0),
        primary key (rev, project_plan, jira_tickets)
    );

    create table project_project_plans (
        project number(19,0) not null,
        project_plans number(19,0) not null,
        unique (project_plans)
    );

    create table project_project_plans_aud (
        rev number(19,0) not null,
        project number(19,0) not null,
        project_plans number(19,0) not null,
        revtype number(3,0),
        primary key (rev, project, project_plans)
    );
*/
    alter table athena.billable_item
        add constraint FK4A845AB199F23B52
        foreign key (product_order_sample)
        references athena.product_order_sample;

    alter table athena.billable_item
        add constraint FK4A845AB19C899366
        foreign key (price_item)
        references athena.price_item;

    alter table athena.billable_item_aud
        add constraint FK66B244228A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.price_item_aud
        add constraint FK704245BA8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product
        add constraint FKED8DCCEFD454B488
        foreign key (product_family)
        references athena.product_family;

    alter table athena.product
        add constraint FKED8DCCEFBBCD5024
        foreign key (default_price_item)
        references athena.price_item;

    alter table athena.product_add_ons
        add constraint FK4D043286D5B81626
        foreign key (add_ons)
        references athena.product;

    alter table athena.product_add_ons
        add constraint FK4D0432867B6C53F
        foreign key (product)
        references athena.product;

    alter table athena.product_add_ons_aud
        add constraint FKF32829778A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product_aud
        add constraint FKA71C67608A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product_family_aud
        add constraint FK377320A58A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product_order
        add constraint FK526407FE89257C13
        foreign key (research_project)
        references athena.research_project;

    alter table athena.product_order
        add constraint FK526407FE7B6C53F
        foreign key (product)
        references athena.product;

    alter table athena.product_order_aud
        add constraint FKF13082EF8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product_order_sample
        add constraint FK99F8110B3D5D323B
        foreign key (product_order)
        references athena.product_order;

    alter table athena.product_order_sample_aud
        add constraint FKCE4F3D7C8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.product_price_items
        add constraint FK62535DFA639CCC67
        foreign key (price_items)
        references athena.price_item;

    alter table athena.product_price_items
        add constraint FK62535DFA7B6C53F
        foreign key (product)
        references athena.product;

    alter table athena.product_price_items_aud
        add constraint FK2D4C7AEB8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.project_person
        add constraint FK33E64F3B89257C13
        foreign key (research_project)
        references athena.research_project;

    alter table athena.project_person_aud
        add constraint FK2D92E3AC8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.research_project_aud
        add constraint FKC7BD4FE68A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.research_project_cohort
        add constraint FKEA8D82FF89257C13
        foreign key (research_project)
        references athena.research_project;

    alter table athena.research_project_cohort_aud
        add constraint FK123295708A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.research_project_funding
        add constraint FK10620D3389257C13
        foreign key (research_project)
        references athena.research_project;

    alter table athena.research_project_funding_aud
        add constraint FKC98EE5A48A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table athena.research_projectirb
        add constraint FK2779DFC489257C13
        foreign key (research_project)
        references athena.research_project;

    alter table athena.research_projectirb_aud
        add constraint FK146907B58A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.jira_ticket
        add constraint FKA8F540BDA1B8F5BF
        foreign key (lab_batch)
        references mercury.lab_batch;

    alter table mercury.jira_ticket_aud
        add constraint FKB6E9442E8A39BE24
        foreign key (rev)
        references mercury.rev_info;
/*
    alter table mercury.lab_batch
        add constraint FKD102BE085DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;
*/
    alter table mercury.lab_batch
        add constraint FKD102BE084334B1F1
        foreign key (jira_ticket)
        references mercury.jira_ticket;

    alter table mercury.lab_batch_aud
        add constraint FKF61123F98A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.lab_batch_starting_samples
--         add constraint FK6D18FC61A1B8F5BF
--         foreign key (lab_batch)
--         references mercury.lab_batch;
--
--     alter table mercury.lab_batch_starting_samples
--         add constraint FK6D18FC61AD133513
--         foreign key (starting_samples)
--         references mercury.starting_sample;
--
--     alter table mercury.lab_batch_starting_samples_aud
--         add constraint FK5BE8DD28A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

    alter table mercury.lab_event
        add constraint FKD1365968A1B8F5BF
        foreign key (lab_batch)
        references mercury.lab_batch;
/*
    alter table mercury.lab_event
        add constraint FKD1365968A12D5FAA
        foreign key (project_plan_override)
        references mercury.project_plan;
*/
    alter table mercury.lab_event
        add constraint FKD13659683B570EF2
        foreign key (event_operator)
        references mercury.person;

    alter table mercury.lab_event
        add constraint FKD13659685E7EBB8A
        foreign key (in_place_lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lab_event_aud
        add constraint FK32480F598A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lab_event_reagents
        add constraint FKC39566F8387730B2
        foreign key (reagents)
        references mercury.reagent;

    alter table mercury.lab_event_reagents
        add constraint FKC39566F82C83FED1
        foreign key (lab_event)
        references mercury.lab_event;

    alter table mercury.lab_event_reagents_aud
        add constraint FK9C45D4E98A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.lab_vessel
--         add constraint FK71AE15281149B707
--         foreign key (read_bucket_authority)
--         references mercury.lab_vessel;
/*
    alter table mercury.lab_vessel
        add constraint FK71AE15286E580798
        foreign key (project)
        references mercury.project;
*/
--     alter table mercury.lab_vessel
--         add constraint FK71AE152876284BB0
--         foreign key (aliquot)
--         references mercury.starting_sample;

    alter table mercury.lab_vessel
        add constraint FK71AE1528A095034B
        foreign key (plate)
        references mercury.lab_vessel;

--     alter table mercury.lab_vessel
--         add constraint FK71AE15286DE9BBF6
--         foreign key (molecular_state)
--         references mercury.molecular_state;
/*
    alter table mercury.lab_vessel
        add constraint FK71AE15283BA3A1ED
        foreign key (project_authority)
        references mercury.lab_vessel;
*/
    alter table mercury.lab_vessel_aud
        add constraint FK14FBEB198A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lab_vessel_containers
        add constraint FK656B3B8980D2E462
        foreign key (containers)
        references mercury.lab_vessel;

    alter table mercury.lab_vessel_containers
        add constraint FK656B3B89E9D8B578
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lab_vessel_containers_aud
        add constraint FKFF0CF8FA8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lab_vessel_lab_batches
        add constraint FKD6E7601F6C01A06D
        foreign key (lab_batches)
        references mercury.lab_batch;

    alter table mercury.lab_vessel_lab_batches
        add constraint FKD6E7601FE9D8B578
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lab_vessel_lab_batches_aud
        add constraint FK5618E2908A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lab_vessel_notes
        add constraint FKC66248EAF2891A7B
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lab_vessel_notes
        add constraint FKC66248EAE78C6356
        foreign key (notes)
        references mercury.status_note;

    alter table mercury.lab_vessel_notes_aud
        add constraint FKC90C6DDB8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lab_vessel_tickets_created
        add constraint FK37FD60793DACB6C4
        foreign key (tickets_created)
        references mercury.jira_ticket;

    alter table mercury.lab_vessel_tickets_created
        add constraint FK37FD6079E9D8B578
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lab_vessel_tickets_created_aud
        add constraint FK412325EA8A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.lab_work_queue_aud
--         add constraint FK815E93C68A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

    alter table mercury.lb_starting_lab_vessels
        add constraint FKD2D0A1F5A1B8F5BF
        foreign key (lab_batch)
        references mercury.lab_batch;

    alter table mercury.lb_starting_lab_vessels
        add constraint FKD2D0A1F5D21A0F1C
        foreign key (starting_lab_vessels)
        references mercury.lab_vessel;

    alter table mercury.lb_starting_lab_vessels_aud
        add constraint FKD54149668A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lv_map_position_to_vessel
        add constraint FK226C96FCBE63D5D
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_map_position_to_vessel
        add constraint FK226C96FCDB8A2CBB
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_map_position_to_vessel
        add constraint FK226C96FC7D256C16
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_map_position_to_vessel
        add constraint FK226C96FC1CCACD11
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_map_position_to_vessel
        add constraint FK226C96FC251700F7
        foreign key (map_position_to_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_map_position_to_vessel_aud
        add constraint FK5C3D62ED8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.lv_reagent_contents
        add constraint FK58A6721C76784EB8
        foreign key (reagent_contents)
        references mercury.reagent;

    alter table mercury.lv_reagent_contents
        add constraint FK58A6721CE9D8B578
        foreign key (lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.lv_reagent_contents_aud
        add constraint FKDDD4AE0D8A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.molecular_envelope_aud
--         add constraint FK6CC133708A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

    alter table mercury.molecular_index_aud
        add constraint FK1BCC2E748A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.molecular_index_position
        add constraint FKA0F59FE5725A93D9
        foreign key (scheme_id)
        references mercury.molecular_indexing_scheme;

    alter table mercury.molecular_index_position
        add constraint FKA0F59FE5C0F3394B
        foreign key (index_id)
        references mercury.molecular_index;

    alter table mercury.molecular_index_position_aud
        add constraint FKEBD5CF568A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.molecular_indexing_scheme_aud
        add constraint FKDD2EDE568A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.molecular_state
--         add constraint FK3453D9C2218B0C4C
--         foreign key (molecular_envelope)
--         references mercury.molecular_envelope;

--     alter table mercury.molecular_state
--         add constraint FK3453D9C221C79D25
--         foreign key (molecular_state_template)
--         references mercury.molecular_state_template;

--     alter table mercury.molecular_state_aud
--         add constraint FKCF99D2B38A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

--     alter table mercury.molecular_state_template_aud
--         add constraint FK1CFFA4C88A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

    alter table mercury.person_aud
        add constraint FK287892C68A39BE24
        foreign key (rev)
        references mercury.rev_info;
/*
    alter table mercury.pp_map_start_smpl_to_aliqt
        add constraint FK7FAEB807A5892C6C
        foreign key (map_starting_sample_to_aliquot)
        references mercury.lab_vessel;

    alter table mercury.pp_map_start_smpl_to_aliqt
        add constraint FK7FAEB8075DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;

    alter table mercury.pp_map_start_smpl_to_aliqt
        add constraint FK7FAEB8076C2F598C
        foreign key (mapkey)
        references mercury.starting_sample;

    alter table mercury.pp_map_start_smpl_to_aliqt_aud
        add constraint FK1A2206788A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.pp_map_start_vssl_to_aliqt
        add constraint FK380FC4078ED27060
        foreign key (map_starting_vessel_to_aliquot)
        references mercury.lab_vessel;

    alter table mercury.pp_map_start_vssl_to_aliqt
        add constraint FK380FC4075DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;

    alter table mercury.pp_map_start_vssl_to_aliqt
        add constraint FK380FC40737B64BD3
        foreign key (mapkey)
        references mercury.lab_vessel;

    alter table mercury.pp_map_start_vssl_to_aliqt_aud
        add constraint FK631D12788A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.pp_starting_lab_vessels
        add constraint FK56E73BABD21A0F1C
        foreign key (starting_lab_vessels)
        references mercury.lab_vessel;

    alter table mercury.pp_starting_lab_vessels
        add constraint FK56E73BAB5DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;

    alter table mercury.pp_starting_lab_vessels_aud
        add constraint FKD468181C8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.pp_starting_samples
        add constraint FKE9D69B695DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;

    alter table mercury.pp_starting_samples
        add constraint FKE9D69B69AD133513
        foreign key (starting_samples)
        references mercury.starting_sample;

    alter table mercury.pp_starting_samples_aud
        add constraint FK9D0568DA8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.project
        add constraint FKED904B19742C3630
        foreign key (platform_owner)
        references mercury.person;

    alter table mercury.project
        add constraint FKED904B194334B1F1
        foreign key (jira_ticket)
        references mercury.jira_ticket;

    alter table mercury.project_aud
        add constraint FKC7FFC08A8A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.project_plan
        add constraint FK37FF008F95D0F29F
        foreign key (workflow_description)
        references mercury.workflow_description;

    alter table mercury.project_plan
        add constraint FK37FF008F6E580798
        foreign key (project)
        references mercury.project;

    alter table mercury.project_plan
        add constraint FK37FF008F937855C0
        foreign key (quote)
        references mercury.quote;

    alter table mercury.project_plan_aud
        add constraint FK288ACB008A39BE24
        foreign key (rev)
        references mercury.rev_info;
*/
    alter table mercury.quote_aud
        add constraint FKA7A04A6D8A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.reagent
--         add constraint FK40671CB2218B0C4C
--         foreign key (molecular_envelope)
--         references mercury.molecular_envelope;

    alter table mercury.reagent
        add constraint FK40671CB2AA5A4C9
        foreign key (molecular_indexing_scheme)
        references mercury.molecular_indexing_scheme;

    alter table mercury.reagent_aud
        add constraint FK49721DA38A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.reagent_containers
        add constraint FK5A5AF9BFF58850AE
        foreign key (reagent)
        references mercury.reagent;

    alter table mercury.reagent_containers
        add constraint FK5A5AF9BF80D2E462
        foreign key (containers)
        references mercury.lab_vessel;

    alter table mercury.reagent_containers_aud
        add constraint FK5CE6AC308A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.seq_run_run_cartridges
        add constraint FK720E6C90B450EC64
        foreign key (run_cartridges)
        references mercury.lab_vessel;

    alter table mercury.seq_run_run_cartridges
        add constraint FK720E6C907CA79ECF
        foreign key (sequencing_run)
        references mercury.sequencing_run;

    alter table mercury.seq_run_run_cartridges_aud
        add constraint FKB5124E818A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table mercury.sequencing_run
        add constraint FKB8B4430A746A02ED
        foreign key (operator)
        references mercury.person;

    alter table mercury.sequencing_run_aud
        add constraint FKEF9457FB8A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.starting_sample
--         add constraint FK18C9CA8979B8E476
--         foreign key (bsp_sample_authority_twodtube)
--         references mercury.lab_vessel;
/*
    alter table mercury.starting_sample
        add constraint FK18C9CA895DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;
*/
--     alter table mercury.starting_sample_aud
--         add constraint FKB28C07FA8A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

--     alter table mercury.state_change_aud
--         add constraint FK7F42220F8A39BE24
--         foreign key (rev)
--         references mercury.rev_info;

    alter table mercury.status_note_aud
        add constraint FKBF6042908A39BE24
        foreign key (rev)
        references mercury.rev_info;

    create index ix_vtvt_lab_event on mercury.vessel_transfer (lab_event);

    create index ix_vtst_lab_event on mercury.vessel_transfer (lab_event);

    create index ix_st_lab_event on mercury.vessel_transfer (lab_event);

    create index ix_cpt_lab_event on mercury.vessel_transfer (lab_event);

    alter table mercury.vessel_transfer
        add constraint FK4040E3D441C8B08D
        foreign key (target_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4A2234F92
        foreign key (target_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4881037
        foreign key (target_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D430E420D9
        foreign key (target_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4ED698F4
        foreign key (target_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4BE182AC3
        foreign key (source_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D41E72C9C8
        foreign key (source_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D47CD78A6D
        foreign key (source_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4AD339B0F
        foreign key (source_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D4AA16A4E6
        foreign key (target_lab_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D48B26132A
        foreign key (source_vessel)
        references mercury.lab_vessel;

    alter table mercury.vessel_transfer
        add constraint FK4040E3D42C83FED1
        foreign key (lab_event)
        references mercury.lab_event;

    alter table mercury.vessel_transfer_aud
        add constraint FKAAEF83C58A39BE24
        foreign key (rev)
        references mercury.rev_info;

--     alter table mercury.workflow_description_aud
--         add constraint FK34339F6D8A39BE24
--         foreign key (rev)
--         references mercury.rev_info;
/*
    alter table project_available_quotes
        add constraint FK2E7B8C136E580798
        foreign key (project)
        references mercury.project;

    alter table project_available_quotes
        add constraint FK2E7B8C13F88D16F1
        foreign key (available_quotes)
        references mercury.quote;

    alter table project_available_quotes_aud
        add constraint FK2F0DF4848A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table project_available_work_qs
        add constraint FKD4445EF49697300F
        foreign key (available_work_queues)
        references mercury.lab_work_queue;

    alter table project_available_work_qs
        add constraint FKD4445EF46E580798
        foreign key (project)
        references mercury.project;

    alter table project_available_work_qs_aud
        add constraint FK4C806EE58A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table project_plan_jira_tickets
        add constraint FK2196E106FF2488A
        foreign key (jira_tickets)
        references mercury.jira_ticket;

    alter table project_plan_jira_tickets
        add constraint FK2196E1065DAC64D7
        foreign key (project_plan)
        references mercury.project_plan;

    alter table project_plan_jira_tickets_aud
        add constraint FKC9BF97F78A39BE24
        foreign key (rev)
        references mercury.rev_info;

    alter table project_project_plans
        add constraint FK994D4F1E6E580798
        foreign key (project)
        references mercury.project;

    alter table project_project_plans
        add constraint FK994D4F1EED8E760C
        foreign key (project_plans)
        references mercury.project_plan;

    alter table project_project_plans_aud
        add constraint FK85E13A0F8A39BE24
        foreign key (rev)
        references mercury.rev_info;
*/
    create sequence athena.SEQ_BILLABLE_ITEM start with 1 increment by 50;

    create sequence athena.SEQ_ORDER_SAMPLE start with 1 increment by 50;

    create sequence athena.SEQ_PRICE_ITEM start with 1 increment by 50;

    create sequence athena.SEQ_PRODUCT start with 1 increment by 50;

    create sequence athena.SEQ_PRODUCT_FAMILY start with 1 increment by 50;

    create sequence athena.SEQ_PRODUCT_ORDER start with 1 increment by 50;

    create sequence athena.seq_project_person_index start with 1 increment by 50;

    create sequence athena.seq_research_project_index start with 1 increment by 50;

    create sequence athena.seq_rp_cohort_index start with 1 increment by 50;

    create sequence athena.seq_rp_funding_index start with 1 increment by 50;

    create sequence athena.seq_rp_irb_index start with 1 increment by 50;

    create sequence mercury.SEQ_LAB_BATCH start with 1 increment by 50;

    create sequence mercury.SEQ_LAB_EVENT start with 1 increment by 50;

    create sequence mercury.SEQ_LAB_VESSEL start with 1 increment by 50;

--     create sequence mercury.SEQ_LAB_WORK_QUEUE start with 1 increment by 50;

--     create sequence mercury.SEQ_MOLECULAR_ENVELOPE start with 1 increment by 50;

--     create sequence mercury.SEQ_MOLECULAR_STATE start with 1 increment by 50;

--     create sequence mercury.SEQ_MOLECULAR_STATE_TEMPLATE start with 1 increment by 50;

    create sequence mercury.SEQ_PERSON start with 1 increment by 50;
/*
    create sequence mercury.SEQ_PROJECT start with 1 increment by 50;

    create sequence mercury.SEQ_PROJECT_PLAN start with 1 increment by 50;
*/
    create sequence mercury.SEQ_REAGENT start with 1 increment by 50;

    create sequence mercury.SEQ_REV_INFO start with 1 increment by 50;

    create sequence mercury.SEQ_SEQUENCING_RUN start with 1 increment by 50;

--     create sequence mercury.SEQ_STARTING_SAMPLE start with 1 increment by 50;

--     create sequence mercury.SEQ_STATE_CHANGE start with 1 increment by 50;

    create sequence mercury.SEQ_VESSEL_TRANSFER start with 1 increment by 50;

--     create sequence mercury.SEQ_WORKFLOW_DESCRIPTION start with 1 increment by 50;

    create sequence mercury.seq_molecular_index start with 1 increment by 50;

    create sequence mercury.seq_molecular_indexing_scheme start with 1 increment by 50;

GRANT SELECT, INSERT, UPDATE, DELETE ON mercury.rev_info to athena;
GRANT SELECT on mercury.seq_rev_info to athena;

grant references (rev_info_id) on mercury.rev_info to athena;

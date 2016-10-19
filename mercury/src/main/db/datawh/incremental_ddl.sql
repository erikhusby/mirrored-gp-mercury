--drop table array_process_flow;

create table array_process_flow (
  product_order_id number(19) not null,
  batch_name varchar2(40) not null,
  lcset_sample_name varchar2(40) not null,
  hyb_event_id number(19) null,
  hyb_station varchar2(255) null,
  hyb_position varchar2(32) null,
  sample_name varchar2(40) null,
  hyb_date date null,
  chip varchar2(32) null,
  chip_well_barcode varchar2(32) null,
  amp_event_id number(19) null,
  amp_station varchar2(255) null,
  amp_plate_position varchar2(32) null,
  amp_date date null,
  amp_plate varchar2(32) null,
  plating_event_id number(19) null,
  plating_dilution_station varchar2(255) null,
  dna_plate_position varchar2(32) null,
  plating_dilution_date date null,
  dna_plate varchar2(32) null,
  post_frag_event_id number(19) null,
  post_frag_hyb_oven varchar2(255) null,
  post_frag_hyb_oven_date date null,
  frag_event_id number(19) null,
  frag_station varchar2(255) null,
  frag_date date null,
  precip_event_id number(19) null,
  precip_station varchar2(255) null,
  precip_date date null,
  post_precip_event_id number(19) null,
  post_precip_hyb_oven varchar2(255) null,
  post_precip_hyb_oven_date date null,
  precip_ipa_event_id number(19) null,
  precip_ipa_station varchar2(255) null,
  precip_ipa_date date null,
  resuspension_event_id number(19) null,
  resuspension_station varchar2(255) null,
  resuspension_date date null,
  postresusphyboven_event_id number(19) null,
  postresusphyboven_station varchar2(255) null,
  postresusphyboven_date date null,
  posthybhybovenloaded_event_id number(19) null,
  posthybhybovenloaded_station varchar2(255) null,
  posthybhybovenloaded_date date null,
  hybchamberloaded_event_id number(19) null,
  hybchamberloaded varchar2(255) null,
  hybchamberloaded_date date null,
  xstain_event_id number(19) null,
  xstain varchar2(255) null,
  xstain_date date null,
  autocall_event_id number(19) null,
  autocall_started date null
);

grant select on array_process_flow to mercurydwro;

--DROP index idx_array_process_flow_pdo_etl;

create index idx_array_process_flow_pdo_etl
  on array_process_flow (product_order_id, batch_name, lcset_sample_name);

--DROP index idx_array_process_flow_pdo_qry;

create index idx_array_process_flow_pdo_qry
  on array_process_flow (product_order_id, sample_name);

-- ****  PAUSE ETL BEFORE THIS SECTION AND ALLOW ALL EXISTING FILES TO BE PROCESSED ************
-- Truncates all import tables
BEGIN
  prep_import;
END;
/

-- Copies Infinium array events into import table for processing
INSERT INTO IM_EVENT_FACT ( LINE_NUMBER,
                            ETL_DATE,
                            IS_DELETE,
                            LAB_EVENT_ID,
                            WORKFLOW_ID,
                            PROCESS_ID,
                            LAB_EVENT_TYPE,
                            PRODUCT_ORDER_ID,
                            SAMPLE_NAME,
                            LCSET_SAMPLE_NAME,
                            BATCH_NAME,
                            STATION_NAME,
                            LAB_VESSEL_ID,
                            POSITION,
                            PROGRAM_NAME,
                            MOLECULAR_INDEXING_SCHEME,
                            EVENT_DATE,
                            EVENT_FACT_ID,
                            LIBRARY_NAME )
  SELECT rownum,
    ETL_DATE,
    'F',
    LAB_EVENT_ID,
    WORKFLOW_ID,
    PROCESS_ID,
    LAB_EVENT_TYPE,
    PRODUCT_ORDER_ID,
    SAMPLE_NAME,
    LCSET_SAMPLE_NAME,
    BATCH_NAME,
    STATION_NAME,
    LAB_VESSEL_ID,
    POSITION,
    PROGRAM_NAME,
    MOLECULAR_INDEXING_SCHEME,
    EVENT_DATE,
    EVENT_FACT_ID,
    LIBRARY_NAME
  FROM EVENT_FACT
  WHERE product_order_id  IS NOT NULL
    AND lcset_sample_name IS NOT NULL
    AND NVL(batch_name, 'NONE') <> 'NONE'
    AND lab_event_type IN (
      'ArrayPlatingDilution',
      'InfiniumHybridization',
      'InfiniumAmplification',
      'InfiniumPostFragmentationHybOvenLoaded',
      'InfiniumFragmentation',
      'InfiniumPrecipitation',
      'InfiniumPostPrecipitationHeatBlockLoaded',
      'InfiniumPrecipitationIsopropanolAddition',
      'InfiniumResuspension',
      'InfiniumPostResuspensionHybOven',
      'InfiniumPostHybridizationHybOvenLoaded',
      'InfiniumHybChamberLoaded',
      'InfiniumXStain',
      'InfiniumAutocallSomeStarted',
      'InfiniumAutoCallAllStarted' );

commit;

SET SERVEROUTPUT ON;


-- Steal procedure verbatim from merge_import to avoid re-processing event_fact
--PROCEDURE MERGE_ARRAY_PROCESS_FLOW
--IS
DECLARE
V_COUNT     PLS_INTEGER;
V_IS_INSERT CHAR;
V_THE_ROWID ROWID;
BEGIN
  V_COUNT := 0;

  FOR new IN (
  SELECT *
  FROM im_event_fact
  WHERE is_delete = 'F'
        AND product_order_id  IS NOT NULL
        AND lcset_sample_name IS NOT NULL
        AND NVL(batch_name, 'NONE') <> 'NONE'
        AND lab_event_type IN (
    'ArrayPlatingDilution',
    'InfiniumHybridization',
    'InfiniumAmplification',
    'InfiniumPostFragmentationHybOvenLoaded',
    'InfiniumFragmentation',
    'InfiniumPrecipitation',
    'InfiniumPostPrecipitationHeatBlockLoaded',
    'InfiniumPrecipitationIsopropanolAddition',
    'InfiniumResuspension',
    'InfiniumPostResuspensionHybOven',
    'InfiniumPostHybridizationHybOvenLoaded',
    'InfiniumHybChamberLoaded',
    'InfiniumXStain',
    'InfiniumAutocallSomeStarted',
    'InfiniumAutoCallAllStarted' ) )
  LOOP
    -- Find initial base row
    BEGIN
      SELECT ROWID INTO V_THE_ROWID
      FROM array_process_flow
      WHERE product_order_id  = new.product_order_id
            AND lcset_sample_name = new.lcset_sample_name
            AND batch_name = new.batch_name;
      V_IS_INSERT := 'N';
      EXCEPTION WHEN NO_DATA_FOUND THEN
      V_IS_INSERT := 'Y';
    END;

    BEGIN

      IF V_IS_INSERT = 'Y' THEN
        -- Have to create initial base row data
        INSERT INTO array_process_flow (
          product_order_id, batch_name, lcset_sample_name, sample_name )
        VALUES( new.product_order_id, new.batch_name, new.lcset_sample_name, new.sample_name )
        RETURNING ROWID INTO V_THE_ROWID;
      END IF;

      -- Update applicable process flow values in base row
      CASE new.lab_event_type
        WHEN 'ArrayPlatingDilution' THEN
        UPDATE array_process_flow
        SET plating_event_id = new.lab_event_id
          , plating_dilution_station = new.station_name
          , dna_plate_position = new.position
          , plating_dilution_date = new.event_date
          -- Strip position suffix from label to get plate barcode
          , dna_plate = ( SELECT REGEXP_REPLACE( LABEL, new.position || '$', '' ) FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumHybridization' THEN
        UPDATE array_process_flow
        SET hyb_event_id = new.lab_event_id
          , hyb_station = new.station_name
          , hyb_position = new.position
          , hyb_date = new.event_date
          -- Append underscore and position suffix to chip barcode to get chip well pseudo-barcode
          , ( chip, chip_well_barcode ) = ( SELECT LABEL, LABEL || '_' || new.position FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumAmplification' THEN
        UPDATE array_process_flow
        SET amp_event_id = new.lab_event_id
          , amp_station = new.station_name
          , amp_plate_position = new.position
          , amp_date = new.event_date
          , amp_plate = ( SELECT LABEL FROM LAB_VESSEL WHERE LAB_VESSEL_ID = new.LAB_VESSEL_ID )
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPostFragmentationHybOvenLoaded' THEN
        UPDATE array_process_flow
        SET post_frag_event_id = new.lab_event_id
          , post_frag_hyb_oven = new.station_name
          , post_frag_hyb_oven_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumFragmentation' THEN
        UPDATE array_process_flow
        SET frag_event_id = new.lab_event_id
          , frag_station = new.station_name
          , frag_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPrecipitation' THEN
        UPDATE array_process_flow
        SET precip_event_id = new.lab_event_id
          , precip_station = new.station_name
          , precip_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPostPrecipitationHeatBlockLoaded' THEN
        UPDATE array_process_flow
        SET post_precip_event_id = new.lab_event_id
          , post_precip_hyb_oven = new.station_name
          , post_precip_hyb_oven_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPrecipitationIsopropanolAddition' THEN
        UPDATE array_process_flow
        SET precip_ipa_event_id = new.lab_event_id
          , precip_ipa_station = new.station_name
          , precip_ipa_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumResuspension' THEN
        UPDATE array_process_flow
        SET resuspension_event_id = new.lab_event_id
          , resuspension_station = new.station_name
          , resuspension_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPostResuspensionHybOven' THEN
        UPDATE array_process_flow
        SET postresusphyboven_event_id = new.lab_event_id
          , postresusphyboven_station = new.station_name
          , postresusphyboven_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumPostHybridizationHybOvenLoaded' THEN
        UPDATE array_process_flow
        SET posthybhybovenloaded_event_id = new.lab_event_id
          , posthybhybovenloaded_station = new.station_name
          , posthybhybovenloaded_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumHybChamberLoaded' THEN
        UPDATE array_process_flow
        SET hybchamberloaded_event_id = new.lab_event_id
          , hybchamberloaded = new.station_name
          , hybchamberloaded_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumXStain' THEN
        UPDATE array_process_flow
        SET xstain_event_id = new.lab_event_id
          , xstain = new.station_name
          , xstain_date = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN 'InfiniumAutocallSomeStarted' THEN
        UPDATE array_process_flow
        SET autocall_event_id = new.lab_event_id
          --, scanner = new.station_name
          , autocall_started = new.event_date
        WHERE ROWID = V_THE_ROWID;
        WHEN  'InfiniumAutoCallAllStarted' THEN
        UPDATE array_process_flow
        SET autocall_event_id = new.lab_event_id
          --, scanner = new.station_name
          , autocall_started = new.event_date
        WHERE ROWID = V_THE_ROWID
              AND ( autocall_event_id IS NULL
                    OR
                    -- Don't overwrite some started with all started
                    new.event_date < autocall_started );
      ELSE
        -- Shouldn't get here if event types match case list, but avoid error and don't count
        V_COUNT := V_COUNT - 1;
      END CASE;

      V_COUNT := V_COUNT + 1;
      EXCEPTION WHEN OTHERS THEN
      --errmsg := SQLERRM;
      DBMS_OUTPUT.PUT_LINE(
          TO_CHAR(new.etl_date, 'YYYYMMDDHH24MISS') || '_event_fact.dat line ' || new.line_number || ' (array_process_flow merge) ' || SQLERRM);
      CONTINUE;
    END;
  END LOOP;
  --SHOW_ETL_STATS(  V_COUNT, 0, ' array process flow events added' );
  DBMS_OUTPUT.PUT_LINE( 'Processed ' || V_COUNT || ' array process flow events' );
END; -- MERGE_ARRAY_PROCESS_FLOW;
/

COMMIT;

SELECT COUNT(*) FROM array_process_flow;


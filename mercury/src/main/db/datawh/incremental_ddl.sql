-- GPLIM-6483 ETL Lab Metrics Metadata
--DROP TABLE LAB_METRIC_METADATA;

CREATE TABLE LAB_METRIC_METADATA (
                                     LAB_METRIC_ID NUMBER(19)   NOT NULL,
                                     METADATA_ID   NUMBER(19)   NOT NULL,
                                     METADATA_KEY  VARCHAR2(64) NOT NULL, -- 255 in prod, biggest is 26
                                     VALUE         VARCHAR2(64) NULL,
                                     DATE_VALUE    DATE         NULL,
                                     NUMBER_VALUE  NUMBER(19,2) NULL,
                                     ETL_DATE DATE NOT          NULL
);

--DROP TABLE IM_LAB_METRIC_METADATA;

CREATE TABLE IM_LAB_METRIC_METADATA (
                                        LINE_NUMBER   NUMBER(9) NOT NULL,
                                        ETL_DATE      DATE NOT NULL,
                                        IS_DELETE     CHAR NOT NULL,
                                        LAB_METRIC_ID NUMBER(19)  NULL,
                                        METADATA_ID   NUMBER(19)    NULL,
                                        METADATA_KEY  VARCHAR2(64) NULL,
                                        VALUE         VARCHAR2(64) NULL,
                                        DATE_VALUE    DATE NULL,
                                        NUMBER_VALUE  NUMBER(19,2) NULL
);

CREATE UNIQUE INDEX PK_METRIC_METADATA
    ON LAB_METRIC_METADATA( LAB_METRIC_ID, METADATA_ID );

ALTER TABLE LAB_METRIC_METADATA
    ADD CONSTRAINT PK_METRIC_METADATA PRIMARY KEY ( LAB_METRIC_ID, METADATA_ID )
        USING INDEX PK_METRIC_METADATA;


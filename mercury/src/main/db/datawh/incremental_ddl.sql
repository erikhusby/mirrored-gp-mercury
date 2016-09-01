ALTER TABLE im_event_fact
ADD (
  library_name varchar2(64)
);

ALTER TABLE event_fact
ADD (
  library_name varchar2(64)
);

ALTER TABLE IM_LAB_VESSEL ADD NAME VARCHAR2(255);
ALTER TABLE LAB_VESSEL ADD NAME VARCHAR2(255);

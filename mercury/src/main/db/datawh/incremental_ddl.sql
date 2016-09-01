ALTER TABLE im_event_fact
ADD (
  library_name varchar2(64)
);

ALTER TABLE event_fact
ADD (
  library_name varchar2(64)
);

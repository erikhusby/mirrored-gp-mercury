-------------------------------------------------------
-- For release 1.28
-------------------------------------------------------
alter table im_event_fact add (batch_name VARCHAR(40));
alter table im_sequencing_sample_fact add (batch_name VARCHAR(40));
alter table sequencing_sample_fact add (batch_name VARCHAR(40));
alter table event_fact add (batch_name VARCHAR(40));

update event_fact ef set ef.batch_name = (select lb.batch_name from lab_batch lb where lb.lab_batch_id = ef.lab_batch_id);

alter table event_fact drop constraint fk_event_lab_batch;
alter table event_fact drop column lab_batch_id;
drop table lab_batch cascade constraints;
drop table im_lab_batch cascade constraints;


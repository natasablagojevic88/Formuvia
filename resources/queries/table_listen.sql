CREATE OR REPLACE FUNCTION #additional_schema#.table_listen()
	RETURNS trigger
	LANGUAGE plpgsql
AS $function$
	BEGIN

	perform pg_notify('listen_'||TG_TABLE_NAME,'Changed');

	return new;

	END;
$function$
;
CREATE OR replace trigger table_listen after
insert
    or
delete
    or
update
    on
    #current_schema#.#table_name# for each statement 
    execute function #additional_schema#.table_listen();
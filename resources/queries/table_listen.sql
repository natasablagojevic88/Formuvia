CREATE OR REPLACE FUNCTION #additional_schema#.#table_name_listen_name#()
	RETURNS trigger
	LANGUAGE plpgsql
AS $function$
	BEGIN

	perform pg_notify('#table_name_listen_name#','Changed');

	return new;

	END;
$function$
;
CREATE OR replace trigger #table_name_listen_name# after
insert
    or
delete
    or
update
    on
    #current_schema#.#table_name# for each row 
    execute function #additional_schema#.#table_name_listen_name#();
CREATE OR REPLACE FUNCTION #additional_schema#.table_listen_notification()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
numberOfUnread json;
pUser uuid;
	BEGIN
		
	if TG_OP = 'DELETE' then
        pUser:=old.app_user;
    else
        pUser:=new.app_user;
    end if;
	
	select to_json(t.*) into numberOfUnread from 
	(select pUser user, count(*) count from #current_schema#.notification n where pUser=n.app_user and n.seen is false) t;

	perform pg_notify('listen_'||TG_TABLE_NAME,numberOfUnread::TEXT);

	return new;

END;
$function$
;
create trigger table_listen after
insert
    or
delete
    or
update
    on
    #current_schema#.notification for each row execute function #additional_schema#.table_listen_notification();
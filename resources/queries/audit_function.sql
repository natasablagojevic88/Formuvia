CREATE OR REPLACE FUNCTION #additional_schema#.create_audit()
    RETURNS trigger
    LANGUAGE plpgsql
AS $function$
declare
inId uuid;
newData jsonb;
oldData jsonb;
    BEGIN

    if TG_OP = 'UPDATE' and old is not distinct from new then
        return null;
    end if;

    if TG_OP = 'DELETE' then
        inId:=old.id;
    else
        inId:=new.id;
    end if;

    newData:=to_jsonb(new);
    oldData:=to_jsonb(old);

    insert into #current_schema#.track(table_name,data_id,action,old_data,new_data)
    values
    (TG_TABLE_NAME,inId,TG_OP,oldData,newData);

    return null;

    END;
$function$
;
CREATE OR replace trigger create_audit after
insert
    or
delete
    or
update
    on
    #current_schema#.#table_name# for each row 
    execute function #additional_schema#.create_audit();
select 
count(*)
from
notification
where 
seen is false 
and app_user = ?
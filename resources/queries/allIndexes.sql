select 
tablename,
indexname
from 
pg_indexes
where 
schemaname=current_schema
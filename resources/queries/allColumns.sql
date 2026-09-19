select 
table_name,
column_name 
from 
information_schema.columns t
where 
t.table_catalog =current_catalog 
and t.table_schema =current_schema 
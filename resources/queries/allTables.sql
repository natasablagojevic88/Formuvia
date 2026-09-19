select 
table_name 
from 
information_schema.tables t
where 
t.table_catalog =current_catalog 
and t.table_schema =current_schema 
and t.table_type ='BASE TABLE'
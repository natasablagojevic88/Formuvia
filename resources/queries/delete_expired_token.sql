delete
from 
token
where 
now()>refresh_token_exipires 
or active is false
select 
qjb.job_name,
(case when qt.job_name is null then false else true end )active,
to_timestamp(nullif(qt.prev_fire_time, -1)/1000),
to_timestamp(nullif(qt.next_fire_time, -1)/1000),
s.status,
s.end_time,
s.error_text,
ji.id,
ji.report
from 
qrtz_job_details qjb
left join qrtz_triggers qt on qt.job_name=qjb.job_name
and qt.job_group=qjb.job_group
left join job_item ji on ji.name = qjb.job_name 
left join (select job_item, status, end_time, error_text from (
        select jl.*, row_number() over (partition by jl.job_item order by jl.end_time desc nulls last) as rn
        from job_log jl
) jl where jl.rn = 1) s on s.job_item=ji.id 
left join role r on r.id =ji.role
where 
qjb.job_group = ? 
and coalesce(r.code,'admin') = ANY(?)
AND coalesce(ji.id::text,'') LIKE ?
order by 1
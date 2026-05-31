-- Drop existing foreign key constraint
ALTER TABLE smartmaint.tarea_audit_log DROP CONSTRAINT fk_tarea_audit_log_tarea;

-- Recreate foreign key constraint with ON DELETE CASCADE
ALTER TABLE smartmaint.tarea_audit_log 
ADD CONSTRAINT fk_tarea_audit_log_tarea 
FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id) ON DELETE CASCADE;

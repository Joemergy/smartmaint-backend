-- Drop existing foreign key constraint
ALTER TABLE smartmaint.tarea_archivos DROP CONSTRAINT fk_tarea_archivos_tarea;

-- Recreate foreign key constraint with ON DELETE CASCADE
ALTER TABLE smartmaint.tarea_archivos 
ADD CONSTRAINT fk_tarea_archivos_tarea 
FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id) ON DELETE CASCADE;

-- Drop existing foreign key constraint
ALTER TABLE smartmaint.nota_tarea DROP CONSTRAINT fk_nota_tarea_tarea;

-- Recreate foreign key constraint with ON DELETE CASCADE
ALTER TABLE smartmaint.nota_tarea 
ADD CONSTRAINT fk_nota_tarea_tarea 
FOREIGN KEY (tarea_id) REFERENCES smartmaint.tareas(id) ON DELETE CASCADE;

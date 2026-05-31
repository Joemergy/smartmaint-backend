-- Drop existing foreign key constraint
ALTER TABLE smartmaint.notificaciones DROP CONSTRAINT fk_notificaciones_usuario;

-- Recreate foreign key constraint with ON DELETE CASCADE
ALTER TABLE smartmaint.notificaciones 
ADD CONSTRAINT fk_notificaciones_usuario 
FOREIGN KEY (usuario_id) REFERENCES smartmaint.usuarios(id) ON DELETE CASCADE;

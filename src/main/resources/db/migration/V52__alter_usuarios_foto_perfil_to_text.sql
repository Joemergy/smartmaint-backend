-- Alter foto_perfil column to TEXT type to support base64 image data
ALTER TABLE smartmaint.usuarios ALTER COLUMN foto_perfil TYPE TEXT;

/* ================================================================
   FUT SYSTEM - I.E. N.° 20191 ALFONSO UGARTE
   SQL Server | Schema for backend ASE252S3_T05-be-develop
   Modules: Applicants, Procedure Types, New Request and FUT Management
   Idempotent: creates missing objects and adds missing columns;
   does not drop tables or existing records.
   ================================================================ */

USE master;
GO
IF DB_ID(N'AlfonsoUgarteDB') IS NULL CREATE DATABASE AlfonsoUgarteDB;
GO
USE AlfonsoUgarteDB;
GO

/* ----------------------- REQUIRED CATALOGS ----------------------- */
IF OBJECT_ID(N'dbo.tipo_documento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tipo_documento (
        id_tipo_documento INT IDENTITY(1,1) PRIMARY KEY,
        codigo VARCHAR(10) NOT NULL UNIQUE,
        nombre NVARCHAR(80) NOT NULL,
        estado BIT NOT NULL CONSTRAINT DF_tipo_documento_estado DEFAULT (1)
    );
END;
GO
IF OBJECT_ID(N'dbo.ubigeo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ubigeo (
        id_ubigeo INT IDENTITY(1,1) PRIMARY KEY,
        codigo VARCHAR(6) NULL,
        departamento NVARCHAR(100) NOT NULL,
        provincia NVARCHAR(100) NOT NULL,
        distrito NVARCHAR(100) NOT NULL,
        estado BIT NOT NULL CONSTRAINT DF_ubigeo_estado DEFAULT (1)
    );
END;
GO
IF OBJECT_ID(N'dbo.area', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.area (
        id_area INT IDENTITY(1,1) PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL UNIQUE,
        descripcion NVARCHAR(250) NULL,
        estado BIT NOT NULL CONSTRAINT DF_area_estado DEFAULT (1)
    );
END;
GO

/* ----------------------------- APPLICANTS ----------------------------- */
IF OBJECT_ID(N'dbo.solicitante', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.solicitante (
        id_solicitante INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_solicitante PRIMARY KEY,
        id_tipo_documento INT NOT NULL,
        id_ubigeo INT NOT NULL,
        departamento NVARCHAR(100) NULL,
        provincia NVARCHAR(100) NULL,
        distrito NVARCHAR(100) NULL,
        numero_documento VARCHAR(20) NOT NULL,
        nombres VARCHAR(100) NOT NULL,
        apellido_paterno VARCHAR(100) NOT NULL,
        apellido_materno VARCHAR(100) NOT NULL,
        telefono VARCHAR(20) NULL,
        correo VARCHAR(150) NULL,
        direccion VARCHAR(250) NULL,
        estado CHAR(1) NOT NULL CONSTRAINT DF_solicitante_estado DEFAULT ('A'),
        fecha_registro DATETIME2 NOT NULL CONSTRAINT DF_solicitante_fecha_registro DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,
        CONSTRAINT FK_solicitante_tipo_documento FOREIGN KEY (id_tipo_documento) REFERENCES dbo.tipo_documento(id_tipo_documento),
        CONSTRAINT FK_solicitante_ubigeo FOREIGN KEY (id_ubigeo) REFERENCES dbo.ubigeo(id_ubigeo),
        CONSTRAINT CK_solicitante_estado CHECK (estado IN ('A','I'))
    );
END;
GO
/* Additive migration for a solicitante table created by an earlier version. */
IF COL_LENGTH('dbo.solicitante','departamento') IS NULL ALTER TABLE dbo.solicitante ADD departamento NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','provincia') IS NULL ALTER TABLE dbo.solicitante ADD provincia NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','distrito') IS NULL ALTER TABLE dbo.solicitante ADD distrito NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','fecha_actualizacion') IS NULL ALTER TABLE dbo.solicitante ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.solicitante','fecha_eliminacion') IS NULL ALTER TABLE dbo.solicitante ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.solicitante','fecha_restauracion') IS NULL ALTER TABLE dbo.solicitante ADD fecha_restauracion DATETIME2 NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_solicitante_numero_documento' AND object_id=OBJECT_ID('dbo.solicitante'))
    CREATE INDEX IX_solicitante_numero_documento ON dbo.solicitante(numero_documento);
GO

/* ------------------------ PROCEDURE TYPES ------------------------ */
IF OBJECT_ID(N'dbo.tipo_tramite', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tipo_tramite (
        id_tipo_tramite INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tipo_tramite PRIMARY KEY,
        codigo VARCHAR(20) NULL,
        nombre VARCHAR(100) NOT NULL,
        categoria NVARCHAR(60) NULL,
        descripcion VARCHAR(250) NULL,
        requisitos VARCHAR(500) NULL,
        plazo_estimado_dias INT NULL,
        requiere_documentos BIT NOT NULL CONSTRAINT DF_tipo_tramite_requiere_documentos DEFAULT (0),
        vigente_desde DATE NULL,
        estado BIT NOT NULL CONSTRAINT DF_tipo_tramite_estado DEFAULT (1),
        fecha_registro DATETIME2 NOT NULL CONSTRAINT DF_tipo_tramite_fecha_registro DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL
    );
END;
GO
/* Columns used by the Java model and Angular list; added without dropping data. */
IF COL_LENGTH('dbo.tipo_tramite','codigo') IS NULL ALTER TABLE dbo.tipo_tramite ADD codigo VARCHAR(20) NULL;
IF COL_LENGTH('dbo.tipo_tramite','categoria') IS NULL ALTER TABLE dbo.tipo_tramite ADD categoria NVARCHAR(60) NULL;
IF COL_LENGTH('dbo.tipo_tramite','requiere_documentos') IS NULL ALTER TABLE dbo.tipo_tramite ADD requiere_documentos BIT NOT NULL CONSTRAINT DF_tipo_tramite_requiere_documentos_mig DEFAULT (0);
IF COL_LENGTH('dbo.tipo_tramite','vigente_desde') IS NULL ALTER TABLE dbo.tipo_tramite ADD vigente_desde DATE NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_registro') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_registro DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_actualizacion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_eliminacion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_restauracion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_restauracion DATETIME2 NULL;
GO

/* ----------------------- NEW FUT REQUEST ----------------------- */
/* Java model @Table(name="tramite_fut") uses this name, not solicitud. */
IF OBJECT_ID(N'dbo.tramite_fut', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tramite_fut (
        id_solicitud BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tramite_fut PRIMARY KEY,
        id_solicitante INT NULL,
        id_tipo_tramite INT NULL,
        codigo_seguimiento VARCHAR(60) NOT NULL,
        solicito NVARCHAR(500) NOT NULL,
        nombre_completo NVARCHAR(200) NOT NULL,
        dni VARCHAR(20) NOT NULL,
        tipo_documento VARCHAR(10) NOT NULL CONSTRAINT DF_tramite_fut_tipo_documento DEFAULT ('DNI'),
        tipo_solicitante NVARCHAR(100) NULL,
        departamento NVARCHAR(100) NULL,
        numero_depto NVARCHAR(50) NULL,
        telefono_fijo VARCHAR(20) NULL,
        folios INT NOT NULL,
        archivo_nombre NVARCHAR(255) NULL,
        archivo_tipo VARCHAR(150) NULL,
        archivo_tamano BIGINT NULL,
        idempotency_key VARCHAR(80) NULL,
        correo NVARCHAR(150) NULL,
        celular VARCHAR(9) NOT NULL,
        domicilio NVARCHAR(250) NOT NULL,
        distrito NVARCHAR(100) NOT NULL,
        provincia NVARCHAR(100) NOT NULL,
        fundamento NVARCHAR(2000) NOT NULL,
        documentos_adjuntos NVARCHAR(1000) NULL,
        fecha_declarada DATE NOT NULL,
        nota_firma NVARCHAR(500) NULL,
        estado VARCHAR(30) NOT NULL CONSTRAINT DF_tramite_fut_estado DEFAULT ('RECIBIDO'),
        observacion NVARCHAR(2000) NULL,
        fecha_registro DATETIME2 NOT NULL CONSTRAINT DF_tramite_fut_fecha_registro DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,
        CONSTRAINT UQ_tramite_fut_codigo_seguimiento UNIQUE (codigo_seguimiento),
        CONSTRAINT FK_tramite_fut_solicitante FOREIGN KEY (id_solicitante) REFERENCES dbo.solicitante(id_solicitante),
        CONSTRAINT FK_tramite_fut_tipo_tramite FOREIGN KEY (id_tipo_tramite) REFERENCES dbo.tipo_tramite(id_tipo_tramite),
        CONSTRAINT CK_tramite_fut_estado CHECK (estado IN ('RECIBIDO','EN_REVISION','OBSERVADO','ATENDIDO','RECHAZADO')),
        CONSTRAINT CK_tramite_fut_celular CHECK (LEN(celular)=9 AND celular NOT LIKE '%[^0-9]%')
    );
END;
GO
IF COL_LENGTH('dbo.tramite_fut','id_tipo_tramite') IS NULL ALTER TABLE dbo.tramite_fut ADD id_tipo_tramite INT NULL;
IF COL_LENGTH('dbo.tramite_fut','tipo_documento') IS NULL ALTER TABLE dbo.tramite_fut ADD tipo_documento VARCHAR(10) NOT NULL CONSTRAINT DF_tramite_fut_tipo_documento_mig DEFAULT ('DNI');
IF COL_LENGTH('dbo.tramite_fut','fecha_actualizacion') IS NULL ALTER TABLE dbo.tramite_fut ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tramite_fut','fecha_restauracion') IS NULL ALTER TABLE dbo.tramite_fut ADD fecha_restauracion DATETIME2 NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_tramite_fut_idempotency_key' AND object_id=OBJECT_ID('dbo.tramite_fut'))
    CREATE UNIQUE INDEX UX_tramite_fut_idempotency_key ON dbo.tramite_fut(idempotency_key) WHERE idempotency_key IS NOT NULL;
GO

/* ----------------------- FUT PROCEDURE MANAGEMENT ----------------------- */
IF OBJECT_ID(N'dbo.gestion_tramite_fut', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.gestion_tramite_fut (
        id_gestion INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_gestion_tramite_fut PRIMARY KEY,
        id_solicitante INT NOT NULL,
        nombre_responsable NVARCHAR(150) NULL,
        nombres_responsable NVARCHAR(100) NULL,
        apellido_paterno_responsable NVARCHAR(100) NULL,
        apellido_materno_responsable NVARCHAR(100) NULL,
        id_area_responsable INT NULL,
        fecha_recepcion DATE NOT NULL,
        tipo_gestion VARCHAR(30) NOT NULL,
        prioridad VARCHAR(15) NOT NULL CONSTRAINT DF_gestion_tramite_fut_prioridad DEFAULT ('NORMAL'),
        fecha_limite DATE NULL,
        estado_gestion VARCHAR(20) NOT NULL CONSTRAINT DF_gestion_tramite_fut_estado_gestion DEFAULT ('PENDIENTE'),
        decision NVARCHAR(500) NULL,
        fundamento_decision NVARCHAR(2000) NULL,
        fecha_resolucion DATE NULL,
        observaciones NVARCHAR(2000) NULL,
        estado CHAR(1) NOT NULL CONSTRAINT DF_gestion_tramite_fut_estado DEFAULT ('A'),
        fecha_registro DATETIME2 NOT NULL CONSTRAINT DF_gestion_tramite_fut_fecha_registro DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,
        CONSTRAINT FK_gestion_tramite_fut_solicitante FOREIGN KEY (id_solicitante) REFERENCES dbo.solicitante(id_solicitante),
        CONSTRAINT FK_gestion_tramite_fut_area FOREIGN KEY (id_area_responsable) REFERENCES dbo.area(id_area),
        CONSTRAINT CK_gestion_tramite_fut_tipo CHECK (tipo_gestion IN ('RECEPCION','EVALUACION','DERIVACION','APROBACION','RECHAZO','SUBSANACION')),
        CONSTRAINT CK_gestion_tramite_fut_prioridad CHECK (prioridad IN ('NORMAL','URGENTE','MUY_URGENTE')),
        CONSTRAINT CK_gestion_tramite_fut_estado_gestion CHECK (estado_gestion IN ('PENDIENTE','EN_PROCESO','OBSERVADO','APROBADO','RECHAZADO')),
        CONSTRAINT CK_gestion_tramite_fut_estado CHECK (estado IN ('A','I'))
    );
END;
GO
/* Additive migration fields already used by earlier versions. */
IF COL_LENGTH('dbo.gestion_tramite_fut','prioridad') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD prioridad VARCHAR(15) NOT NULL CONSTRAINT DF_gestion_prioridad_mig DEFAULT ('NORMAL');
IF COL_LENGTH('dbo.gestion_tramite_fut','estado_gestion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD estado_gestion VARCHAR(20) NOT NULL CONSTRAINT DF_gestion_estado_gestion_mig DEFAULT ('PENDIENTE');
IF COL_LENGTH('dbo.gestion_tramite_fut','estado') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD estado CHAR(1) NOT NULL CONSTRAINT DF_gestion_estado_mig DEFAULT ('A');
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_actualizacion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_eliminacion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_restauracion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_restauracion DATETIME2 NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_gestion_fut_estado_fecha' AND object_id=OBJECT_ID('dbo.gestion_tramite_fut'))
    CREATE INDEX IX_gestion_fut_estado_fecha ON dbo.gestion_tramite_fut(estado, fecha_registro DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_gestion_fut_solicitante_fecha' AND object_id=OBJECT_ID('dbo.gestion_tramite_fut'))
    CREATE INDEX IX_gestion_fut_solicitante_fecha ON dbo.gestion_tramite_fut(id_solicitante, fecha_registro DESC);
GO

/* --------------------------- TRACKING --------------------------- */
IF OBJECT_ID(N'dbo.seguimiento_tramite', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.seguimiento_tramite (
        id_seguimiento BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_seguimiento_tramite PRIMARY KEY,
        id_solicitud BIGINT NOT NULL,
        estado_anterior VARCHAR(30) NULL,
        estado_nuevo VARCHAR(30) NOT NULL,
        observacion NVARCHAR(2000) NULL,
        fecha_cambio DATETIME2 NOT NULL CONSTRAINT DF_seguimiento_fecha_cambio DEFAULT SYSDATETIME(),
        estado CHAR(1) NOT NULL CONSTRAINT DF_seguimiento_estado DEFAULT ('A'),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,
        CONSTRAINT FK_seguimiento_tramite_fut FOREIGN KEY (id_solicitud) REFERENCES dbo.tramite_fut(id_solicitud),
        CONSTRAINT CK_seguimiento_estado CHECK (estado IN ('A','I'))
    );
END;
GO
IF COL_LENGTH('dbo.seguimiento_tramite','estado') IS NULL ALTER TABLE dbo.seguimiento_tramite ADD estado CHAR(1) NOT NULL CONSTRAINT DF_seguimiento_estado_mig DEFAULT ('A');
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_actualizacion') IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_eliminacion') IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_restauracion') IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_restauracion DATETIME2 NULL;
GO

/* ------------------------ CATALOG DATA ------------------------ */
IF NOT EXISTS (SELECT 1 FROM dbo.tipo_documento WHERE codigo='DNI')
    INSERT dbo.tipo_documento(codigo,nombre) VALUES ('DNI',N'Documento Nacional de Identidad');
IF NOT EXISTS (SELECT 1 FROM dbo.tipo_documento WHERE codigo='CNE')
    INSERT dbo.tipo_documento(codigo,nombre) VALUES ('CNE',N'Carné de Extranjería');
IF NOT EXISTS (SELECT 1 FROM dbo.ubigeo WHERE departamento=N'Lima' AND provincia=N'Cañete' AND distrito=N'San Vicente de Cañete')
    INSERT dbo.ubigeo(codigo,departamento,provincia,distrito) VALUES ('150501',N'Lima',N'Cañete',N'San Vicente de Cañete');
IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre=N'DIRECCIÓN') INSERT dbo.area(nombre,descripcion,estado) VALUES (N'DIRECCIÓN',N'Institutional management office',1);
IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre=N'ADMINISTRACIÓN') INSERT dbo.area(nombre,descripcion,estado) VALUES (N'ADMINISTRACIÓN',N'Administration department',1);
IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre=N'SECRETARÍA') INSERT dbo.area(nombre,descripcion,estado) VALUES (N'SECRETARÍA',N'Secretary department',1);
UPDATE dbo.area SET estado=CASE WHEN nombre IN (N'DIRECCIÓN',N'ADMINISTRACIÓN',N'SECRETARÍA') THEN 1 ELSE 0 END;
GO

/* Procedure type options for the FUT form. Does not duplicate existing names. */
DECLARE @Tipos TABLE(codigo VARCHAR(20), nombre VARCHAR(100), categoria NVARCHAR(60), descripcion VARCHAR(250), requisitos VARCHAR(500), dias INT, docs BIT);
INSERT INTO @Tipos VALUES
('TR-001','SOLICITUD DE VACANTES',N'ENROLLMENT','Application for school enrollment vacancy',NULL,7,1),
('TR-002','TRASLADO DE MATRÍCULA',N'ENROLLMENT','Application for enrollment transfer',NULL,7,1),
('TR-003','CONSTANCIA DE ESTUDIOS O DE MATRÍCULA',N'CERTIFICATES','Issuance of certificate of studies or enrollment',NULL,5,0),
('TR-004','CERTIFICADO DE ESTUDIOS',N'CERTIFICATES','Issuance of academic certificate',NULL,10,1),
('TR-005','RECTIFICACIÓN DE DATOS',N'RECORDS','Correction of registered personal data',NULL,5,1),
('TR-006','JUSTIFICACIÓN DE INASISTENCIAS PROLONGADAS',N'ABSENCES','Justification for extended absences',NULL,3,1),
('TR-007','LICENCIAS Y PERMISOS',N'PERMITS','Application for leave or permit',NULL,5,1),
('TR-008','CONSTANCIA DE PAGO O DE TRABAJO',N'CERTIFICATES','Issuance of payment or employment certificate',NULL,5,0),
('TR-009','ENTREGA DE LEGAJOS Y DOCUMENTOS',N'DOCUMENTS','Delivery of files and documents',NULL,5,1),
('TR-010','RECLAMOS O ABSOLUCIÓN DE CONSULTAS ADMINISTRATIVAS',N'COMPLAINTS','Administrative complaint or inquiry',NULL,10,1),
('TR-011','OTROS',N'OTHER','Other administrative procedures',NULL,10,0);
INSERT dbo.tipo_tramite(codigo,nombre,categoria,descripcion,requisitos,plazo_estimado_dias,requiere_documentos,estado)
SELECT t.codigo,t.nombre,t.categoria,t.descripcion,t.requisitos,t.dias,t.docs,1
FROM @Tipos t
WHERE NOT EXISTS (SELECT 1 FROM dbo.tipo_tramite x WHERE x.nombre=t.nombre);
GO

/* --------------------------- SYSTEM USERS --------------------------- */
IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuario (
        id_usuario INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_usuario PRIMARY KEY,
        id_rol INT NOT NULL,
        id_area INT NOT NULL CONSTRAINT DF_usuario_id_area DEFAULT (1),
        id_ubigeo INT NOT NULL CONSTRAINT DF_usuario_id_ubigeo DEFAULT (1),
        nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
        nombres VARCHAR(80) NOT NULL,
        apellido_paterno VARCHAR(60) NOT NULL,
        apellido_materno VARCHAR(60) NOT NULL,
        correo VARCHAR(120) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL,
        fecha_registro DATETIME2 NOT NULL CONSTRAINT DF_usuario_fecha_registro DEFAULT SYSUTCDATETIME(),
        estado BIT NOT NULL CONSTRAINT DF_usuario_estado DEFAULT (1),
        fecha_creacion DATETIME2 NOT NULL CONSTRAINT DF_usuario_fecha_creacion DEFAULT SYSUTCDATETIME(),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,
        CONSTRAINT FK_usuario_area FOREIGN KEY (id_area) REFERENCES dbo.area(id_area),
        CONSTRAINT FK_usuario_ubigeo FOREIGN KEY (id_ubigeo) REFERENCES dbo.ubigeo(id_ubigeo)
    );
END;
GO
IF COL_LENGTH('dbo.usuario','id_area') IS NULL ALTER TABLE dbo.usuario ADD id_area INT NOT NULL CONSTRAINT DF_usuario_id_area_mig DEFAULT (1);
IF COL_LENGTH('dbo.usuario','id_ubigeo') IS NULL ALTER TABLE dbo.usuario ADD id_ubigeo INT NOT NULL CONSTRAINT DF_usuario_id_ubigeo_mig DEFAULT (1);
IF COL_LENGTH('dbo.usuario','fecha_creacion') IS NULL ALTER TABLE dbo.usuario ADD fecha_creacion DATETIME2 NOT NULL CONSTRAINT DF_usuario_fecha_creacion_mig DEFAULT SYSUTCDATETIME();
IF COL_LENGTH('dbo.usuario','fecha_actualizacion') IS NULL ALTER TABLE dbo.usuario ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','fecha_eliminacion') IS NULL ALTER TABLE dbo.usuario ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','fecha_restauracion') IS NULL ALTER TABLE dbo.usuario ADD fecha_restauracion DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','estado') IS NULL ALTER TABLE dbo.usuario ADD estado BIT NOT NULL CONSTRAINT DF_usuario_estado_mig DEFAULT (1);
GO
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'directora.demo')
BEGIN
    INSERT dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (1, (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'DIRECCIÓN' ORDER BY id_area), (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo), 'directora.demo', N'Directora', N'Demo', N'Apellido', 'directora.demo@ie20191.edu.pe', '$2b$10$CMAcfF39TLpMWC08Ma9K/O50MQ5i3KRmat6oI/CA5Cb0YAIOrjCBS');
END;
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'secretaria.demo')
BEGIN
    INSERT dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (2, (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'SECRETARÍA' ORDER BY id_area), (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo), 'secretaria.demo', N'Secretaria', N'Demo', N'Apellido', 'secretaria.demo@ie20191.edu.pe', '$2b$10$ylzLIwi.KRuRqAUhcjGuAeAk6Wr5PKkSYkd3XqbK45rT5e2Q/RH8q');
END;
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'admin.demo')
BEGIN
    INSERT dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (3, (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'ADMINISTRACIÓN' ORDER BY id_area), (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo), 'admin.demo', N'Administrador', N'Demo', N'Apellido', 'admin.demo@ie20191.edu.pe', '$2b$10$CMAcfF39TLpMWC08Ma9K/O50MQ5i3KRmat6oI/CA5Cb0YAIOrjCBS');
END;
GO

/* -------------------------- VALIDATION QUERIES -------------------------- */
SELECT id_solicitante, numero_documento, apellido_paterno, apellido_materno, nombres,
       departamento, provincia, distrito, direccion, telefono, correo, estado,
       fecha_registro, fecha_actualizacion, fecha_eliminacion, fecha_restauracion
FROM dbo.solicitante ORDER BY fecha_registro DESC;
GO
SELECT id_tipo_tramite, codigo, nombre, categoria, descripcion, requisitos,
       plazo_estimado_dias, requiere_documentos, vigente_desde, estado,
       fecha_registro, fecha_actualizacion, fecha_eliminacion, fecha_restauracion
FROM dbo.tipo_tramite ORDER BY nombre;
GO
SELECT g.id_gestion, s.numero_documento, s.apellido_paterno, s.apellido_materno, s.nombres,
       COALESCE(NULLIF(LTRIM(RTRIM(g.nombre_responsable)),N''),
                NULLIF(LTRIM(RTRIM(CONCAT(g.nombres_responsable,N' ',g.apellido_paterno_responsable,N' ',g.apellido_materno_responsable))),N'')) AS responsable,
       a.nombre AS area_responsable, g.fecha_recepcion, g.tipo_gestion, g.prioridad,
       g.fecha_limite, g.estado_gestion, g.decision, g.fecha_resolucion,
       g.fecha_registro, g.fecha_actualizacion, g.fecha_eliminacion, g.fecha_restauracion
FROM dbo.gestion_tramite_fut g
JOIN dbo.solicitante s ON s.id_solicitante=g.id_solicitante
LEFT JOIN dbo.area a ON a.id_area=g.id_area_responsable
ORDER BY g.fecha_registro DESC;
GO
SELECT id_solicitud, codigo_seguimiento, tipo_documento, dni, nombre_completo,
       tipo_solicitante, id_tipo_tramite, solicito, departamento, provincia, distrito,
       domicilio, celular, correo, fundamento, documentos_adjuntos, archivo_nombre,
       archivo_tipo, archivo_tamano, folios, fecha_declarada, estado, fecha_registro,
       fecha_actualizacion, fecha_eliminacion, fecha_restauracion
FROM dbo.tramite_fut ORDER BY fecha_registro DESC;
GO
SELECT id_area, nombre AS area_responsable FROM dbo.area
WHERE estado=1 AND nombre IN (N'DIRECCIÓN',N'ADMINISTRACIÓN',N'SECRETARÍA') ORDER BY nombre;
GO
PRINT 'FUT script completed. Review validation results and test the backend endpoints.';
GO

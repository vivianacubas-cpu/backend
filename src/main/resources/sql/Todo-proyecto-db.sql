/*
===============================================================================
  FUT SYSTEM — I.E. N.° 20191 ALFONSO UGARTE
  Database    : AlfonsoUgarteDB
  Engine      : Microsoft SQL Server
  Script      : Todo-proyecto-db.sql
  Description : Complete idempotent script.
                Creates the database, all tables, indexes, catalog data,
                10 demo applicants and 10 demo FUT management records.
                Does not drop tables or existing records.
                Run in SQL Server Management Studio or Azure Data Studio.
===============================================================================
*/

USE master;
GO

IF DB_ID(N'AlfonsoUgarteDB') IS NULL
BEGIN
    CREATE DATABASE AlfonsoUgarteDB;
    PRINT 'Database AlfonsoUgarteDB created.';
END
ELSE
    PRINT 'Database AlfonsoUgarteDB already exists.';
GO

USE AlfonsoUgarteDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/*=============================================================================
  1. DOCUMENT TYPE
=============================================================================*/

IF OBJECT_ID(N'dbo.tipo_documento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tipo_documento
    (
        id_tipo_documento INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_tipo_documento PRIMARY KEY,
        codigo    VARCHAR(10)   NOT NULL CONSTRAINT UQ_tipo_documento_codigo UNIQUE,
        nombre    NVARCHAR(80)  NOT NULL,
        estado    BIT           NOT NULL CONSTRAINT DF_tipo_documento_estado DEFAULT (1)
    );
    PRINT 'Table tipo_documento created.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.tipo_documento WHERE codigo = 'DNI')
    INSERT INTO dbo.tipo_documento (codigo, nombre, estado)
    VALUES ('DNI', N'Documento Nacional de Identidad', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.tipo_documento WHERE codigo = 'CNE')
    INSERT INTO dbo.tipo_documento (codigo, nombre, estado)
    VALUES ('CNE', N'Carné de Extranjería', 1);
GO

/*=============================================================================
  2. UBIGEO — Lima / Cañete and its districts
=============================================================================*/

IF OBJECT_ID(N'dbo.ubigeo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ubigeo
    (
        id_ubigeo    INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ubigeo PRIMARY KEY,
        codigo       VARCHAR(6)         NULL,
        departamento NVARCHAR(100)      NOT NULL,
        provincia    NVARCHAR(100)      NOT NULL,
        distrito     NVARCHAR(100)      NOT NULL,
        estado       BIT                NOT NULL CONSTRAINT DF_ubigeo_estado DEFAULT (1)
    );
    PRINT 'Table ubigeo created.';
END;
GO

INSERT INTO dbo.ubigeo (codigo, departamento, provincia, distrito, estado)
SELECT v.codigo, N'Lima', N'Cañete', v.distrito, 1
FROM (VALUES
    ('150501', N'San Vicente de Cañete'),
    ('150507', N'Imperial'),
    ('150509', N'Mala'),
    ('150510', N'Nuevo Imperial'),
    ('150512', N'Quilmaná'),
    ('150504', N'Cerro Azul'),
    ('150514', N'San Luis'),
    ('150502', N'Asia'),
    ('150508', N'Lunahuaná'),
    ('150511', N'Pacarán')
) AS v(codigo, distrito)
WHERE NOT EXISTS
(
    SELECT 1 FROM dbo.ubigeo u
    WHERE u.departamento = N'Lima'
      AND u.provincia    = N'Cañete'
      AND u.distrito     = v.distrito
);
GO

/*=============================================================================
  3. AREA — Management, Secretary and Administration
=============================================================================*/

IF OBJECT_ID(N'dbo.area', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.area
    (
        id_area     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_area PRIMARY KEY,
        nombre      NVARCHAR(100)      NOT NULL CONSTRAINT UQ_area_nombre UNIQUE,
        descripcion NVARCHAR(250)      NULL,
        estado      BIT                NOT NULL CONSTRAINT DF_area_estado DEFAULT (1)
    );
    PRINT 'Table area created.';
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre = N'DIRECCIÓN')
    INSERT INTO dbo.area (nombre, descripcion, estado)
    VALUES (N'DIRECCIÓN', N'Institutional management office', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre = N'SECRETARÍA')
    INSERT INTO dbo.area (nombre, descripcion, estado)
    VALUES (N'SECRETARÍA', N'Academic secretary office', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.area WHERE nombre = N'ADMINISTRACIÓN')
    INSERT INTO dbo.area (nombre, descripcion, estado)
    VALUES (N'ADMINISTRACIÓN', N'Administration department', 1);
GO

/*=============================================================================
  4. PROCEDURE TYPE (TIPO_TRAMITE)
=============================================================================*/

IF OBJECT_ID(N'dbo.tipo_tramite', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tipo_tramite
    (
        id_tipo_tramite      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tipo_tramite PRIMARY KEY,
        codigo               VARCHAR(20)        NULL,
        nombre               VARCHAR(100)       NOT NULL,
        categoria            NVARCHAR(60)       NULL,
        descripcion          VARCHAR(250)       NULL,
        requisitos           VARCHAR(500)       NULL,
        plazo_estimado_dias  INT                NULL,
        requiere_documentos  BIT                NOT NULL CONSTRAINT DF_tipo_tramite_req_doc DEFAULT (0),
        vigente_desde        DATE               NULL,
        estado               BIT                NOT NULL CONSTRAINT DF_tipo_tramite_estado DEFAULT (1),
        fecha_registro       DATETIME2          NOT NULL CONSTRAINT DF_tipo_tramite_fecha_reg DEFAULT SYSDATETIME(),
        fecha_actualizacion  DATETIME2          NULL,
        fecha_eliminacion    DATETIME2          NULL,
        fecha_restauracion   DATETIME2          NULL
    );
    PRINT 'Table tipo_tramite created.';
END;
GO

/* Additive migrations in case the table already existed without some columns */
IF COL_LENGTH('dbo.tipo_tramite','codigo') IS NULL ALTER TABLE dbo.tipo_tramite ADD codigo VARCHAR(20) NULL;
IF COL_LENGTH('dbo.tipo_tramite','categoria') IS NULL ALTER TABLE dbo.tipo_tramite ADD categoria NVARCHAR(60) NULL;
IF COL_LENGTH('dbo.tipo_tramite','requiere_documentos') IS NULL ALTER TABLE dbo.tipo_tramite ADD requiere_documentos BIT NOT NULL CONSTRAINT DF_tipo_tramite_req_doc_mig DEFAULT (0);
IF COL_LENGTH('dbo.tipo_tramite','vigente_desde') IS NULL ALTER TABLE dbo.tipo_tramite ADD vigente_desde DATE NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_registro') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_registro DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_actualizacion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_eliminacion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_eliminacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tipo_tramite','fecha_restauracion') IS NULL ALTER TABLE dbo.tipo_tramite ADD fecha_restauracion DATETIME2 NULL;
GO

DECLARE @Tipos TABLE (codigo VARCHAR(20), nombre VARCHAR(100), categoria NVARCHAR(60), descripcion VARCHAR(250), dias INT, docs BIT);
INSERT INTO @Tipos VALUES
('TR-001','SOLICITUD DE VACANTES',          N'ENROLLMENT',    'Application for school enrollment vacancy',      7,  1),
('TR-002','TRASLADO DE MATRÍCULA',           N'ENROLLMENT',    'Application for enrollment transfer',            7,  1),
('TR-003','CONSTANCIA DE ESTUDIOS O DE MATRÍCULA', N'CERTIFICATES','Certificate of studies or enrollment',      5,  0),
('TR-004','CERTIFICADO DE ESTUDIOS',         N'CERTIFICATES',  'Issuance of academic certificate',              10, 1),
('TR-005','RECTIFICACIÓN DE DATOS',          N'RECORDS',       'Correction of registered personal data',         5,  1),
('TR-006','JUSTIFICACIÓN DE INASISTENCIAS PROLONGADAS', N'ABSENCES','Justification for extended absences',      3,  1),
('TR-007','LICENCIAS Y PERMISOS',            N'PERMITS',       'Application for leave or permit',               5,  1),
('TR-008','CONSTANCIA DE PAGO O DE TRABAJO', N'CERTIFICATES',  'Payment or employment certificate',             5,  0),
('TR-009','ENTREGA DE LEGAJOS Y DOCUMENTOS', N'DOCUMENTS',     'Delivery of files and documents',               5,  1),
('TR-010','RECLAMOS O ABSOLUCIÓN DE CONSULTAS ADMINISTRATIVAS', N'COMPLAINTS','Administrative complaint or inquiry', 10, 1),
('TR-011','OTROS',                           N'OTHER',         'Other administrative procedures',               10, 0);

INSERT INTO dbo.tipo_tramite (codigo, nombre, categoria, descripcion, plazo_estimado_dias, requiere_documentos, estado)
SELECT t.codigo, t.nombre, t.categoria, t.descripcion, t.dias, t.docs, 1
FROM @Tipos t
WHERE NOT EXISTS (SELECT 1 FROM dbo.tipo_tramite x WHERE x.nombre = t.nombre);
GO

/*=============================================================================
  5. APPLICANT (SOLICITANTE)
=============================================================================*/

IF OBJECT_ID(N'dbo.solicitante', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.solicitante
    (
        id_solicitante    INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_solicitante PRIMARY KEY,
        id_tipo_documento INT               NOT NULL,
        id_ubigeo         INT               NOT NULL,
        departamento      NVARCHAR(100)     NULL,
        provincia         NVARCHAR(100)     NULL,
        distrito          NVARCHAR(100)     NULL,
        numero_documento  VARCHAR(20)       NOT NULL,
        nombres           VARCHAR(100)      NOT NULL,
        apellido_paterno  VARCHAR(100)      NOT NULL,
        apellido_materno  VARCHAR(100)      NOT NULL,
        telefono          VARCHAR(20)       NULL,
        correo            VARCHAR(150)      NULL,
        direccion         VARCHAR(250)      NULL,
        estado            CHAR(1)           NOT NULL CONSTRAINT DF_solicitante_estado DEFAULT ('A'),
        fecha_registro    DATETIME2         NOT NULL CONSTRAINT DF_solicitante_fecha_reg DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2       NULL,
        fecha_eliminacion   DATETIME2       NULL,
        fecha_restauracion  DATETIME2       NULL,
        CONSTRAINT FK_solicitante_tipo_documento FOREIGN KEY (id_tipo_documento) REFERENCES dbo.tipo_documento(id_tipo_documento),
        CONSTRAINT FK_solicitante_ubigeo         FOREIGN KEY (id_ubigeo)         REFERENCES dbo.ubigeo(id_ubigeo),
        CONSTRAINT CK_solicitante_estado         CHECK (estado IN ('A','I'))
    );
    PRINT 'Table solicitante created.';
END;
GO

/* Additive migration */
IF COL_LENGTH('dbo.solicitante','departamento')      IS NULL ALTER TABLE dbo.solicitante ADD departamento      NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','provincia')         IS NULL ALTER TABLE dbo.solicitante ADD provincia         NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','distrito')          IS NULL ALTER TABLE dbo.solicitante ADD distrito          NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.solicitante','fecha_actualizacion') IS NULL ALTER TABLE dbo.solicitante ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.solicitante','fecha_eliminacion')   IS NULL ALTER TABLE dbo.solicitante ADD fecha_eliminacion   DATETIME2 NULL;
IF COL_LENGTH('dbo.solicitante','fecha_restauracion')  IS NULL ALTER TABLE dbo.solicitante ADD fecha_restauracion  DATETIME2 NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_solicitante_numero_documento' AND object_id = OBJECT_ID('dbo.solicitante'))
    CREATE INDEX IX_solicitante_numero_documento ON dbo.solicitante (numero_documento);
GO

/* ---- 10 demo applicants ---- */

DECLARE @Sol TABLE
(
    numero_documento VARCHAR(20) PRIMARY KEY,
    nombres          VARCHAR(100),
    ap_paterno       VARCHAR(100),
    ap_materno       VARCHAR(100),
    distrito         NVARCHAR(100),
    direccion        VARCHAR(250),
    telefono         VARCHAR(20),
    correo           VARCHAR(150)
);

INSERT INTO @Sol VALUES
('70123456', 'Lucía Fernanda',  'Sánchez', 'Pérez',    N'Asia',                'Calle Las Flores 89',      '914567823', 'lucia@example.com'),
('71234567', 'Ana Lucía',       'Torres',  'Mendoza',  N'Nuevo Imperial',      'Av. Principal 250',        '956789123', 'ana@example.com'),
('72345678', 'Daniela Sofía',   'Rojas',   'Vargas',   N'Cerro Azul',          'Jr. Lima 320',             '934567812', 'daniela@example.com'),
('73456789', 'Carlos Alberto',  'García',  'López',    N'Mala',                'Jr. Comercio 120',         '912345678', 'carlos@example.com'),
('74561238', 'José Miguel',     'Flores',  'Castillo', N'Quilmaná',            'Calle Los Pinos 45',       '945678123', 'jose@example.com'),
('75678912', 'Pedro Antonio',   'Morales', 'Díaz',     N'San Luis',            'Av. Costanera 150',        '923456781', 'pedro@example.com'),
('76543456', 'Viviana',         'Cubas',   'Rufasto',  N'San Vicente de Cañete','Jr. Los Álamos 123',      '925554572', 'viviana@example.com'),
('77891234', 'Jorge Luis',      'Vásquez', 'Huamán',   N'Lunahuaná',           'Jr. Grau 210',             '978123456', 'jorge@example.com'),
('78912345', 'Rosa Elena',      'Castro',  'Reyes',    N'Pacarán',             'Calle Los Olivos 75',      '965432187', 'rosa@example.com'),
('70812345', 'Mariana',         'Quispe',  'Ramírez',  N'Imperial',            'Av. Mariscal Benavides 456','987654321','mariana@example.com');

DECLARE @IdDNI INT = (SELECT TOP 1 id_tipo_documento FROM dbo.tipo_documento WHERE codigo = 'DNI');

BEGIN TRY
    BEGIN TRANSACTION;

    /* Update existing applicants */
    UPDATE s
    SET
        s.id_tipo_documento = @IdDNI,
        s.id_ubigeo         = u.id_ubigeo,
        s.nombres           = v.nombres,
        s.apellido_paterno  = v.ap_paterno,
        s.apellido_materno  = v.ap_materno,
        s.departamento      = N'Lima',
        s.provincia         = N'Cañete',
        s.distrito          = v.distrito,
        s.direccion         = v.direccion,
        s.telefono          = v.telefono,
        s.correo            = v.correo,
        s.estado            = 'A'
    FROM dbo.solicitante s
    INNER JOIN @Sol v ON v.numero_documento = s.numero_documento
    INNER JOIN dbo.ubigeo u
        ON u.departamento = N'Lima' AND u.provincia = N'Cañete' AND u.distrito = v.distrito;

    /* Insert new applicants */
    INSERT INTO dbo.solicitante
        (id_tipo_documento, id_ubigeo, departamento, provincia, distrito,
         numero_documento, nombres, apellido_paterno, apellido_materno,
         telefono, correo, direccion, estado, fecha_registro)
    SELECT
        @IdDNI, u.id_ubigeo, N'Lima', N'Cañete', v.distrito,
        v.numero_documento, v.nombres, v.ap_paterno, v.ap_materno,
        v.telefono, v.correo, v.direccion, 'A', SYSDATETIME()
    FROM @Sol v
    INNER JOIN dbo.ubigeo u
        ON u.departamento = N'Lima' AND u.provincia = N'Cañete' AND u.distrito = v.distrito
    WHERE NOT EXISTS
        (SELECT 1 FROM dbo.solicitante s WHERE s.numero_documento = v.numero_documento);

    COMMIT TRANSACTION;
    PRINT '10 applicants verified and registered.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/*=============================================================================
  6. FUT REQUEST (TRAMITE_FUT)
=============================================================================*/

IF OBJECT_ID(N'dbo.tramite_fut', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tramite_fut
    (
        id_solicitud        BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tramite_fut PRIMARY KEY,
        id_solicitante      INT                  NULL,
        id_tipo_tramite     INT                  NULL,
        codigo_seguimiento  VARCHAR(60)          NOT NULL,
        solicito            NVARCHAR(500)        NOT NULL,
        nombre_completo     NVARCHAR(200)        NOT NULL,
        dni                 VARCHAR(20)          NOT NULL,
        tipo_documento      VARCHAR(10)          NOT NULL CONSTRAINT DF_tramite_fut_tipo_documento DEFAULT ('DNI'),
        tipo_solicitante    NVARCHAR(100)        NULL,
        departamento        NVARCHAR(100)        NULL,
        numero_depto        NVARCHAR(50)         NULL,
        telefono_fijo       VARCHAR(20)          NULL,
        folios              INT                  NOT NULL,
        archivo_nombre      NVARCHAR(255)        NULL,
        archivo_tipo        VARCHAR(150)         NULL,
        archivo_tamano      BIGINT               NULL,
        idempotency_key     VARCHAR(80)          NULL,
        correo              NVARCHAR(150)        NULL,
        celular             VARCHAR(9)           NOT NULL,
        domicilio           NVARCHAR(250)        NOT NULL,
        distrito            NVARCHAR(100)        NOT NULL,
        provincia           NVARCHAR(100)        NOT NULL,
        fundamento          NVARCHAR(2000)       NOT NULL,
        documentos_adjuntos NVARCHAR(1000)       NULL,
        fecha_declarada     DATE                 NOT NULL,
        nota_firma          NVARCHAR(500)        NULL,
        estado              VARCHAR(30)          NOT NULL CONSTRAINT DF_tramite_fut_estado DEFAULT ('RECIBIDO'),
        observacion         NVARCHAR(2000)       NULL,
        fecha_registro      DATETIME2            NOT NULL CONSTRAINT DF_tramite_fut_fecha_reg DEFAULT SYSDATETIME(),
        fecha_actualizacion DATETIME2            NULL,
        fecha_eliminacion   DATETIME2            NULL,
        fecha_restauracion  DATETIME2            NULL,
        CONSTRAINT UQ_tramite_fut_codigo_seg     UNIQUE (codigo_seguimiento),
        CONSTRAINT FK_tramite_fut_solicitante    FOREIGN KEY (id_solicitante)  REFERENCES dbo.solicitante(id_solicitante),
        CONSTRAINT FK_tramite_fut_tipo_tramite   FOREIGN KEY (id_tipo_tramite) REFERENCES dbo.tipo_tramite(id_tipo_tramite),
        CONSTRAINT CK_tramite_fut_estado         CHECK (estado IN ('RECIBIDO','EN_REVISION','OBSERVADO','ATENDIDO','RECHAZADO')),
        CONSTRAINT CK_tramite_fut_celular        CHECK (LEN(celular) = 9 AND celular NOT LIKE '%[^0-9]%')
    );
    PRINT 'Table tramite_fut created.';
END;
GO

/* Additive migration */
IF COL_LENGTH('dbo.tramite_fut','id_tipo_tramite')    IS NULL ALTER TABLE dbo.tramite_fut ADD id_tipo_tramite INT NULL;
IF COL_LENGTH('dbo.tramite_fut','tipo_documento')     IS NULL ALTER TABLE dbo.tramite_fut ADD tipo_documento VARCHAR(10) NOT NULL CONSTRAINT DF_tramite_fut_tipo_doc_mig DEFAULT ('DNI');
IF COL_LENGTH('dbo.tramite_fut','fecha_actualizacion') IS NULL ALTER TABLE dbo.tramite_fut ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.tramite_fut','fecha_restauracion')  IS NULL ALTER TABLE dbo.tramite_fut ADD fecha_restauracion  DATETIME2 NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_tramite_fut_idempotency_key' AND object_id = OBJECT_ID('dbo.tramite_fut'))
    CREATE UNIQUE INDEX UX_tramite_fut_idempotency_key ON dbo.tramite_fut (idempotency_key) WHERE idempotency_key IS NOT NULL;
GO

/*=============================================================================
  7. TRACKING (SEGUIMIENTO_TRAMITE)
=============================================================================*/

IF OBJECT_ID(N'dbo.seguimiento_tramite', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.seguimiento_tramite
    (
        id_seguimiento  BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_seguimiento_tramite PRIMARY KEY,
        id_solicitud    BIGINT               NOT NULL,
        estado_anterior VARCHAR(30)          NULL,
        estado_nuevo    VARCHAR(30)          NOT NULL,
        observacion     NVARCHAR(2000)       NULL,
        fecha_cambio    DATETIME2            NOT NULL CONSTRAINT DF_seguimiento_fecha_cambio DEFAULT SYSDATETIME(),
        estado          CHAR(1)              NOT NULL CONSTRAINT DF_seguimiento_estado DEFAULT ('A'),
        fecha_actualizacion DATETIME2        NULL,
        fecha_eliminacion   DATETIME2        NULL,
        fecha_restauracion  DATETIME2        NULL,
        CONSTRAINT FK_seguimiento_tramite_fut FOREIGN KEY (id_solicitud) REFERENCES dbo.tramite_fut(id_solicitud),
        CONSTRAINT CK_seguimiento_estado      CHECK (estado IN ('A','I'))
    );
    PRINT 'Table seguimiento_tramite created.';
END;
GO

/* Additive migration */
IF COL_LENGTH('dbo.seguimiento_tramite','estado')             IS NULL ALTER TABLE dbo.seguimiento_tramite ADD estado             CHAR(1) NOT NULL CONSTRAINT DF_seg_estado_mig DEFAULT ('A');
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_actualizacion') IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_eliminacion')   IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_eliminacion   DATETIME2 NULL;
IF COL_LENGTH('dbo.seguimiento_tramite','fecha_restauracion')  IS NULL ALTER TABLE dbo.seguimiento_tramite ADD fecha_restauracion  DATETIME2 NULL;
GO

/*=============================================================================
  8. PROCEDURE RESPONSE (RESPUESTA_TRAMITE)
=============================================================================*/

IF OBJECT_ID(N'dbo.respuesta_tramite', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.respuesta_tramite
    (
        id_respuesta    BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_respuesta_tramite PRIMARY KEY,
        id_solicitud    BIGINT               NOT NULL,
        id_usuario      INT                  NULL,
        respuesta       NVARCHAR(2000)       NOT NULL,
        fecha_respuesta DATETIME2            NOT NULL CONSTRAINT DF_respuesta_fecha DEFAULT SYSDATETIME(),
        CONSTRAINT FK_respuesta_tramite_fut FOREIGN KEY (id_solicitud) REFERENCES dbo.tramite_fut(id_solicitud)
    );
    PRINT 'Table respuesta_tramite created.';
END;
GO

/*=============================================================================
  9. FUT PROCEDURE MANAGEMENT (GESTION_TRAMITE_FUT)
     estado_gestion accepts: PENDIENTE | EN_PROCESO | OBSERVADO | APROBADO | RECHAZADO
=============================================================================*/

IF OBJECT_ID(N'dbo.gestion_tramite_fut', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.gestion_tramite_fut
    (
        id_gestion                     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_gestion_tramite_fut PRIMARY KEY,
        id_solicitante                 INT               NOT NULL,
        nombre_responsable             NVARCHAR(150)     NULL,
        nombres_responsable            NVARCHAR(100)     NULL,
        apellido_paterno_responsable   NVARCHAR(100)     NULL,
        apellido_materno_responsable   NVARCHAR(100)     NULL,
        id_area_responsable            INT               NULL,
        fecha_recepcion                DATE              NOT NULL,
        tipo_gestion                   VARCHAR(30)       NOT NULL,
        prioridad                      VARCHAR(15)       NOT NULL CONSTRAINT DF_gestion_prioridad     DEFAULT ('NORMAL'),
        fecha_limite                   DATE              NULL,
        estado_gestion                 VARCHAR(20)       NOT NULL CONSTRAINT DF_gestion_estado_gestion DEFAULT ('PENDIENTE'),
        decision                       NVARCHAR(500)     NULL,
        fundamento_decision            NVARCHAR(2000)    NULL,
        fecha_resolucion               DATE              NULL,
        observaciones                  NVARCHAR(2000)    NULL,
        estado                         CHAR(1)           NOT NULL CONSTRAINT DF_gestion_estado DEFAULT ('A'),
        fecha_registro                 DATETIME2         NOT NULL CONSTRAINT DF_gestion_fecha_reg DEFAULT SYSDATETIME(),
        fecha_actualizacion            DATETIME2         NULL,
        fecha_eliminacion              DATETIME2         NULL,
        fecha_restauracion             DATETIME2         NULL,
        CONSTRAINT FK_gestion_tramite_fut_solicitante FOREIGN KEY (id_solicitante)     REFERENCES dbo.solicitante(id_solicitante),
        CONSTRAINT FK_gestion_tramite_fut_area        FOREIGN KEY (id_area_responsable) REFERENCES dbo.area(id_area),
        CONSTRAINT CK_gestion_tipo           CHECK (tipo_gestion   IN ('RECEPCION','EVALUACION','DERIVACION','APROBACION','RECHAZO','SUBSANACION')),
        CONSTRAINT CK_gestion_prioridad      CHECK (prioridad      IN ('NORMAL','URGENTE','MUY_URGENTE')),
        CONSTRAINT CK_gestion_estado_gestion CHECK (estado_gestion IN ('PENDIENTE','EN_PROCESO','OBSERVADO','APROBADO','RECHAZADO')),
        CONSTRAINT CK_gestion_estado          CHECK (estado         IN ('A','I'))
    );
    PRINT 'Table gestion_tramite_fut created.';
END;
GO

/* Additive migration */
IF COL_LENGTH('dbo.gestion_tramite_fut','nombre_responsable')           IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD nombre_responsable           NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','nombres_responsable')          IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD nombres_responsable          NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','apellido_paterno_responsable') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD apellido_paterno_responsable NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','apellido_materno_responsable') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD apellido_materno_responsable NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','prioridad')      IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD prioridad      VARCHAR(15) NOT NULL CONSTRAINT DF_gestion_prioridad_mig      DEFAULT ('NORMAL');
IF COL_LENGTH('dbo.gestion_tramite_fut','estado_gestion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD estado_gestion VARCHAR(20) NOT NULL CONSTRAINT DF_gestion_estado_gestion_mig DEFAULT ('PENDIENTE');
IF COL_LENGTH('dbo.gestion_tramite_fut','estado')         IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD estado         CHAR(1)     NOT NULL CONSTRAINT DF_gestion_estado_mig         DEFAULT ('A');
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_actualizacion') IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_eliminacion')   IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_eliminacion   DATETIME2 NULL;
IF COL_LENGTH('dbo.gestion_tramite_fut','fecha_restauracion')  IS NULL ALTER TABLE dbo.gestion_tramite_fut ADD fecha_restauracion  DATETIME2 NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_gestion_fut_estado_fecha' AND object_id = OBJECT_ID('dbo.gestion_tramite_fut'))
    CREATE INDEX IX_gestion_fut_estado_fecha ON dbo.gestion_tramite_fut (estado, fecha_registro DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_gestion_fut_solicitante_fecha' AND object_id = OBJECT_ID('dbo.gestion_tramite_fut'))
    CREATE INDEX IX_gestion_fut_solicitante_fecha ON dbo.gestion_tramite_fut (id_solicitante, fecha_registro DESC);
GO

/*=============================================================================
  10. SYSTEM USER (USUARIO)
=============================================================================*/

IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuario
    (
        id_usuario          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_usuario PRIMARY KEY,
        id_rol              INT               NOT NULL,
        id_area             INT               NOT NULL CONSTRAINT DF_usuario_id_area    DEFAULT (1),
        id_ubigeo           INT               NOT NULL CONSTRAINT DF_usuario_id_ubigeo  DEFAULT (1),
        nombre_usuario      VARCHAR(50)       NOT NULL UNIQUE,
        nombres             VARCHAR(80)       NOT NULL,
        apellido_paterno    VARCHAR(60)       NOT NULL,
        apellido_materno    VARCHAR(60)       NOT NULL,
        correo              VARCHAR(120)      NOT NULL UNIQUE,
        password_hash       VARCHAR(255)      NOT NULL,
        fecha_registro      DATETIME2         NOT NULL CONSTRAINT DF_usuario_fecha_reg   DEFAULT SYSUTCDATETIME(),
        estado              BIT               NOT NULL CONSTRAINT DF_usuario_estado       DEFAULT (1),
        fecha_creacion      DATETIME2         NOT NULL CONSTRAINT DF_usuario_fecha_crea   DEFAULT SYSUTCDATETIME(),
        fecha_actualizacion DATETIME2         NULL,
        fecha_eliminacion   DATETIME2         NULL,
        fecha_restauracion  DATETIME2         NULL,
        CONSTRAINT FK_usuario_area   FOREIGN KEY (id_area)   REFERENCES dbo.area(id_area),
        CONSTRAINT FK_usuario_ubigeo FOREIGN KEY (id_ubigeo) REFERENCES dbo.ubigeo(id_ubigeo)
    );
    PRINT 'Table usuario created.';
END;
GO

/* Additive migration */
IF COL_LENGTH('dbo.usuario','id_area')             IS NULL ALTER TABLE dbo.usuario ADD id_area      INT NOT NULL CONSTRAINT DF_usuario_id_area_mig    DEFAULT (1);
IF COL_LENGTH('dbo.usuario','id_ubigeo')           IS NULL ALTER TABLE dbo.usuario ADD id_ubigeo    INT NOT NULL CONSTRAINT DF_usuario_id_ubigeo_mig  DEFAULT (1);
IF COL_LENGTH('dbo.usuario','fecha_creacion')      IS NULL ALTER TABLE dbo.usuario ADD fecha_creacion DATETIME2 NOT NULL CONSTRAINT DF_usuario_fecha_crea_mig DEFAULT SYSUTCDATETIME();
IF COL_LENGTH('dbo.usuario','fecha_actualizacion') IS NULL ALTER TABLE dbo.usuario ADD fecha_actualizacion DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','fecha_eliminacion')   IS NULL ALTER TABLE dbo.usuario ADD fecha_eliminacion   DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','fecha_restauracion')  IS NULL ALTER TABLE dbo.usuario ADD fecha_restauracion  DATETIME2 NULL;
IF COL_LENGTH('dbo.usuario','estado')              IS NULL ALTER TABLE dbo.usuario ADD estado BIT NOT NULL CONSTRAINT DF_usuario_estado_mig DEFAULT (1);
GO

/* ---- Demo users (BCrypt hashed passwords) ---- */
-- directora.demo  → DirectoraDemo2026!
-- secretaria.demo → SecretariaDemo2026!
-- admin.demo      → AdminDemo2026!

IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'directora.demo')
    INSERT INTO dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (1,
        (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'DIRECCIÓN'),
        (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo),
        'directora.demo', N'Directora Demo', N'Demo', N'Apellido',
        'directora.demo@ie20191.edu.pe',
        '$2a$10$CMAcfF39TLpMWC08Ma9K/O50MQ5i3KRmat6oI/CA5Cb0YAIOrjCBS');

IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'secretaria.demo')
    INSERT INTO dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (2,
        (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'SECRETARÍA'),
        (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo),
        'secretaria.demo', N'Secretaria Demo', N'Demo', N'Apellido',
        'secretaria.demo@ie20191.edu.pe',
        '$2a$10$ylzLIwi.KRuRqAUhcjGuAeAk6Wr5PKkSYkd3XqbK45rT5e2Q/RH8q');

IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE nombre_usuario = 'admin.demo')
    INSERT INTO dbo.usuario (id_rol, id_area, id_ubigeo, nombre_usuario, nombres, apellido_paterno, apellido_materno, correo, password_hash)
    VALUES (3,
        (SELECT TOP 1 id_area FROM dbo.area WHERE nombre = N'ADMINISTRACIÓN'),
        (SELECT TOP 1 id_ubigeo FROM dbo.ubigeo ORDER BY id_ubigeo),
        'admin.demo', N'Administrador Demo', N'Demo', N'Apellido',
        'admin.demo@ie20191.edu.pe',
        '$2a$10$CMAcfF39TLpMWC08Ma9K/O50MQ5i3KRmat6oI/CA5Cb0YAIOrjCBS');
GO

/*=============================================================================
  11. DEMO DATA: 10 FUT MANAGEMENT RECORDS
      estado_gestion uses ONLY the values allowed by the CHECK constraint:
      PENDIENTE | EN_PROCESO | OBSERVADO | APROBADO | RECHAZADO

      Distribution of the 10 records:
        APROBADO  → 6  (DNI: 73456789, 74561238, 75678912, 76543456, 77891234, 78912345)
        OBSERVADO → 2  (DNI: 70123456, 71234567)
        RECHAZADO → 1  (DNI: 70812345)
        PENDIENTE → 1  (DNI: 72345678)
=============================================================================*/

DECLARE @Gestiones TABLE
(
    numero_documento      VARCHAR(20) PRIMARY KEY,
    resp_nombres          NVARCHAR(100),
    resp_paterno          NVARCHAR(100),
    resp_materno          NVARCHAR(100),
    area                  NVARCHAR(100),
    tipo_gestion          VARCHAR(30),
    prioridad             VARCHAR(15),
    fecha_recepcion       DATE,
    fecha_limite          DATE,
    estado_gestion        VARCHAR(20),   -- PENDIENTE|EN_PROCESO|OBSERVADO|APROBADO|RECHAZADO
    fecha_resolucion      DATE,
    decision              NVARCHAR(500),
    fundamento            NVARCHAR(2000),
    observaciones         NVARCHAR(2000)
);

INSERT INTO @Gestiones VALUES
/* 1 - APPROVED */
('73456789', N'Ana María',    N'Flores',   N'Quispe',  N'SECRETARÍA',    'APROBACION', 'NORMAL',   '20261104', '20261110', 'APROBADO',  '20261104',
 N'APROBADO',
 N'The submitted documentation meets all the established academic requirements.',
 N'Procedure handled satisfactorily.'),

/* 2 - APPROVED */
('74561238', N'Rosa Isabel',  N'García',   N'Pérez',   N'SECRETARÍA',    'APROBACION', 'URGENTE',  '20261104', '20261114', 'APROBADO',  '20261104',
 N'APROBADO',
 N'The request meets the required administrative and academic conditions.',
 N'Request evaluated and approved correctly.'),

/* 3 - APPROVED */
('75678912', N'Lucía Fernanda',N'Vargas',  N'León',    N'ADMINISTRACIÓN','APROBACION', 'NORMAL',   '20261104', '20261109', 'APROBADO',  '20261104',
 N'APROBADO',
 N'Administrative requirements have been satisfactorily verified.',
 N'Processing completed with a favorable outcome.'),

/* 4 - APPROVED */
('76543456', N'María Elena',  N'López',    N'Campos',  N'SECRETARÍA',    'APROBACION', 'NORMAL',   '20261104', '20261111', 'APROBADO',  '20261104',
 N'APROBADO',
 N'The request meets the established requirements for processing.',
 N'Request reviewed and registered correctly.'),

/* 5 - APPROVED */
('77891234', N'Jorge Luis',   N'Torres',   N'Vega',    N'DIRECCIÓN',     'APROBACION', 'URGENTE',  '20261104', '20261115', 'APROBADO',  '20261104',
 N'APROBADO',
 N'The request complies with the applicable institutional regulations.',
 N'Management concluded satisfactorily.'),

/* 6 - APPROVED */
('78912345', N'Elena Patricia',N'Ramos',   N'Flores',  N'ADMINISTRACIÓN','APROBACION', 'NORMAL',   '20261104', '20261112', 'APROBADO',  '20261104',
 N'APROBADO',
 N'The documentation meets all required administrative standards.',
 N'Request evaluated and approved by Administration.'),

/* 7 - OBSERVED */
('70123456', N'Pedro Antonio', N'Sánchez', N'Morales', N'SECRETARÍA',    'EVALUACION', 'URGENTE',  '20261104', '20261106', 'OBSERVADO', '20261104',
 N'OBSERVADO',
 N'The request requires the applicant to correct the documents noted by the Secretary.',
 N'The corresponding observations have been communicated to the applicant.'),

/* 8 - OBSERVED */
('71234567', N'Carlos Alberto',N'Mendoza', N'Rojas',   N'ADMINISTRACIÓN','EVALUACION', 'URGENTE',  '20261104', '20261106', 'OBSERVADO', '20261104',
 N'OBSERVADO',
 N'Additional documentation is required to continue processing the request.',
 N'The applicant must address the noted observations.'),

/* 9 - REJECTED */
('70812345', N'José Luis',    N'Ramírez',  N'Torres',  N'DIRECCIÓN',     'RECHAZO',    'URGENTE',  '20261104', '20261107', 'RECHAZADO', '20261104',
 N'RECHAZADO',
 N'The submitted documentation does not meet the minimum required standards.',
 N'The applicant was notified of the rejection of their request.'),

/* 10 - PENDING */
('72345678', N'Miguel Ángel', N'Castillo', N'Díaz',    N'DIRECCIÓN',     'RECEPCION',  'NORMAL',   '20261104', '20261112', 'PENDIENTE', NULL,
 NULL,
 N'The documentation complies with the preliminary institutional regulations.',
 N'Management record pending review by Management.');

BEGIN TRY
    BEGIN TRANSACTION;

    /* Update already existing management records for those applicants */
    UPDATE g
    SET
        g.nombre_responsable           = CONCAT(v.resp_nombres, N' ', v.resp_paterno, N' ', v.resp_materno),
        g.nombres_responsable          = v.resp_nombres,
        g.apellido_paterno_responsable = v.resp_paterno,
        g.apellido_materno_responsable = v.resp_materno,
        g.id_area_responsable          = a.id_area,
        g.fecha_recepcion              = v.fecha_recepcion,
        g.tipo_gestion                 = v.tipo_gestion,
        g.prioridad                    = v.prioridad,
        g.fecha_limite                 = v.fecha_limite,
        g.estado_gestion               = v.estado_gestion,
        g.fecha_resolucion             = v.fecha_resolucion,
        g.decision                     = v.decision,
        g.fundamento_decision          = v.fundamento,
        g.observaciones                = v.observaciones,
        g.estado                       = 'A',
        g.fecha_actualizacion          = SYSDATETIME()
    FROM dbo.gestion_tramite_fut g
    INNER JOIN dbo.solicitante s   ON s.id_solicitante = g.id_solicitante
    INNER JOIN @Gestiones v        ON v.numero_documento = s.numero_documento
    INNER JOIN dbo.area a          ON a.nombre = v.area;

    /* Insert management records that do not yet exist */
    INSERT INTO dbo.gestion_tramite_fut
    (
        id_solicitante, nombre_responsable, nombres_responsable,
        apellido_paterno_responsable, apellido_materno_responsable,
        id_area_responsable, fecha_recepcion, tipo_gestion, prioridad,
        fecha_limite, estado_gestion, fecha_resolucion, decision,
        fundamento_decision, observaciones, estado, fecha_registro
    )
    SELECT
        s.id_solicitante,
        CONCAT(v.resp_nombres, N' ', v.resp_paterno, N' ', v.resp_materno),
        v.resp_nombres, v.resp_paterno, v.resp_materno,
        a.id_area, v.fecha_recepcion, v.tipo_gestion, v.prioridad,
        v.fecha_limite, v.estado_gestion, v.fecha_resolucion, v.decision,
        v.fundamento, v.observaciones, 'A', SYSDATETIME()
    FROM @Gestiones v
    INNER JOIN dbo.solicitante s ON s.numero_documento = v.numero_documento
    INNER JOIN dbo.area a        ON a.nombre = v.area
    WHERE NOT EXISTS
    (
        SELECT 1 FROM dbo.gestion_tramite_fut g
        WHERE g.id_solicitante = s.id_solicitante
    );

    COMMIT TRANSACTION;
    PRINT '10 FUT management records verified and registered.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/*=============================================================================
  12. FINAL VERIFICATION QUERIES
=============================================================================*/

-- Count by table
SELECT
    (SELECT COUNT(*) FROM dbo.tipo_documento)      AS tipo_documento,
    (SELECT COUNT(*) FROM dbo.ubigeo)              AS ubigeo,
    (SELECT COUNT(*) FROM dbo.area)                AS area,
    (SELECT COUNT(*) FROM dbo.tipo_tramite)        AS tipo_tramite,
    (SELECT COUNT(*) FROM dbo.solicitante)         AS solicitante,
    (SELECT COUNT(*) FROM dbo.tramite_fut)         AS tramite_fut,
    (SELECT COUNT(*) FROM dbo.gestion_tramite_fut) AS gestion_tramite_fut,
    (SELECT COUNT(*) FROM dbo.usuario)             AS usuario;
GO

-- Distribution of management records by estado_gestion
SELECT estado_gestion, COUNT(*) AS total
FROM dbo.gestion_tramite_fut
WHERE estado = 'A'
GROUP BY estado_gestion
ORDER BY estado_gestion;
GO

-- Full applicant list (same columns as the frontend)
SELECT
    ROW_NUMBER() OVER (ORDER BY s.id_solicitante)                       AS [#],
    td.codigo                                                           AS [DOC TYPE],
    s.numero_documento                                                  AS [DOC NUMBER],
    s.apellido_paterno                                                  AS [PATERNAL LAST NAME],
    s.apellido_materno                                                  AS [MATERNAL LAST NAME],
    s.nombres                                                           AS [FIRST NAMES],
    COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito)      AS [DISTRICT],
    COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia)     AS [PROVINCE],
    COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento)  AS [DEPARTMENT],
    s.direccion                                                         AS [ADDRESS],
    s.telefono                                                          AS [PHONE],
    s.correo                                                            AS [EMAIL],
    CASE WHEN s.estado = 'A' THEN 'ACTIVE' ELSE 'INACTIVE' END         AS [STATUS],
    CONVERT(VARCHAR(10), s.fecha_registro,      103)                    AS [REGISTERED AT],
    CONVERT(VARCHAR(10), s.fecha_actualizacion, 103)                    AS [UPDATED AT],
    CONVERT(VARCHAR(10), s.fecha_eliminacion,   103)                    AS [DELETED AT],
    CONVERT(VARCHAR(10), s.fecha_restauracion,  103)                    AS [RESTORED AT]
FROM dbo.solicitante s
INNER JOIN dbo.tipo_documento td ON td.id_tipo_documento = s.id_tipo_documento
LEFT  JOIN dbo.ubigeo u          ON u.id_ubigeo = s.id_ubigeo
ORDER BY s.id_solicitante;
GO

-- Full management record view (same columns as the frontend)
SELECT
    ROW_NUMBER() OVER (ORDER BY g.id_gestion)   AS [N.],
    s.numero_documento                           AS [DOC NUMBER],
    s.apellido_paterno                           AS [PATERNAL LAST NAME],
    s.apellido_materno                           AS [MATERNAL LAST NAME],
    s.nombres                                    AS [FIRST NAMES],
    g.nombre_responsable                         AS [RESPONSIBLE],
    a.nombre                                     AS [RESPONSIBLE AREA],
    g.tipo_gestion                               AS [MANAGEMENT TYPE],
    g.prioridad                                  AS [PRIORITY],
    g.estado_gestion                             AS [STATUS],
    g.fecha_recepcion                            AS [RECEPTION DATE],
    g.fecha_limite                               AS [DEADLINE],
    g.fecha_resolucion                           AS [RESOLUTION DATE],
    g.decision                                   AS [DECISION],
    g.fundamento_decision                        AS [DECISION BASIS],
    g.observaciones                              AS [OBSERVATIONS],
    g.fecha_registro                             AS [REGISTERED AT],
    g.fecha_actualizacion                        AS [UPDATED AT],
    g.fecha_eliminacion                          AS [DELETED AT],
    g.fecha_restauracion                         AS [RESTORED AT]
FROM dbo.gestion_tramite_fut g
INNER JOIN dbo.solicitante s ON s.id_solicitante    = g.id_solicitante
INNER JOIN dbo.area a        ON a.id_area           = g.id_area_responsable
ORDER BY g.id_gestion;
GO

PRINT '=== Script Todo-proyecto-db.sql completed successfully. ===';
GO

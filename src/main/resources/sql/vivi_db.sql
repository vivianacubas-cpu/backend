/*
===============================================================================
FUT SYSTEM - I.E. N.° 20191 ALFONSO UGARTE
DATABASE: AlfonsoUgarteDB
ENGINE: Microsoft SQL Server
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
BEGIN
    PRINT 'Database AlfonsoUgarteDB already exists.';
END;
GO

USE AlfonsoUgarteDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

/*=============================================================================
  1. MASTER TABLE: DOCUMENT TYPE
=============================================================================*/

IF OBJECT_ID(N'dbo.tipo_documento', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tipo_documento
    (
        id_tipo_documento INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_tipo_documento PRIMARY KEY,
        codigo VARCHAR(10) NOT NULL
            CONSTRAINT UQ_tipo_documento_codigo UNIQUE,
        nombre NVARCHAR(80) NOT NULL,
        estado BIT NOT NULL
            CONSTRAINT DF_tipo_documento_estado DEFAULT (1)
    );
    PRINT 'Table tipo_documento created.';
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.tipo_documento WHERE codigo = 'DNI'
)
BEGIN
    INSERT INTO dbo.tipo_documento (codigo, nombre, estado)
    VALUES ('DNI', N'Documento Nacional de Identidad', 1);
END;
GO

/*=============================================================================
  2. MASTER TABLE: UBIGEO
     Location: department Lima, province Cañete and various districts.
=============================================================================*/

IF OBJECT_ID(N'dbo.ubigeo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ubigeo
    (
        id_ubigeo INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_ubigeo PRIMARY KEY,
        codigo VARCHAR(6) NULL,
        departamento NVARCHAR(100) NOT NULL,
        provincia NVARCHAR(100) NOT NULL,
        distrito NVARCHAR(100) NOT NULL,
        estado BIT NOT NULL
            CONSTRAINT DF_ubigeo_estado DEFAULT (1)
    );
    PRINT 'Table ubigeo created.';
END;
GO

INSERT INTO dbo.ubigeo
    (codigo, departamento, provincia, distrito, estado)
SELECT
    v.codigo, N'Lima', N'Cañete', v.distrito, 1
FROM
(
    VALUES
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
    SELECT 1
    FROM dbo.ubigeo u
    WHERE u.departamento = N'Lima'
      AND u.provincia = N'Cañete'
      AND u.distrito = v.distrito
);
GO

/*=============================================================================
  3. MASTER TABLE: AREA
     Three areas are used: Management (Dirección), Secretary (Secretaría) and
     Administration (Administración). No Coordination area is added.
=============================================================================*/

IF OBJECT_ID(N'dbo.area', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.area
    (
        id_area INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_area PRIMARY KEY,
        nombre NVARCHAR(100) NOT NULL
            CONSTRAINT UQ_area_nombre UNIQUE,
        descripcion NVARCHAR(250) NULL,
        estado BIT NOT NULL
            CONSTRAINT DF_area_estado DEFAULT (1)
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
  4. TRANSACTIONAL TABLE: APPLICANT (SOLICITANTE)
     Each applicant is linked to:
       - tipo_documento via id_tipo_documento
       - location via id_ubigeo
=============================================================================*/

IF OBJECT_ID(N'dbo.solicitante', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.solicitante
    (
        id_solicitante INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_solicitante PRIMARY KEY,
        id_tipo_documento INT NOT NULL,
        id_ubigeo INT NOT NULL,
        numero_documento VARCHAR(20) NOT NULL,
        nombres VARCHAR(100) NOT NULL,
        apellido_paterno VARCHAR(100) NOT NULL,
        apellido_materno VARCHAR(100) NOT NULL,
        departamento NVARCHAR(100) NULL,
        provincia NVARCHAR(100) NULL,
        distrito NVARCHAR(100) NULL,
        direccion VARCHAR(250) NULL,
        telefono VARCHAR(20) NULL,
        correo VARCHAR(150) NULL,
        estado CHAR(1) NOT NULL
            CONSTRAINT DF_solicitante_estado DEFAULT ('A'),
        fecha_registro DATETIME NOT NULL
            CONSTRAINT DF_solicitante_fecha_registro DEFAULT (GETDATE()),
        fecha_actualizacion DATETIME NULL,
        fecha_eliminacion DATETIME NULL,
        fecha_restauracion DATETIME NULL,

        CONSTRAINT FK_solicitante_tipo_documento
            FOREIGN KEY (id_tipo_documento)
            REFERENCES dbo.tipo_documento(id_tipo_documento),

        CONSTRAINT FK_solicitante_ubigeo
            FOREIGN KEY (id_ubigeo)
            REFERENCES dbo.ubigeo(id_ubigeo)
    );
    PRINT 'Table solicitante created.';
END;
GO

/* Add location columns if the table already existed without them. */
IF COL_LENGTH(N'dbo.solicitante', N'departamento') IS NULL
    ALTER TABLE dbo.solicitante ADD departamento NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.solicitante', N'provincia') IS NULL
    ALTER TABLE dbo.solicitante ADD provincia NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.solicitante', N'distrito') IS NULL
    ALTER TABLE dbo.solicitante ADD distrito NVARCHAR(100) NULL;
GO

/* Demo applicant data. If the DNI already exists, the existing record is kept
   and updated with the demo data from this script. */

DECLARE @Solicitantes TABLE
(
    numero_documento VARCHAR(20) PRIMARY KEY,
    nombres VARCHAR(100),
    apellido_paterno VARCHAR(100),
    apellido_materno VARCHAR(100),
    distrito NVARCHAR(100),
    direccion VARCHAR(250),
    telefono VARCHAR(20),
    correo VARCHAR(150)
);

INSERT INTO @Solicitantes
VALUES
('76543456', 'Viviana', 'Cubas', 'Rufasto', N'San Vicente de Cañete', 'Jr. Los Álamos 123', '925554572', 'viviana@example.com'),
('70812345', 'Mariana', 'Quispe', 'Ramírez', N'Imperial', 'Av. Mariscal Benavides 456', '987654321', 'mariana@example.com'),
('73456789', 'Carlos Alberto', 'García', 'López', N'Mala', 'Jr. Comercio 120', '912345678', 'carlos@example.com'),
('71234567', 'Ana Lucía', 'Torres', 'Mendoza', N'Nuevo Imperial', 'Av. Principal 250', '956789123', 'ana@example.com'),
('74561238', 'José Miguel', 'Flores', 'Castillo', N'Quilmaná', 'Calle Los Pinos 45', '945678123', 'jose@example.com'),
('72345678', 'Daniela Sofía', 'Rojas', 'Vargas', N'Cerro Azul', 'Jr. Lima 320', '934567812', 'daniela@example.com'),
('75678912', 'Pedro Antonio', 'Morales', 'Díaz', N'San Luis', 'Av. Costanera 150', '923456781', 'pedro@example.com'),
('70123456', 'Lucía Fernanda', 'Sánchez', 'Pérez', N'Asia', 'Calle Las Flores 89', '914567823', 'lucia@example.com'),
('77891234', 'Jorge Luis', 'Vásquez', 'Huamán', N'Lunahuaná', 'Jr. Grau 210', '978123456', 'jorge@example.com'),
('78912345', 'Rosa Elena', 'Castro', 'Reyes', N'Pacarán', 'Calle Los Olivos 75', '965432187', 'rosa@example.com');

DECLARE @IdTipoDNI INT =
(
    SELECT TOP (1) id_tipo_documento
    FROM dbo.tipo_documento
    WHERE codigo = 'DNI'
);

IF @IdTipoDNI IS NULL
    THROW 50010, 'Document type DNI not found.', 1;

IF EXISTS
(
    SELECT 1
    FROM @Solicitantes v
    LEFT JOIN dbo.ubigeo u
      ON u.departamento = N'Lima'
     AND u.provincia = N'Cañete'
     AND u.distrito = v.distrito
    WHERE u.id_ubigeo IS NULL
)
    THROW 50011, 'One or more districts from Cañete are missing in dbo.ubigeo.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    /* Update applicants that already exist by DNI. */
    UPDATE s
    SET
        s.id_tipo_documento = @IdTipoDNI,
        s.id_ubigeo = u.id_ubigeo,
        s.nombres = v.nombres,
        s.apellido_paterno = v.apellido_paterno,
        s.apellido_materno = v.apellido_materno,
        s.departamento = N'Lima',
        s.provincia = N'Cañete',
        s.distrito = v.distrito,
        s.direccion = v.direccion,
        s.telefono = v.telefono,
        s.correo = v.correo,
        s.estado = 'A'
    FROM dbo.solicitante s
    INNER JOIN @Solicitantes v
        ON v.numero_documento = s.numero_documento
    INNER JOIN dbo.ubigeo u
        ON u.departamento = N'Lima'
       AND u.provincia = N'Cañete'
       AND u.distrito = v.distrito;

    /* Insert only the applicants that do not yet exist. */
    INSERT INTO dbo.solicitante
    (
        id_tipo_documento, id_ubigeo, numero_documento,
        nombres, apellido_paterno, apellido_materno,
        departamento, provincia, distrito, direccion,
        telefono, correo, estado, fecha_registro
    )
    SELECT
        @IdTipoDNI, u.id_ubigeo, v.numero_documento,
        v.nombres, v.apellido_paterno, v.apellido_materno,
        N'Lima', N'Cañete', v.distrito, v.direccion,
        v.telefono, v.correo, 'A', GETDATE()
    FROM @Solicitantes v
    INNER JOIN dbo.ubigeo u
        ON u.departamento = N'Lima'
       AND u.provincia = N'Cañete'
       AND u.distrito = v.distrito
    WHERE NOT EXISTS
    (
        SELECT 1
        FROM dbo.solicitante s
        WHERE s.numero_documento = v.numero_documento
    );

    COMMIT TRANSACTION;
    PRINT '10 applicants verified and registered.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/*=============================================================================
  5. TRANSACTIONAL TABLE: FUT PROCEDURE MANAGEMENT
     Each record is linked to:
       - applicant via id_solicitante
       - responsible area via id_area_responsable
=============================================================================*/

IF OBJECT_ID(N'dbo.gestion_tramite_fut', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.gestion_tramite_fut
    (
        id_gestion INT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_gestion_tramite_fut PRIMARY KEY,
        id_solicitante INT NOT NULL,

        nombre_responsable NVARCHAR(150) NOT NULL,
        nombres_responsable NVARCHAR(100) NOT NULL,
        apellido_paterno_responsable NVARCHAR(100) NOT NULL,
        apellido_materno_responsable NVARCHAR(100) NOT NULL,

        id_area_responsable INT NOT NULL,
        fecha_recepcion DATE NOT NULL,
        tipo_gestion VARCHAR(30) NOT NULL,
        prioridad VARCHAR(15) NOT NULL,
        fecha_limite DATE NOT NULL,
        estado_gestion VARCHAR(20) NOT NULL,
        fecha_resolucion DATE NOT NULL,
        decision NVARCHAR(500) NOT NULL,
        fundamento_decision NVARCHAR(2000) NOT NULL,
        observaciones NVARCHAR(2000) NOT NULL,

        estado CHAR(1) NOT NULL
            CONSTRAINT DF_gestion_tramite_fut_estado DEFAULT ('A'),
        fecha_registro DATETIME2 NOT NULL
            CONSTRAINT DF_gestion_tramite_fut_fecha_registro DEFAULT (SYSDATETIME()),
        fecha_actualizacion DATETIME2 NULL,
        fecha_eliminacion DATETIME2 NULL,
        fecha_restauracion DATETIME2 NULL,

        CONSTRAINT FK_gestion_fut_solicitante
            FOREIGN KEY (id_solicitante)
            REFERENCES dbo.solicitante(id_solicitante),

        CONSTRAINT FK_gestion_fut_area
            FOREIGN KEY (id_area_responsable)
            REFERENCES dbo.area(id_area)
    );
    PRINT 'Table gestion_tramite_fut created.';
END;
GO

/* Complete data for the 10 management records.
   Linked by applicant DNI and area name.
   All operational fields are filled in; deletion and restoration
   dates remain NULL because they are audit data. */

DECLARE @Gestiones TABLE
(
    numero_documento VARCHAR(20) PRIMARY KEY,
    responsable_nombres NVARCHAR(100),
    responsable_paterno NVARCHAR(100),
    responsable_materno NVARCHAR(100),
    area NVARCHAR(100),
    tipo_gestion VARCHAR(30),
    prioridad VARCHAR(15),
    fecha_recepcion DATE,
    fecha_limite DATE,
    estado_gestion VARCHAR(20),
    fecha_resolucion DATE,
    decision NVARCHAR(500),
    fundamento NVARCHAR(2000),
    observaciones NVARCHAR(2000)
);

INSERT INTO @Gestiones
VALUES
('76543456', N'María Elena', N'López', N'Campos', N'SECRETARÍA',
 'EVALUACION', 'NORMAL', '20261104', '20261111', 'APROBADO',
 '20261104', N'APROBADO',
 N'The request meets the established requirements for processing.',
 N'Request reviewed and registered correctly.'),

('70812345', N'José Luis', N'Ramírez', N'Torres', N'DIRECCIÓN',
 'RECEPCION', 'URGENTE', '20261104', '20261107', 'APROBADO',
 '20261104', N'APROBADO',
 N'The documentation was reviewed and complies with institutional regulations.',
 N'Procedure reviewed by Management.'),

('73456789', N'Ana María', N'Flores', N'Quispe', N'SECRETARÍA',
 'EVALUACION', 'NORMAL', '20261104', '20261110', 'APROBADO',
 '20261104', N'APROBADO',
 N'The submitted documentation meets the academic requirements.',
 N'Procedure handled satisfactorily.'),

('71234567', N'Carlos Alberto', N'Mendoza', N'Rojas', N'ADMINISTRACIÓN',
 'RECEPCION', 'URGENTE', '20261104', '20261106', 'OBSERVADO',
 '20261104', N'OBSERVADO',
 N'Additional documentation is required to continue processing.',
 N'The applicant must address the noted observations.'),

('74561238', N'Rosa Isabel', N'García', N'Pérez', N'SECRETARÍA',
 'EVALUACION', 'NORMAL', '20261104', '20261114', 'APROBADO',
 '20261104', N'APROBADO',
 N'The request meets the administrative and academic conditions.',
 N'Request evaluated correctly.'),

('72345678', N'Miguel Ángel', N'Castillo', N'Díaz', N'DIRECCIÓN',
 'RECEPCION', 'NORMAL', '20261104', '20261112', 'APROBADO',
 '20261104', N'APROBADO',
 N'The documentation complies with institutional regulations.',
 N'Management reviewed by the Director.'),

('75678912', N'Lucía Fernanda', N'Vargas', N'León', N'ADMINISTRACIÓN',
 'RECEPCION', 'NORMAL', '20261104', '20261109', 'APROBADO',
 '20261104', N'APROBADO',
 N'Administrative requirements have been satisfactorily verified.',
 N'Processing completed satisfactorily.'),

('70123456', N'Pedro Antonio', N'Sánchez', N'Morales', N'SECRETARÍA',
 'EVALUACION', 'URGENTE', '20261104', '20261106', 'OBSERVADO',
 '20261104', N'OBSERVADO',
 N'The request requires the documents noted by the Secretary to be corrected.',
 N'The corresponding observations have been communicated.'),

('77891234', N'Jorge Luis', N'Torres', N'Vega', N'DIRECCIÓN',
 'RECEPCION', 'NORMAL', '20261104', '20261115', 'APROBADO',
 '20261104', N'APROBADO',
 N'The request complies with the applicable regulations.',
 N'Management concluded satisfactorily.'),

('78912345', N'Elena Patricia', N'Ramos', N'Flores', N'ADMINISTRACIÓN',
 'RECEPCION', 'NORMAL', '20261104', '20261112', 'APROBADO',
 '20261104', N'APROBADO',
 N'The documentation meets the administrative requirements.',
 N'Request evaluated by Administration.');

IF EXISTS
(
    SELECT 1
    FROM @Gestiones v
    LEFT JOIN dbo.solicitante s
        ON s.numero_documento = v.numero_documento
    WHERE s.id_solicitante IS NULL
)
    THROW 50020, 'One or more applicants are missing. Verify the 10 DNIs before registering management records.', 1;

IF EXISTS
(
    SELECT 1
    FROM @Gestiones v
    LEFT JOIN dbo.area a
        ON a.nombre = v.area
    WHERE a.id_area IS NULL
)
    THROW 50021, 'One of the responsible areas is missing: Management, Secretary or Administration.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    /* Update the 10 management records if they already exist for those applicants. */
    UPDATE g
    SET
        g.nombre_responsable = CONCAT(v.responsable_nombres, N' ',
                                      v.responsable_paterno, N' ',
                                      v.responsable_materno),
        g.nombres_responsable = v.responsable_nombres,
        g.apellido_paterno_responsable = v.responsable_paterno,
        g.apellido_materno_responsable = v.responsable_materno,
        g.id_area_responsable = a.id_area,
        g.fecha_recepcion = v.fecha_recepcion,
        g.tipo_gestion = v.tipo_gestion,
        g.prioridad = v.prioridad,
        g.fecha_limite = v.fecha_limite,
        g.estado_gestion = v.estado_gestion,
        g.fecha_resolucion = v.fecha_resolucion,
        g.decision = v.decision,
        g.fundamento_decision = v.fundamento,
        g.observaciones = v.observaciones,
        g.estado = 'A',
        g.fecha_actualizacion = SYSDATETIME()
    FROM dbo.gestion_tramite_fut g
    INNER JOIN dbo.solicitante s
        ON s.id_solicitante = g.id_solicitante
    INNER JOIN @Gestiones v
        ON v.numero_documento = s.numero_documento
    INNER JOIN dbo.area a
        ON a.nombre = v.area;

    /* Insert management records that do not yet exist. */
    INSERT INTO dbo.gestion_tramite_fut
    (
        id_solicitante,
        nombre_responsable,
        nombres_responsable,
        apellido_paterno_responsable,
        apellido_materno_responsable,
        id_area_responsable,
        fecha_recepcion,
        tipo_gestion,
        prioridad,
        fecha_limite,
        estado_gestion,
        fecha_resolucion,
        decision,
        fundamento_decision,
        observaciones,
        estado,
        fecha_registro
    )
    SELECT
        s.id_solicitante,
        CONCAT(v.responsable_nombres, N' ',
               v.responsable_paterno, N' ',
               v.responsable_materno),
        v.responsable_nombres,
        v.responsable_paterno,
        v.responsable_materno,
        a.id_area,
        v.fecha_recepcion,
        v.tipo_gestion,
        v.prioridad,
        v.fecha_limite,
        v.estado_gestion,
        v.fecha_resolucion,
        v.decision,
        v.fundamento,
        v.observaciones,
        'A',
        SYSDATETIME()
    FROM @Gestiones v
    INNER JOIN dbo.solicitante s
        ON s.numero_documento = v.numero_documento
    INNER JOIN dbo.area a
        ON a.nombre = v.area
    WHERE NOT EXISTS
    (
        SELECT 1
        FROM dbo.gestion_tramite_fut g
        WHERE g.id_solicitante = s.id_solicitante
    );

    COMMIT TRANSACTION;
    PRINT '10 management records verified and registered successfully.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/*=============================================================================
  6. FINAL QUERY: THE 10 APPLICANTS
=============================================================================*/

SELECT
    ROW_NUMBER() OVER (ORDER BY s.id_solicitante) AS [N.],
    td.codigo AS [DOC TYPE],
    s.numero_documento AS [DOC NUMBER],
    s.apellido_paterno AS [PATERNAL LAST NAME],
    s.apellido_materno AS [MATERNAL LAST NAME],
    s.nombres AS [FIRST NAMES],
    CONCAT(u.departamento, N' - ', u.provincia, N' - ', u.distrito) AS [LOCATION],
    s.direccion AS [ADDRESS],
    s.telefono AS [PHONE],
    s.correo AS [EMAIL],
    CASE WHEN s.estado = 'A' THEN N'ACTIVE' ELSE N'INACTIVE' END AS [STATUS],
    s.fecha_registro AS [REGISTERED AT],
    s.fecha_actualizacion AS [UPDATED AT],
    s.fecha_eliminacion AS [DELETED AT],
    s.fecha_restauracion AS [RESTORED AT]
FROM dbo.solicitante s
INNER JOIN dbo.tipo_documento td
    ON td.id_tipo_documento = s.id_tipo_documento
INNER JOIN dbo.ubigeo u
    ON u.id_ubigeo = s.id_ubigeo
WHERE s.numero_documento IN
(
    '76543456','70812345','73456789','71234567','74561238',
    '72345678','75678912','70123456','77891234','78912345'
)
ORDER BY s.id_solicitante;
GO

/*=============================================================================
  7. FINAL QUERY: THE 10 FUT MANAGEMENT RECORDS AND THEIR APPLICANTS
=============================================================================*/

SELECT
    g.id_gestion AS [N.],
    s.numero_documento AS [DOC NUMBER],
    s.apellido_paterno AS [PATERNAL LAST NAME],
    s.apellido_materno AS [MATERNAL LAST NAME],
    s.nombres AS [FIRST NAMES],
    g.nombre_responsable AS [RESPONSIBLE],
    a.nombre AS [RESPONSIBLE AREA],
    g.tipo_gestion AS [MANAGEMENT TYPE],
    g.prioridad AS [PRIORITY],
    g.estado_gestion AS [STATUS],
    g.fecha_recepcion AS [RECEPTION DATE],
    g.fecha_limite AS [DEADLINE],
    g.fecha_resolucion AS [RESOLUTION DATE],
    g.decision AS [DECISION],
    g.fundamento_decision AS [DECISION BASIS],
    g.observaciones AS [OBSERVATIONS],
    g.fecha_registro AS [REGISTERED AT],
    g.fecha_actualizacion AS [UPDATED AT],
    g.fecha_eliminacion AS [DELETED AT],
    g.fecha_restauracion AS [RESTORED AT]
FROM dbo.gestion_tramite_fut g
INNER JOIN dbo.solicitante s
    ON s.id_solicitante = g.id_solicitante
INNER JOIN dbo.area a
    ON a.id_area = g.id_area_responsable
WHERE s.numero_documento IN
(
    '76543456','70812345','73456789','71234567','74561238',
    '72345678','75678912','70123456','77891234','78912345'
)
ORDER BY g.id_gestion;
GO

/*=============================================================================
  8. RECORD COUNT SUMMARY
=============================================================================*/

SELECT
    (SELECT COUNT(*) FROM dbo.tipo_documento WHERE codigo = 'DNI') AS [DNI DOC TYPES],
    (SELECT COUNT(*) FROM dbo.ubigeo
     WHERE departamento = N'Lima' AND provincia = N'Cañete'
       AND distrito IN
       (N'San Vicente de Cañete',N'Imperial',N'Mala',N'Nuevo Imperial',
        N'Quilmaná',N'Cerro Azul',N'San Luis',N'Asia',N'Lunahuaná',N'Pacarán')
    ) AS [DISTRICTS REGISTERED],
    (SELECT COUNT(*) FROM dbo.solicitante
     WHERE numero_documento IN
     ('76543456','70812345','73456789','71234567','74561238',
      '72345678','75678912','70123456','77891234','78912345')
    ) AS [APPLICANTS],
    (SELECT COUNT(*) FROM dbo.gestion_tramite_fut g
     INNER JOIN dbo.solicitante s ON s.id_solicitante = g.id_solicitante
     WHERE s.numero_documento IN
     ('76543456','70812345','73456789','71234567','74561238',
      '72345678','75678912','70123456','77891234','78912345')
    ) AS [FUT MANAGEMENT RECORDS];
GO

/* ============================================================
   QUERY: APPLICANT LIST
   PURPOSE: Display all applicants with their full details
   ============================================================ */

SELECT
    s.id_solicitante AS [ID],
    td.codigo AS [DOCUMENT TYPE],
    s.numero_documento AS [DOC NUMBER],
    s.apellido_paterno AS [PATERNAL LAST NAME],
    s.apellido_materno AS [MATERNAL LAST NAME],
    s.nombres AS [FIRST NAMES],
    u.departamento AS [DEPARTMENT],
    u.provincia AS [PROVINCE],
    u.distrito AS [DISTRICT],
    s.direccion AS [ADDRESS],
    s.telefono AS [PHONE],
    s.correo AS [EMAIL],
    CASE
        WHEN s.estado = 'A' THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END AS [STATUS],
    s.fecha_registro AS [REGISTERED AT],
    s.fecha_actualizacion AS [UPDATED AT],
    s.fecha_eliminacion AS [DELETED AT],
    s.fecha_restauracion AS [RESTORED AT]
FROM dbo.solicitante s
INNER JOIN dbo.tipo_documento td
    ON td.id_tipo_documento = s.id_tipo_documento
INNER JOIN dbo.ubigeo u
    ON u.id_ubigeo = s.id_ubigeo
ORDER BY s.id_solicitante;
GO


/* ============================================================
   QUERY: FUT MANAGEMENT RECORD TRACKING
   PURPOSE: Display all management records with applicant and area details
   ============================================================ */

SELECT
    g.id_gestion AS [N.],
    s.numero_documento AS [DOC NUMBER],
    s.apellido_paterno AS [PATERNAL LAST NAME],
    s.apellido_materno AS [MATERNAL LAST NAME],
    s.nombres AS [FIRST NAMES],
    g.nombre_responsable AS [RESPONSIBLE],
    a.nombre AS [RESPONSIBLE AREA],
    g.tipo_gestion AS [MANAGEMENT TYPE],
    g.prioridad AS [PRIORITY],
    g.estado_gestion AS [STATUS],
    g.fecha_recepcion AS [RECEPTION DATE],
    g.fecha_limite AS [DEADLINE],
    g.fecha_resolucion AS [RESOLUTION DATE],
    g.decision AS [DECISION],
    g.fundamento_decision AS [DECISION BASIS],
    g.observaciones AS [OBSERVATIONS],
    g.fecha_registro AS [REGISTERED AT],
    g.fecha_actualizacion AS [UPDATED AT],
    g.fecha_eliminacion AS [DELETED AT],
    g.fecha_restauracion AS [RESTORED AT]
FROM dbo.gestion_tramite_fut g
INNER JOIN dbo.solicitante s
    ON s.id_solicitante = g.id_solicitante
INNER JOIN dbo.area a
    ON a.id_area = g.id_area_responsable
ORDER BY g.id_gestion;
GO

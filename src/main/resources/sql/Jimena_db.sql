tipo tramite

USE AlfonsoUgarteDB;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- ACTUALIZAR LOS 10 REGISTROS DEMO

UPDATE tt
SET
    categoria = v.categoria,
    descripcion = v.descripcion,
    requisitos = v.requisitos,
    plazo_estimado_dias = v.dias,
    requiere_documentos = v.documentos,
    vigente_desde = '2026-01-01',
    vigente = 1,
    estado = 1
FROM dbo.tipo_tramite tt
INNER JOIN
(
    VALUES
    ('DEMO-001', N'Academico',
     N'Solicitud para obtener una vacante en la institucion educativa.',
     N'Copia del DNI del estudiante y del padre o apoderado.',
     5, 1),

    ('DEMO-002', N'Academico',
     N'Solicitud de traslado de matricula.',
     N'DNI del estudiante y constancia de matricula.',
     7, 1),

    ('DEMO-003', N'Academico',
     N'Emision de constancia de estudios o matricula.',
     N'Copia del DNI del estudiante.',
     3, 1),

    ('DEMO-004', N'Academico',
     N'Solicitud de certificado oficial de estudios.',
     N'DNI del estudiante y datos del ano escolar.',
     10, 1),

    ('DEMO-005', N'Administrativo',
     N'Solicitud de correccion de datos personales o academicos.',
     N'Copia del DNI y documento que sustente la correccion.',
     5, 1),

    ('DEMO-006', N'Administrativo',
     N'Solicitud de justificacion de inasistencias.',
     N'Copia del DNI y documento justificativo.',
     3, 1),

    ('DEMO-007', N'Docente',
     N'Solicitud de licencia o permiso del personal.',
     N'Solicitud formal y documento sustentatorio.',
     5, 1),

    ('DEMO-008', N'Administrativo',
     N'Emision de constancia de pago o trabajo.',
     N'Solicitud formal y copia del DNI.',
     5, 0),

    ('DEMO-009', N'Administrativo',
     N'Solicitud de entrega de legajos o documentos.',
     N'Copia del DNI y documento que acredite la solicitud.',
     7, 1),

    ('DEMO-010', N'Administrativo',
     N'Registro y atencion de reclamos o consultas.',
     N'Solicitud fundamentada y documentos de sustento.',
     10, 0)

) AS v(codigo, categoria, descripcion, requisitos, dias, documentos)
    ON tt.codigo = v.codigo;


-- ELIMINAR LOS 10 REGISTROS CORTOS DUPLICADOS
-- Solo se eliminan si fueron creados como ejemplos.

DELETE FROM dbo.tipo_tramite
WHERE codigo IN
(
    'VAC', 'TRA', 'CES', 'CER', 'REC',
    'JUS', 'LIC', 'CTP', 'LEG', 'RAC'
);

COMMIT TRANSACTION;
GO

-- VERIFICAR LOS 10 REGISTROS COMPLETOS

SELECT
    codigo AS CODIGO,
    nombre AS NOMBRE,
    categoria AS [CAT.],
    descripcion AS DESCRIPCION,
    requisitos AS REQUISITOS,
    plazo_estimado_dias AS DIAS,
    CASE
        WHEN requiere_documentos = 1 THEN 'Si'
        ELSE 'No'
    END AS [DOCS.],
    CASE
        WHEN vigente = 1 THEN 'Si'
        ELSE 'No'
    END AS VIGENTE,
    CASE
        WHEN estado = 1 THEN 'ACTIVO'
        ELSE 'INACTIVO'
    END AS ESTADO,
    fecha_registro AS [F. REGISTRO],
    fecha_actualizacion AS [F. ACTUALIZACION],
    fecha_eliminacion AS [F. ELIMINACION],
    fecha_restauracion AS [F. RESTAURACION]
FROM dbo.tipo_tramite
WHERE codigo LIKE 'DEMO-%'
ORDER BY codigo;
GO

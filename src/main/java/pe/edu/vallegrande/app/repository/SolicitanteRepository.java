package pe.edu.vallegrande.app.repository;
import pe.edu.vallegrande.app.model.Solicitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SolicitanteRepository extends JpaRepository<Solicitante, Long> {

    List<Solicitante> findByEstado(String estado);
    Optional<Solicitante> findByNumeroDocumento(String numeroDocumento);

    // ── SQL fragment reused in all 4 enriched-response queries ──────────────
    // Aliases must match exactly the getters in SolicitanteResponseProjection.
    //
    // Location resolution logic:
    //   1. Each component takes the free-text value from the applicant if not empty;
    //      if empty it falls back to the ubigeo catalog.
    //   2. The combined "District, Province, Department" string is only built
    //      when all three resolved components are non-null and non-empty.
    //      If any is missing, NULL is returned (the frontend shows empty, not ", , ").

    @Query(value = """
            SELECT s.id_solicitante AS idSolicitante,
                   s.id_tipo_documento AS idTipoDocumento,
                   s.id_ubigeo AS idUbigeo,
                   td.codigo AS tipoDocumento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento) AS departamento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia)    AS provincia,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito)     AS distrito,
                   CASE
                     WHEN NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento))), '') IS NOT NULL
                     THEN CONCAT(
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento)))
                          )
                     ELSE NULL
                   END AS ubicacion,
                   s.numero_documento AS numeroDocumento,
                   s.nombres AS nombres,
                   s.apellido_paterno AS apellidoPaterno,
                   s.apellido_materno AS apellidoMaterno,
                   s.telefono AS telefono,
                   s.correo AS correo,
                   s.direccion AS direccion,
                   s.estado AS estado,
                   CASE WHEN s.estado = 'A' THEN 'Activo' ELSE 'Inactivo' END AS estadoTexto,
                   s.fecha_registro AS fechaRegistro,
                   s.fecha_actualizacion AS fechaActualizacion,
                   s.fecha_eliminacion AS fechaEliminacion,
                   s.fecha_restauracion AS fechaRestauracion
            FROM dbo.solicitante s
            LEFT JOIN dbo.tipo_documento td ON td.id_tipo_documento = s.id_tipo_documento
            LEFT JOIN dbo.ubigeo u ON u.id_ubigeo = s.id_ubigeo
            WHERE s.id_solicitante = :id
            """, nativeQuery = true)
    Optional<SolicitanteResponseProjection> findResponseById(@Param("id") Long id);

    @Query(value = """
            SELECT s.id_solicitante AS idSolicitante,
                   s.id_tipo_documento AS idTipoDocumento,
                   s.id_ubigeo AS idUbigeo,
                   td.codigo AS tipoDocumento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento) AS departamento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia)    AS provincia,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito)     AS distrito,
                   CASE
                     WHEN NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento))), '') IS NOT NULL
                     THEN CONCAT(
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento)))
                          )
                     ELSE NULL
                   END AS ubicacion,
                   s.numero_documento AS numeroDocumento,
                   s.nombres AS nombres,
                   s.apellido_paterno AS apellidoPaterno,
                   s.apellido_materno AS apellidoMaterno,
                   s.telefono AS telefono,
                   s.correo AS correo,
                   s.direccion AS direccion,
                   s.estado AS estado,
                   CASE WHEN s.estado = 'A' THEN 'Activo' ELSE 'Inactivo' END AS estadoTexto,
                   s.fecha_registro AS fechaRegistro,
                   s.fecha_actualizacion AS fechaActualizacion,
                   s.fecha_eliminacion AS fechaEliminacion,
                   s.fecha_restauracion AS fechaRestauracion
            FROM dbo.solicitante s
            LEFT JOIN dbo.tipo_documento td ON td.id_tipo_documento = s.id_tipo_documento
            LEFT JOIN dbo.ubigeo u ON u.id_ubigeo = s.id_ubigeo
            """, nativeQuery = true)
    List<SolicitanteResponseProjection> findAllResponses();

    @Query(value = """
            SELECT s.id_solicitante AS idSolicitante,
                   s.id_tipo_documento AS idTipoDocumento,
                   s.id_ubigeo AS idUbigeo,
                   td.codigo AS tipoDocumento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento) AS departamento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia)    AS provincia,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito)     AS distrito,
                   CASE
                     WHEN NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento))), '') IS NOT NULL
                     THEN CONCAT(
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento)))
                          )
                     ELSE NULL
                   END AS ubicacion,
                   s.numero_documento AS numeroDocumento,
                   s.nombres AS nombres,
                   s.apellido_paterno AS apellidoPaterno,
                   s.apellido_materno AS apellidoMaterno,
                   s.telefono AS telefono,
                   s.correo AS correo,
                   s.direccion AS direccion,
                   s.estado AS estado,
                   CASE WHEN s.estado = 'A' THEN 'Activo' ELSE 'Inactivo' END AS estadoTexto,
                   s.fecha_registro AS fechaRegistro,
                   s.fecha_actualizacion AS fechaActualizacion,
                   s.fecha_eliminacion AS fechaEliminacion,
                   s.fecha_restauracion AS fechaRestauracion
            FROM dbo.solicitante s
            LEFT JOIN dbo.tipo_documento td ON td.id_tipo_documento = s.id_tipo_documento
            LEFT JOIN dbo.ubigeo u ON u.id_ubigeo = s.id_ubigeo
            WHERE s.estado = :estado
            """, nativeQuery = true)
    List<SolicitanteResponseProjection> findResponsesByEstado(@Param("estado") String estado);

    @Query(value = """
            SELECT s.id_solicitante AS idSolicitante,
                   s.id_tipo_documento AS idTipoDocumento,
                   s.id_ubigeo AS idUbigeo,
                   td.codigo AS tipoDocumento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento) AS departamento,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia)    AS provincia,
                   COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito)     AS distrito,
                   CASE
                     WHEN NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    '') IS NOT NULL
                      AND NULLIF(LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento))), '') IS NOT NULL
                     THEN CONCAT(
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.distrito)),     ''), u.distrito))),     ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.provincia)),    ''), u.provincia))),    ', ',
                            LTRIM(RTRIM(COALESCE(NULLIF(LTRIM(RTRIM(s.departamento)), ''), u.departamento)))
                          )
                     ELSE NULL
                   END AS ubicacion,
                   s.numero_documento AS numeroDocumento,
                   s.nombres AS nombres,
                   s.apellido_paterno AS apellidoPaterno,
                   s.apellido_materno AS apellidoMaterno,
                   s.telefono AS telefono,
                   s.correo AS correo,
                   s.direccion AS direccion,
                   s.estado AS estado,
                   CASE WHEN s.estado = 'A' THEN 'Activo' ELSE 'Inactivo' END AS estadoTexto,
                   s.fecha_registro AS fechaRegistro,
                   s.fecha_actualizacion AS fechaActualizacion,
                   s.fecha_eliminacion AS fechaEliminacion,
                   s.fecha_restauracion AS fechaRestauracion
            FROM dbo.solicitante s
            LEFT JOIN dbo.tipo_documento td ON td.id_tipo_documento = s.id_tipo_documento
            LEFT JOIN dbo.ubigeo u ON u.id_ubigeo = s.id_ubigeo
            WHERE s.numero_documento = :numeroDocumento
            """, nativeQuery = true)
    Optional<SolicitanteResponseProjection> findResponseByNumeroDocumento(
            @Param("numeroDocumento") String numeroDocumento);

    @Query(value = "SELECT * FROM dbo.solicitante WHERE id_solicitante IN (:ids)", nativeQuery = true)
    List<Solicitante> findByIdSolicitanteIn(@Param("ids") List<Long> ids);

    @Query(value = "SELECT TOP 1 id_ubigeo FROM dbo.ubigeo WHERE UPPER(departamento) = UPPER(:departamento) AND UPPER(provincia) = UPPER(:provincia) AND UPPER(distrito) = UPPER(:distrito) AND estado = 1", nativeQuery = true)
    Optional<Integer> findUbigeoId(@Param("departamento") String departamento,
                                   @Param("provincia") String provincia,
                                   @Param("distrito") String distrito);

    /** Returns the document type ID whose code matches (e.g. "DNI"). Case-insensitive search. */
    @Query(value = "SELECT TOP 1 id_tipo_documento FROM dbo.tipo_documento WHERE UPPER(codigo) = UPPER(:codigo)", nativeQuery = true)
    Optional<Integer> findTipoDocumentoIdByCodigo(@Param("codigo") String codigo);

    /**
     * Returns the most recent nombre_completo for the applicant from tramite_fut.
     * Used to recover names when the applicant has legacy incomplete data.
     */
    @Query(value = "SELECT TOP 1 t.nombre_completo FROM dbo.tramite_fut t " +
                   "WHERE t.id_solicitante = :idSolicitante " +
                   "  AND t.nombre_completo IS NOT NULL " +
                   "  AND LTRIM(RTRIM(t.nombre_completo)) <> '' " +
                   "ORDER BY t.fecha_registro DESC", nativeQuery = true)
    Optional<String> findNombreCompletoFromTramite(@Param("idSolicitante") Long idSolicitante);

    // ── Reference catalogs for the Applicant Master (frontend) ──────────────

    /** Lists all document types (id, codigo, nombre). */
    @Query(value = "SELECT id_tipo_documento, codigo, nombre FROM dbo.tipo_documento ORDER BY id_tipo_documento", nativeQuery = true)
    List<Object[]> findAllTiposDocumento();

    /** Lists all ubigeos (id, departamento, provincia, distrito, estado). */
    @Query(value = "SELECT id_ubigeo, departamento, provincia, distrito, estado FROM dbo.ubigeo ORDER BY departamento, provincia, distrito", nativeQuery = true)
    List<Object[]> findAllUbigeos();
}

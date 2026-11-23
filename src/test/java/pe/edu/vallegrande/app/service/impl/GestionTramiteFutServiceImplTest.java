package pe.edu.vallegrande.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.vallegrande.app.dto.GestionTramiteResponse;
import pe.edu.vallegrande.app.model.Area;
import pe.edu.vallegrande.app.model.GestionTramiteFut;
import pe.edu.vallegrande.app.model.Solicitante;
import pe.edu.vallegrande.app.repository.AreaRepository;
import pe.edu.vallegrande.app.repository.GestionTramiteFutRepository;
import pe.edu.vallegrande.app.repository.SolicitanteRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestionTramiteFutServiceImplTest {

    private GestionTramiteFutServiceImpl buildService(
            GestionTramiteFutRepository repo,
            SolicitanteRepository solRepo,
            AreaRepository areaRepo) {
        GestionTramiteFutServiceImpl svc = new GestionTramiteFutServiceImpl(repo, solRepo, areaRepo);
        return svc;
    }

    @Test
    void findActivasReturnsResponseWithApplicantAndAreaDisplayFields() {
        GestionTramiteFutRepository gestionRepository =
                mock(GestionTramiteFutRepository.class);
        SolicitanteRepository solicitanteRepository =
                mock(SolicitanteRepository.class);
        AreaRepository areaRepository = mock(AreaRepository.class);

        GestionTramiteFut gestion = new GestionTramiteFut();
        gestion.setIdGestion(100);
        gestion.setIdSolicitante(1133L);
        gestion.setIdAreaResponsable(1);
        when(gestionRepository.findByEstadoOrderByFechaRegistroDesc("A"))
                .thenReturn(List.of(gestion));

        Solicitante solicitante = new Solicitante();
        solicitante.setIdSolicitante(1133L);
        solicitante.setNumeroDocumento("72345001");
        solicitante.setApellidoPaterno("Mendoza");
        solicitante.setApellidoMaterno("Quispe");
        solicitante.setNombres("Carlos Alberto");
        when(solicitanteRepository.findAllById(List.of(1133L)))
                .thenReturn(List.of(solicitante));

        Area area = new Area();
        area.setIdArea(1);
        area.setNombre("SECRETARÍA");
        when(areaRepository.findAllById(List.of(1))).thenReturn(List.of(area));

        GestionTramiteFutServiceImpl service =
                buildService(gestionRepository, solicitanteRepository, areaRepository);

        GestionTramiteResponse result = service.findActivas(null).get(0);

        // firstNames shows the applicant's first names
        assertEquals("Carlos Alberto", result.firstNames());
        // responsibleArea shows the area name
        assertEquals("SECRETARÍA", result.responsibleArea());
        // managementId is correctly populated
        assertEquals(100, result.managementId());
    }

    @Test
    void responseSerializesAllEnglishFieldNames() {
        GestionTramiteResponse response = new GestionTramiteResponse(
                99,                         // managementId
                1133L,                      // applicantId
                "70812345",                 // documentNumber
                "Quispe",                   // paternalLastName
                "Ramirez",                  // maternalLastName
                "Mariana",                  // firstNames
                "Viviana Cubas Rufasto",    // responsible
                "Viviana",                  // responsibleFirstNames
                "Cubas",                    // responsiblePaternalLastName
                "Rufasto",                  // responsibleMaternalLastName
                1,                          // responsibleAreaId
                "SECRETARIA",               // responsibleArea
                "Aprobación",               // managementType
                "Urgente",                  // priority
                "EN_PROCESO",               // managementStatus
                "ACTIVO",                   // recordStatus
                null,                       // receptionDate
                null,                       // deadlineDate
                null,                       // resolutionDate
                "APROBADO",                 // decision
                null,                       // decisionBasis
                null,                       // observations
                null,                       // createdAt
                null,                       // updatedAt
                null,                       // deletedAt
                null                        // restoredAt
        );

        var json = new ObjectMapper().findAndRegisterModules().valueToTree(response);

        // Verify all field names are in English
        assertEquals("SECRETARIA",            json.get("responsibleArea").asText());
        assertEquals("Mariana",               json.get("firstNames").asText());
        assertEquals("Viviana Cubas Rufasto", json.get("responsible").asText());
        assertEquals("EN_PROCESO",            json.get("managementStatus").asText());
        assertEquals("ACTIVO",                json.get("recordStatus").asText());
        assertEquals("70812345",              json.get("documentNumber").asText());
        assertEquals("Quispe",                json.get("paternalLastName").asText());
        assertEquals("Ramirez",               json.get("maternalLastName").asText());
        assertEquals("APROBADO",              json.get("decision").asText());
    }
}

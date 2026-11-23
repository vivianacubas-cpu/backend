package pe.edu.vallegrande.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.app.model.Area;
import pe.edu.vallegrande.app.repository.AreaRepository;

import java.util.List;

/**
 * Initializes the institutional areas when Spring Boot starts.
 * If the areas already exist, they are not duplicated.
 */
@Component
public class AreaDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AreaDataInitializer.class);

    private final AreaRepository areaRepository;

    public AreaDataInitializer(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
        List<String> required = List.of("DIRECCIÓN", "SECRETARÍA", "ADMINISTRACIÓN");

        for (String name : required) {
            boolean exists = areaRepository.findAll()
                    .stream()
                    .anyMatch(a -> normalize(a.getNombre()).equals(normalize(name)));

            if (!exists) {
                Area area = new Area();
                area.setNombre(name);
                areaRepository.save(area);
                log.info("Area automatically inserted: {}", name);
            } else {
                log.debug("Area already exists: {}", name);
            }
        }
        } catch (Exception e) {
            log.warn("AreaDataInitializer: could not seed areas — no database connection: {}", e.getMessage());
        }
    }

    /** Normalizes for comparison by ignoring accents and case. */
    private String normalize(String s) {
        if (s == null) return "";
        return s.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ü", "U")
                .trim();
    }
}

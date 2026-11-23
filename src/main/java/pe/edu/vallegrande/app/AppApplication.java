package pe.edu.vallegrande.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the FUT System backend application.
 * I.E. N.° 20191 Alfonso Ugarte — San Vicente de Cañete.
 */
@EnableScheduling
@SpringBootApplication
public class AppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }
}

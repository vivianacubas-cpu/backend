package pe.edu.vallegrande.app.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Temporary endpoint for generating BCrypt hashes.
 * REMOVE in production.
 */
@Hidden
@RestController
@RequestMapping("/v1/api/hash")
public class HashRest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @GetMapping
    public Map<String, String> hash(@RequestParam String password) {
        return Map.of("hash", encoder.encode(password));
    }
}

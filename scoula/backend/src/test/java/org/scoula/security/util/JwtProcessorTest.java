package org.scoula.security.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProcessorTest {

    private final JwtProcessor jwtProcessor = new JwtProcessor();

    @Test
    void generatedTokenContainsUsernameAndIsValid() {
        String token = jwtProcessor.generateToken("deposit-user");

        assertEquals("deposit-user", jwtProcessor.getUsername(token));
        assertTrue(jwtProcessor.validateToken(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtProcessor.generateToken("deposit-user");
        String[] tokenParts = token.split("\\.");
        char firstSignatureCharacter = tokenParts[2].charAt(0);
        char replacement = firstSignatureCharacter == 'a' ? 'b' : 'a';
        tokenParts[2] = replacement + tokenParts[2].substring(1);
        String tamperedToken = String.join(".", tokenParts);

        assertThrows(JwtException.class, () -> jwtProcessor.getUsername(tamperedToken));
    }
}

package com.lv2dev.echonet.auth;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TokenServiceTest {

    @Autowired
    @InjectMocks
    private TokenService tokenService;

    @MockBean
    private MemberRepository memberRepository;

    @Value("${secretKey}")
    private String secretKey;

    private Member member;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        member = new Member();
        member.setId(1L);
        when(memberRepository.findById(anyLong())).thenReturn(java.util.Optional.of(member));
    }

    @Test
    @DisplayName("Create Access Token - Should not be null and follow JWT format")
    void createAccessToken() {
        String token = tokenService.createAccessToken(member);
        assertNotNull(token, "Access Token should not be null");
        assertTrue(token.split("\\.").length == 3, "The token must have 3 parts separated by dots.");
    }

    @Test
    @DisplayName("Validate Token - Should return true for a valid token")
    void isTokenValid() {
        String validToken = tokenService.createAccessToken(member);
        boolean isValid = tokenService.isTokenValid(validToken);
        assertTrue(isValid, "The token validation should return true for a valid token");
    }

    @Test
    @DisplayName("Verify Token Claims - Should correctly set subject, issuedAt, and expiration")
    void tokenClaimsAreValid() {
        String token = tokenService.createAccessToken(member);
        Claims claims = Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        assertEquals(member.getId().toString(), claims.getSubject(), "The subject should match the member's ID.");
        assertNotNull(claims.getIssuedAt(), "Token should have an issue date.");
        assertNotNull(claims.getExpiration(), "Token should have an expiration date.");
    }

    @Test
    @DisplayName("Create Refresh Token - Should not be null")
    void createRefreshToken() {
        String refreshToken = tokenService.createRefreshToken(member);
        assertNotNull(refreshToken, "Refresh Token should not be null");
    }

    @Test
    @DisplayName("Validate Refresh Token - Should return true for a valid token")
    void isRefreshTokenValid() {
        String validRefreshToken = tokenService.createRefreshToken(member);
        boolean isValid = tokenService.isRefreshTokenValid(validRefreshToken);
        assertTrue(isValid, "The refresh token validation should return true for a valid token");
    }

    @Test
    @DisplayName("Refresh Access Token with Invalid Token - Should throw IllegalArgumentException")
    void refreshAccessTokenWithInvalidTokenShouldThrowException() {
        String invalidRefreshToken = "invalidToken";
        assertThrows(IllegalArgumentException.class, () -> tokenService.refreshAccessToken(invalidRefreshToken), "Expected IllegalArgumentException for invalid refresh token.");
    }
}

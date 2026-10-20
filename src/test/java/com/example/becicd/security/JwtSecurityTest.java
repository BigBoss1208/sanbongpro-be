package com.example.becicd.security;

import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import javax.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSecurityTest {
    private JwtTokenProvider provider;
    @BeforeEach void setup() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "isolated-test-only-signing-key-32-bytes-long");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60000);
        provider.validateConfiguration();
        SecurityContextHolder.clearContext();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void validAndTamperedTokens() {
        String token = provider.generateToken("customer", "KHACH_HANG");
        assertTrue(provider.validateToken(token));
        assertEquals("customer", provider.getUsernameFromToken(token));
        assertFalse(provider.validateToken(token + "broken"));
    }
    @Test void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", -1000);
        assertFalse(provider.validateToken(provider.generateToken("customer", "KHACH_HANG")));
    }
    @Test void weakSecretFailsAtStartup() {
        ReflectionTestUtils.setField(provider, "jwtSecret", "");
        assertTrue(assertThrows(IllegalStateException.class, provider::validateConfiguration).getMessage().startsWith("JWT_SECRET is missing or blank"));
    }
    @Test void missingSecretFailsAtStartup() {
        ReflectionTestUtils.setField(provider, "jwtSecret", null);
        assertTrue(assertThrows(IllegalStateException.class, provider::validateConfiguration).getMessage().startsWith("JWT_SECRET is missing or blank"));
    }
    @Test void shortSecretReportsLengthWithoutRevealingSecret() {
        String shortSecret = "short-test-only";
        ReflectionTestUtils.setField(provider, "jwtSecret", shortSecret);
        String message = assertThrows(IllegalStateException.class, provider::validateConfiguration).getMessage();
        assertTrue(message.contains("configured length is 15 bytes"));
        assertFalse(message.contains(shortSecret));
        assertFalse(message.contains("JWT_EXPIRATION_MS"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, -1})
    void invalidExpirationReportsExpirationOnly(int expiration) {
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", expiration);
        String message = assertThrows(IllegalStateException.class, provider::validateConfiguration).getMessage();
        assertTrue(message.contains("JWT_EXPIRATION_MS must be positive"));
        assertFalse(message.contains("JWT_SECRET"));
    }
    @Test void minimumSecretLengthUsesUtf8Bytes() {
        ReflectionTestUtils.setField(provider, "jwtSecret", "é".repeat(16));
        assertDoesNotThrow(provider::validateConfiguration);
        assertTrue(provider.validateToken(provider.generateToken("customer", "KHACH_HANG")));
    }
    @Test void sameTokenStopsWorkingAfterLock() throws Exception {
        NguoiDung user = NguoiDung.builder().username("customer").vaiTro(VaiTro.KHACH_HANG).trangThai(true).build();
        UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
        when(users.loadUserByUsername("customer")).thenAnswer(invocation -> new CustomUserDetails(user));
        JwtAuthFilter filter = new JwtAuthFilter(provider, users);
        String token = provider.generateToken("customer", "ADMIN");
        MockHttpServletRequest first = new MockHttpServletRequest(); first.addHeader("Authorization", "Bearer " + token);
        FilterChain firstChain = mock(FilterChain.class);
        filter.doFilter(first, new MockHttpServletResponse(), firstChain);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_KHACH_HANG")));
        SecurityContextHolder.clearContext();
        user.setTrangThai(false);
        MockHttpServletRequest second = new MockHttpServletRequest(); second.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(second, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }
}

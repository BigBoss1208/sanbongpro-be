package com.example.becicd;

import com.example.becicd.config.RequiredConfiguration;
import com.example.becicd.config.VNPayConfig;
import com.example.becicd.service.VNPayService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisabledVNPayTest {
    @Test void coreConfigurationNeedsNoDisabledIntegrationCredentials() {
        new RequiredConfiguration(new MockEnvironment()
            .withProperty("minio.enabled", "false").withProperty("vnpay.enabled", "false")
            .withProperty("spring.datasource.url", "jdbc:h2:mem:isolated")
            .withProperty("spring.datasource.username", "test")
            .withProperty("spring.datasource.password", "test-only")).validate();
    }
    @Test void disabledVNPayCannotCreateOrSettlePayments() {
        VNPayConfig config = mock(VNPayConfig.class);
        VNPayService service = new VNPayService(config, null, null, null, null);
        ReflectionTestUtils.setField(service, "enabled", false);
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.createPaymentUrl(1L, null)).getStatus().value());
        assertFalse(service.validSignature(Map.of()));
        assertEquals("99", service.handleIpn(Map.of()).get("RspCode"));
        assertFalse(service.handleReturn(Map.of()));
        verifyNoInteractions(config);
    }
}

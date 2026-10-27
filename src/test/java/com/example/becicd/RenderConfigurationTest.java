package com.example.becicd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class RenderConfigurationTest {
    private String returnExpression() {
        YamlPropertiesFactoryBean yaml=new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        return java.util.Objects.requireNonNull(yaml.getObject()).getProperty("vnpay.return-url");
    }
    @Test void derivesReturnFromRealRenderServiceOrigin() {
        MockEnvironment env=new MockEnvironment().withProperty("RENDER_EXTERNAL_URL","https://owned-backend.example.test");
        assertEquals("https://owned-backend.example.test/api/thanh-toan/vnpay/return",env.resolveRequiredPlaceholders(returnExpression()));
    }
    @Test void explicitReturnUrlStillOverridesRender() {
        MockEnvironment env=new MockEnvironment().withProperty("VNPAY_RETURN_URL","https://custom.example.test/api/thanh-toan/vnpay/return");
        assertEquals("https://custom.example.test/api/thanh-toan/vnpay/return",env.resolveRequiredPlaceholders(returnExpression()));
    }
    @Test void noInventedBackendUrlWhenUnconfigured() {
        MockEnvironment env=new MockEnvironment()
            .withProperty("spring.datasource.url","jdbc:h2:mem:config-only")
            .withProperty("spring.datasource.username","isolated")
            .withProperty("spring.datasource.password","test-only")
            .withProperty("minio.endpoint","http://localhost:9000")
            .withProperty("minio.access-key","test-only")
            .withProperty("minio.secret-key","test-only")
            .withProperty("minio.bucket","isolated")
            .withProperty("vnpay.tmn-code","TEST")
            .withProperty("vnpay.hash-secret","test-only")
            .withProperty("vnpay.pay-url","https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        env.setProperty("vnpay.return-url",env.resolveRequiredPlaceholders(returnExpression()));
        assertNull(java.net.URI.create(env.getRequiredProperty("vnpay.return-url")).getHost());
        assertThrows(IllegalStateException.class,()->new com.example.becicd.config.RequiredConfiguration(env).validate());
    }
}

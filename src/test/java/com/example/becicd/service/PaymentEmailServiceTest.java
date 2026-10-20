package com.example.becicd.service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PaymentEmailServiceTest {
    @SuppressWarnings("unchecked")
    @Test void missingConfigurationDisabledAndMissingRecipientNeverSend() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class); when(provider.getIfAvailable()).thenReturn(sender);
        PaymentEmailService service = new PaymentEmailService(provider);
        ReflectionTestUtils.setField(service,"from","sender@example.test");
        ReflectionTestUtils.setField(service,"appPassword","");
        service.reportConfiguration(); service.sendConfirmation(event("customer@example.test"));
        ReflectionTestUtils.setField(service,"enabled",true);
        service.reportConfiguration(); service.sendConfirmation(event("customer@example.test"));
        ReflectionTestUtils.setField(service,"appPassword","isolated-test-password");
        service.sendConfirmation(event("")); verifyNoInteractions(sender);
    }
    @SuppressWarnings("unchecked")
    @Test void authenticationFailureIsContainedAndContentIncludesBookingDetails() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class); when(provider.getIfAvailable()).thenReturn(sender);
        PaymentEmailService service = new PaymentEmailService(provider);
        ReflectionTestUtils.setField(service,"enabled",true);
        ReflectionTestUtils.setField(service,"from","sender@example.test");
        ReflectionTestUtils.setField(service,"appPassword","isolated-test-password");
        doThrow(new MailAuthenticationException("private SMTP details must not be logged")).when(sender).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendConfirmation(event("customer@example.test")));
        var captured=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(captured.capture());
        assertTrue(captured.getValue().getText().contains("Mã đơn: #33"));
        assertTrue(captured.getValue().getText().contains("15/10/2026"));
        assertTrue(captured.getValue().getText().contains("Customer"));
        assertTrue(captured.getValue().getText().contains("Field"));
        assertTrue(captured.getValue().getText().contains("300.000"));
    }
    private PaymentConfirmedEmail event(String recipient) {
        return new PaymentConfirmedEmail(33L,recipient,"Customer","Field",LocalDate.of(2026,10,15),LocalTime.of(18,0),LocalTime.of(19,0),BigDecimal.valueOf(300000));
    }
}

package com.example.becicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PaymentEmailService {
    private static final Logger log = LoggerFactory.getLogger(PaymentEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    @Value("${app.payment-email-enabled:false}") private boolean enabled;
    @Value("${spring.mail.username:}") private String from;
    @Value("${spring.mail.password:}") private String appPassword;

    public PaymentEmailService(ObjectProvider<JavaMailSender> mailSender) { this.mailSender = mailSender; }

    @javax.annotation.PostConstruct
    public void reportConfiguration() {
        if (!enabled) {
            log.warn("Payment email disabled: set GMAIL_ENABLED=true and configure GMAIL_USERNAME/GMAIL_APP_PASSWORD");
        } else if (from == null || from.isBlank() || appPassword == null || appPassword.isBlank()) {
            log.warn("Payment email enabled but Gmail SMTP username/App Password is missing; payment processing remains available");
        } else {
            log.info("Payment email enabled; Gmail authentication has not yet been verified");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendConfirmation(PaymentConfirmedEmail event) {
        if (!enabled) {
            log.info("Payment email skipped for booking {}: GMAIL_ENABLED=false", event.getBookingId());
            return;
        }
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || from == null || from.isBlank() || appPassword == null || appPassword.isBlank() || event.getRecipient() == null || event.getRecipient().isBlank()) {
                log.warn("Payment email skipped for booking {}: missing mail configuration or recipient", event.getBookingId());
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.getRecipient());
            message.setSubject("SÂN BÓNG PRO - Xác nhận thanh toán đơn #" + event.getBookingId());
            DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
            message.setText("Xin chào " + event.getCustomerName() + ",\n\n"
                + "Bạn đã thanh toán VNPay thành công.\n"
                + "Mã đơn: #" + event.getBookingId() + "\n"
                + "Sân: " + event.getFieldName() + "\n"
                + "Ngày đá: " + event.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                + "Khung giờ: " + event.getStart().format(time) + " - " + event.getEnd().format(time) + "\n"
                + "Tổng tiền: " + NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(event.getAmount()) + " đ\n\n"
                + "Đơn đặt sân đã được xác nhận. Cảm ơn bạn đã sử dụng SÂN BÓNG PRO!");
            sender.send(message);
            log.info("Payment email accepted by SMTP for booking {}", event.getBookingId());
        } catch (Exception ex) {
            // Never log SMTP credentials, recipient addresses or the raw exception message.
            log.warn("Payment email failed for booking {} ({})", event.getBookingId(), failureCode(ex));
        }
    }
    private String failureCode(Exception ex) {
        Throwable cause = ex;
        for (int depth = 0; cause != null && depth < 10; depth++, cause = cause.getCause()) {
            if (cause instanceof org.springframework.mail.MailAuthenticationException || cause instanceof javax.mail.AuthenticationFailedException) return "SMTP_AUTH_FAILED";
            if (cause instanceof java.net.SocketTimeoutException) return "SMTP_TIMEOUT";
            if (cause instanceof java.net.ConnectException) return "SMTP_CONNECTION_FAILED";
        }
        return ex.getClass().getSimpleName();
    }
}

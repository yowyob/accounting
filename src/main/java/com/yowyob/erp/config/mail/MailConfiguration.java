package com.yowyob.erp.config.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Properties;

@Configuration
@Slf4j
public class MailConfiguration {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender dummyMailSender() {
        log.warn("Attributes 'spring.mail.host' not found. Using Dummy Mail Sender (logs only).");
        return new DummyJavaMailSender();
    }

    @Slf4j
    static class DummyJavaMailSender implements JavaMailSender {
        @Override
        public MimeMessage createMimeMessage() {
            return null; // Not implemented for dummy
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            return null; // Not implemented for dummy
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            log.info("Dummy Mail Sender: MimeMessage sent (simulated)");
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            log.info("Dummy Mail Sender: {} MimeMessages sent (simulated)", mimeMessages.length);
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            log.info("Dummy Mail Sender: MimeMessagePreparator sent (simulated)");
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            log.info("Dummy Mail Sender: {} MimeMessagePreparators sent (simulated)", mimeMessagePreparators.length);
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            log.info("Dummy Mail Sender: Sending email to {}, subject: {}, body: {}",
                    simpleMessage.getTo(), simpleMessage.getSubject(), simpleMessage.getText());
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage msg : simpleMessages) {
                send(msg);
            }
        }
    }
}

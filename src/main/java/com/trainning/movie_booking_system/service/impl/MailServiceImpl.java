package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.EmailDTO;
import com.trainning.movie_booking_system.dto.AttachmentDTO;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link MailService} for handling various types of email sending,
 * including simple text, HTML templates, and attachments, both synchronous and asynchronous.
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine mailTemplateEngine;

    public MailServiceImpl(JavaMailSender mailSender,
                           @Qualifier("mailTemplateEngine") TemplateEngine mailTemplateEngine) {
        this.mailSender = mailSender;
        this.mailTemplateEngine = mailTemplateEngine;
    }

    /**
     * Send email based on a DTO containing full information.
     *
     * @param emailDTO DTO including from, to, subject, body, attachments, etc.
     */
    @Override
    public void sendEmail(EmailDTO emailDTO) {
        try {
            if (emailDTO.isHtml() || emailDTO.getAttachments() != null || StringUtils.hasText(emailDTO.getTemplateName())) {
                sendMimeMessage(emailDTO);
            } else {
                sendSimpleMessage(emailDTO);
            }
            log.info("Email sent successfully to {}", emailDTO.getTo());
        } catch (Exception e) {
            log.error("SendMail failed: {}", e.getMessage(), e);
            throw new NotFoundException("SendMail failed");
        }
    }

    /**
     * Send an HTML email using a template (Thymeleaf/Freemarker).
     *
     * @param to           Recipient email address
     * @param subject      Subject line
     * @param templateName Template file name (e.g. welcome.html)
     * @param variables    Map of variables to inject into the template
     */
    @Override
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendTemplateEmail(new String[]{to}, subject, templateName, variables);
    }

    /**
     * Send a simple plain text email (no template).
     * <p>Useful for OTPs, password reset, quick notifications.</p>
     *
     * @param to      Recipient email address
     * @param subject Subject line
     * @param content Plain text content
     */
    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        sendSimpleEmail(new String[]{to}, subject, content);
    }

    /**
     * Send an HTML email using a template (to multiple recipients).
     *
     * @param to           Array of recipient emails
     * @param subject      Subject line
     * @param templateName Template file name (e.g. welcome.html)
     * @param variables    Map of variables to inject into the template
     */
    @Override
    public void sendTemplateEmail(String[] to, String subject, String templateName, Map<String, Object> variables) {
        EmailDTO emailDTO = EmailDTO.builder()
                .to(Arrays.asList(to))
                .subject(subject)
                .templateName(templateName)
                .templateVariables(variables)
                .isHtml(true)
                .build();
        sendEmail(emailDTO);
    }

    /**
     * Send a simple plain text email (to multiple recipients).
     *
     * @param to      Array of recipient emails
     * @param subject Subject line
     * @param content Plain text content
     */
    @Override
    public void sendSimpleEmail(String[] to, String subject, String content) {
        EmailDTO emailDTO = EmailDTO.builder()
                .to(Arrays.asList(to))
                .subject(subject)
                .textContent(content)
                .isHtml(false)
                .build();
        sendEmail(emailDTO);
    }

    //========== ASYNC METHODS ==========//

    /**
     * Send email asynchronously based on a DTO.
     *
     * @param emailDTO DTO including from, to, subject, body, attachments, etc.
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendEmailAsync(EmailDTO emailDTO) {
        CompletableFuture.runAsync(() -> sendEmail(emailDTO));
    }

    /**
     * Send an HTML email asynchronously using a template.
     *
     * @param to           Recipient email address
     * @param subject      Subject line
     * @param templateName Template file name
     * @param variables    Map of variables to inject into the template
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendTemplateEmailAsync(String to, String subject, String templateName, Map<String, Object> variables) {
        CompletableFuture.runAsync(() -> sendTemplateEmail(to, subject, templateName, variables));
    }

    /**
     * Send an HTML email asynchronously using a template (to multiple recipients).
     *
     * @param to           Array of recipient emails
     * @param subject      Subject line
     * @param templateName Template file name
     * @param variables    Map of variables to inject into the template
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendTemplateEmailAsync(String[] to, String subject, String templateName, Map<String, Object> variables) {
        CompletableFuture.runAsync(() -> sendTemplateEmail(to, subject, templateName, variables));
    }

    /**
     * Send a plain text email asynchronously.
     *
     * @param to      Recipient email address
     * @param subject Subject line
     * @param content Plain text content
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendSimpleEmailAsync(String to, String subject, String content) {
        CompletableFuture.runAsync(() -> sendSimpleEmail(to, subject, content));
    }

    /**
     * Send a plain text email asynchronously (to multiple recipients).
     *
     * @param to      Array of recipient emails
     * @param subject Subject line
     * @param content Plain text content
     */
    @Override
    @Async("mailTaskExecutor")
    public void sendSimpleEmailAsync(String[] to, String subject, String content) {
        CompletableFuture.runAsync(() -> sendSimpleEmail(to, subject, content));
    }

    //========== PRIVATE METHODS ==========//

    private void sendSimpleMessage(EmailDTO emailDto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailDto.getTo().toArray(new String[0]));

        if (emailDto.getCc() != null && !emailDto.getCc().isEmpty()) {
            message.setCc(emailDto.getCc().toArray(new String[0]));
        }
        if (emailDto.getBcc() != null && !emailDto.getBcc().isEmpty()) {
            message.setBcc(emailDto.getBcc().toArray(new String[0]));
        }

        message.setSubject(emailDto.getSubject());
        message.setText(emailDto.getTextContent());
        mailSender.send(message);
    }

    private void sendMimeMessage(EmailDTO emailDTO) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emailDTO.getTo().toArray(new String[0]));
        if (emailDTO.getCc() != null && !emailDTO.getCc().isEmpty()) {
            helper.setCc(emailDTO.getCc().toArray(new String[0]));
        }
        if (emailDTO.getBcc() != null && !emailDTO.getBcc().isEmpty()) {
            helper.setBcc(emailDTO.getBcc().toArray(new String[0]));
        }

        helper.setSubject(emailDTO.getSubject());
        String content = getMailContent(emailDTO);
        helper.setText(content, emailDTO.isHtml());

        if (emailDTO.getAttachments() != null) {
            addAttachments(helper, emailDTO.getAttachments());
        }

        mailSender.send(message);
    }

    private String getMailContent(EmailDTO emailDTO) {
        if (StringUtils.hasText(emailDTO.getTemplateName())) {
            return processTemplate(emailDTO.getTemplateName(), emailDTO.getTemplateVariables());
        } else if (emailDTO.isHtml() && StringUtils.hasText(emailDTO.getHtmlContent())) {
            return emailDTO.getHtmlContent();
        } else {
            return emailDTO.getTextContent();
        }
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            return mailTemplateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Failed to process email template: {}", templateName, e);
            throw new NotFoundException("Email template not found");
        }
    }

    private void addAttachments(MimeMessageHelper helper, List<AttachmentDTO> attachments) throws MessagingException {
        for (AttachmentDTO attachment : attachments) {
            if (attachment.getContent() != null) {
                ByteArrayResource resource = new ByteArrayResource(attachment.getContent());
                if (attachment.isInline() && StringUtils.hasText(attachment.getContentId())) {
                    helper.addInline(attachment.getContentId(), resource, attachment.getContentType());
                } else {
                    helper.addAttachment(attachment.getFilename(), resource);
                }
            } else if (StringUtils.hasText(attachment.getFilePath())) {
                File file = new File(attachment.getFilePath());
                if (file.exists()) {
                    FileSystemResource resource = new FileSystemResource(file);
                    if (attachment.isInline() && StringUtils.hasText(attachment.getContentId())) {
                        helper.addInline(attachment.getContentId(), resource);
                    } else {
                        helper.addAttachment(attachment.getFilename(), resource);
                    }
                }
            }
        }
    }
}

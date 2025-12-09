package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.EmailDTO;

import java.util.Map;

public interface MailService {
    /**
     * Send email based on a DTO containing full information.
     *
     * @param emailDTO DTO including from, to, subject, body, attachments, etc.
     */
    void sendEmail(EmailDTO emailDTO);

    /**
     * Send an HTML email using a template (Thymeleaf/Freemarker).
     *
     * @param to           Recipient email address
     * @param subject      Subject line
     * @param templateName Template file name (e.g. welcome.html)
     * @param variables    Map of variables to inject into the template
     */
    void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Send a simple plain text email (no template).
     * <p>Useful for OTPs, password reset, quick notifications.</p>
     *
     * @param to      Recipient email address
     * @param subject Subject line
     * @param content Plain text content
     */
    void sendSimpleEmail(String to, String subject, String content);

    /**
     * Send an HTML email using a template (to multiple recipients).
     *
     * @param to           Array of recipient emails
     * @param subject      Subject line
     * @param templateName Template file name (e.g. welcome.html)
     * @param variables    Map of variables to inject into the template
     */
    void sendTemplateEmail(String[] to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Send a simple plain text email (to multiple recipients).
     *
     * @param to      Array of recipient emails
     * @param subject Subject line
     * @param content Plain text content
     */
    void sendSimpleEmail(String[] to, String subject, String content);

    //========== ASYNC METHODS ==========//

    /**
     * Send email asynchronously based on a DTO.
     *
     * @param emailDTO DTO including from, to, subject, body, attachments, etc.
     */
    void sendEmailAsync(EmailDTO emailDTO);

    /**
     * Send an HTML email asynchronously using a template.
     *
     * @param to           Recipient email address
     * @param subject      Subject line
     * @param templateName Template file name
     * @param variables    Map of variables to inject into the template
     */
    void sendTemplateEmailAsync(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Send an HTML email asynchronously using a template (to multiple recipients).
     *
     * @param to           Array of recipient emails
     * @param subject      Subject line
     * @param templateName Template file name
     * @param variables    Map of variables to inject into the template
     */
    void sendTemplateEmailAsync(String[] to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Send a plain text email asynchronously.
     *
     * @param to      Recipient email address
     * @param subject Subject line
     * @param content Plain text content
     */
    void sendSimpleEmailAsync(String to, String subject, String content);

    /**
     * Send a plain text email asynchronously (to multiple recipients).
     *
     * @param to      Array of recipient emails
     * @param subject Subject line
     * @param content Plain text content
     */
    void sendSimpleEmailAsync(String[] to, String subject, String content);
}

package com.trainning.movie_booking_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EmailDTO {

    private String from;

    @NotEmpty(message = "Recipients cannot be empty")
    private List<@Email(message = "Invalid email format") String> to;

    private List<@Email(message = "Invalid email format") String> cc;
    private List<@Email(message = "Invalid email format") String> bcc;

    @NotBlank(message = "Subject cannot be blank")
    private String subject;

    private String textContent;
    private String htmlContent;
    private String templateName;
    private Map<String, Object> templateVariables;
    private List<AttachmentDTO> attachments;

    @Builder.Default
    private boolean isHtml = true;
}
package com.trainning.movie_booking_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentDTO {

    @NotBlank(message = "Filename cannot be blank")
    private String filename;

    @NotBlank(message = "Content type cannot be blank")
    private String contentType;

    private byte[] content;
    private String filePath;

    @Builder.Default
    private boolean inline = false;

    private String contentId;
}
package com.taskboard.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for creating or updating a label.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabelRequest {

    @NotBlank(message = "Label name is required")
    @Size(min = 1, max = 50, message = "Label name must be between 1 and 50 characters")
    private String name;

    @Builder.Default
    private String color = "#3498db";
}


package com.hrflow.request.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private String text;
    private String commentText;

    public String effectiveText() {
        if (commentText != null && !commentText.isBlank()) {
            return commentText;
        }
        return text;
    }
}

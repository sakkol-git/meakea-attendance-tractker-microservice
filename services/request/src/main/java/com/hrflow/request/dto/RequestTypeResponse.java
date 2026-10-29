package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTypeResponse {
    private String id;
    private String code;
    private String name;
    private String category;
    private String formSchema;
    private boolean active;
}

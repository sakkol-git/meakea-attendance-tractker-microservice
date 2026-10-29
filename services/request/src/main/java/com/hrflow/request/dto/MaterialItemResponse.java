package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialItemResponse {
    private String id;
    private String code;
    private String name;
    private String category;
    private int advisoryStock;
    private String unit;
    private boolean active;
}

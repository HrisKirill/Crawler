package com.uapp.agro.crawler.image.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ImageCreateDto {
    private String originalUrl;
    private String filePath;
}

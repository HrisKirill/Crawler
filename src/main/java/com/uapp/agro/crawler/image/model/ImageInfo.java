package com.uapp.agro.crawler.image.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "image_info")
public class ImageInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String originalUrl;
    @Column(nullable = false)
    private String filePath;
    @Column(nullable = false)
    private long originalSize;
    @Column(nullable = false)
    private long compressedSize;

    public ImageInfo(String originalUrl, String filePath, long originalSize, long compressedSize) {
        this.originalUrl = originalUrl;
        this.filePath = filePath;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
    }
}

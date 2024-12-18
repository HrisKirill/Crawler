package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;
import com.uapp.agro.crawler.image.repository.ImageInfoRepository;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageInfoServiceImpl implements ImageInfoService {
    private final ImageInfoRepository imageInfoRepository;

    @Override
    public ImageInfo createIfNotExists(ImageCreateDto dto) {
        return imageInfoRepository.findByOriginalUrl(dto.getOriginalUrl())
                .orElseGet(() -> {
                    ImageInfo imageInfo = new ImageInfo(
                            dto.getOriginalUrl(),
                            dto.getFilePath(),
                            dto.getOriginalSize(),
                            dto.getCompressedSize()
                    );
                    log.info("Save image: {}", imageInfo.getOriginalUrl());
                    return imageInfoRepository.save(imageInfo);
                });

    }
}

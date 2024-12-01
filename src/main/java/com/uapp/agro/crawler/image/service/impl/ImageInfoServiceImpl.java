package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;
import com.uapp.agro.crawler.image.repository.ImageInfoRepository;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageInfoServiceImpl implements ImageInfoService {
    private final ImageInfoRepository imageInfoRepository;

    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
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

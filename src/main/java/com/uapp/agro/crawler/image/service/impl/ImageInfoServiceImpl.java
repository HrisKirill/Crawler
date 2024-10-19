package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;
import com.uapp.agro.crawler.image.repository.ImageInfoRepository;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageInfoServiceImpl implements ImageInfoService {
    private final ImageInfoRepository imageInfoRepository;

    @Override
    @Transactional
    public ImageInfo createIfNotExists(ImageCreateDto dto) {
        Optional<ImageInfo> optionalImageInfo = imageInfoRepository.findByOriginalUrl(dto.getOriginalUrl());
        if (optionalImageInfo.isEmpty()) {
            ImageInfo imageInfo = new ImageInfo(dto.getOriginalUrl(), dto.getFilePath());
            return imageInfoRepository.save(imageInfo);
        }
        return optionalImageInfo.get();
    }
}

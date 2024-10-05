package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;
import com.uapp.agro.crawler.image.repository.ImageInfoRepository;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageInfoServiceImpl implements ImageInfoService {
    private final ImageInfoRepository imageInfoRepository;

    @Override
    public ImageInfo save(ImageCreateDto dto) {
        var imageInfo = new ImageInfo(dto.getOriginalUrl(), dto.getFilePath());
        return imageInfoRepository.save(imageInfo);
    }
}

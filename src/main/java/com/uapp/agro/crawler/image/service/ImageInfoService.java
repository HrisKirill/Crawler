package com.uapp.agro.crawler.image.service;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;

public interface ImageInfoService {
    ImageInfo createIfNotExists(ImageCreateDto dto);
}

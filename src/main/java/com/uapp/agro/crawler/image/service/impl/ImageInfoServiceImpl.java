package com.uapp.agro.crawler.image.service.impl;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.model.ImageInfo;
import com.uapp.agro.crawler.image.repository.ImageInfoRepository;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ImageInfoServiceImpl implements ImageInfoService {
    private final ImageInfoRepository imageInfoRepository;
    private final Lock lock = new ReentrantLock();

    @Override
    public ImageInfo createIfNotExists(ImageCreateDto dto) {
        try {
            lock.lock();
            return imageInfoRepository.findByOriginalUrl(dto.getOriginalUrl())
                    .orElseGet(() -> {
                        ImageInfo imageInfo = new ImageInfo(dto.getOriginalUrl(), dto.getFilePath());
                        return imageInfoRepository.save(imageInfo);
                    });
        } finally {
            lock.unlock();
        }
    }
}

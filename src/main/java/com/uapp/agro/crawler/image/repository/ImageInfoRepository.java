package com.uapp.agro.crawler.image.repository;

import com.uapp.agro.crawler.image.model.ImageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageInfoRepository extends JpaRepository<ImageInfo, Long> {
    Optional<ImageInfo> findByOriginalUrl(String url);
}

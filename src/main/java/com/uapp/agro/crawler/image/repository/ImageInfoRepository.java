package com.uapp.agro.crawler.image.repository;

import com.uapp.agro.crawler.image.model.ImageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageInfoRepository extends JpaRepository<ImageInfo, Long> {
}

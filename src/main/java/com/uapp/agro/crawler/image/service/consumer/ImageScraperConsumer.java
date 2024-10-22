package com.uapp.agro.crawler.image.service.consumer;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
public class ImageScraperConsumer implements Runnable {
    private final BlockingQueue<String> imageQueue;
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
    private final String compressedImageFolderPath;
    private final ImageInfoService infoService;
    private final AtomicInteger producersCount;
    private final List<String> availableFormats;
    private final Lock localWriteLock = new ReentrantLock();


    @Override
    public void run() {
        try {
            while (true) {
                String image = imageQueue.poll();

                if (image == null) {
                    if (isQueueEmptyAndNoProducer()) {
                        break;
                    }
                    continue;
                }

                if (visitedImages.add(image)) {
                    try {
                        saveImage(image);
                    } catch (Exception e) {
                        visitedImages.remove(image);
                        log.warn("Problem during {} image processing", image);
                    }
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.warn(e.getMessage());
        }
    }

    private boolean isQueueEmptyAndNoProducer() {
        return producersCount.get() == 0 && imageQueue.isEmpty();
    }

    private void saveImage(String image) {
        ImageCreateDto imageInfo = new ImageCreateDto(image, compressedImageFolderPath);
        saveLocal(image);
        saveToDb(imageInfo);
    }

    private void saveToDb(ImageCreateDto dto) {
        infoService.createIfNotExists(dto);
    }


    public void saveLocal(String imageUrl) {
        try {
            String format = getImageFormat(imageUrl);
            if (availableFormats.contains(format)) {
                String outputFileName = getFileNameFromUrl(imageUrl);
                BufferedImage originalImage = downloadImage(imageUrl);

                try {
                    localWriteLock.lock();
                    File outputFile = new File(compressedImageFolderPath + outputFileName + "." + format);
                    if (!outputFile.exists()) {
                        BufferedImage compressedImage = compressImage(originalImage);
                        writeToFile(format, compressedImage, outputFile);
                        log.info("Compressed image {} saved to: {}", imageUrl, outputFile.getAbsolutePath());
                    } else {
                        log.info("Skip file by path: {}", outputFile.getAbsolutePath());
                    }
                } finally {
                    localWriteLock.unlock();
                }
            }
        } catch (IOException ex) {
            log.warn(ex.getMessage());
        }
    }

    private BufferedImage compressImage(BufferedImage originalImage) {
        int newWidth = originalImage.getWidth() / 2;
        int newHeight = originalImage.getHeight() / 2;
        BufferedImage compressedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = null;
        try {
            g2d = compressedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        } finally {
            if (g2d != null) {
                g2d.dispose();
            }
        }
        return compressedImage;
    }

    private void writeToFile(String format, BufferedImage compressedImage, File outputFile) throws IOException {
        ImageIO.write(compressedImage, format, outputFile);
    }

    private BufferedImage downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        return ImageIO.read(url);
    }

    private String getFileNameFromUrl(String imageUrl) {
        return FilenameUtils.getBaseName(imageUrl);
    }

    private String getImageFormat(String imageUrl) {
        return FilenameUtils.getExtension(imageUrl);
    }

}

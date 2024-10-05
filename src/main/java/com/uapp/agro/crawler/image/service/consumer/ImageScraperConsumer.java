package com.uapp.agro.crawler.image.service.consumer;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ImageScraperConsumer implements Runnable {
    private final BlockingQueue<String> imageQueue;
    private final Set<String> visitedImages = ConcurrentHashMap.newKeySet();
    private final String compressedImageFolderPath;
    private final Lock lock = new ReentrantLock();
    private final ImageInfoService infoService;
    private final AtomicInteger producersCount;

    public ImageScraperConsumer(BlockingQueue<String> imageQueue,
                                String compressedImageFolderPath,
                                ImageInfoService infoService,
                                AtomicInteger producersCount) {
        this.imageQueue = imageQueue;
        this.compressedImageFolderPath = compressedImageFolderPath;
        this.infoService = infoService;
        this.producersCount = producersCount;
    }

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

                if (visitedImages.contains(image)) {
                    log.info("Skip image: {}", image);
                    continue;
                }

                try {
                    lock.lock();
                    saveImage(image);
                    visitedImages.add(image);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.info(e.getMessage());
        }
    }

    private boolean isQueueEmptyAndNoProducer() {
        return producersCount.get() == 0 && imageQueue.isEmpty();
    }

    private void saveImage(String image) {
        var imageInfo = new ImageCreateDto(image, compressedImageFolderPath);
        log.info("Save image: {}", image);
        saveLocal(image);
        saveToDb(imageInfo);
    }

    private void saveToDb(ImageCreateDto dto) {
        infoService.save(dto);
    }

    public void saveLocal(String imageUrl) {
        try {
            var originalImage = downloadImage(imageUrl);
            compressImageAndSave(imageUrl, originalImage);
        } catch (IOException ex) {
            log.info(ex.getMessage());
        }
    }

    private void compressImageAndSave(String imageUrl, BufferedImage originalImage) throws IOException {
        var compressedImage = compressImage(originalImage);
        writeToFile(imageUrl, compressedImage);
    }

    private BufferedImage compressImage(BufferedImage originalImage) {
        int newWidth = originalImage.getWidth() / 2;
        int newHeight = originalImage.getHeight() / 2;
        var compressedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());

        var g2d = compressedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return compressedImage;
    }

    private void writeToFile(String imageUrl, BufferedImage compressedImage) throws IOException {
        var outputFileName = getFileNameFromUrl(imageUrl);
        var format = getImageFormat(imageUrl);

        var outputFile = new File(compressedImageFolderPath + outputFileName + "." + format);
        ImageIO.write(compressedImage, format, outputFile);
        log.info("Compressed image saved to: {}", outputFile.getAbsolutePath());
    }

    private BufferedImage downloadImage(String imageUrl) throws IOException {
        var url = new URL(imageUrl);
        return ImageIO.read(url);
    }

    private String getFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1, imageUrl.lastIndexOf("."));
    }

    private String getImageFormat(String imageUrl) {
        if (imageUrl.endsWith(".jpg") || imageUrl.endsWith(".jpeg")) {
            return "jpg";
        }
        return "png";
    }

}

package com.uapp.agro.crawler.consumer;

import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
public class ImageScraperConsumer implements Runnable {
    private final BlockingQueue<String> imageQueue;
    private final Set<String> processedImages;
    private final String compressedImageFolderPath;
    private final ImageInfoService infoService;
    private final AtomicInteger producersCount;
    private final Set<String> availableFormats;
    private final Lock localWriteLock = new ReentrantLock();

    @Override
    public void run() {
        try {
            processImages();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.warn(e.getMessage());
        }
    }

    private void processImages() {
        while (true) {
            String imageUrl = imageQueue.poll();
            if (imageUrl == null && isQueueEmptyAndNoProducer()) {
                break;
            }

            if (imageUrl != null && processedImages.add(imageUrl)) {
                saveImage(imageUrl);
            }
        }
    }

    private boolean isQueueEmptyAndNoProducer() {
        return producersCount.get() == 0 && imageQueue.isEmpty();
    }

    private void saveImage(String imageUrl) {
        try {
            String format = getImageFormat(imageUrl);
            if (!availableFormats.contains(format)) {
                return;
            }

            BufferedImage originalImage = downloadImage(imageUrl);
            long originalSize = calculateImageSize(originalImage, format);

            byte[] compressedImage = compressImage(originalImage, format);
            long compressedSize = compressedImage.length;
            File outputFile = createOutputFile(imageUrl, format);

            saveCompressedImage(imageUrl, compressedImage, outputFile, format);
            saveToDb(imageUrl, originalSize, compressedSize, outputFile);
        } catch (Exception e) {
            log.warn("Error saving image: {}", imageUrl, e);
        }
    }

    private File createOutputFile(String imageUrl, String format) {
        String outputFileName = getFileNameFromUrl(imageUrl) + "." + format;
        return new File(compressedImageFolderPath + outputFileName);
    }

    private void saveCompressedImage(String imageUrl, byte[] compressedImage, File outputFile, String format) throws IOException {
        try {
            localWriteLock.lock();
            if (!outputFile.exists()) {
                writeToFile(imageUrl, compressedImage, outputFile, format);
            } else {
                log.info("Skip file by path: {}", outputFile.getAbsolutePath());
            }
        } finally {
            localWriteLock.unlock();
        }
    }


    private long calculateImageSize(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.size();
        }
    }

    private void saveToDb(String imageUrl, long originalSize, long compressedSize, File outputFile) {
        ImageCreateDto imageInfo = new ImageCreateDto(
                imageUrl,
                outputFile.getAbsolutePath(),
                originalSize,
                compressedSize
        );
        infoService.createIfNotExists(imageInfo);
    }

    private byte[] compressImage(BufferedImage originalImage, String format) throws IOException {
        BigDecimal dividedBy = BigDecimal.valueOf(2);
        BigDecimal originalSizeKB = BigDecimal.valueOf(calculateImageSize(originalImage, format))
                .divide(BigDecimal.valueOf(1024), RoundingMode.CEILING);
        log.info("originalSizeKB: {}", originalSizeKB);
        BigDecimal maxCompressedSize = originalSizeKB.divide(dividedBy, 4, RoundingMode.CEILING);
        log.info("maxCompressedSize: {}", maxCompressedSize);

        return compressToTargetSize(originalImage, format, maxCompressedSize);
    }

    private byte[] compressToTargetSize(BufferedImage originalImage, String format, BigDecimal targetSize) throws IOException {
        BigDecimal quality = BigDecimal.ONE;
        BigDecimal scaling = BigDecimal.ONE;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            while (quality.floatValue() > 0. || scaling.floatValue() > 0.) {
                baos.reset();

                applyCompression(originalImage, format, quality, scaling, baos);

                byte[] compressedData = baos.toByteArray();
                BigDecimal compressedDataLengthKB = BigDecimal.valueOf(compressedData.length / 1024);
                if (targetSize.compareTo(compressedDataLengthKB) >= 0) {
                    return compressedData;
                }

                if (quality.floatValue() > 0.) {
                    quality = quality.subtract(BigDecimal.valueOf(0.05));
                }

                if (quality.floatValue() == 0 && scaling.floatValue() > 0.) {
                    scaling = scaling.subtract(BigDecimal.valueOf(0.05));
                }
            }

            return baos.toByteArray();
        }
    }

    private void applyCompression(BufferedImage image, String format, BigDecimal quality, BigDecimal scaling, ByteArrayOutputStream baos) throws IOException {
        switch (format) {
            case "jpg", "jpeg" -> Thumbnails.of(image)
                    .scale(scaling.floatValue())
                    .outputQuality(quality.floatValue())
                    .outputFormat(format)
                    .toOutputStream(baos);
            case "png" -> Thumbnails.of(image)
                    .scale(scaling.floatValue())
                    .outputFormat(format)
                    .toOutputStream(baos);
        }
    }

    private void writeToFile(String imageUrl, byte[] compressedImage, File outputFile, String format) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedImage)) {
            BufferedImage bImage = ImageIO.read(bis);
            ImageIO.write(bImage, format, outputFile);
            log.info("Compressed image {} saved to: {}", imageUrl, outputFile.getAbsolutePath());
        }
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

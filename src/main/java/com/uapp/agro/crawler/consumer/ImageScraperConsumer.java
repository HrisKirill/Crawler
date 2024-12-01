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
            while (true) {
                String image = imageQueue.poll();

                if (image == null) {
                    if (isQueueEmptyAndNoProducer()) {
                        break;
                    }
                    continue;
                }

                if (processedImages.add(image)) {
                    try {
                        saveImage(image);
                    } catch (Exception e) {
                        processedImages.remove(image);
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

    private void saveImage(String imageUrl) {
        try {
            String format = getImageFormat(imageUrl);
            if (availableFormats.contains(format)) {
                BufferedImage originalImage = downloadImage(imageUrl);
                long originalSize = calculateImageSize(originalImage, format);

                byte[] compressedImage = compressImage(originalImage, format);
                long compressedSize = compressedImage.length;

                String outputFileName = getFileNameFromUrl(imageUrl);
                File outputFile = new File(compressedImageFolderPath + outputFileName + "." + format);

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
                saveToDb(imageUrl, originalSize, compressedSize, outputFile);
            }
        } catch (Exception e) {
            log.warn("Error saving image: {}", imageUrl, e);
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
        BigDecimal minCompressedSize;
        BigDecimal dividedBy = BigDecimal.valueOf(2);
        try (ByteArrayOutputStream initialBaos = new ByteArrayOutputStream()) {
            ImageIO.write(originalImage, format, initialBaos);
            BigDecimal originalSizeKB = BigDecimal.valueOf(initialBaos.toByteArray().length / 1024);
            log.info("originalSizeKB: {}", originalSizeKB);
            minCompressedSize = originalSizeKB.divide(dividedBy, 4, RoundingMode.CEILING);
            log.info("minCompressedSize: {}", minCompressedSize);
        }

        BigDecimal compressionStartQuality = new BigDecimal("1");
        BigDecimal scaling = new BigDecimal("1");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            while (compressionStartQuality.floatValue() > 0. || scaling.floatValue() > 0.) {
                baos.reset();

                log.info("Quality: {}", compressionStartQuality);
                log.info("Scaling: {}", scaling);

                switch (format) {
                    case "jpg", "jpeg" -> Thumbnails.of(originalImage)
                            .scale(scaling.floatValue())
                            .outputFormat(format)
                            .outputQuality(compressionStartQuality.floatValue())
                            .toOutputStream(baos);
                    case "png" -> {
                        compressionStartQuality = new BigDecimal("0");
                        Thumbnails.of(originalImage)
                                .outputFormat(format)
                                .scale(scaling.floatValue())
                                .toOutputStream(baos);
                    }
                }

                byte[] compressedData = baos.toByteArray();
                BigDecimal compressedDataLengthKB = BigDecimal.valueOf(compressedData.length / 1024);
                log.info("compressedDataLengthKB: {}", compressedDataLengthKB);
                if (minCompressedSize.compareTo(compressedDataLengthKB) >= 0) {
                    return compressedData;
                }

                if (compressionStartQuality.floatValue() > 0.) {
                    compressionStartQuality = compressionStartQuality.subtract(BigDecimal.valueOf(0.05));
                }

                if (compressionStartQuality.floatValue() == 0 && scaling.floatValue() > 0.) {
                    scaling = scaling.subtract(BigDecimal.valueOf(0.05));
                }
            }

            return baos.toByteArray();
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

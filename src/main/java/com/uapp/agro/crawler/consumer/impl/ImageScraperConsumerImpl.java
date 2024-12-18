package com.uapp.agro.crawler.consumer.impl;

import com.uapp.agro.crawler.config.ScraperConfiguration;
import com.uapp.agro.crawler.consumer.ImageScraperConsumer;
import com.uapp.agro.crawler.image.dto.ImageCreateDto;
import com.uapp.agro.crawler.image.service.ImageInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;

import static com.uapp.agro.crawler.config.RabbitMQConfiguration.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImageScraperConsumerImpl implements ImageScraperConsumer {
    private final ScraperConfiguration configuration;
    private final ImageInfoService infoService;
    private final RabbitTemplate rabbitTemplate;
    private static final Integer MAX_RETRIES_COUNT = 3;
    private static final String HEADER_X_RETRIES_COUNT = "HEADER_X_RETRIES_COUNT";


    @RabbitListener(queues = PARKING_LOT_IMAGES_QUEUE)
    public void processParkingLotQueue(Message failedMessage) {
        log.info("Received message in parking lot queue");
    }

    @Override
    @RabbitListener(queues = DLQ_IMAGES_QUEUE)
    public void processFailedMessages(Message failedMessage) {
        log.info("Received failed image: {}", failedMessage);
        Integer retriesCnt = (Integer) failedMessage.getMessageProperties()
                .getHeaders().get(HEADER_X_RETRIES_COUNT);
        if (retriesCnt == null) {
            retriesCnt = 1;
        }
        if (retriesCnt > MAX_RETRIES_COUNT) {
            log.info("Discarding message");
            rabbitTemplate.send(PARKING_LOT_IMAGES_EXCHANGE,
                    failedMessage.getMessageProperties().getReceivedRoutingKey(), failedMessage);
            return;
        }
        log.info("Retrying message for the {} time", retriesCnt);
        failedMessage.getMessageProperties()
                .getHeaders().put(HEADER_X_RETRIES_COUNT, ++retriesCnt);
        rabbitTemplate.convertAndSend(IMAGES_EXCHANGE,
                failedMessage.getMessageProperties().getReceivedRoutingKey(), failedMessage);
    }

    @Override
    @RabbitListener(queues = IMAGES_QUEUE, concurrency = "1-3")
    public void saveImage(String imageUrl) {
        try {
            processImage(imageUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processImage(String imageUrl) throws IOException {
        String format = getImageFormat(imageUrl);
        if (!configuration.getAvailableFormats().contains(format)) {
            return;
        }

        BufferedImage originalImage = downloadImage(imageUrl);
        long originalSize = calculateImageSize(originalImage, format);
        if (originalSize < 10_000) {
            throw new IOException("test");
        }
        byte[] compressedImage = compressImage(originalImage, format);
        long compressedSize = compressedImage.length;
        File outputFile = createOutputFile(imageUrl, format);

        saveCompressedImage(imageUrl, compressedImage, outputFile, format);
        saveToDb(imageUrl, originalSize, compressedSize, outputFile);
    }

    private File createOutputFile(String imageUrl, String format) {
        String outputFileName = getFileNameFromUrl(imageUrl) + "." + format;
        return new File(configuration.getFolderPath() + outputFileName);
    }

    private void saveCompressedImage(String imageUrl, byte[] compressedImage, File outputFile, String format) throws IOException {
        if (!outputFile.exists()) {
            writeToFile(imageUrl, compressedImage, outputFile, format);
        } else {
            log.info("Skip file by path: {}", outputFile.getAbsolutePath());
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

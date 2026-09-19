package com.travelbird.file.service;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * 실제 S3 객체 바이트가 진짜 WebP인지(파일 시그니처) + 가로/세로 크기를 검증한다
 * (기능명세서 3.5.1 "서버가 실제 객체의 ... 파일 시그니처 ... 이미지 가로 크기를 검증한다").
 * WebP 리더는 JDK 기본 ImageIO에 없어서 별도 라이브러리(twelvemonkeys)로 등록한다.
 */
@Component
public class WebpImageValidator {

    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    public boolean hasWebpSignature(byte[] bytes) {
        return bytes.length >= 12
                && matches(bytes, 0, RIFF)
                && matches(bytes, 8, WEBP);
    }

    /**
     * 호출 전에 {@link #hasWebpSignature(byte[])}로 먼저 확인해야 한다 — 시그니처가 아니면
     * 디코딩 과정에서 예외가 날 수 있다.
     */
    public Dimension readDimensions(byte[] bytes) {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("webp");
        if (!readers.hasNext()) {
            throw new IllegalStateException("WebP ImageIO reader가 classpath에 등록되어 있지 않습니다.");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(iis);
            return new Dimension(reader.getWidth(0), reader.getHeight(0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            reader.dispose();
        }
    }

    private boolean matches(byte[] bytes, int offset, byte[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}

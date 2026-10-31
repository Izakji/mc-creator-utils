package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * One-shot world capture at the end of level rendering, before any GUI is drawn.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class ScreenshotCapture {
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int MAX_JPEG_BYTES = 1024 * 1024;
    private static final long CAPTURE_TIMEOUT_SECONDS = 5;
    private static final Object LOCK = new Object();
    private static CompletableFuture<NativeImage> pending;

    private ScreenshotCapture() {}

    /**
     * Requests one capture on the next rendered level frame.
     *
     * @return raw image, or a failed future when another capture is active
     */
    public static CompletableFuture<NativeImage> request() {
        CompletableFuture<NativeImage> future = new CompletableFuture<>();
        synchronized (LOCK) {
            if (pending != null) {
                future.completeExceptionally(new IllegalStateException("A screenshot capture is already running."));
                return future;
            }
            pending = future;
        }
        future.orTimeout(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((image, failure) -> {
            synchronized (LOCK) {
                if (pending == future) {
                    pending = null;
                }
            }
        });
        return future;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        CompletableFuture<NativeImage> target;
        synchronized (LOCK) {
            target = pending;
            pending = null;
        }
        if (target == null) {
            return;
        }
        try {
            NativeImage image =
                    Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
            if (target.isDone()) {
                image.close();
            } else {
                target.complete(image);
            }
        } catch (Throwable failure) {
            target.completeExceptionally(failure);
        }
    }

    /**
     * Saves the full-resolution PNG and produces a bounded JPEG for the model.
     *
     * @param gameDirectory game directory
     * @param image captured image, closed by this method
     * @return saved screenshot details
     * @throws IOException when the image cannot be written or converted
     */
    public static Captured save(Path gameDirectory, NativeImage image) throws IOException {
        Path screenshots = gameDirectory.resolve("screenshots");
        Files.createDirectories(screenshots);
        Path file = uniqueFile(screenshots);
        try {
            image.writeToFile(file);
        } finally {
            image.close();
        }
        BufferedImage source = ImageIO.read(file.toFile());
        if (source == null) {
            throw new IOException("Could not read the saved screenshot.");
        }
        BufferedImage scaled = scale(source);
        byte[] jpeg = toJpeg(scaled);
        return new Captured(file, source.getWidth(), source.getHeight(), jpeg);
    }

    private static Path uniqueFile(Path directory) {
        String name = Util.getFilenameFormattedDateTime();
        Path file = directory.resolve(name + ".png");
        int suffix = 1;
        while (Files.exists(file)) {
            file = directory.resolve(name + "_" + suffix + ".png");
            suffix++;
        }
        return file;
    }

    private static BufferedImage scale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double factor = Math.min(1.0, (double) MAX_IMAGE_DIMENSION / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * factor));
        int targetHeight = Math.max(1, (int) Math.round(height * factor));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static byte[] toJpeg(BufferedImage image) throws IOException {
        float quality = 0.85f;
        byte[] encoded = encodeJpeg(image, quality);
        while (encoded.length > MAX_JPEG_BYTES && quality > 0.45f) {
            quality -= 0.1f;
            encoded = encodeJpeg(image, quality);
        }
        return encoded;
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        javax.imageio.ImageWriter writer =
                ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.plugins.jpeg.JPEGImageWriteParam params =
                new javax.imageio.plugins.jpeg.JPEGImageWriteParam(java.util.Locale.ROOT);
        params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    /**
     * One saved screenshot and its model-sized copy.
     *
     * @param file saved PNG
     * @param width source width
     * @param height source height
     * @param jpeg bounded JPEG bytes
     */
    public record Captured(Path file, int width, int height, byte[] jpeg) {}
}

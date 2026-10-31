package com.materialstudio.service;

import com.materialstudio.domain.Asset;
import com.materialstudio.domain.Template;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class ResultRenderer {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 1500;
    private static final Color LIME = new Color(182, 243, 74);
    private static final Color INK = new Color(16, 19, 19);
    private static final Color PAPER = new Color(245, 242, 232);

    private final StorageService storageService;

    public ResultRenderer(StorageService storageService) {
        this.storageService = storageService;
    }

    public void render(Path destination, Template template, List<Asset> assets) {
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("At least one asset is required");
        }
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            configure(graphics);
            graphics.setPaint(new GradientPaint(0, 0, new Color(29, 35, 32), WIDTH, HEIGHT, INK));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setColor(new Color(182, 243, 74, 30));
            graphics.fillOval(780, -260, 650, 650);
            graphics.setColor(new Color(255, 255, 255, 16));
            graphics.fillOval(-280, 940, 720, 720);

            drawHeader(graphics, template);
            drawImages(graphics, assets);
            drawFooter(graphics, template);
        } finally {
            graphics.dispose();
        }

        try {
            if (!ImageIO.write(canvas, "png", destination.toFile())) {
                throw new IOException("PNG writer is unavailable");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render generated image", exception);
        }
    }

    private void drawHeader(Graphics2D graphics, Template template) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.setColor(LIME);
        graphics.drawString("MATERIAL STUDIO  /  " + template.getCategory().toUpperCase(), 72, 76);

        graphics.setFont(new Font(Font.SERIF, Font.BOLD, 96));
        graphics.setColor(PAPER);
        graphics.drawString(template.getTitle(), 68, 208);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 27));
        graphics.setColor(new Color(225, 226, 216, 185));
        drawWrappedText(graphics, template.getDescription(), 72, 265, 680, 39);

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(new Color(255, 255, 255, 48));
        graphics.drawLine(72, 365, 1128, 365);
    }

    private void drawImages(Graphics2D graphics, List<Asset> assets) {
        if (assets.size() == 1) {
            drawImageCover(graphics, assets.get(0), 72, 425, 1056, 790, 34);
        } else if (assets.size() == 2) {
            drawImageCover(graphics, assets.get(0), 72, 425, 512, 790, 34);
            drawImageCover(graphics, assets.get(1), 616, 425, 512, 790, 34);
        } else {
            drawImageCover(graphics, assets.get(0), 72, 425, 635, 790, 34);
            drawImageCover(graphics, assets.get(1), 735, 425, 393, 385, 28);
            drawImageCover(graphics, assets.get(2), 735, 838, 393, 377, 28);
        }
    }

    private void drawFooter(Graphics2D graphics, Template template) {
        graphics.setColor(new Color(255, 255, 255, 35));
        graphics.fillRoundRect(72, 1260, 1056, 154, 34, 34);
        graphics.setColor(LIME);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.drawString("已使用 " + countLabel(template.getRequiredImageCount()) + " 制作", 108, 1320);
        graphics.setColor(new Color(236, 236, 226, 180));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 21));
        graphics.drawString("模拟异步流水线 · MATERIAL STUDIO", 108, 1362);

        graphics.setColor(LIME);
        graphics.fillOval(1020, 1300, 56, 56);
        graphics.setColor(INK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.drawString("M", 1037, 1339);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        graphics.setColor(new Color(255, 255, 255, 80));
        graphics.drawString("GENERATED · " + template.getId().toUpperCase(), 72, 1460);
    }

    private void drawImageCover(
            Graphics2D graphics,
            Asset asset,
            int x,
            int y,
            int width,
            int height,
            int radius
    ) {
        BufferedImage source;
        try {
            source = ImageIO.read(storageService.resolveAsset(asset.getStorageName()).toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read uploaded image", exception);
        }
        if (source == null) {
            throw new IllegalStateException("Uploaded image cannot be decoded");
        }

        java.awt.Shape oldClip = graphics.getClip();
        graphics.setClip(new RoundRectangle2D.Float(x, y, width, height, radius, radius));
        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
        int drawWidth = (int) Math.ceil(source.getWidth() * scale);
        int drawHeight = (int) Math.ceil(source.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        graphics.drawImage(source, drawX, drawY, drawWidth, drawHeight, null);
        graphics.setClip(oldClip);

        graphics.setColor(new Color(255, 255, 255, 55));
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new RoundRectangle2D.Float(x, y, width, height, radius, radius));
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawWrappedText(
            Graphics2D graphics,
            String text,
            int x,
            int y,
            int maxWidth,
            int lineHeight
    ) {
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (char character : text.toCharArray()) {
            String candidate = line + String.valueOf(character);
            if (graphics.getFontMetrics().stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                graphics.drawString(line.toString(), x, currentY);
                line.setLength(0);
                currentY += lineHeight;
            }
            line.append(character);
        }
        if (!line.isEmpty()) {
            graphics.drawString(line.toString(), x, currentY);
        }
    }

    private String countLabel(int count) {
        return switch (count) {
            case 1 -> "1 张素材";
            case 2 -> "2 张素材";
            default -> count + " 张素材";
        };
    }
}

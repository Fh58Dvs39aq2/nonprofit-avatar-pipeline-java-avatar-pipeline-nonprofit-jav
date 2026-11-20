package org.example.avatar;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AvatarServiceTest {
    public static void main(String[] args) throws Exception {
        Path input = Files.createTempFile("avatar", ".jpg");
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(900, 600,
                java.awt.image.BufferedImage.TYPE_INT_RGB), "jpeg", input.toFile());
        int expected = 600;
        if (AvatarService.squareSide(input) != expected) throw new AssertionError("square crop decision changed");
        Files.delete(input);
        System.out.println("avatar policy test passed: 900x600 input -> 600x600 crop");
    }
}

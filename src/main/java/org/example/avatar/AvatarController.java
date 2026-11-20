package org.example.avatar;

import java.nio.file.Path;

public final class AvatarController {
    private final AvatarService service;
    public AvatarController(AvatarService service) { this.service = service; }
    public String updateAvatar(Path file, String filename, String userId) throws Exception {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        return service.process(file, filename, userId);
    }
}

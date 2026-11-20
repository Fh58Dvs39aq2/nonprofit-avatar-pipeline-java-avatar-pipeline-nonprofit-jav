package org.example.avatar;

import java.nio.file.Path;

public final class AvatarApplication {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: <image> <filename> <user-id>");
        AvatarController controller = new AvatarController(new AvatarService(new InfraiClient(LayeredConfig.fromEnvironment())));
        String result = controller.updateAvatar(Path.of(args[0]), args[1], args[2]);
        System.out.println("avatar updated for " + args[2] + ": " + result);
    }
}

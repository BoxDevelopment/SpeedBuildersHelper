package box.com;

import net.minecraft.client.MinecraftClient;

public interface MinecraftInstance {
    MinecraftClient mc = MinecraftClient.getInstance();
}
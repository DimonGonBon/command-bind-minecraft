package ru.dmitry.commandkeys;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CommandKeysClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        CommandBindings.load();
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.commandkeys.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.commandkeys"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                client.setScreen(new CommandKeysScreen());
            }
            CommandBindings.tick(client);
        });
    }

    static void showMessage(Minecraft client, String text) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(text), true);
        }
    }
}

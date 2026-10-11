package ru.dmitry.commandkeys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CommandBindings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Binding>>() {}.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("commandkeys.json");
    private static final List<Binding> BINDINGS = new ArrayList<>();

    private CommandBindings() {}

    static List<Binding> all() {
        return BINDINGS;
    }

    static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            List<Binding> saved = GSON.fromJson(reader, LIST_TYPE);
            if (saved != null) BINDINGS.addAll(saved);
        } catch (Exception e) {
            System.err.println("[CommandKeys] Не удалось прочитать config/commandkeys.json: " + e.getMessage());
        }
    }

    static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(BINDINGS, LIST_TYPE, writer);
            }
        } catch (Exception e) {
            System.err.println("[CommandKeys] Не удалось сохранить бинды: " + e.getMessage());
        }
    }

    static void add(Binding binding) {
        BINDINGS.add(binding);
        save();
    }

    static void remove(int index) {
        if (index >= 0 && index < BINDINGS.size()) {
            BINDINGS.remove(index);
            save();
        }
    }

    static void tick(Minecraft client) {
        if (client.getWindow() == null || client.screen != null) return;
        long window = client.getWindow().getWindow();
        for (Binding binding : BINDINGS) {
            // Shift/Ctrl are ignored; mouse buttons and keyboard keys share the same edge trigger.
            boolean down = binding.mouse
                    ? GLFW.glfwGetMouseButton(window, binding.mouseButton) == GLFW.GLFW_PRESS
                    : GLFW.glfwGetKey(window, binding.keyCode) == GLFW.GLFW_PRESS;
            if (down && !binding.wasDown && client.player != null && client.getConnection() != null) {
                String command = binding.command.trim();
                if (command.startsWith("/")) command = command.substring(1);
                if (!command.isBlank()) client.getConnection().sendCommand(command);
            }
            binding.wasDown = down;
        }
    }

    static final class Binding {
        String command;
        int keyCode;
        boolean mouse;
        int mouseButton;
        transient boolean wasDown;

        Binding(String command, int keyCode) {
            this.command = command;
            this.keyCode = keyCode;
            this.mouse = false;
            this.mouseButton = -1;
        }

        static Binding forMouse(String command, int mouseButton) {
            Binding binding = new Binding(command, GLFW.GLFW_KEY_UNKNOWN);
            binding.mouse = true;
            binding.mouseButton = mouseButton;
            return binding;
        }

        static String mouseButtonName(int mouseButton) {
            return "Мышь " + (mouseButton + 1);
        }

        String keyName() {
            if (mouse) return mouseButtonName(mouseButton);
            String key = GLFW.glfwGetKeyName(keyCode, 0);
            if (key == null || key.isBlank()) key = "Key " + keyCode;
            return key.toUpperCase();
        }
    }
}

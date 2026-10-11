package ru.dmitry.commandkeys;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CommandKeysScreen extends Screen {
    private EditBox commandBox;
    private Button captureButton;
    private boolean capturing;
    private int capturedKey = GLFW.GLFW_KEY_UNKNOWN;
    private int capturedMouseButton = -1;
    private int firstShown;

    public CommandKeysScreen() {
        super(Component.literal("Бинды команд"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        String previousCommand = commandBox == null ? "" : commandBox.getValue();
        commandBox = new EditBox(font, center - 150, 72, 300, 20, Component.literal("Команда"));
        commandBox.setHint(Component.literal("Например: /spawn"));
        commandBox.setValue(previousCommand);
        addRenderableWidget(commandBox);

        captureButton = Button.builder(captureLabel(), button -> {
                    capturing = true;
                    capturedKey = GLFW.GLFW_KEY_UNKNOWN;
                    capturedMouseButton = -1;
                    captureButton.setMessage(captureLabel());
                })
                .bounds(center - 150, 100, 190, 20).build();
        addRenderableWidget(captureButton);

        Button addButton = Button.builder(Component.literal("Добавить бинд"), button -> addBinding())
                .bounds(center + 45, 100, 105, 20).build();
        addRenderableWidget(addButton);

        int maxVisible = Math.max(1, (height - 210) / 23);
        int shown = Math.min(CommandBindings.all().size() - firstShown, maxVisible);
        for (int i = 0; i < shown; i++) {
            final int index = firstShown + i;
            CommandBindings.Binding binding = CommandBindings.all().get(index);
            int y = 155 + i * 23;
            Button row = Button.builder(Component.literal(binding.keyName() + "  →  " + trim(binding.command, 29)), b -> {})
                    .bounds(center - 150, y, 260, 20).build();
            Button remove = Button.builder(Component.literal("×"), b -> {
                CommandBindings.remove(index);
                firstShown = Math.min(firstShown, Math.max(0, CommandBindings.all().size() - Math.max(1, (height - 210) / 23)));
                rebuildWidgets();
            }).bounds(center + 115, y, 35, 20).build();
            addRenderableWidget(row);
            addRenderableWidget(remove);
        }

        addRenderableWidget(Button.builder(Component.literal("Готово"), button -> onClose())
                .bounds(center - 45, height - 32, 90, 20).build());
    }

    private static String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private void addBinding() {
        if (capturedKey == GLFW.GLFW_KEY_UNKNOWN && capturedMouseButton < 0) {
            CommandKeysClient.showMessage(net.minecraft.client.Minecraft.getInstance(), "Сначала выбери клавишу или кнопку мыши");
            return;
        }
        String command = commandBox.getValue().trim();
        if (command.isBlank()) {
            CommandKeysClient.showMessage(net.minecraft.client.Minecraft.getInstance(), "Введи команду");
            return;
        }
        CommandBindings.add(capturedMouseButton >= 0
                ? CommandBindings.Binding.forMouse(command, capturedMouseButton)
                : new CommandBindings.Binding(command, capturedKey));
        firstShown = Math.max(0, CommandBindings.all().size() - Math.max(1, (height - 210) / 23));
        capturedKey = GLFW.GLFW_KEY_UNKNOWN;
        capturedMouseButton = -1;
        commandBox.setValue("");
        capturing = false;
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int modifiers = event.modifiers();
        if (capturing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false;
                updateCaptureLabel();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
                return true;
            }
            capturedKey = keyCode;
            capturedMouseButton = -1;
            capturing = false;
            updateCaptureLabel();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capturing) {
            capturedKey = GLFW.GLFW_KEY_UNKNOWN;
            capturedMouseButton = event.button();
            capturing = false;
            updateCaptureLabel();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void updateCaptureLabel() {
        captureButton.setMessage(captureLabel());
    }

    private Component captureLabel() {
        if (capturing) return Component.literal("Нажми клавишу или мышь…");
        if (capturedMouseButton >= 0) return Component.literal(CommandBindings.Binding.mouseButtonName(capturedMouseButton));
        if (capturedKey == GLFW.GLFW_KEY_UNKNOWN) return Component.literal("Выбрать клавишу/мышь");
        String key = GLFW.glfwGetKeyName(capturedKey, 0);
        if (key == null) key = "Key " + capturedKey;
        return Component.literal(key.toUpperCase());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Screen's background path may already have applied the frame blur. Avoid
        // invoking it again here: Minecraft 1.21.11 throws if a frame is blurred twice.
        graphics.fill(0, 0, width, height, 0xC0101010);
        graphics.drawCenteredString(font, title, width / 2, 38, 0xFFFFFF);
        graphics.drawCenteredString(font, "Клавиатура и мышь; Shift/Ctrl игнорируются (K — меню)", width / 2, 56, 0xAAAAAA);
        if (capturing) graphics.drawCenteredString(font, "Нажми клавишу или кнопку мыши", width / 2, 128, 0xFFFF55);
        else graphics.drawCenteredString(font, "Бинды сохраняются автоматически", width / 2, 128, 0xAAAAAA);
        int maxVisible = Math.max(1, (height - 210) / 23);
        if (CommandBindings.all().size() > maxVisible) {
            graphics.drawCenteredString(font, "Колёсико мыши прокручивает список", width / 2, height - 52, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = Math.max(1, (height - 210) / 23);
        int maxFirst = Math.max(0, CommandBindings.all().size() - maxVisible);
        int next = (int) Math.max(0, Math.min(maxFirst, firstShown - Math.signum(verticalAmount)));
        if (next != firstShown) {
            firstShown = next;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        CommandBindings.save();
        super.onClose();
    }
}

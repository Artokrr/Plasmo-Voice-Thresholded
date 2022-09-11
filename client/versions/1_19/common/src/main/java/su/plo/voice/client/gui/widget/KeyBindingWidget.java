package su.plo.voice.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import su.plo.voice.api.client.config.keybind.KeyBinding;
import su.plo.voice.client.config.keybind.KeyBindingConfigEntry;
import su.plo.voice.client.gui.GuiUtil;
import su.plo.voice.client.gui.tab.KeyBindingTabWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class KeyBindingWidget extends Button implements UpdatableWidget {

    private final KeyBindingTabWidget parent;
    private final KeyBindingConfigEntry entry;
    private final List<KeyBinding.Key> pressedKeys = new ArrayList<>();

    public KeyBindingWidget(KeyBindingTabWidget parent, int x, int y, int width, int height, KeyBindingConfigEntry entry) {
        super(x, y, width, height, Component.empty(), button -> {
        });
        this.parent = parent;
        this.entry = entry;

        updateValue();
    }

    public boolean isActive() {
        return parent.getFocusedBinding() != null && parent.getFocusedBinding().equals(this);
    }

    public void keysReleased() {
        entry.value().getKeys().clear();
        entry.value().getKeys().addAll(ImmutableList.copyOf(pressedKeys));
        pressedKeys.clear();
        parent.setFocusedKeyBinding(null);
    }

    @Override
    public void updateValue() {
        MutableComponent text = Component.literal("");
        if (entry.value().getKeys().size() == 0) {
            text.append(Component.translatable("gui.none"));
        } else {
            formatKeys(text, entry.value().getKeys());
        }

        if (isActive()) {
            if (pressedKeys.size() > 0) {
                text = Component.literal("");
                List<KeyBinding.Key> sorted = pressedKeys.stream()
                        .sorted(Comparator.comparingInt(key -> key.getType().ordinal()))
                        .toList();

                formatKeys(text, sorted);
            }

            setMessage((Component.literal("> ")).append(text.withStyle(ChatFormatting.YELLOW)).append(" <").withStyle(ChatFormatting.YELLOW));
        } else {
            setMessage(text);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isActive()
                && !(button == GLFW.GLFW_MOUSE_BUTTON_1 && pressedKeys.size() == 0)
                && pressedKeys.stream().anyMatch(key -> key.getType() == KeyBinding.Type.MOUSE && key.getCode() == button)
        ) {
            keysReleased();
            updateValue();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isActive()) {
            if (pressedKeys.size() < 3) {
                pressedKeys.add(KeyBinding.Type.MOUSE.getOrCreate(button));
            }
            updateValue();
            return true;
        } else if (clicked(mouseX, mouseY)) {
            parent.setFocusedKeyBinding(this);
            updateValue();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (pressedKeys.size() > 0) {
                    keysReleased();
                } else {
                    parent.setFocusedKeyBinding(null);
                    entry.value().getKeys().clear();
                    updateValue();
                }
                return true;
            }

            if (pressedKeys.size() < 3) {
                pressedKeys.add(KeyBinding.Type.KEYSYM.getOrCreate(keyCode));
            }
            updateValue();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isActive()
                && pressedKeys.stream().anyMatch(key -> key.getType() == KeyBinding.Type.KEYSYM && key.getCode() == keyCode)
        ) {
            keysReleased();
            updateValue();
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderToolTip(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        if (parent.getFocusedBinding() == null || !parent.getFocusedBinding().equals(this)) {
            int width = Minecraft.getInstance().font.width(getMessage());
            if (width > width - 16) {
                parent.setTooltip(ImmutableList.of(getMessage()));
            }
        }

        super.renderToolTip(poseStack, mouseX, mouseY);
    }

    @Override
    public void renderButton(@NotNull PoseStack poseStack, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int i = getYImage(isHoveredOrFocused());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        blit(poseStack, x, y, 0, 46 + i * 20, width / 2, height);
        blit(poseStack, x + width / 2, y, 200 - width / 2, 46 + i * 20, width / 2, height);
        renderBg(poseStack, minecraftClient, mouseX, mouseY);
        int j = active ? 16777215 : 10526880;

        if (parent.getFocusedBinding() != null && parent.getFocusedBinding().equals(this)) {
            drawCenteredString(poseStack, textRenderer, getMessage(), x + width / 2, y + (height - 8) / 2, j | Mth.ceil(alpha * 255.0F) << 24);
        } else {
            FormattedCharSequence orderedText = GuiUtil.getOrderedText(textRenderer, getMessage(), width - 16);
            textRenderer.drawShadow(
                    poseStack,
                    orderedText,
                    (float) ((x + width / 2) - textRenderer.width(orderedText) / 2),
                    y + (float) (height - 8) / 2,
                    j | Mth.ceil(alpha * 255.0F) << 24
            );
        }

        if (isHoveredOrFocused()) {
            renderToolTip(poseStack, mouseX, mouseY);
        }
    }

    private void formatKeys(MutableComponent text, Collection<KeyBinding.Key> keys) {
        for (KeyBinding.Key key : keys) {
            InputConstants.Key inputKey;

            if (key.getType() == KeyBinding.Type.KEYSYM) {
                inputKey = InputConstants.Type.KEYSYM.getOrCreate(key.getCode());
            } else if (key.getType() == KeyBinding.Type.MOUSE) {
                inputKey = InputConstants.Type.MOUSE.getOrCreate(key.getCode());
            } else if (key.getType() == KeyBinding.Type.SCANCODE) {
                inputKey = InputConstants.Type.SCANCODE.getOrCreate(key.getCode());
            } else {
                continue;
            }

            text.append(inputKey.getDisplayName());
            text.append(Component.literal(" + "));
        }

        text.getSiblings().remove(text.getSiblings().size() - 1);
    }
}
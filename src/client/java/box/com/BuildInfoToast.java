package box.com;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BuildInfoToast implements Toast {
    private static final Object TYPE = new Object();
    private static final long DISPLAY_TIME_MS = 5000L;
    private static final int DEFAULT_WIDTH = 160;
    private static final int TEXT_START_X = 30;
    private static final int RIGHT_PADDING = 6;

    private final Component title;
    private final Component message;
    private final ItemStack icon;
    private final int width;
    private long startTime;
    private Visibility visibility = Visibility.SHOW;

    private BuildInfoToast(Component title, Component message, ItemStack icon) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.width = calculateWidth(title, message);
    }

    public static void show(Component title, Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        ToastManager toastManager = client.gui.toastManager();
        toastManager.addToast(new BuildInfoToast(title, message, new ItemStack(Items.CRAFTING_TABLE)));
    }

    @Override
    public Object getToken() {
        return TYPE;
    }

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (startTime == 0L) {
            startTime = time;
        }

        if (time - startTime >= DISPLAY_TIME_MS) {
            visibility = Visibility.HIDE;
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, Font font, long startTime) {
        // Dark framed panel to mimic advancement-style toast look.
        context.fill(0, 0, width(), height(), 0xF0101010);
        context.fill(0, 0, width(), 1, 0xFFB38F3A);
        context.fill(0, height() - 1, width(), height(), 0xFF7A5F22);
        context.fill(0, 0, 1, height(), 0xFFB38F3A);
        context.fill(width() - 1, 0, width(), height(), 0xFF7A5F22);

        // Text color expects ARGB; include full alpha or text can render transparent.
        context.text(font, title, TEXT_START_X, 7, 0xFFFFFF88, true);
        context.text(font, message, TEXT_START_X, 18, 0xFFFFFFFF, true);
        context.item(icon, 8, 8);
    }

    private static int calculateWidth(Component title, Component message) {
        Minecraft client = Minecraft.getInstance();
        Font font = client != null ? client.font : null;
        if (font == null) {
            return DEFAULT_WIDTH;
        }

        int contentWidth = Math.max(font.width(title), font.width(message));
        return Math.max(DEFAULT_WIDTH, TEXT_START_X + contentWidth + RIGHT_PADDING);
    }
}

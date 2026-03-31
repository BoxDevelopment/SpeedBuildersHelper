package box.com;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class BuildInfoToast implements Toast {
    private static final Object TYPE = new Object();
    private static final long DISPLAY_TIME_MS = 5000L;
    private static final int DEFAULT_WIDTH = 160;
    private static final int TEXT_START_X = 30;
    private static final int RIGHT_PADDING = 6;

    private final Text title;
    private final Text message;
    private final ItemStack icon;
    private final int width;
    private long startTime;
    private Visibility visibility = Visibility.SHOW;

    private BuildInfoToast(Text title, Text message, ItemStack icon) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.width = calculateWidth(title, message);
    }

    public static void show(Text title, Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ToastManager toastManager = client.getToastManager();
        toastManager.add(new BuildInfoToast(title, message, new ItemStack(Items.CRAFTING_TABLE)));
    }

    @Override
    public Object getType() {
        return TYPE;
    }

    @Override
    public Visibility getVisibility() {
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
    public int getWidth() {
        return width;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        // Dark framed panel to mimic advancement-style toast look.
        context.fill(0, 0, getWidth(), getHeight(), 0xF0101010);
        context.fill(0, 0, getWidth(), 1, 0xFFB38F3A);
        context.fill(0, getHeight() - 1, getWidth(), getHeight(), 0xFF7A5F22);
        context.fill(0, 0, 1, getHeight(), 0xFFB38F3A);
        context.fill(getWidth() - 1, 0, getWidth(), getHeight(), 0xFF7A5F22);

        // 1.21 text color expects ARGB; include full alpha or text can render transparent.
        context.drawText(textRenderer, title, TEXT_START_X, 7, 0xFFFFFF88, true);
        context.drawText(textRenderer, message, TEXT_START_X, 18, 0xFFFFFFFF, true);
        context.drawItem(icon, 8, 8);
    }

    private static int calculateWidth(Text title, Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client != null ? client.textRenderer : null;
        if (textRenderer == null) {
            return DEFAULT_WIDTH;
        }

        int titleWidth = textRenderer.getWidth(title);
        int messageWidth = textRenderer.getWidth(message);
        int contentWidth = Math.max(titleWidth, messageWidth);
        return Math.max(DEFAULT_WIDTH, TEXT_START_X + contentWidth + RIGHT_PADDING);
    }
}

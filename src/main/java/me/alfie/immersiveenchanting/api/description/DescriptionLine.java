package me.alfie.immersiveenchanting.api.description;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.MousePos;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface DescriptionLine {

    /**
     * Render this line at the given position.
     * @param lineX The left edge of the description box content area
     * @param lineY The top of this line, already offset for its position in the layout

     */
    void render(GuiGraphicsX gx, int lineX, int lineY, MousePos mousePos);

    /**
     * Return the text that is being drawn. This is used to expand the description box.
     * If this line doesn't draw text, you can return Component.empty().
     * Do not return null.
     * @return
     */
    @NotNull
    default Component getText() {
        return Component.empty();
    }

    default int getLineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }
}

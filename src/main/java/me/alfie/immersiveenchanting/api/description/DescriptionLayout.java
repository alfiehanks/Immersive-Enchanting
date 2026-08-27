package me.alfie.immersiveenchanting.api.description;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.TooltipDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DescriptionLayout {
    private static final Logger log = LoggerFactory.getLogger(DescriptionLayout.class);
    protected final List<DescriptionLine> lines = new ArrayList<>();

    /**Add extra padding to the width of the description layout to expand the box manually, useful for descriptions using item rendering.*/
    public int widthPadding = 0;

    public DescriptionLayout(TooltipDescription tooltipDescription) {
    }

    /**
     * Insert a line at the given index, shifting existing lines down.
     * If {@code lineNumber} is beyond the current list size, empty no-op lines are appended
     * until the list is long enough, then the new line is inserted at {@code lineNumber}.
     */
    public void insertLine(int lineNumber, DescriptionLine line) {
        while(lines.size() < lineNumber) {
            lines.add(new DescriptionLine() {
                @Override
                public void render(GuiGraphicsX gx, int lineX, int lineY, MousePos mousePos) {

                }
            });
        }

        lines.add(lineNumber, line);
    }

    public void removeLine(int lineNumber) {
        lines.remove(lineNumber);
    }

    /**
     * Clear the current layout.
     */
    public void clear() {
        lines.clear();
    }

    public void render(GuiGraphicsX gx, int startX, int startY, MousePos mousePos) {
        int yOffset = 0;

        for (DescriptionLine line : lines) {
            line.render(gx, startX, startY + yOffset, mousePos);
            yOffset += line.getLineHeight();
        }
    }



    /**
     * Return the longest string contained in the layout.
     * @return
     */
    private String getLongestString() {
        Component longest = Component.empty();
        for(DescriptionLine line : lines) {
            Component lineText = line.getText();

            if(lineText.getString().length() > longest.getString().length()) {
                longest = lineText;
            }
        }
        return longest.getString();
    }

    /**
     * Get the height of all the lines put together + any spacing.
     * @return
     */
    public int getRenderedHeight() {
        final int padding = 12;

        int height = 0;
        for (DescriptionLine line : lines) {
            height += line.getLineHeight();
        }

        return height + padding;
    }

    public int getRenderedWidth() {
        final int padding = 8;
        return Minecraft.getInstance().font.width(getLongestString()) + padding + widthPadding;
    }
}

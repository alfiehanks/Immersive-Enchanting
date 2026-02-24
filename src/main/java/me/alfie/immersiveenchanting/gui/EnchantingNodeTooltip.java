package me.alfie.immersiveenchanting.gui;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2i;

public class EnchantingNodeTooltip {

    private static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(
            ImmersiveEnchanting.MODID,
            "textures/gui/sprites/widgets.png"
    );

    private static final ResourceLocation MOUSE_HINT_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "immersiveenchanting", "textures/gui/sprites/mouse_hint_off.png");
    private static final ResourceLocation MOUSE_HINT_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "immersiveenchanting", "textures/gui/sprites/mouse_hint_on.png");


    private static final ResourceLocation TITLE_BOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "immersiveenchanting", "title_box");

    private final int padding = 8;
    private final int iconSize = 26;
    private final int costIconSize = 16;
    private final Font font;
    private final EnchantingNode node;
    private final ItemStack costStack;
    private final EnchantingTableScreen screen;
    RenderDirection renderDirection;
    private Vector2i costStackPos = new Vector2i(0, 0);
    private int titleBoxWidth;
    private int titleBoxHeight;
    private int descriptionBoxHeight;
    private Vector2i titleBoxTopLeft = new Vector2i(0, 0);
    private Vector2i descriptionBoxTopLeft = new Vector2i(0, 0);

    public EnchantingNodeTooltip(Font font,
                                 EnchantingNode node,
                                 ItemStack costStack,
                                 EnchantingTableScreen screen) {
        this.font = font;
        this.node = node;
        this.costStack = costStack;
        this.screen = screen;
    }

    public ItemStack getCostStack() {
        return costStack;
    }

    public void renderEnchantmentTooltip(GuiGraphics graphics) {
        //Decide title text
        String titleText;
        if (node.isBranchUnlocked) {
            titleText = node.getEnchantmentHolder().get()
                    .getFullname(node.getEnchantmentLevel())
                    .copy()
                    .withStyle(ChatFormatting.WHITE)
                    .getString();
        } else {
            titleText = Component.translatable("gui.immersiveenchanting.locked_enchantment").withStyle(ChatFormatting.RED).getString();
        }

        //Setup positions/dimensions
        int textWidth = font.width(titleText);
        final int MIN_WIDTH = 64;
        titleBoxWidth = Math.max(padding / 2 + textWidth + EnchantingNode.width,
                MIN_WIDTH);

        titleBoxHeight = Math.max(font.lineHeight, EnchantingNode.height);
        descriptionBoxHeight = titleBoxHeight - iconSize / 2 + costIconSize + padding;
        setRenderDirection();

        drawTitleBox(titleText, graphics);
        drawDescriptionBox(graphics);
    }

    /**
     * If tooltip will render out of bounds, this function will set the render direction accordingly.
     * Automatically sets the correct positions for enchantmentNameBoxTopLeft and costBoxTopLeft.
     */
    private void setRenderDirection() {
        boolean flipX = node.getViewportPosition(screen).x + titleBoxWidth > screen.VIEWPORT_WIDTH;
        boolean flipY = node.getViewportPosition(screen).y + descriptionBoxHeight + titleBoxHeight / 2 - padding / 2 > screen.VIEWPORT_HEIGHT;
        if (flipX && flipY) renderDirection = RenderDirection.LEFT_UP;
        else if (flipX) renderDirection = RenderDirection.LEFT_DOWN;
        else if (flipY) renderDirection = RenderDirection.RIGHT_UP;
        else renderDirection = RenderDirection.RIGHT_DOWN;

        int baseX = node.getRenderedPosition(screen).x;
        int baseY = node.getRenderedPosition(screen).y;
        switch (renderDirection) {
            case RIGHT_DOWN -> {
                titleBoxTopLeft = new Vector2i(baseX, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y + titleBoxHeight / 2);
            }

            case LEFT_DOWN -> {
                titleBoxTopLeft = new Vector2i(baseX - titleBoxWidth + iconSize - 2, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y + titleBoxHeight / 2);
            }

            case RIGHT_UP -> {
                titleBoxTopLeft = new Vector2i(baseX, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y - titleBoxHeight - padding / 2);
            }

            case LEFT_UP -> {
                titleBoxTopLeft = new Vector2i(baseX - titleBoxWidth + iconSize - 2, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y - titleBoxHeight - padding / 2);
            }
        }
    }

    private void drawTitleBox(String titleText,
                              GuiGraphics graphics) {
        //The enchantment name box always stays at the same y position, only flips horizontally.
        int uvOffsetY = node.isObtained() ? 0 : 26;
        graphics.blitNineSliced(WIDGETS, titleBoxTopLeft.x + 1, titleBoxTopLeft.y, titleBoxWidth, titleBoxHeight, 10, 200, 26, 0, uvOffsetY);


        final int titleTextX = titleBoxTopLeft.x + padding / 2
                + (renderDirection.isFlippedX() ? 0 : iconSize - padding / 2); //Add offset if renderDirection is left to right (makes room for the node)

        //Draw contents
        graphics.drawString(font,
                titleText,
                titleTextX,
                titleBoxTopLeft.y + padding - 1,
                ChatFormatting.WHITE.getColor());
    }

    private void drawDescriptionBox(GuiGraphics graphics) {
        graphics.blitNineSliced(WIDGETS, descriptionBoxTopLeft.x, descriptionBoxTopLeft.y, titleBoxWidth, descriptionBoxHeight, 10, 200, 26, 0, 52);

        Vector2i descriptionBoxBottomRight = new Vector2i(
                descriptionBoxTopLeft.x + titleBoxWidth,
                descriptionBoxTopLeft.y + descriptionBoxHeight
        );

        //Draw mouse right click hint
        if (!node.isObtained() && node.isBranchUnlocked) {
            ResourceLocation texture = !screen.isLockHover() ? MOUSE_HINT_OFF_TEXTURE : MOUSE_HINT_ON_TEXTURE;
            graphics.blit(
                    texture,
                    descriptionBoxBottomRight.x - 10,
                    descriptionBoxBottomRight.y - 14,
                    0f, 0f, 8, 8,
                    8, 8
            );
        }


        String hintLabel = node.isObtained() ?
                Component.translatable("gui.immersiveenchanting.equipped").getString()
                : Component.translatable("gui.immersiveenchanting.cost").getString();

        final int costBoxLabelX = font.width(hintLabel);
        final int costBoxLabelY = descriptionBoxTopLeft.y + titleBoxHeight / 2;

        //Draw contents
        if (node.isBranchUnlocked) {
            graphics.drawString(font,
                    hintLabel,
                    descriptionBoxTopLeft.x + padding,
                    costBoxLabelY + padding / 2,
                    ChatFormatting.GREEN.getColor());

            if (!node.isObtained()) {
                if (EnchantmentCostRegistry.getClientRegistry().isXpCostMode()) {
                    int effectiveLevel = (node.getEnchantmentHolder().get().getMaxLevel() == 1) ? 3 : node.getEnchantmentLevel();
                    int xpLevels = effectiveLevel * 3;
                    String xpText = xpLevels + " XP levels";
                    int xpTextX = descriptionBoxTopLeft.x + costBoxLabelX + padding;
                    graphics.drawString(font,
                            xpText,
                            xpTextX,
                            costBoxLabelY + padding / 2,
                            0x80FF20); // XP bar green
                    // Also show lapis cost icon if lapis is required
                    ItemStack lapisCost = EnchantmentCostRegistry.getClientRegistry().getLapisCost();
                    if (!lapisCost.isEmpty()) {
                        costStackPos = new Vector2i(xpTextX + font.width(xpText) + 2, costBoxLabelY);
                        graphics.renderItem(lapisCost, costStackPos.x, costStackPos.y);
                        graphics.renderItemDecorations(font, lapisCost, costStackPos.x, costStackPos.y);
                    }
                } else if (costStack.is(Items.AIR) || costStack.isEmpty()) {
                    graphics.drawString(font,
                            Component.translatable("gui.immersiveenchanting.cost_free"),
                            descriptionBoxTopLeft.x + costBoxLabelX + padding,
                            costBoxLabelY + padding / 2,
                            ChatFormatting.GREEN.getColor()); // optional green color
                } else {
                    costStackPos = new Vector2i(descriptionBoxTopLeft.x + costBoxLabelX + padding,
                            costBoxLabelY);

                    graphics.renderItem(
                            costStack,
                            costStackPos.x,
                            costStackPos.y);
                    graphics.renderItemDecorations(font,
                            costStack,
                            costStackPos.x,
                            costStackPos.y);
                }
            }

        } else {
            Component hint = Component.translatable("gui.immersiveenchanting.locked_enchantment_hint")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.OBFUSCATED);
            graphics.drawString(font, hint, descriptionBoxTopLeft.x + padding / 2, costBoxLabelY + padding / 2, 0x55FF55);
        }


    }

    public Vector2i getCostStackPos() {
        return costStackPos;
    }

    private enum RenderDirection {
        RIGHT_DOWN(false, false), //default: left to right, title on top, cost on bottom
        LEFT_DOWN(true, false), //right to left, title on top, cost on bottom
        RIGHT_UP(false, true), //left to right, cost on top, title on bottom
        LEFT_UP(true, true); //right to left, cost on top, title on bottom

        private final boolean flippedX; //Is x flipped from default (not left to right)
        private final boolean flippedY; //Is y flipped from default (not top to bottom)

        RenderDirection(boolean flippedX, boolean flippedY) {
            this.flippedX = flippedX;
            this.flippedY = flippedY;
        }

        /**
         * Is the render direction horizontally flipped (right to left instead of default left to right)?
         *
         * @return
         */
        public boolean isFlippedX() {
            return flippedX;
        }

        /**
         * Is the render direction vertically flipped (cost on top, title on bottom instead of default title on top, cost on bottom)?
         *
         * @return
         */
        public boolean isFlippedY() {
            return flippedY;
        }
    }
}

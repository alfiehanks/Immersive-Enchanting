package me.alfie.immersiveenchanting.gui.core.tab.book;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.EnchantmentMetadataRegistry;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;

public class EnchantmentBox {

    public final BookTab bookTab;
    private final Holder<Enchantment> enchantmentHolder;

    public EnchantmentBox(BookTab bookTab, @Nullable Holder<Enchantment> enchantmentHolder) {
        this.bookTab = bookTab;
        this.enchantmentHolder = enchantmentHolder;
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        // Determine which sprite to use based on whether the enchantment is unlocked
        Sprite boxSprite = bookTab.screen.getMenu().isEnchantmentUnlocked(enchantmentHolder) ?
                Sprite.ENCHANTMENT_BOX_UNLOCKED : Sprite.ENCHANTMENT_BOX_LOCKED;

        // Render the sprite
        final int spriteWidth = 108;
        final int spriteHeight = 19;
        guiGraphics.blit(
                boxSprite.get(),
                x,
                y,
                0f, 0f, spriteWidth, spriteHeight,
                spriteWidth, spriteHeight
        );

        // Render the enchantment title
        if(enchantmentHolder != null){
            renderText(guiGraphics, x, y);
            renderIcon(guiGraphics, x, y);
        }
    }

    private void renderText(GuiGraphics guiGraphics, int x, int y) {
        final int padding = 4;
        guiGraphics.pose().pushPose();

        float scale = 1.0f;
        String enchantmentTitle = Component.translatable(enchantmentHolder.value().getDescriptionId()).getString();
        if(enchantmentTitle.length() > 15) {
            scale = 0.75f;
            guiGraphics.pose().scale(scale, scale, scale);
        }

        int scaledX = (int)((x + padding) / scale);
        int scaledY = (int)((y + padding) / scale);

        //Create text with styling
        boolean isUnlocked = bookTab.screen.getMenu().isEnchantmentUnlocked(enchantmentHolder);
        Component enchantmentTitleStyled = Component.translatable(enchantmentTitle);
        if(ServerConfig.isObfuscateLockedEnchantments() && !isUnlocked) {
            enchantmentTitleStyled = ImmersiveEnchanting.styleWithAltFont(enchantmentTitleStyled.copy().withStyle(ChatFormatting.GRAY));
        } else {
            enchantmentTitleStyled = enchantmentTitleStyled.copy().withStyle(ChatFormatting.WHITE);

        }


        guiGraphics.drawString(bookTab.screen.getMinecraft().font,
                enchantmentTitleStyled,
                scaledX,
                scaledY,
                0xFFFFFF);
        guiGraphics.pose().popPose();
    }

    private void renderIcon(GuiGraphics guiGraphics, int x, int y) {
        int xPos;
        if(bookTab.screen.getMenu().isEnchantmentUnlocked(enchantmentHolder)) {
            ResourceLocation icon = new ResourceLocation("immersiveenchanting", "textures/item/ancient_book.png");

            //Try to get an icon
            ResourceLocation enchantmentId = enchantmentHolder.unwrapKey().get().location();
            if (EnchantmentMetadataRegistry.getIcons().containsKey(enchantmentId)) {
                icon = EnchantmentMetadataRegistry.getIconTexture(enchantmentId);
            }

            xPos = 91;
            guiGraphics.blit(
                    icon,
                    x+xPos,
                    y+2,
                    0f, 0f, 16, 16,
                    16, 16
            );

        } else {
            final int spriteSize = 19;
            xPos = 89;
            guiGraphics.blit(
                    Sprite.LOCKED_ENCHANTMENT.get(),
                    x+xPos,
                    y,
                    0f, 0f, spriteSize, spriteSize,
                    spriteSize, spriteSize
            );
        }
    }
}

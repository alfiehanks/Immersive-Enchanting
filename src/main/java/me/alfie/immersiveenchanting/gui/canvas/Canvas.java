package me.alfie.immersiveenchanting.gui.canvas;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.ScreenEventListener;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.ScreenState;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.gui.tab.enchanting.EnchantingTab;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.BranchManager;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class Canvas implements ScreenEventListener {

    private final EnchantingTableScreen screen;
    public boolean DEBUG_DISABLE_CULLING = false;

    private int backgroundTileCount;
    private int width;
    private int height;

    private static final int TILE_SIZE = 16;

    public static final int FULL_BRIGHTNESS = 0xFFFFFFFF;
    public static final int TINTED_BRIGHTNESS = 0xFF353535;
    private int currentBrightness = TINTED_BRIGHTNESS;

    public Canvas(EnchantingTableScreen screen) {
        this.screen = screen;

        setSize(32);
    }

    public EnchantingTableScreen screen() {
        return screen;
    }

    public void render(GuiGraphicsX gx) {
        updateBrightness(screen().tooltipManager().hasActiveTooltip(),
                0.02f);
        if(screen.isState(ScreenState.BOOKS)) currentBrightness = FULL_BRIGHTNESS;

        float viewportLeft = screen().camera().VIEWPORT_X;
        float viewportTop = screen().camera().VIEWPORT_Y;
        float viewportRight = viewportLeft + screen().camera().VIEWPORT_WIDTH;
        float viewportBottom = viewportTop + screen().camera().VIEWPORT_HEIGHT;

        for (int x = 0; x < backgroundTileCount; x++) {
            for (int y = 0; y < backgroundTileCount; y++) {

                //Viewport culling for tiles
                float canvasX = x * TILE_SIZE;
                float canvasY = y * TILE_SIZE;
                Vector2f screenPos = canvasToScreen(canvasX, canvasY);

                float size = getScaledLength(TILE_SIZE);
                float left = screenPos.x();
                float top = screenPos.y();
                float right = left + size;
                float bottom = top + size;

                if(right < viewportLeft || left > viewportRight
                        || bottom < viewportTop || top > viewportBottom) continue;

                Sprite tile;
                if(screen().enchantingTab().isDisplay(EnchantingTab.Display.ENCHANTMENTS)) {
                    tile = Sprite.BACKGROUND_TILE;
                } else {
                    tile = Sprite.ALT_BACKGROUND_TILE;
                }

                CanvasRenderable.setColor(gx.graphics(), currentBrightness);
                gx.graphics().blit(
                        tile.id().mc(),
                        x * TILE_SIZE, y * TILE_SIZE,
                        0, 0,
                        Sprite.BACKGROUND_TILE.width(), Sprite.BACKGROUND_TILE.height(),
                        Sprite.BACKGROUND_TILE.width(), Sprite.BACKGROUND_TILE.height()
                );
                CanvasRenderable.resetColor(gx.graphics());

            }
        }
    }

    /**
     * Resizes the canvas so that nodes at the furthest possible branch depth are not clipped.
     *
     * @param highestEnchantmentLevel the maximum enchantment level across all registered enchantments,
     *                                used to determine how far branches can extend
     */
    public void setSizeToFitNodes(int highestEnchantmentLevel) {
        highestEnchantmentLevel = Math.max(5, highestEnchantmentLevel); //Prevent canvas too small

        int tileCount = ((BranchManager.getNodeStep() * 2) * highestEnchantmentLevel + TILE_SIZE - 1) / TILE_SIZE;
        int tileMargin = 2;
        setSize(tileCount + tileMargin);
    }

    public void setSize(int tileCount) {
        this.backgroundTileCount = tileCount;
        this.width = backgroundTileCount * TILE_SIZE;
        this.height = backgroundTileCount * TILE_SIZE;
    }

    public int getSize() {
        return backgroundTileCount * TILE_SIZE;
    }

    public Vector2i getCenter() {
        return new Vector2i(width / 2, height / 2);
    }

    /**
     * Converts a canvas-space position to a screen-space position, accounting for
     * the camera's current scroll offset and zoom level.
     */
    public Vector2f canvasToScreen(Vector2f pos) {
        return new Vector2f(
                (pos.x() - screen().camera().x()) * screen().camera().zoom() + screen().camera().VIEWPORT_X,
                (pos.y() - screen().camera().y()) * screen().camera().zoom() + screen().camera().VIEWPORT_Y
        );
    }

    public Vector2f canvasToScreen(float x, float y) {
        return canvasToScreen(new Vector2f(x, y));
    }

    /**
     * Converts a screen-space position back to canvas-space, the inverse of
     * {@link #canvasToScreen(Vector2f)}.
     */
    public Vector2f screenToCanvas(Vector2f pos) {
        return new Vector2f(
                (pos.x() - screen().camera().VIEWPORT_X) / screen().camera().zoom() + screen().camera().x(),
                (pos.y() - screen().camera().VIEWPORT_Y) / screen().camera().zoom() + screen().camera().y()
        );
    }

    public Vector2f screenToCanvas(float x, float y) {
        return screenToCanvas(new Vector2f(x, y));
    }

    private float getScaledLength(float length) {
        return length * screen().camera().zoom();
    }

    /**
     * Smoothly lerps the canvas tint color toward a target brightness.
     * When a tooltip is active ({@code hovered = true}) the canvas dims to {@link #TINTED_BRIGHTNESS};
     * otherwise it fades back to {@link #FULL_BRIGHTNESS}.
     *
     * @param hovered   whether a tooltip is currently being shown
     * @param deltaTime time delta used to scale the interpolation speed
     */
    public void updateBrightness(boolean hovered, float deltaTime) {
        int targetBrightness = hovered ? TINTED_BRIGHTNESS : FULL_BRIGHTNESS;
        if(screen().getMenu().getToolSlot().getItem().isEmpty()) targetBrightness = TINTED_BRIGHTNESS;

        // Split ARGB components
        int aCurr = (currentBrightness >> 24) & 0xFF;
        int rCurr = (currentBrightness >> 16) & 0xFF;
        int gCurr = (currentBrightness >> 8) & 0xFF;
        int bCurr = currentBrightness & 0xFF;

        int aTarget = (targetBrightness >> 24) & 0xFF;
        int rTarget = (targetBrightness >> 16) & 0xFF;
        int gTarget = (targetBrightness >> 8) & 0xFF;
        int bTarget = targetBrightness & 0xFF;

        // Interpolation speed (adjust for faster/slower fade)
        float speed = 5f; // higher = faster transition

        // Lerp each channel
        aCurr += (int)((aTarget - aCurr) * speed * deltaTime);
        rCurr += (int)((rTarget - rCurr) * speed * deltaTime);
        gCurr += (int)((gTarget - gCurr) * speed * deltaTime);
        bCurr += (int)((bTarget - bCurr) * speed * deltaTime);

        // Recombine into a single ARGB int
        currentBrightness = (aCurr << 24) | (rCurr << 16) | (gCurr << 8) | bCurr;
    }

    public int getCurrentBrightness() {
        return currentBrightness;
    }

    /**
     * Returns true if the mouse is over the specified canvas coordinates.
     * @param canvasX
     * @param canvasY
     * @param width
     * @param height
     * @return
     */
    public boolean isMouseOver(float canvasX, float canvasY,
                               float width, float height,
                               MousePos mousePos) {
        Vector2f screenPos = canvasToScreen(canvasX, canvasY);
        float scaledWidth = getScaledLength(width);
        float scaledHeight = getScaledLength(height);

        float left = screenPos.x();
        float top = screenPos.y();
        float right = left + scaledWidth;
        float bottom = top + scaledHeight;

        float viewportLeft = screen().camera().VIEWPORT_X;
        float viewportTop = screen().camera().VIEWPORT_Y;
        float viewportRight = viewportLeft + screen().camera().VIEWPORT_WIDTH;
        float viewportBottom = viewportTop + screen().camera().VIEWPORT_HEIGHT;

        float visibleLeft = Math.max(left, viewportLeft);
        float visibleTop = Math.max(top, viewportTop);
        float visibleRight = Math.min(right, viewportRight);
        float visibleBottom = Math.min(bottom, viewportBottom);

        if(visibleLeft >= visibleRight || visibleTop >= visibleBottom) return false;

        return mousePos.x() >= visibleLeft && mousePos.x() <= visibleRight
                && mousePos.y() >= visibleTop && mousePos.y() <= visibleBottom;
    }
}

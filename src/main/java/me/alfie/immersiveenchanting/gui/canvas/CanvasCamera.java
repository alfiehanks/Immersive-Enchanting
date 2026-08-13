package me.alfie.immersiveenchanting.gui.canvas;

import com.mojang.blaze3d.platform.InputConstants;
import me.alfie.alfinolib.gui.ScreenEventListener;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;

/**
 * Handles camera movement, zooming, and interaction for a scrollable canvas viewport.
 *
 * The camera represents the top-left corner of the visible region in world space,
 * along with a zoom factor that scales how much of the canvas is visible.
 *
 * Supports:
 * - Dragging (panning)
 * - Scroll zooming (centered on viewport)
 * - Clamping to keep the view within canvas bounds
 */
public class CanvasCamera implements ScreenEventListener {
    private float x;
    private float y;
    private float zoom = 1f;

    private boolean dragging;
    private boolean draggingEnabled;
    private final float ZOOM_STEP = 0.1f;
    private final float MIN_ZOOM = 0.7f;
    private final float MAX_ZOOM = 1.7f;

    private final EnchantingTableScreen screen;

    public final int VIEWPORT_X;
    public final int VIEWPORT_Y;
    public final int VIEWPORT_WIDTH = 248;
    public final int VIEWPORT_HEIGHT = 118;

    /**
     * Creates a new camera tied to a specific screen and viewport position.
     *
     * @param screen The parent screen containing the canvas
     * @param viewportX X position of the viewport on screen
     * @param viewportY Y position of the viewport on screen
     */
    public CanvasCamera(EnchantingTableScreen screen, int viewportX, int viewportY) {
        this.VIEWPORT_X = viewportX;
        this.VIEWPORT_Y = viewportY;
        this.screen = screen;
    }

    /**
     * Enables or disables dragging and zoom interaction.
     */
    public void setDraggingEnabled(boolean enabled) {
        this.draggingEnabled = enabled;
    }

    /**
     * @return true if dragging/zoom interaction is enabled
     */
    public boolean isDraggingEnabled() {
        return draggingEnabled;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** @return current camera X position (world space) */
    public float x() {
        return x;
    }

    /** @return current camera Y position (world space) */
    public float y() {
        return y;
    }

    /** @return current zoom position */
    public float zoom() {
        return zoom;
    }

    /**
     * Sets the camera position directly (top-left in world space).
     */
    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Centers the camera on the middle of the canvas.
     *
     * Takes zoom into account so that the visual center of the viewport
     * aligns with the canvas center.
     */
    public void centerCameraOnCanvas() {
        setPos(
                screen.canvas().getCenter().x - (float) VIEWPORT_WIDTH / (2 * zoom),
                screen.canvas().getCenter().y - (float) VIEWPORT_HEIGHT / (2 * zoom)
        );
        clampPosition();
    }

    /**
     * Sets the zoom position while keeping the viewport center fixed.
     *
     * Without this adjustment, zooming would anchor at the top-left corner.
     * This method ensures the point at the center of the viewport remains
     * visually stable during zoom.
     *
     * @param zoom New zoom position
     */
    public void setZoom(float zoom) {
        float centerX = VIEWPORT_WIDTH / 2f;
        float centerY = VIEWPORT_HEIGHT / 2f;
        float worldCenterX = this.x + centerX / this.zoom;
        float worldCenterY = this.y + centerY / this.zoom;

        // Apply zoom
        this.zoom = zoom;

        float newWorldCenterX = this.x + centerX / this.zoom;
        float newWorldCenterY = this.y + centerY / this.zoom;

        this.x += worldCenterX - newWorldCenterX;
        this.y += worldCenterY - newWorldCenterY;

        clampPosition();
    }

    /**
     * Moves the camera by the given delta in world space.
     */
    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public boolean onMouseClick(MousePos mousePos, int button) {
        if(button != InputConstants.MOUSE_BUTTON_LEFT) return ScreenEventListener.super.onMouseClick(mousePos, button);

        if(isMouseOverViewport(mousePos) && isDraggingEnabled()) {
            dragging = true;
            return true;
        }

        return ScreenEventListener.super.onMouseClick(mousePos, button);
    }

    @Override
    public boolean onMouseDrag(MousePos mousePos, int button, double dx, double dy) {
        if(dragging) {
            move((float) -dx, (float) -dy);
            clampPosition();

            return true;
        }

        return ScreenEventListener.super.onMouseDrag(mousePos, button, dx, dy);
    }

    /**
     * Clamps the camera position so the viewport never goes outside the canvas bounds.
     *
     * Ensures no empty space is visible beyond the edges of the canvas.
     */
    public void clampPosition() {
        float maxX = screen.canvas().getSize() - VIEWPORT_WIDTH / zoom;
        float maxY = screen.canvas().getSize() - VIEWPORT_HEIGHT / zoom;

        setPos(Math.max(0, Math.min(x, maxX)), Math.max(0, Math.min(y, maxY)));
    }

    @Override
    public boolean onMouseRelease(MousePos mousePos, int button) {
        dragging = false;
        return ScreenEventListener.super.onMouseRelease(mousePos, button);
    }

    @Override
    public boolean onMouseScrolled(MousePos mousePos, double scrollY) {
        if(isMouseOverViewport(mousePos) && isDraggingEnabled()) {
            float newZoom = zoom() + (float)(ZOOM_STEP * scrollY);
            newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
            setZoom(newZoom);
        }

        return ScreenEventListener.super.onMouseScrolled(mousePos, scrollY);
    }

    /**
     * Checks whether a given mouse position is inside the viewport bounds.
     */
    public boolean isMouseOverViewport(MousePos mousePos) {
        return mousePos.x() >= VIEWPORT_X &&
                mousePos.x() <= VIEWPORT_X + VIEWPORT_WIDTH &&
                mousePos.y() >= VIEWPORT_Y &&
                mousePos.y() <= VIEWPORT_Y + VIEWPORT_HEIGHT;
    }
}

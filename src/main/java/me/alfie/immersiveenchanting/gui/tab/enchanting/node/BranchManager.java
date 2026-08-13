package me.alfie.immersiveenchanting.gui.tab.enchanting.node;

import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Functions to calculate node step/angles and position.
 */
public class BranchManager {
    private static int nodeStep = 100;
    private static float nodeBranchScale = 1f;

    private final List<NodeBranch> cachedBranches = new ArrayList<>();
    private EnchantingTableScreen screen;

    public BranchManager(EnchantingTableScreen screen) {
        this.screen = screen;
    }

    public List<NodeBranch> branches() {
        return cachedBranches;
    }

    public List<Node> getAllNodes() {
        List<Node> result = new ArrayList<>();
        for(NodeBranch branch : branches()) {
            result.addAll(branch.nodes());
        }

        return result;
    }

    /**
     * You must buildBranches() before positionBranches()!
     * @param stack
     */
    public void buildBranches(ItemStack stack) {
        cachedBranches.clear();
        cachedBranches.addAll(BranchFactory.buildBranches(stack, CostRegistry.client(), screen.canvas()));
        calculateNodeAnglesAndStep();
    }

    /**
     * You must buildBranches() before positionBranches()!
     */
    public void positionBranches() {
        for(NodeBranch branch : cachedBranches) {
            branch.placeNodesAlongLine();
        }
    }

    /**
     * Calculates the node step (distance between nodes along a branch) and the per-branch
     * scale so that no two branches overlap visually.
     *
     * <p>Works by finding the smallest angular gap between any two adjacent branches, then
     * choosing the largest step size that keeps nodes at least one node-diameter apart.
     * If even the maximum step size would cause overlap, nodes are scaled down instead.
     */
    private void calculateNodeAnglesAndStep() {
        final int baseStep = 40;
        final int minStep = 40;
        final int maxStep = 120;
        final float minScale = 0.3f;
        final float maxScale = 1f;
        final int margin = 4;
        final float nodeSize = Math.max(Node.WIDTH + margin, Node.HEIGHT + margin) * Node.DEFAULT_SCALE;

        if(cachedBranches.size() <= 1) {
            nodeStep = baseStep;
            nodeBranchScale = Math.min(maxScale, Node.DEFAULT_SCALE);
            return;
        }

        cachedBranches.sort(Comparator.comparingDouble(NodeBranch::angle));

        //Smallest angular distance
        double smallestAngle = Double.MAX_VALUE;
        for (int i = 0; i < cachedBranches.size(); i++) {
            double a1 = cachedBranches.get(i).angle();
            double a2 = cachedBranches.get((i+1) % cachedBranches.size()).angle();
            double difference = Math.abs(a2 - a1);
            difference = Math.min(difference, 2 * Math.PI - difference);
            smallestAngle = Math.min(smallestAngle, difference);
        }

        //Calculate node step
        nodeStep = baseStep;
        double minDistance = nodeSize;
        double currentDistance = nodeStep * smallestAngle;
        float safety = 1.1f;

        if(currentDistance < minDistance * safety) nodeStep = (int) Math.ceil((minDistance * safety) / smallestAngle);
        nodeStep = Math.max(minStep, Math.min(nodeStep, maxStep));

        //Scale down if max nodeStep
        float scale = Node.DEFAULT_SCALE;
        currentDistance = nodeStep * smallestAngle;
        if(currentDistance < minDistance) {
            scale = (float) (currentDistance / minDistance);
            scale = Math.max(scale, minScale);
        }
        scale = Math.min(scale, maxScale);

        nodeBranchScale = scale;
    }

    public static int getNodeStep() {
        return nodeStep;
    }

    public static float getNodeBranchScale() {
        return nodeBranchScale;
    }
}

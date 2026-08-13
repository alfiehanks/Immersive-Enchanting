package me.alfie.immersiveenchanting.compat;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Optional access to Bundled Not Siloed's public capacity-inventory API. */
public final class BundledNotSiloedCompat {
    private static final String MOD_ID = "bundlednotsiloed";
    private static final String API_CLASS = "com.cappleapple.bundlednotsiloed.api.BundledNotSiloedApi";
    private static final String INVENTORY_CLASS = "com.cappleapple.stacksnotslots.api.ICapacityInventory";
    private static final String ENTRY_CLASS = "com.cappleapple.stacksnotslots.api.LogicalInventoryEntry";
    private static final String EXTRACTION_RESULT_CLASS = "com.cappleapple.stacksnotslots.api.ExtractionResult";

    private static volatile ApiMethods apiMethods;
    private static volatile boolean unavailable;
    private static boolean warned;

    private BundledNotSiloedCompat() { }

    public static Optional<InventoryAccess> inventory(Player player) {
        if(!ModList.get().isLoaded(MOD_ID) || unavailable) return Optional.empty();

        try {
            ApiMethods methods = getApiMethods();
            if(methods == null) return Optional.empty();
            Object inventory = methods.inventory().invoke(null, player);
            return Optional.of(new InventoryAccess(inventory, methods));
        } catch(ReflectiveOperationException | RuntimeException | LinkageError exception) {
            warnOnce("Could not access Bundled Not Siloed's capacity inventory; falling back to the vanilla inventory view.", exception);
            return Optional.empty();
        }
    }

    private static ApiMethods getApiMethods() throws ReflectiveOperationException {
        ApiMethods current = apiMethods;
        if(current != null) return current;

        synchronized(BundledNotSiloedCompat.class) {
            if(apiMethods != null) return apiMethods;
            if(unavailable) return null;

            try {
                Class<?> apiClass = Class.forName(API_CLASS);
                Class<?> inventoryClass = Class.forName(INVENTORY_CLASS);
                Class<?> entryClass = Class.forName(ENTRY_CLASS);
                Class<?> extractionResultClass = Class.forName(EXTRACTION_RESULT_CLASS);

                apiMethods = new ApiMethods(
                        apiClass.getMethod("inventory", Player.class),
                        inventoryClass.getMethod("entries"),
                        inventoryClass.getMethod("count", ItemStack.class),
                        inventoryClass.getMethod("extract", ItemStack.class, int.class, boolean.class),
                        entryClass.getMethod("representative"),
                        entryClass.getMethod("quantity"),
                        extractionResultClass.getMethod("extractedAmount")
                );
                return apiMethods;
            } catch(ReflectiveOperationException | LinkageError exception) {
                unavailable = true;
                throw exception;
            }
        }
    }

    private static synchronized void warnOnce(String message, Throwable exception) {
        if(warned) return;
        warned = true;
        ImmersiveEnchanting.LOGGER.warn(message, exception);
    }

    public record InventoryEntry(ItemStack representative, long quantity) {
        public InventoryEntry {
            representative = representative.copyWithCount(1);
        }

        @Override
        public ItemStack representative() {
            return representative.copy();
        }
    }

    public static final class InventoryAccess {
        private final Object inventory;
        private final ApiMethods methods;

        private InventoryAccess(Object inventory, ApiMethods methods) {
            this.inventory = inventory;
            this.methods = methods;
        }

        public List<InventoryEntry> entries() {
            try {
                List<?> rawEntries = (List<?>)methods.entries().invoke(inventory);
                List<InventoryEntry> entries = new ArrayList<>(rawEntries.size());
                for(Object rawEntry : rawEntries) {
                    ItemStack representative = (ItemStack)methods.representative().invoke(rawEntry);
                    long quantity = ((Number)methods.quantity().invoke(rawEntry)).longValue();
                    entries.add(new InventoryEntry(representative, quantity));
                }
                return List.copyOf(entries);
            } catch(ReflectiveOperationException | RuntimeException | LinkageError exception) {
                warnOnce("Could not read Bundled Not Siloed's capacity inventory.", exception);
                return List.of();
            }
        }

        public long count(ItemStack prototype) {
            try {
                return ((Number)methods.count().invoke(inventory, prototype)).longValue();
            } catch(ReflectiveOperationException | RuntimeException | LinkageError exception) {
                warnOnce("Could not count items in Bundled Not Siloed's capacity inventory.", exception);
                return 0;
            }
        }

        public int extract(ItemStack prototype, int amount) {
            try {
                Object result = methods.extract().invoke(inventory, prototype, amount, false);
                return ((Number)methods.extractedAmount().invoke(result)).intValue();
            } catch(ReflectiveOperationException | RuntimeException | LinkageError exception) {
                warnOnce("Could not extract enchanting payment from Bundled Not Siloed's capacity inventory.", exception);
                return 0;
            }
        }
    }

    private record ApiMethods(Method inventory, Method entries, Method count, Method extract,
                              Method representative, Method quantity, Method extractedAmount) { }
}

# Changelog

## 6.0.2 - 2026-08-12

### Changed

- Allowed enchanting costs and fuel to be paid from the player's inventory, including split stacks. Existing enchanting-table input slots remain supported.
- Added Bundled Not Siloed 1.2 compatibility so enchanting payments can use both its visible inventory and stowed capacity-inventory stacks.

### Added

- Added built-in cost data for these optional-mod enchantments:
  - Combat Roll: Acrobat, Longfooted, Multi Roll
  - Create: Capacity, Potato Recovery
  - When Dungeons Arise: Discharge, Ensnaring, Lolth's Curse, Purification, Voltaic Shot
  - Farmer's Delight: Backstabbing
  - Passable Foliage: Leaf Walker
  - Supplementaries: Stasis
- Added default node sound selections for all of the enchantments above.

### Compatibility

- Minecraft 1.21.1 with NeoForge 21.1.x.
- Optional-mod data is loaded only when the corresponding enchantments are available.
- Bundled Not Siloed 1.2 or newer is supported as an optional inventory provider.

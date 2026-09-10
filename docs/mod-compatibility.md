# IrisV mod compatibility (client API)

This is an extension foundation, not automatic support for every machine.
Vanilla integration stays built in. Custom recipes exposed through Minecraft's
recipe manager use the existing generic recipe preview. Custom recipe sources,
energy displays and machine-specific preview layouts are not supplied by this API.

## Registration

Implement `net.opal.irisv.api.compat.RecipeIntegration` and call
`IrisVCompatibility.register(adapter)` during client setup using `enqueueWork`.
Never reference this client-only API from a dedicated server entry point.
Register optional integrations only when their target mod is loaded; isolate
references to optional mod classes in that integration's client class.

Each adapter has a stable namespaced ID, a translated title and an explicit
`supportsScreen(AbstractContainerScreen<?>)` predicate. Prefer exact screen/menu
types; do not accept every container or override vanilla screens indiscriminately.
Duplicate IDs are rejected. First matching registration wins, so predicates
should not overlap. No classpath scanning or optional mod dependencies are added.

The HUD uses the screen's GUI coordinates and dimensions. Test both small and
large windows: extremely wide machine screens may leave too little side space.

## Progressive support

1. Implement ID, title and screen matching for catalog/favorites/search only.
2. Implement `supportsRecipe` for recipe types accepted by the actual machine.
3. Implement read-only `canTransfer`: quantities, compatible slots, capacity,
   machine restrictions and availability of a server-validated transfer protocol.
4. Implement `transfer(screen, recipe, fillAll)` to issue that protocol's request.
   Return true only when a request was issued. IrisV then returns to the machine.

Defaults deny recipe transfers. IrisV checks that the original menu is still
the player's active menu, is valid, and has an empty cursor before delegation.
The adapter must revalidate before sending and the server MUST independently
validate recipe identity, menu identity, permissions, ingredients and capacity.
Do not trust client quantities. Do not create stacks, directly edit client
inventory or reuse guessed vanilla slot indices for a custom machine.
`fillAll` means the maximum legal quantity; it does not grant infinite items.
Brewing bookmarks are vanilla-specific and are not delegated by this first API.

## Settings and failure handling

Registered adapters appear under Mod Details / Mod Compatibility. Their switches
use `compat:<namespace>:<path>` in `disabledRecipeHudCategories`; absent entries
are enabled, unknown entries remain saved if a mod is removed temporarily.
The global recipe overlay switch remains authoritative.

Runtime exceptions from screen matching and recipe/transfer callbacks are logged
once and quarantine the adapter for the session. Unknown custom screens never
fall back to vanilla transfer slot assumptions. Registration metadata must be
non-null, stable and side-effect-free. An exception after a transfer request
cannot roll it back: prepare and validate the complete request before sending.

## Integration checks

- Open/close and Escape back to the same server menu; nested recipe browsing.
- Enable/disable adapter and restart; global overlay disabled.
- Missing ingredients, full inputs/output, occupied cursor and stale menu.
- One recipe and Shift maximum, respecting tags and item components.
- Multiplayer with and without the adapter's server protocol.
- Faulty callback; unrelated vanilla screens still work.

No external mod adapter has been bundled or verified yet.

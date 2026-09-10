# Wiki, diagnostics and settings exchange

Open **Diagnostics & settings** from the Wiki or the General section of IrisV Options.

## Wiki search

The search field searches translated text and section names across all five Wiki tabs.
All query words must match, regardless of case or accents. Selecting a tab clears the
query. Search text exists only in the screen instance and is never written to config.

## Diagnostics

The page lists explicitly registered recipe integrations. Available means registered
and enabled, not that every machine or recipe is supported. Disabled reflects the
global catalogue switch or the integration category switch. An integration whose
callback threw a runtime exception is suspended for the session; details are in the
game log. The page does not execute recipe transfers or probe inventories.

## Settings exchange

- Export writes `config/irisv/irisv-settings.json` (confirmation before replacement).
- Import reads that same file after confirmation. Open folder locates it.
- The document identifies format `irisv-settings`, version `1`, and a settings object.
- Invalid types, unknown settings, invalid enums and unsupported versions are rejected.
- Partial imports retain settings not specified in the document.
- Favorites, exact recipe bookmarks, favorite order/filter and catalogue navigation
  state are excluded from export and ignored if supplied during import.
- Minecraft key bindings are not part of IrisV's settings file.
- Before import, the full current config is backed up to `options.before-import.json`.
  This backup includes personal favorites; unlike the export, it is not intended for sharing.
- Settings are written through a temporary file and atomic replacement where supported.
  The live config changes only after a successful write. The backup is replaced on
  the next successful import attempt.

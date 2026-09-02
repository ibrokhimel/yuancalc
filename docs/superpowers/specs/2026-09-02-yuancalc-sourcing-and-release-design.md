# YuanCalc — Sourcing mode, cost currency, cargo profiles, release signing

**Date:** 2026-09-02
**Status:** Approved design, not yet planned or implemented
**Follows:** `2026-09-02-yuancalc-design.md`, which this extends rather than replaces.

## Purpose

The shipped app answers "I paid ¥50 — what do I charge?" These four changes
answer the questions that come before and after it:

1. **Sourcing mode** — standing in the market, the question runs backwards:
   *people here pay 299 000 so'm for this, so what is the most I can pay for it?*
2. **Cost currency** — not everything is bought in yuan.
3. **Cargo profiles** — the `$9/kg` is one agent, not a law of nature.
4. **Release signing** — the app is debug-signed, so it cannot be given to
   anyone else or reinstalled cleanly from another machine.

## Scope

**In:** a reverse "Source it" mode on the Calculator, a ¥/$ toggle on the cost
field, named cargo profiles selectable per item, and a signed release build with
a keystore that lives outside the repo.

**Out, deliberately:** saved item catalog, shipment freight reconciliation,
volumetric weight, Russian localization, Play Store distribution, R8 /
minification, so'm-denominated cargo rates, ¥ on the other-costs field. Each is a
separate task, and several are already on the wishlist.

All figures below use the same reference fixture as the original spec:
`cnyToUsd = 0.1488`, `usdToUzs = 11850`, cargo `$9/kg`.

---

## 1. Sourcing mode

### Where it lives

A segmented control at the top of the Calculator, not a fourth tab:

```
[  Price it  |  Source it  ]
```

`Price it` is today's screen, unchanged. `Source it` is the same screen with the
cost input replaced by a target price, and the suggested prices replaced by
maximum costs. Weight, other costs and the cargo profile are **shared** between
the modes, so switching does not retype them. The selected mode persists.

This is not the bidirectional markup/price linkage the original spec rejected.
That rejection was about `MY PRICE` and the tier multiples fighting each other
inside one calculation. This is a different direction of travel — price to cost —
with its own inputs and its own outputs, and the two modes never write to each
other's fields.

### The math

```
targetUsd     = target price, normalized to USD
landedBudget  = targetUsd / multiple
cargoUsd      = (weightGrams / 1000) × cargoRateUsdPerKg
otherUsd      = other costs, normalized to USD
maxProductUsd = landedBudget − cargoUsd − otherUsd
maxCostCny    = maxProductUsd / cnyToUsd
```

`maxProductUsd` is null when `multiple <= 0` or when the result is `<= 0`.

### Rounding: down, never half-up

This is the one rule in this document that is easy to get wrong. Every other
figure in the app rounds half-up for display. **A maximum must round down**, or
the app tells you to pay a price that misses the target you asked for.

At 299 000 so'm, 600 g, 2.3×, the exact answer is `¥37.435914`:

| Displayed | Resulting markup | Verdict |
|---|---|---|
| ¥37.43 (floor) | 2.30018× | clears 2.3× |
| ¥37.44 (half-up) | 2.29987× | **misses** 2.3× |

The same applies to the USD figure: `$5.570464` displays as `$5.57`, not `$5.58`.

### Screen

```
TARGET PRICE  [ 299 000 ] so'm        ($ | so'm toggle)
WEIGHT        [ 600 ] g               (g | kg toggle)
CARGO         [ Truck · $9/kg ]
+ other costs per item

MAX COST
  Soft         [1.8×]   ¥57.91   $8.61
  Profitable   [2.3×]   ¥37.43   $5.57

  at 500 g → ¥43.48     600 g → ¥37.43     700 g → ¥31.38
  Live · updated 2 min ago
```

Both tiers are shown, for the same reason the forward screen shows both: the
useful thing is the range between "worth doing" and "worth doing well". The tier
multiples are the same stored values and are edited inline here too, exactly as
on the forward screen.

The weight strip is the reverse mode's most valuable line — it shows that a
100 g error costs about ¥6 of buying power, which is the difference between a
good buy and a bad one, and is invisible while haggling.

**Target price rounding does not apply.** `priceRoundingStep` rounds *suggested*
prices; the target price here is the user's own number and is used exactly as
entered. This also makes the round-trip exact, which the tests depend on.

### When the price cannot work

If `maxProductUsd <= 0`, show `—` and say why rather than printing a negative
price. At 60 000 so'm, 600 g, 2.3×:

> Cargo alone is $5.40. At 2.3× this price supports only $2.20 of landed cost.

### Consistency with forward mode

Forward mode says 50¥ at 600 g priced at the 2.3× tier is 350 000 so'm. Reverse
mode on 350 000 so'm at 2.3× returns ¥50.01 — slightly *above* ¥50, because the
suggested price was rounded up from 349 954 so'm and that rounding is headroom.
The two modes agree, and the direction of the disagreement is the safe one.

---

## 2. Cost in ¥ or $

Some goods are bought in dollars — from Dubai, Turkey, or a supplier who quotes
in USD. The cost field gets the same currency toggle the other money fields have.

### Model change

`MoneyCurrency` becomes `{ CNY, USD, UZS }`. Kotlin's exhaustive `when` makes the
compiler list every site that needs the new branch, so this is a safe widening.
Persisted values are stored by name, so `USD` and `UZS` continue to read back
unchanged and no settings migration is needed.

`myPriceCurrency` and `otherCostsCurrency` keep their existing ¥-free toggles in
the UI; only the cost field offers CNY. Widening either of those later becomes a
UI change with no model work.

### PricingInput moves to USD

`PricingInput.costCny` becomes `costUsd`, and `landedCost` becomes
`productUsd = input.costUsd`. The `× cnyToUsd` conversion moves to the boundary
where the field is read.

This is the original spec's own rule — *all internal computation is in USD as the
base unit* — applied to the one place that had not followed it. Keeping `costCny`
and converting a dollar entry into yuan just to convert it back would round-trip
through a rate for no reason and lose precision. `PricingTest` fixtures update
with it; the expected landed figures do not change.

### Helper line

The line under the cost field shows the other two currencies, whichever is
selected. At the fixture rate ¥50 and $7.44 are the same money, so:

| Entered | Helper line |
|---|---|
| `50` ¥ | `$7.44 ≈ 88 164 so'm` |
| `7.44` $ | `¥50.00 ≈ 88 164 so'm` |

---

## 3. Cargo profiles

One global `$9/kg` becomes a named list, because air, truck and rail differ by
more than the margin on an item, and most resellers use more than one agent.

### Model

```kotlin
data class CargoProfile(
    val id: String,        // stable across renames
    val name: String,
    val ratePerKgUsd: Double,
)
```

Stored as one JSON string under a `cargoProfiles` key, plus
`selectedCargoProfileId`. DataStore Preferences has no list type, and
kotlinx-serialization is already a dependency, so this needs no new library and
no database.

### Migration — must not lose the user's rate

The user has a real value in `cargoRateUsdPerKg` on their phone. On the first
read where `cargoProfiles` is absent, synthesize a single profile from it:

```
CargoProfile(id = <generated>, name = "Cargo", ratePerKgUsd = <existing value>)
```

selected by default. The old key is left in place, unread, for one release, so a
downgrade does not lose it either. A migration that silently reset the rate to
9.0 would corrupt every figure in the app for a user who had changed it.

### Invariants

- At least one profile always exists. Deleting the last one is refused.
- Deleting the selected profile selects the first remaining one.
- Renaming never changes `id`, so the selection survives a rename.
- Rates are `$/kg`. Agents who quote in so'm/kg are out of scope for this round.

### UI

Selecting is on the Calculator — a chip next to Weight reading `Truck · $9/kg`,
which opens a picker. Switching agents is a per-item decision, so burying it in
Settings would mean leaving the screen mid-calculation.

Creating, renaming, editing and deleting profiles is in Settings, where the
single cargo-rate field is today.

---

## 4. Release signing

### Goal

A signed release APK that can be handed to another person and updated in place.
**Play Store distribution stays out of scope** — it needs a store listing, privacy
policy and content rating, and none of that is required to share an APK.

### Keystore handling

The keystore and its passwords never enter the repo. `app/build.gradle.kts` reads
an optional `keystore.properties`:

```
storeFile=../yuancalc-release.jks
storePassword=…
keyAlias=yuancalc
keyPassword=…
```

The `signingConfigs.release` block is registered **only if that file exists**, and
`buildTypes.release.signingConfig` is assigned only then. On a machine without the
keystore the project still configures and `assembleDebug` still works — a build
that breaks for everyone without the signing key is worse than an unsigned release
build.

Add to `.gitignore`:

```
keystore.properties
*.jks
*.keystore
```

### Losing the keystore is unrecoverable

Android identifies an app by its signing key. If the keystore or its password is
lost, no future build can update an installed copy — the only path is a new
package name and a manual reinstall by every user. It must be backed up somewhere
that is not this machine, and that is a task for the user, not the build.

### Debug and release side by side

Debug and release are signed by different keys, so Android refuses to install one
over the other. Installing the first signed release will require uninstalling the
debug build, **which deletes its DataStore settings** — cargo rate, tiers,
rounding, pinned rates, language.

Recommended: give the debug build `applicationIdSuffix = ".debug"` so the two can
coexist from now on. The cost is that the debug app becomes `uz.yuancalc.debug`, a
separate app with its own fresh settings — the currently installed debug build
keeps working but is orphaned and should be uninstalled by hand. The handful of
settings takes under a minute to re-enter, once, and never again.

### Version and minification

`versionCode` 1 → 2, `versionName` "1.0" → "1.1" for this feature set.

`isMinifyEnabled` stays **false**. R8 needs keep rules for kotlinx-serialization,
and turning it on in the same release that introduces signing would confuse two
independent failure modes. It goes on the wishlist.

### Verification

```
./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Then install on the phone and confirm the app opens, rates fetch, and the launcher
icon is correct — a release build resolves resources differently enough from debug
that the icon is worth re-checking.

---

## Testing

Same discipline as the original spec: exact expectations at the reference fixture,
and JVM unit tests over pure logic.

**Sourcing math**, at 600 g and no other costs:

| Case | Expectation |
|---|---|
| 299 000 so'm, 2.3× | exact $5.570464 → ¥37.43, $5.57 |
| 299 000 so'm, 1.8× | exact $8.617815 → ¥57.91, $8.61 |
| 299 000 so'm, 2.3×, 500 g | ¥43.48 |
| 299 000 so'm, 2.3×, 700 g | ¥31.38 |
| 350 000 so'm, 2.3× | ¥50.01 — above the ¥50 the forward tier came from |
| 60 000 so'm, 2.3× | null; cargo exceeds the budget |
| any price, multiple = 0 | null |
| weight = 0 | cargo = 0, whole budget available for product |

**The floor-rounding test is the important one**, because half-up passes a naive
assertion and still gives wrong advice: assert ¥37.43, and separately assert that
feeding ¥37.44 back through forward mode yields a markup **below** 2.3×.

**Round-trip property:** for a range of prices, weights and multiples,
`markupForPrice(landedCost(reverse(price, m)), price) >= m`, and the excess is
under one display unit. This is the invariant that says the two modes describe the
same arithmetic.

**Cost currency:** `50` in ¥ and `7.44` in $ produce identical landed cost at the
fixture · helper line shows the other two currencies in both directions · a $ cost
survives a mode switch and an app restart.

**Cargo profiles:** migration from a non-default `cargoRateUsdPerKg` preserves the
value · deleting the selected profile reselects · deleting the last is refused ·
rename preserves selection · switching profiles recalculates landed cost.

## Verification on device

The original spec's rule stands: tests are not the finish line. Install on the
Xiaomi 2203129G and check, in both languages — sourcing mode against a real item
whose price is known, the ¥/$ cost toggle, switching cargo profiles mid-item, and
the signed release build installing and running.

The offline path was verified on device on 2026-09-02 and works; it no longer
needs re-testing unless the rates layer changes.

## Open items

Three decisions worth confirming before implementation starts, none blocking the
design:

1. **`applicationIdSuffix = ".debug"`** — recommended above, but it orphans the
   debug app currently installed on the phone. The alternative is keeping one
   package name and uninstalling/reinstalling at every switch between debug and
   release.
2. **Both tiers in sourcing mode, or only the profitable one.** Specced as both.
3. **Cargo rates in so'm/kg.** Some agents quote that way. Out of scope here; it
   would mean giving `CargoProfile` a currency rather than assuming USD.

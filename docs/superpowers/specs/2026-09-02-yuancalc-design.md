# YuanCalc — Design

**Date:** 2026-09-02
**Status:** Approved design, ready for implementation planning

## Purpose

An Android app for a reseller importing clothing from China and selling in
Uzbekistan. It answers four questions on one screen:

1. What does this item cost me, landed, once cargo is included?
2. What should I charge for it?
3. If I charge X, what is my markup and profit?
4. What is this yuan amount in so'm?

The math already exists informally — the app removes the manual currency
lookups and arithmetic, and makes the weight assumption visible instead of
hidden.

## Scope

**In:** single-item pricing calculator, currency converter, live exchange rates
with manual override, English and Uzbek interface, sideloadable debug APK.

**Out:** saved product catalog, batch/shipment freight allocation, customs duty
tables, Russian localization, Play Store release signing. Each is a separate
task if wanted later.

## Platform

| Setting | Value |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Package | `uz.yuancalc` |
| App name | YuanCalc |
| minSdk | 24 |
| compileSdk / targetSdk | 35 |
| JDK | 17 |
| Build | Gradle KTS + version catalog |

minSdk 24 covers older low-cost handsets, which matters for this market.

Verified present on the build machine: JDK 17.0.20 (Temurin), Android SDK
platform 35, build-tools 34.0.0 and 35.0.0, emulator AVD `wl_test`
(android-34 image), Android Studio.

## The math

All internal computation is in USD as the base unit.

```
productUsd = costCny × cnyToUsd
cargoUsd   = (weightGrams / 1000) × cargoRateUsdPerKg
otherUsd   = otherCosts, normalized to USD
landedUsd  = productUsd + cargoUsd + otherUsd

priceForMarkup(markup) = landedUsd × markup
markupForPrice(price)  = price / landedUsd        (null when landedUsd <= 0)
profitUsd              = priceUsd − landedUsd
```

Rates are stored as `cnyToUsd` and `usdToUzs`. The APIs return `usdToCny`, so
`cnyToUsd = 1 / usdToCny` at the boundary; nothing downstream deals in
`usdToCny`.

### Rounding rules

These are display rules, applied at the edge. Internal math is never rounded.

- **USD:** 2 decimals, half-up. `$12.84`
- **Markup:** 2 decimals, `×` suffix. `1.97×`
- **UZS, landed cost and profit readouts:** nearest 1 so'm.
- **UZS, suggested prices:** rounded to a configurable step, default **up to
  the nearest 1,000 so'm**. Real prices are round numbers, and rounding down
  quietly eats margin — but both are user choices, not fixed rules.

**Price rounding is configurable in Settings:**

| Setting | Options | Default |
|---|---|---|
| `priceRoundingStep` | off · 500 · 1 000 · 5 000 · 10 000 so'm | 1 000 |
| `priceRoundingMode` | up · nearest | up |

`off` shows the exact figure to the nearest 1 so'm. The rounding applies only to
the two suggested prices; `MY PRICE` is whatever the user typed, and landed cost
and profit readouts are always exact.

The profit and markup shown beside each suggested price are computed **from the
rounded price**, not the exact one, so every figure on screen is internally
consistent whatever the rounding setting.

### Number formatting

Fixed, not derived from the active locale, so the display is identical in both
languages: space-grouped thousands (`299 000`), dot decimal separator.

### Input parsing

- Accepts `.` and `,` as decimal separator.
- Strips ordinary and non-breaking spaces before parsing.
- Empty cost / weight / other-costs are treated as 0.
- Empty `MY PRICE` hides its result block rather than showing zero.
- Negative values are rejected at input.
- `landedUsd <= 0` yields a null markup, displayed as `—`. No division by zero.

## Screens

Three tabs: **Calculator**, **Convert**, **Settings**.

### Calculator

Every figure below is exact at the reference fixture
(`cnyToUsd = 0.1488`, `usdToUzs = 11850`, cargo `$9/kg`) and is reproduced by
the unit tests.

```
COST      [ 50 ]  ¥          50¥ = $7.44 ≈ 88 164 so'm
WEIGHT    [ 600 ] g          (g | kg toggle)
+ other costs per item       (collapsed; $ | so'm toggle)

LANDED COST
  $12.84  ≈ 152 154 so'm
  50¥ = $7.44  +  cargo 0.6 kg × $9 = $5.40

SUGGESTED PRICES
  Soft         [1.8×]   $23.12   274 000 so'm    profit  $10.28 / 121 846
  Profitable   [2.3×]   $29.54   350 000 so'm    profit  $16.70 / 197 846

MY PRICE  [ 299 000 ] so'm  ($ | so'm toggle)
  →  1.97×  ● amber    profit $12.39 ≈ 146 846 so'm

  at 500 g → 2.11×     600 g → 1.97×     700 g → 1.84×
  Live · updated 2 min ago
```

Note the band colour: at soft 1.8× / profitable 2.3×, a markup of 1.97× is
**amber**, not green — it clears the floor but has not reached the target.

**Tier multiples are edited inline**, on this screen, not in Settings. Tapping
`2.3×` and typing `2.0` recalculates immediately and persists as the new
default. This single mechanic covers presets, arbitrary custom markups, and
customization together — which is why there is no separate markup input field
and no bidirectional markup/price linkage.

**Tapping a suggested price** copies it into `MY PRICE`.

**`MY PRICE`** is an independent field for checking a price already decided on.
It accepts either so'm or USD via a toggle; the value is converted to USD before
entering the math.

**Colour band** on the `MY PRICE` markup, tied to the user's own configured
tiers rather than hardcoded thresholds:

| Condition | Colour |
|---|---|
| `markup < soft` | red |
| `soft <= markup < profitable` | amber |
| `markup >= profitable` | green |

If the user sets soft above profitable, the band uses `min`/`max` of the two so
it still behaves sensibly; a validation hint is shown but nothing is blocked.

**Weight sensitivity strip** shows the markup at `weight − 100 g`, `weight`, and
`weight + 100 g`, omitting any row at or below zero grams. When `MY PRICE` is
set the rows show the markup at that price; when it is empty they show the
profitable-tier price at each weight instead.

**Rate status line** always states which rates produced the numbers above it:
`Live · updated 2 min ago` / `Offline · rates from 1 Sep` / `Pinned rates`.
Tapping it opens Settings. A stale rate corrupts every figure on screen
silently, so this is never hidden.

### Convert

A plain currency converter for amounts with no product attached — a supplier
invoice, for example. One amount in, the other two currencies out, any direction
between ¥, $ and so'm. Uses the same rates and the same status line.

### Settings

Cargo rate ($/kg, default 9.0) · soft and profitable multiples (also editable on
the Calculator) · price rounding step and mode · rate pinning for `cnyToUsd` and
`usdToUzs` independently · manual rate refresh · language (System / English /
Uzbek) · weight unit default.

Nothing that affects a displayed number is hardcoded: every rate, rate applied
per kg, tier multiple, rounding rule and unit is user-changeable, and the colour
band derives from the user's own tiers rather than fixed thresholds.

## Exchange rates

`RatesRepository` resolves rates through a fallback chain:

1. **Primary:** `https://open.er-api.com/v6/latest/USD` — reads `rates.UZS`,
   `rates.CNY`, `time_last_update_unix`.
2. **Fallback:** `https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json`
   — reads `usd.uzs`, `usd.cny`, `date`.
3. **Cache:** last successfully fetched pair, from DataStore.
4. **Bundled defaults:** `cnyToUsd = 0.1485`, `usdToUzs = 11817`, so a first run
   with no network still works.

Both endpoints are free and require no API key; both were verified on
2026-09-02 to return UZS and CNY, agreeing within 0.05%.

Timeout is 10 seconds per endpoint. Fetch happens on app start and on manual
refresh only — never per keystroke.

**Sanity check.** A fetched value is rejected, and treated as a failed fetch, if
`usdToUzs` falls outside `[1 000, 100 000]` or `usdToCny` outside `[1, 50]`.
This stops a malformed response from silently destroying every calculation.

**Pinning.** Either rate can be pinned independently in Settings. A pinned rate
is never overwritten by a fetch, because the rate a cargo agent actually charges
is often not the market rate. The status line reads `Pinned rates` when either
is pinned.

`source` is one of `LIVE`, `CACHED`, `PINNED`, `BUNDLED`, and drives the status
line.

## Persistence

DataStore Preferences. There is no database and no saved-item list.

| Key | Default |
|---|---|
| `cargoRateUsdPerKg` | 9.0 |
| `softMultiple` | 1.8 |
| `profitableMultiple` | 2.3 |
| `priceRoundingStep` | 1000 |
| `priceRoundingMode` | UP |
| `weightUnit` | GRAMS |
| `myPriceCurrency` | UZS |
| `otherCostsCurrency` | UZS |
| `pinnedCnyToUsd` | null |
| `pinnedUsdToUzs` | null |
| `cachedCnyToUsd`, `cachedUsdToUzs`, `cachedAtEpochSeconds` | null |
| `language` | SYSTEM |
| last-entered cost / weight / other / my price | empty |

Defaults for the tier multiples come from the user's own stated thresholds:
2–2.5× is needed for real profit, and 1.3–1.5× barely survives a bad batch, so
1.8× sits just above the danger zone.

Restoring the last-entered inputs on reopen is a convenience, not a product
catalog; it does not conflict with "nothing saved."

## Localization

`values/strings.xml` (English) and `values-uz/strings.xml` (Uzbek, Latin
script). Switched in-app with
`AppCompatDelegate.setApplicationLocales(LocaleListCompat)`, which requires
`androidx.appcompat` and, for API < 33, the
`AppLocalesMetadataHolderService` manifest entry.

Number and currency formatting stays fixed across languages (see above).

## Structure

Pure logic is kept free of Android imports so it runs under fast JVM unit tests.

```
app/src/main/java/uz/yuancalc/
  MainActivity.kt
  core/Pricing.kt          landed cost, price, markup, profit — pure
  core/Rates.kt            Rates model, RateSource, sanity bounds — pure
  core/NumberParsing.kt    comma/dot/space handling — pure
  core/Formatting.kt       USD / UZS / markup formatting, rounding — pure
  data/RatesApi.kt         OkHttp + kotlinx.serialization, both endpoints
  data/RatesRepository.kt  fallback chain, cache, pin merge
  data/SettingsRepository.kt  DataStore
  ui/CalculatorViewModel.kt
  ui/CalculatorScreen.kt
  ui/ConvertScreen.kt
  ui/SettingsScreen.kt
  ui/theme/
app/src/main/res/values/strings.xml
app/src/main/res/values-uz/strings.xml
app/src/test/java/uz/yuancalc/
```

Dependencies: Compose BOM, Material 3, lifecycle-viewmodel-compose,
datastore-preferences, OkHttp, kotlinx-serialization-json, appcompat, JUnit.

Files stay under 500 lines; a screen that outgrows that splits into composables
in its own file.

## Testing

JVM unit tests, anchored on figures from real use so they double as regression
tests.

**Pricing.** All cases use the reference fixture `cnyToUsd = 0.1488`,
`usdToUzs = 11850`, cargo `$9/kg`. Expectations are exact to the stated
precision, not approximate.

| Case | Expectation |
|---|---|
| 50¥, 600 g | product $7.44, cargo $5.40, landed $12.84, landed 152 154 so'm |
| 100¥, 600 g | landed $20.28 |
| 50¥, 600 g priced at 299 000 so'm | markup 1.97×, profit $12.39 / 146 846 so'm |
| 50¥, 700 g priced at 299 000 so'm | markup 1.84× |
| 50¥, 800 g priced at 299 000 so'm | markup 1.72× |
| 50¥, 600 g, soft tier 1.8× | exact $23.112 → 274 000 so'm → $23.12, profit $10.28 |
| 50¥, 600 g, profitable tier 2.3× | exact $29.532 → 350 000 so'm → $29.54, profit $16.70 |

Note the direction of travel for a suggested price: the exact USD price is
converted to so'm, **rounded there**, and the displayed USD is converted back
from the rounded so'm figure. So the soft tier displays `$23.12`, not the
unrounded `$23.11`. Rounding the so'm price but showing the unrounded dollar
price would put two figures on screen that disagree.
| landed = 0 | markup is null |
| weight = 0 | cargo = 0 |
| other costs included | added to landed, reflected in both currencies |

The fixture rate of 0.1488 is what reproduces the `$7.44` and `$12.84` figures
from the original hand analysis. That analysis used a rounded `$0.149` for the
100¥ case and reported `$20.30`; the exact value at the fixture rate is
`$20.28`. The tests assert the exact value.

**Parsing:** `1,5`→1.5 · `1.5`→1.5 · `299 000`→299000 · non-breaking space
→299000 · empty→null · `-5`→rejected

**Formatting:** 299000→`299 000` · 12.8437→`$12.84` · 1.9735→`1.97×`

**Price rounding**, on the exact soft-tier value 273 877 so'm:

| Step | Mode | Result |
|---|---|---|
| 1 000 | up | 274 000 |
| 1 000 | nearest | 274 000 |
| 5 000 | up | 275 000 |
| 5 000 | nearest | 275 000 |
| 10 000 | up | 280 000 |
| 10 000 | nearest | 270 000 |
| off | — | 273 877 |

The 10 000 row is the one that proves `up` and `nearest` actually differ, and
the profit shown beside each must be recomputed from the rounded figure.

**RatesRepository**, against a fake HTTP client: live success · primary fails,
fallback succeeds · both fail, cache used · both fail, no cache, bundled used ·
pinned rate survives a fetch · out-of-range response rejected

**ViewModel:** inline tier edit recalculates and persists · `MY PRICE` currency
toggle converts correctly · sensitivity rows at ±100 g · rows omitted at or
below zero grams

## Verification

Tests are not the finish line. After they pass: assemble the debug APK, install
it on the `wl_test` emulator, and capture screenshots of the Calculator and
Convert tabs in both languages. The APK is debug-signed for sideloading.

## Open items

None blocking. Release signing and Play Store distribution are deliberately out
of scope.

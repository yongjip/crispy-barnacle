# WarehouseWranglerIOS (UIKit)

Home-testing iOS app for warehouse picking UX.

## Run

1) Generate Xcode project (only needed if you change `project.yml`):

```sh
cd ios/WarehouseWranglerIOS
xcodegen generate
```

2) Open `ios/WarehouseWranglerIOS/WarehouseWranglerIOS.xcodeproj` in Xcode and run.

## How To Test

- Tap **HUD Preview** to see the glasses HUD UI on the phone (useful on Simulator).
- Use **Manual scan** + **Simulate Scan** to drive the picking flow:
  - Location confirm: `A-01-01`
  - Item scan: `SKU-APPLE-001` or `8800000000001`
- **Camera Scan** uses the iPhone camera (Simulator won't scan).

## XREAL Glasses

If iOS exposes the glasses as an external display, the app will open a dedicated HUD on it via an external-display scene.
If the system is mirroring only, using **HUD Preview** full-screen will still let you see the HUD in the glasses.


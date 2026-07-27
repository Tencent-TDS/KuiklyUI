## ADDED Requirements

### Requirement: OHOS toImage FILE mode returns correct success code
The system SHALL return `code=0` and the file path in `data` when `toImage(ImageType.FILE)` succeeds on HarmonyOS.

#### Scenario: FILE mode screenshot succeeds
- **WHEN** the user calls `view.toImage(ImageType.FILE)` on HarmonyOS
- **AND** the ArkTS `componentSnapshot.get` succeeds and `KRPixelMapUtil.toFile` saves the image
- **THEN** the C++ layer SHALL return `code=0` with the file URI in `data`
- **AND** the business callback SHALL receive a non-empty file path

### Requirement: OHOS toImage FILE mode returns meaningful error on failure
The system SHALL return a readable `message` when `toImage(ImageType.FILE)` fails on HarmonyOS.

#### Scenario: FILE mode screenshot fails at ArkTS layer
- **WHEN** the user calls `view.toImage(ImageType.FILE)` on HarmonyOS
- **AND** the ArkTS `componentSnapshot.get` fails
- **THEN** the C++ layer SHALL receive the error via callback
- **AND** the business callback SHALL receive `code=-1` with a non-empty `message`

#### Scenario: FILE mode screenshot fails with empty path
- **WHEN** the user calls `view.toImage(ImageType.FILE)` on HarmonyOS
- **AND** the ArkTS callback returns an empty `path`
- **THEN** the C++ layer SHALL return `code=-1` with `message="snapshot path is empty"`

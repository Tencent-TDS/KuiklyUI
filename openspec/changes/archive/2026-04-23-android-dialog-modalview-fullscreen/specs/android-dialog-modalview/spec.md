## ADDED Requirements

### Requirement: ModalView SHALL display as true full-screen on Android
The Dialog-based ModalView SHALL cover the entire screen including the status bar, navigation bar, and display cutout (notch) areas.

#### Scenario: Full-screen coverage on standard device
- **WHEN** the ModalView is shown on a device without notch
- **THEN** the Dialog Window dimensions SHALL equal the full display size (e.g., 1080x2412)

#### Scenario: Full-screen coverage on notch device
- **WHEN** the ModalView is shown on a device with display cutout
- **THEN** the Dialog Window SHALL extend into the cutout area and SHALL NOT be constrained to the safe area

### Requirement: ModalView SHALL isolate accessibility focus
The Dialog Window SHALL prevent screen readers from accessing underlying Activity content while the modal is visible.

#### Scenario: TalkBack navigation within modal
- **WHEN** TalkBack is enabled and the ModalView is displayed
- **THEN** swipe navigation SHALL be confined to elements within the modal
- **AND** underlying Activity elements SHALL NOT receive focus

### Requirement: ModalView SHALL manage Dialog lifecycle automatically
The ModalView SHALL create the Dialog when added to the view hierarchy and dismiss it when destroyed.

#### Scenario: Dialog creation on attach
- **WHEN** the ModalView is added to its parent
- **THEN** a full-screen Dialog SHALL be created and shown
- **AND** the ModalView SHALL be removed from its original parent before being set as the Dialog content

#### Scenario: Dialog dismissal on destroy
- **WHEN** the ModalView receives an `onDestroy` event
- **THEN** the associated Dialog SHALL be dismissed
- **AND** the Dialog reference SHALL be cleared to prevent leaks

### Requirement: ModalView SHALL use a non-floating Dialog theme
The Dialog SHALL use a custom theme with `windowIsFloating=false` to eliminate default margins and minimum width constraints.

#### Scenario: Theme application
- **WHEN** the Dialog is instantiated
- **THEN** it SHALL use the `MyDialogFullScreen` theme
- **AND** the Window SHALL NOT have floating behavior or default padding

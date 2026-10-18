# Device testing restrictions

- The physical device with ADB serial `5c7da359` is the user's primary phone. Never use it for automated tests, APK installation or replacement, uninstalling packages, clearing app data, screenshots, or other device operations.
- Do not run `connectedAndroidTest`, `connectedDebugAndroidTest`, or other commands that automatically target connected physical devices. Their installation and cleanup can remove the user's application and data.
- Use local compilation, static checks, and host unit tests by default. Use an emulator only after identifying it explicitly and ensuring the command targets only that emulator.
- Permission to implement code does not authorize operations on the user's primary phone. Do not infer device-testing permission from a connected ADB device or a UI skill's validation checklist.

// Prevent h5App.js from using UMD wrapper that overwrites global properties
// (e.g., window.com, window.callKotlinMethod) set by nativevue2.js.
// h5App.js is an executable entry, not a library, so it doesn't need to export anything.
config.output = config.output || {};
config.output.libraryTarget = undefined;
config.output.library = undefined;
config.output.iife = true;

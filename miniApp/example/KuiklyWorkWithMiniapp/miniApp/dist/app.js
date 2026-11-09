var business = require('./business/nativevue2.js')
var render = require('./lib/miniApp.js')

if (typeof globalThis !== 'undefined') {
    globalThis.com = business.com;
    globalThis.callKotlinMethod = business.callKotlinMethod;
}

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

render.initApp()
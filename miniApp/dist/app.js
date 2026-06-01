var business = require('./business/nativevue2')
var render = require('./lib/miniprogramApp.js')

if (typeof globalThis !== 'undefined') {
    globalThis.com = business.com;
    globalThis.callKotlinMethod = business.callKotlinMethod;
}

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

global.getAssetJson = function(path) {
    var json = require('./assets/' + path.replace('.json','.js'))
    return json
}

// Load custom fonts
global.loadCustomFonts = function() {
    wx.loadFontFace({
        global: true,
        family: 'Satisfy-Regular',
        source: 'url("/assets/fonts/Satisfy-Regular.ttf")',
        success: function(res) {
            console.log('[Font] Satisfy-Regular loaded successfully', res)
        },
        fail: function(err) {
            console.warn('[Font] Satisfy-Regular load failed', err)
        }
    })
}
global.loadCustomFonts()

render.initApp()

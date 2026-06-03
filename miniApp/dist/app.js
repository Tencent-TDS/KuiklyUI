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
    try {
        var fontDataUri = require('./assets/fonts/Satisfy-Regular.js')

        wx.loadFontFace({
            global: true,
            family: 'Satisfy-Regular',
            source: 'url("' + fontDataUri + '")',
            scopes: ['webview', 'native'],
            success: function(res) {
                render.fontLoaded()
            },
            fail: function(err) {
                console.warn('loadFontFace failed:', err.errMsg || JSON.stringify(err))
            }
        })
    } catch(e) {
        console.warn('loadCustomFonts failed:', e.message || e)
    }
}

global.loadCustomFonts()

render.initApp()

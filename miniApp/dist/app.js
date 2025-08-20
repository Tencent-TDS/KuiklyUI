var business = require('./business/nativevue2')
var render = require('./lib/miniprogramApp.js')

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

global.getAssetJson = function(path) {
    var json = require('./assets/' + path.replace('.json','.js'))
    return json
}

render.initApp()

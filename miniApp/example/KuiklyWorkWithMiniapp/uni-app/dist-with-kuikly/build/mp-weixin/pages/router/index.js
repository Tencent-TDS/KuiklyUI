var business = require('../../business/nativevue2.js')
var render = require('../../lib/miniApp.js')

global.com = business.com;
global.callKotlinMethod = business.callKotlinMethod;

render.renderView({
    pageName: "router"
})
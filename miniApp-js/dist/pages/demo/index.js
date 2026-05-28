// 使用 global.render（由 app.js 设置）
var render = global.render

if (render && render.renderView) {
    render.renderView({
        pageName: "HelloWorldPage"
    })
} else {
    console.error('[pages/demo] global.render not found! app.js may not have loaded correctly.')
}

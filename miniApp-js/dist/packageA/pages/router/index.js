// 分包页面使用 global.render（在 app.js 中已初始化）
var render = global.render

if (render && render.renderView) {
    render.renderView({
        pageName: "HelloWorldPage"
    })
} else {
    console.error('[packageA/pages/router] global.render not found! app.js may not have loaded correctly.')
}

const Koa = require('koa')
const serve = require('koa-static')
const bodyParser = require('koa-bodyparser')
require('console-error')
require('console-info')
require('console-warn')

const serveConf = require('./config/serve.conf')
const shell = require("shelljs");

const app = new Koa()

shell.exec('w2 start -p 8083');
shell.exec('w2 add --force')
process.on('SIGINT', () => {
    shell.exec('w2 stop');
    process.exit()
})
process.on('exit', _ => {
    shell.exec('w2 stop');
})

app.use(serve(serveConf.staticPath))

app.use(bodyParser())

app.use(async (ctx) => {
    console.log(ctx.url);
})

app.listen(serveConf.port, () => {
    console.log(`
    nv2 serve is starting at port ${serveConf.port}

    visit: localhost:${serveConf.port}

    whistle: http://localhost:8017/

    quit: control + c
  `)
})
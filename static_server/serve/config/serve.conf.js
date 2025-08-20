/**
 * server config
 */

const path = require('path')

const staticPath = '../../static'

module.exports = {
  port: 8017,
  staticPath: path.join( __dirname, staticPath)
}
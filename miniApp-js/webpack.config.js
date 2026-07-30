const path = require('path')
const fs = require('fs')
const CopyWebpackPlugin = require('copy-webpack-plugin')

module.exports = (env, argv) => {
	const isDevelopment = argv.mode === 'development'

	return {
		// 入口文件：先加载 Kotlin 库，再加载业务代码
		// 打包为单个 kuiklyRender.js 文件
		entry: './src/entry.js',
		output: {
			path: path.resolve(__dirname, './dist/lib'),
			filename: 'kuiklyRender.js',
			clean: false,
			// 使用 CommonJS 导出，小程序支持 require
			library: {
				type: 'commonjs2',
			},
			// 确保兼容性
			environment: {
				arrowFunction: false,
			},
		},
		target: 'node',
		module: {
			rules: [
				{
					test: /\.js$/,
					exclude: /node_modules/,
					use: {
						loader: 'babel-loader',
						options: {
							presets: ['@babel/preset-env'],
						},
					},
				},
			],
		},
		plugins: [
			// 自定义插件：在复制前清理目标目录
			{
				apply: (compiler) => {
					compiler.hooks.beforeRun.tap('CleanBeforeCopy', () => {
						const assetsDir = path.resolve(__dirname, './dist/assets')
						const businessDir = path.resolve(
							__dirname,
							'./dist/business',
						)

						// 清理 assets 目录
						if (fs.existsSync(assetsDir)) {
							fs.rmSync(assetsDir, { recursive: true, force: true })
							console.log(`已清理目录: ${assetsDir}`)
						}

						// 清理 business 目录
						if (fs.existsSync(businessDir)) {
							fs.rmSync(businessDir, { recursive: true, force: true })
							console.log(`已清理目录: ${businessDir}`)
						}
					})
				},
			},
			new CopyWebpackPlugin({
				patterns: [
					// Copy assets from demo
					{
						from: path.resolve(__dirname, '../demo/src/commonMain/assets'),
						to: path.resolve(__dirname, './dist/assets'),
						noErrorOnMissing: true,
					},
					// Copy page bundles with transformation for miniprogram compatibility
					{
						from: path.resolve(__dirname, 'src/bundles'),
						to: path.resolve(__dirname, './dist/business'),
						noErrorOnMissing: true,
					
					},
				],
			}),
		],
		resolve: {
			extensions: ['.js'],
			alias: {
				'@': path.resolve(__dirname, 'src'),
				'@components': path.resolve(__dirname, 'src/components'),
				'@modules': path.resolve(__dirname, 'src/modules'),
			},
		},
		devtool: isDevelopment ? 'source-map' : false,
		optimization: {
			minimize: !isDevelopment,
		},
	}
}

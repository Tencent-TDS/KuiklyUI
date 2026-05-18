/**
 * App Info Module (JavaScript Implementation)
 */

/**
 * 获取全局对象
 */
function getGlobal() {
	return typeof global !== 'undefined'
		? global
		: typeof window !== 'undefined'
		? window
		: this
}

export class KRAppInfoModule {
	static MODULE_NAME = 'KuiklyAppInfoModule'

	constructor() {
		// Required by IKuiklyRenderModuleExport interface
		this.kuiklyRenderContext = null
	}

	/**
	 * Kotlin 调用入口（通过 @JsName("_call") 映射）
	 * @param {string} method - 方法名
	 * @param {*} params - 参数（可以是任意类型）
	 * @param {Function} callback - 回调函数
	 * @returns {*}
	 */
	_call(method, params, callback) {
		return this.call(method, params, callback)
	}

	/**
	 * 调用模块方法
	 * @param {string} method - 方法名
	 * @param {string} params - JSON 字符串参数
	 * @param {Function} callback - 回调函数
	 * @returns {*}
	 */
	call(method, params, callback) {
		const g = getGlobal()

		switch (method) {
			case 'getAppInfo':
				return this.getAppInfo()

			case 'getLoginStatus': // 兼容老接口
			case 'getIsLogin': // 新接口
				return this.getIsLogin()

			case 'getUserInfoString':
				return g.getUserInfoString ? g.getUserInfoString() : ''

			case 'cusUserAgent':
				return g.cusUserAgent
					? g.cusUserAgent()
					: g.navigator
					? g.navigator.userAgent
					: 'Kuikly-MiniApp'

			case 'getUnReadMsg':
				return g.getUnReadMsg ? g.getUnReadMsg() : 0

			case 'login':
				this.login(callback)
				return

			case 'switchAccountIfNeed':
				this.switchAccountIfNeed(params, callback)
				return

			case 'switchAccount':
				if (g.switchAccount) {
					g.switchAccount()
				}
				return

			case 'updateUserInfo':
				if (g.updateUserInfo) {
					g.updateUserInfo()
				}
				return

			case 'getServerTime':
				return g.getServerTime ? g.getServerTime() : Date.now()

			case 'updateServerTime':
				if (g.updateServerTime) {
					g.updateServerTime(params)
				}
				return

			case 'getUnderAgeStatus':
				this.getUnderAgeStatus(callback)
				return

			default:
				console.error(`[KRAppInfoModule] ${method} not found`)
				if (callback) {
					callback({
						code: -1,
						message: 'Method not found',
					})
				}
				return null
		}
	}

	getAppInfo() {
		const g = getGlobal()
		let appInfo
		if (typeof g.getAppInfo === 'function') {
			appInfo = g.getAppInfo()
		} else {
			// Return minimal default info if host method missing
			appInfo = {
				env: 1,
				version: '1.0.0',
				osVersion: 'Unknown',
				loginType: '',
				userInfo: {},
				wxUserInfo: {},
				qqUserInfo: {},
			}
		}
		
		// Kotlin 期望 JSON 字符串，不是对象
		// 如果已经是字符串就直接返回，否则转换为字符串
		if (typeof appInfo === 'string') {
			return appInfo
		}
		return JSON.stringify(appInfo)
	}

	getIsLogin() {
		const g = getGlobal()
		if (typeof g.getIsLogin === 'function') {
			return g.getIsLogin() // Should return "1" or "0"
		}
		return '0'
	}

    login(callback) {
        console.info('[KRAppInfoModule] login')
		const g = getGlobal()
		if (typeof g.jumpToLoginPage === 'function') {
			g.jumpToLoginPage((res) => {
				if (callback) callback(res)
			})
		} else {
			console.error('[KRAppInfoModule] global.login not found')
			if (callback) callback(null)
		}
	}

	switchAccountIfNeed(params, callback) {
		const g = getGlobal()
		if (typeof g.switchAccountIfNeed === 'function') {
			g.switchAccountIfNeed(params, (res) => {
				if (callback) callback(res)
			})
		} else {
			console.error('[KRAppInfoModule] global.switchAccountIfNeed not found')
			// 根据 Android 逻辑，失败时会调用 switchAccount，这里我们简单返回 fail
			if (callback) callback({ result: 'fail' })
		}
	}

	getUnderAgeStatus(callback) {
		const g = getGlobal()
		if (typeof g.getUnderAgeStatus === 'function') {
			g.getUnderAgeStatus((res) => {
				if (callback) callback(res)
			})
		} else {
			console.error('[KRAppInfoModule] global.getUnderAgeStatus not found')
			if (callback) callback(null)
		}
	}

	/**
	 * 清理模块资源 (IKuiklyRenderModuleExport 接口要求)
	 */
	onDestroy() {
		// 清理工作（如果需要）
	}
}

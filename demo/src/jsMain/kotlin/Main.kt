
import com.tencent.kuikly.core.js.KuiklyCoreEntry
import com.tencent.kuikly.core.manager.BridgeManager

@JsName("callKotlinMethod")
@JsExport
fun callKotlinMethod(
    methodId: Int,
    arg0: Any? = null,
    arg1: Any? = null,
    arg2: Any? = null,
    arg3: Any? = null,
    arg4: Any? = null,
    arg5: Any? = null
) {
    BridgeManager.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
}

fun main() {
    // 1. Register pages
    KuiklyCoreEntry().triggerRegisterPages()
    
    // 2. Expose callKotlinMethod to global scope (fallback)
    val globalScope = js("typeof globalThis !== 'undefined' ? globalThis : (typeof global !== 'undefined' ? global : window)")
    
    if (globalScope.callKotlinMethod == undefined) {
         globalScope.callKotlinMethod = { methodId: Int, arg0: Any?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?, arg5: Any? ->
             callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
         }
    }
    
    console.log("Kuikly demo initialized")
}

package impl

import com.squareup.kotlinpoet.*

/**
 * JS Target Entry Builder for H5/Web platform (Single Module Mode)
 *
 * Processing flow:
 * 1. KSP detects JS target via outputSourceSet.jsFamily() in KuiklyCoreProcessorProvider
 * 2. Scans all classes annotated with @Page
 * 3. Generates KuiklyCoreEntry.kt with:
 *    - registerAllPages(): Registers all page routes to BridgeManager
 *    - callKotlinMethodImpl(): Bridge for JS to call Kotlin methods
 *    - registerCallNativeImpl(): Registers native callbacks for each pager
 *    - main(): Entry point that sets up window.callKotlinMethod and
 *      window.com.tencent.kuikly.core.nvi.registerCallNative for H5 render layer
 *
 * Runtime call chain:
 * index.html -> h5App.js -> main() -> registerAllPages()
 *                                  -> window.callKotlinMethod = ...
 *                                  -> window.com.tencent.kuikly.core.nvi.registerCallNative = ...
 *                       -> nativevue2.js calls these registered functions
 *
 * Created by xuwakao on 2025/12
 */
open class JSTargetEntryBuilder : KuiklyCoreAbsEntryBuilder() {

    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        // Add JS-specific imports
        builder.addImport("com.tencent.kuikly.core.nvi", "CallNativeCallback")
        builder.addImport("kotlinx.browser", "window")

        // Add private properties
        builder.addProperty(
            PropertySpec.builder("didInit", Boolean::class)
                .addModifiers(KModifier.PRIVATE)
                .mutable()
                .initializer("false")
                .build()
        )

        // Add registerAllPages function
        builder.addFunction(createRegisterAllPagesFunc(pagesAnnotations))

        // Add callKotlinMethodImpl function
        builder.addFunction(createCallKotlinMethodImplFunc())

        // Add registerCallNativeImpl function
        builder.addFunction(createRegisterCallNativeImplFunc())

        // Add main function
        builder.addFunction(createMainFunc())
    }

    private fun createRegisterAllPagesFunc(pagesAnnotations: List<PageInfo>): FunSpec {
        return FunSpec.builder("registerAllPages")
            .addModifiers(KModifier.PRIVATE)
            .addStatement("if (didInit) return")
            .addStatement("didInit = true")
            .addStatement("BridgeManager.init()")
            .addRegisterPageRouteStatement(pagesAnnotations)
            .build()
    }

    private fun createCallKotlinMethodImplFunc(): FunSpec {
        // Use Any? instead of dynamic for the function signature
        // The actual dynamic behavior is handled in the lambda in main()
        val anyNullable = Any::class.asTypeName().copy(nullable = true)
        return FunSpec.builder("callKotlinMethodImpl")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("methodId", Int::class)
            .addParameter("arg0", anyNullable)
            .addParameter("arg1", anyNullable)
            .addParameter("arg2", anyNullable)
            .addParameter("arg3", anyNullable)
            .addParameter("arg4", anyNullable)
            .addParameter("arg5", anyNullable)
            .returns(anyNullable)
            .addStatement("BridgeManager.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)")
            .addStatement("return Unit")
            .build()
    }

    private fun createRegisterCallNativeImplFunc(): FunSpec {
        return FunSpec.builder("registerCallNativeImpl")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("pagerId", String::class)
            .addParameter("callback", ClassName("com.tencent.kuikly.core.nvi", "CallNativeCallback"))
            .addStatement("NativeBridge.registerCallNativeCallback(pagerId, callback)")
            .addStatement("if (!BridgeManager.containNativeBridge(pagerId)) {")
            .addStatement("    BridgeManager.registerNativeBridge(pagerId, NativeBridge())")
            .addStatement("}")
            .build()
    }

    private fun createMainFunc(): FunSpec {
        return FunSpec.builder("main")
            .addCode("""
                |registerAllPages()
                |
                |// Register callKotlinMethod on window
                |window.asDynamic().callKotlinMethod = { methodId: Int, arg0: dynamic, arg1: dynamic, arg2: dynamic, arg3: dynamic, arg4: dynamic, arg5: dynamic ->
                |    callKotlinMethodImpl(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
                |}
                |
                |// Create namespace path: window.com.tencent.kuikly.core.nvi
                |js(${'"'}${'"'}${'"'}
                |    if (typeof window.com === 'undefined') window.com = {};
                |    if (typeof window.com.tencent === 'undefined') window.com.tencent = {};
                |    if (typeof window.com.tencent.kuikly === 'undefined') window.com.tencent.kuikly = {};
                |    if (typeof window.com.tencent.kuikly.core === 'undefined') window.com.tencent.kuikly.core = {};
                |    if (typeof window.com.tencent.kuikly.core.nvi === 'undefined') window.com.tencent.kuikly.core.nvi = {};
                |${'"'}${'"'}${'"'})
                |
                |// Register registerCallNative function
                |window.asDynamic().com.tencent.kuikly.core.nvi.registerCallNative = { pagerId: String, callback: CallNativeCallback ->
                |    registerCallNativeImpl(pagerId, callback)
                |}
                |""".trimMargin())
            .build()
    }

    override fun entryFileName(): String {
        return "KuiklyCoreEntry"
    }

    override fun packageName(): String {
        return ""
    }
}

package impl.submodule

import com.squareup.kotlinpoet.*
import impl.JSTargetEntryBuilder
import impl.PageInfo

/**
 * JS Multi-Module Target Entry Builder for H5/Web platform
 *
 * Extends JSTargetEntryBuilder to support multi-module architecture where pages
 * are distributed across multiple Gradle modules.
 *
 * KSP options:
 * - enableMultiModule: true to enable multi-module mode
 * - isMainModule: true if this is the main module
 * - subModules: "&" separated list of sub-module IDs (e.g., "home&settings&profile")
 * - moduleId: ID of current module (for sub-modules)
 *
 * Generated code:
 *
 * Sub-module (isMainModule=false):
 *   object KuiklyCoreEntry_<moduleId> {
 *       fun triggerRegisterPages() {
 *           BridgeManager.registerPageRouter("page1") { Page1() }
 *       }
 *   }
 *
 * Main module (isMainModule=true):
 *   private fun registerAllPages() {
 *       BridgeManager.registerPageRouter(...)  // local pages
 *       KuiklyCoreEntry_home.triggerRegisterPages()      // sub-modules
 *       KuiklyCoreEntry_settings.triggerRegisterPages()
 *   }
 *   fun main() { ... }
 *
 * Created by xuwakao on 2025/12
 */
class JSMultiTargetEntryBuilder(
    private val isMainModule: Boolean,
    private val subModules: String,
    private val moduleId: String
) : JSTargetEntryBuilder() {

    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        if (!isMainModule) {
            // Sub-module: generate an object with triggerRegisterPages()
            builder.addType(
                TypeSpec.objectBuilder(entryFileName() + "_" + moduleId)
                    .addFunction(createSubModuleRegisterPagesFuncSpec(pagesAnnotations))
                    .build()
            )
        } else {
            // Main module: generate full entry with main()
            builder.addImport("com.tencent.kuikly.core.nvi", "CallNativeCallback")
            builder.addImport("kotlinx.browser", "window")

            builder.addProperty(
                PropertySpec.builder("didInit", Boolean::class)
                    .addModifiers(KModifier.PRIVATE)
                    .mutable()
                    .initializer("false")
                    .build()
            )

            builder.addFunction(createRegisterAllPagesFunc(pagesAnnotations))
            builder.addFunction(createCallKotlinMethodImplFunc())
            builder.addFunction(createRegisterCallNativeImplFunc())
            builder.addFunction(createMainFunc())
        }
    }

    private fun createSubModuleRegisterPagesFuncSpec(pagesAnnotations: List<PageInfo>): FunSpec {
        return FunSpec.builder("triggerRegisterPages")
            .addRegisterPageRouteStatement(pagesAnnotations)
            .build()
    }

    private fun createRegisterAllPagesFunc(pagesAnnotations: List<PageInfo>): FunSpec {
        return FunSpec.builder("registerAllPages")
            .addModifiers(KModifier.PRIVATE)
            .addStatement("if (didInit) return")
            .addStatement("didInit = true")
            .addStatement("BridgeManager.init()")
            .addRegisterPageRouteStatement(pagesAnnotations)
            .addSubModuleStatement()
            .build()
    }

    private fun FunSpec.Builder.addSubModuleStatement(): FunSpec.Builder {
        subModules.split("&").forEach {
            val name = it.trim()
            if (name.isNotEmpty()) {
                addStatement(entryFileName() + "_" + name + ".triggerRegisterPages()")
            }
        }
        return this
    }

    private fun createCallKotlinMethodImplFunc(): FunSpec {
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
                |js(${'\"'}${'\"'}${'\"'}
                |    if (typeof window.com === 'undefined') window.com = {};
                |    if (typeof window.com.tencent === 'undefined') window.com.tencent = {};
                |    if (typeof window.com.tencent.kuikly === 'undefined') window.com.tencent.kuikly = {};
                |    if (typeof window.com.tencent.kuikly.core === 'undefined') window.com.tencent.kuikly.core = {};
                |    if (typeof window.com.tencent.kuikly.core.nvi === 'undefined') window.com.tencent.kuikly.core.nvi = {};
                |${'\"'}${'\"'}${'\"'})
                |
                |// Register registerCallNative function
                |window.asDynamic().com.tencent.kuikly.core.nvi.registerCallNative = { pagerId: String, callback: CallNativeCallback ->
                |    registerCallNativeImpl(pagerId, callback)
                |}
                |""".trimMargin())
            .build()
    }
}

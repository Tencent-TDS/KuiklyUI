/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef KR_SHADOW_EXPORT_C_H
#define KR_SHADOW_EXPORT_C_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Opaque handle representing a custom shadow instance.
 * The business module allocates and owns this memory; the SDK calls back
 * through the function pointers in KRShadowVTable to interact with it.
 */
typedef void *KRShadowHandle;

/**
 * Called by the SDK when a shadow property is updated.
 * @param handle   Shadow instance handle
 * @param prop_key Property name (null-terminated)
 * @param prop_val Property value as string (null-terminated, may be empty)
 */
typedef void (*KRShadowSetPropFunc)(KRShadowHandle handle,
                                    const char *prop_key,
                                    const char *prop_val);

/**
 * Called by the SDK to calculate the intrinsic size of the custom component.
 * @param handle            Shadow instance handle
 * @param constraint_width  Width constraint from flex layout (px)
 * @param constraint_height Height constraint from flex layout (px)
 * @param[out] out_width    Computed width (px)
 * @param[out] out_height   Computed height (px)
 */
typedef void (*KRShadowCalculateSizeFunc)(KRShadowHandle handle,
                                          double constraint_width,
                                          double constraint_height,
                                          double *out_width,
                                          double *out_height);

/**
 * Called by the SDK when the shadow instance is destroyed.
 * The business module must free the handle and any associated resources.
 * @param handle Shadow instance handle
 */
typedef void (*KRShadowDestroyFunc)(KRShadowHandle handle);

/**
 * Factory: creates a new shadow instance.
 * @return Opaque handle (must not be NULL). Ownership transfers to the SDK;
 *         the SDK will call @ref KRShadowDestroyFunc when done.
 */
typedef KRShadowHandle (*KRShadowCreateFunc)(void);

/**
 * Virtual table of C function pointers for a custom shadow type.
 * All fields except set_prop are required.
 */
typedef struct {
    /** Optional: property update callback (may be NULL) */
    KRShadowSetPropFunc set_prop;
    /** Required: size calculation callback */
    KRShadowCalculateSizeFunc calculate_size;
    /** Required: destruction callback */
    KRShadowDestroyFunc destroy;
} KRShadowVTable;

/**
 * Register a custom shadow type with the Kuikly render SDK.
 *
 * Must be called once per shadow type during initialization (before any
 * component that references the view_name is created).
 *
 * @param view_name Unique component name (e.g. "XXXWeb", "MyXXXView")
 * @param create    Factory function (called once per component instance)
 * @param vtable    Function pointers for lifecycle operations
 */
void KRRegisterShadowCreator(const char *view_name,
                             KRShadowCreateFunc create,
                             const KRShadowVTable *vtable);

#ifdef __cplusplus
}
#endif

#endif /* KR_SHADOW_EXPORT_C_H */

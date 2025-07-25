/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.tdf.module;

import com.tencent.tdf.annotation.TDFModule;

/**
 * 通用 Module 信息
 */
class TDFModuleInfo {

    private String mName;
    private String[] mNames;
    private final TDFModuleContext mContext;
    private final TDFModuleProvider mProvider;
    private volatile boolean mInit = false;
    private TDFBaseModule mInstance;

    TDFModuleInfo(TDFModuleContext context, Class<? extends TDFBaseModule> cls, TDFModuleProvider provider) {
        mContext = context;
        mProvider = provider;
        TDFModule annotation = cls.getAnnotation(TDFModule.class);
        if (annotation != null) {
            mName = annotation.name();
            mNames = annotation.names();
        }
    }

    public TDFBaseModule getInstance() {
        if (mInit) {
            return mInstance;
        }
        synchronized (this) {
            if (!mInit) {
                mInstance = mProvider.get(mContext);
                mInstance.init();
                mInit = true;
            }
        }
        return mInstance;
    }

    public void destroy() {
        if (mInstance != null) {
            mInstance.destroy();
        }
    }

    public String getName() {
        return mName;
    }

    public String[] getNames() {
        return mNames;
    }

}

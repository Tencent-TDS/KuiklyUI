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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TDFModuleManager {

    private final TDFModuleContext mContext;

    private final ConcurrentHashMap<String, TDFModuleInfo> mModuleInfo;

    public TDFModuleManager(TDFModuleContext context) {
        mContext = context;
        mModuleInfo = new ConcurrentHashMap<>();
    }

    public void addModule(Class<? extends TDFBaseModule> cls, TDFModuleProvider provider) {
        if (provider == null) {
            return;
        }
        TDFModuleInfo moduleInfo = new TDFModuleInfo(mContext, cls, provider);
        String[] names = moduleInfo.getNames();
        if (names != null) {
            for (String name : names) {
                if (!mModuleInfo.containsKey(name)) {
                    mModuleInfo.put(name, moduleInfo);
                }
            }
        }
        String name = moduleInfo.getName();
        if (name != null && !mModuleInfo.containsKey(name)) {
            mModuleInfo.put(name, moduleInfo);
        }
    }

    public TDFBaseModule getModule(String moduleName) {
        if (moduleName == null || moduleName.length() == 0) {
            return null;
        }
        TDFModuleInfo moduleInfo = mModuleInfo.get(moduleName);
        if (moduleInfo != null) {
            return moduleInfo.getInstance();
        }
        return null;
    }

    public void destroy() {
       for (Entry<String, TDFModuleInfo> entry : mModuleInfo.entrySet()) {
          if (entry == null) {
             continue;
          }
          TDFModuleInfo moduleInfo = entry.getValue();
          if (moduleInfo != null) {
             moduleInfo.destroy();
          }
       }
       mModuleInfo.clear();
    }

}

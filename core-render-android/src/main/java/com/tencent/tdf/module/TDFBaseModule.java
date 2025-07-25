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

import com.tencent.tdf.annotation.TDFMethod;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TDFBaseModule {

   private TDFModuleContext moduleContext;

   private Map<String, TDFMethodInfo> mMethods;

   public TDFBaseModule(TDFModuleContext context) {
      this.moduleContext = context;
   }

   public void init() {
      initMethods();
   }

   /**
    * 初始化方法列表
    */
   private void initMethods() {
      if (mMethods != null) {
         return;
      }
      synchronized (this) {
         if (mMethods != null) {
            return;
         }
         mMethods = new ConcurrentHashMap<>();
         Method[] targetMethods = getClass().getMethods();
         for (Method targetMethod : targetMethods) {
            TDFMethod tdfMethod = targetMethod.getAnnotation(TDFMethod.class);
            if (tdfMethod == null) {
               continue;
            }
            String methodName = tdfMethod.name();
            if (methodName == null || methodName.length() == 0) {
               methodName = targetMethod.getName();
            }
            if (mMethods.containsKey(methodName)) {
               continue;
            }
            mMethods.put(methodName, new TDFMethodInfo(targetMethod, tdfMethod.isSync()));
         }
      }
   }

   public Object invoke(String methodName, List<Object> params, TDFModulePromise promise) throws Exception {
      if (methodName == null) {
         return null;
      }
      TDFMethodInfo method = mMethods.get(methodName);
      if (method != null) {
         return method.invoke(this, params, promise);
      }
      return null;
   }

   public TDFMethodInfo getMethod(String methodName) {
      if (methodName == null) {
         return null;
      }
      return mMethods.get(methodName);
   }

   public void destroy() {}

}

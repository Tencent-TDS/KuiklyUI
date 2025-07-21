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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tencent.tdf.utils.TDFParamsUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

class TDFMethodInfo {

    @NonNull
    private final Method mMethod;
    @Nullable
    private final Type[] mParamTypes;
    private final boolean mIsSync;

    TDFMethodInfo(@NonNull Method method, boolean isSync) {
        mMethod = method;
        mParamTypes = method.getGenericParameterTypes();
        mIsSync = isSync;
    }

    public boolean isSync() {
        return mIsSync;
    }

    public Object invoke(@NonNull Object receiver, List<Object> args, TDFModulePromise promise) throws Exception {
        Object[] params = null;
        if (args != null) {
            params = prepareArguments(args, promise);
        }
        return mMethod.invoke(receiver, params);
    }

    private Object[] prepareArguments(@NonNull List<Object> args, TDFModulePromise promise) {
        if (mParamTypes == null || mParamTypes.length == 0) {
            return null;
        }
        Object[] params = new Object[mParamTypes.length];
        int index = 0;
        int size = params.length;
        for (int i = 0; i < mParamTypes.length; i++) {
            Type paramType = mParamTypes[i];
            if (paramType == TDFModulePromise.class) {
                params[i] = promise;
            } else {
                if (size <= index) {
                    throw new IllegalArgumentException("The number of parameters does not match");
                }
                params[i] = TDFParamsUtils.parseParams(args, paramType, index);
                index++;
            }
        }
        return params;
    }

}

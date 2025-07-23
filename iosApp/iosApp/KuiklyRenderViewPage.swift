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

import Foundation
import SwiftUI

struct KuiklyRenderViewPage : UIViewControllerRepresentable {
    var pageName: String
    var data: Dictionary<String, Any>

    func makeUIViewController(context: Context) -> UINavigationController {
        let hrVC = kuiklyPage()
        return UINavigationController.init(rootViewController: hrVC)
    }
    
    func kuiklyPage() -> KuiklyRenderViewController {
        return KuiklyRenderViewController(pageName: pageName, pageData: data)
    }
    
    func nativePage() -> RootViewController {
        return RootViewController()
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {

    }

    func dealloc() {

    }


}

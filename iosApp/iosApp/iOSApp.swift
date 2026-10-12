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

import SwiftUI

#if LLVM_PGO_INSTRUMENT
import UIKit

enum LLVMProfileBootstrap {
    static var profilePath: String {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return docs.appendingPathComponent("kuikly_ios.profraw").path
    }

    static func configure() {
        KRLLVMProfileSetFilename(profilePath)
        NSLog("[LLVM_PGO] configured path=%@", profilePath)
    }

    static func writeIfAvailable() {
        KRLLVMProfileDump()
        let path = profilePath
        let exists = FileManager.default.fileExists(atPath: path)
        var size: UInt64 = 0
        if exists, let attrs = try? FileManager.default.attributesOfItem(atPath: path) {
            size = (attrs[.size] as? NSNumber)?.uint64Value ?? 0
        }
        NSLog("[LLVM_PGO] file exists=%@ size=%llu path=%@", exists ? "YES" : "NO", size, path)
    }
}

final class LLVMProfileAppDelegate: NSObject, UIApplicationDelegate {
    private var dumpTimer: Timer?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        LLVMProfileBootstrap.configure()
        // 定时落盘：不依赖 SwiftUI onReceive
        dumpTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { _ in
            LLVMProfileBootstrap.writeIfAvailable()
        }
        if let dumpTimer {
            RunLoop.main.add(dumpTimer, forMode: .common)
        }
        // 立即异步写一次
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            LLVMProfileBootstrap.writeIfAvailable()
        }
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        LLVMProfileBootstrap.writeIfAvailable()
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        LLVMProfileBootstrap.writeIfAvailable()
    }
}
#endif

@main
struct iOSApp: App {
#if LLVM_PGO_INSTRUMENT
    @UIApplicationDelegateAdaptor(LLVMProfileAppDelegate.self) var appDelegate
#endif

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

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

struct KuiklyRenderViewPage : NSViewControllerRepresentable {
    typealias NSViewControllerType = KuiklyPageViewController
    
    var pageName: String
    var data: Dictionary<String, Any>
    
    func makeNSViewController(context: Context) -> KuiklyPageViewController {
        return KuiklyPageViewController(pageName: pageName, data: data)
    }
    
    func updateNSViewController(_ nsViewController: KuiklyPageViewController, context: Context) {
        // Update the view controller when SwiftUI state changes
        nsViewController.update(with: pageName, data: data)
    }
}

// Custom NSViewController for rendering Kuikly pages
class KuiklyPageViewController: NSViewController {
    private var currentPageName: String
    private var currentData: [String: Any]
    
    init(pageName: String, data: [String: Any]) {
        self.currentPageName = pageName
        self.currentData = data
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        self.currentPageName = ""
        self.currentData = [:]
        super.init(coder: coder)
    }
    
    override func loadView() {
        // Create the main view
        self.view = NSView()
        self.view.translatesAutoresizingMaskIntoConstraints = false
        
        // Set up your custom rendering logic here
        setupRendering()
    }
    
    func update(with pageName: String, data: [String: Any]) {
        // Only update if the data has changed
        if self.currentPageName != pageName || !NSDictionary(dictionary: self.currentData).isEqual(to: data) {
            self.currentPageName = pageName
            self.currentData = data
            setupRendering()
        }
    }
    
    private func setupRendering() {
        // Clear existing subviews
        self.view.subviews.forEach { $0.removeFromSuperview() }
        
        // Add your custom rendering logic here based on pageName and data
        // This is where you would integrate with your Kuikly rendering engine
        
        let label = NSTextField(labelWithString: "Rendering: \(currentPageName)")
        label.translatesAutoresizingMaskIntoConstraints = false
        self.view.addSubview(label)
        
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: self.view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: self.view.centerYAnchor)
        ])
        
        // Log the data for debugging
        print("Rendering page: \(currentPageName) with data: \(currentData)")
    }
}

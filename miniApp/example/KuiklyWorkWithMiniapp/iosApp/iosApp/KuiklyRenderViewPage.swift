import Foundation
import SwiftUI

struct KuiklyRenderViewPage : UIViewControllerRepresentable {
    var pageName: String
    var data: Dictionary<String, Any>
  //  typealiaUIViewControllerType = UINavigationController
    func makeUIViewController(context: Context) -> UINavigationController {
        let hrVC = KuiklyRenderViewController(pageName: pageName, pageData: data)
        return UINavigationController.init(rootViewController: hrVC)
    }

    func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {

    }

    func dealloc() {

    }

}
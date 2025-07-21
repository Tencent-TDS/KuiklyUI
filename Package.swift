// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "OpenKuiklyIOSRender",
    platforms: [
        .iOS(.v12)
    ],
    products: [
        .library(
            name: "OpenKuiklyIOSRender",
            targets: ["KuiklyIOSRender"]
        ),
    ],
    targets: [
        .target(
            name: "KuiklyIOSRender",
            path: "core-render-ios",
            publicHeadersPath: "include",
        ),
    ],
)

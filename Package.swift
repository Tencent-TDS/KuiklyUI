// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "OpenKuiklyIOSRender",
    platforms: [
        .iOS(.v12),
        .macOS(.v10_13)
    ],
    products: [
        .library(
            name: "OpenKuiklyIOSRender",
            targets: ["OpenKuiklyIOSRender"]
        ),
    ],
    targets: [
        .target(
            name: "OpenKuiklyIOSRender",
            path: "core-render-ios",
            publicHeadersPath: "include",
        ),
    ],
)

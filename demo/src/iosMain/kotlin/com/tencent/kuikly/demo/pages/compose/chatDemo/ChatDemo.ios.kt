//package com.tencent.kuikly.demo.pages.compose.chatDemo
//
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.darwin.Darwin
//
//internal actual fun getPlatform(): String = "iOS"
//
//internal actual object NetworkClient {
//    actual val client: Any?
//        get() = HttpClient(Darwin)
//}
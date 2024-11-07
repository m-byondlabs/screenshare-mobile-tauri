// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/

use std::sync::mpsc;
use tauri::{ AppHandle};
use tauri_plugin_mobile_screen_capture::{MobileScreenCaptureExt};
use image::{ImageBuffer, Rgba};
use tauri::ipc::InvokeResponseBody;

use std::io;
use std::io::Write;
use std::time::Duration;
use std::time::Instant;
use std::sync::Mutex;
use std::sync::Arc;

use tauri::Emitter;
use fast_image_resize::ResizeOptions;
use webp::Encoder;
use webp::WebPMemory;
use base64::decode;

use tauri::ipc::{Channel};
use tauri::Manager;

use base64::{engine::general_purpose::STANDARD as base64, Engine};
use serde_json::Value;
use serde::Deserialize;
use serde::Serialize;
use flate2::read::GzDecoder;
use std::io::Read;

// create a callback to be used as the channel for the mobile screen capture plugin

#[derive(Clone, Serialize)]
pub(crate) struct WebViewFrame {
    pub(crate) frame: Vec<u8>, // WebP for WebView
    pub(crate) width: u32,
    pub(crate) height: u32,
}

#[derive(Clone, Serialize)]
pub(crate) struct WebRTCFrame {
    pub(crate) frame: Vec<u8>, // Raw RGBA for WebRTC
    pub(crate) width: u32,
    pub(crate) height: u32,
}

#[derive(Debug, Deserialize)]
struct JsonData {
    frame: String,
    width: u32,
    height: u32,
}

#[tauri::command]
async fn greet<R: tauri::Runtime>(
    app_handle: AppHandle<R>,
    channel: tauri::ipc::Channel<WebViewFrame>,
) -> Result<String, String> {
    let response = app_handle.mobile_screen_capture().start_screen_capture();
    Ok("Hello! You've been greeted from Rust!".to_string())
}

#[tauri::command]
async fn stop_screen_capture<R: tauri::Runtime>(
    app_handle: AppHandle<R>,
) -> Result<String, String> {
    let stop_capture_response = app_handle.mobile_screen_capture().stop_screen_capture();
    Ok("Screen capture stopped".to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    println!("pulkit run function");

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_mobile_screen_capture::init())
        .invoke_handler(tauri::generate_handler![greet, stop_screen_capture])
        .setup(|app| {
            // Store the app handle globally
            println!("pulkit app invoke_key is: {}", app.invoke_key());
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}

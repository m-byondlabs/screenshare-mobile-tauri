use tauri::ipc::Channel;
use tauri::{command, AppHandle, Runtime};

use crate::MobileScreenCaptureExt;
use crate::Result;

#[command]
pub async fn start_screen_capture<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    app.mobile_screen_capture().start_screen_capture()
}

#[command]
pub async fn stop_screen_capture<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    app.mobile_screen_capture().stop_screen_capture()
}
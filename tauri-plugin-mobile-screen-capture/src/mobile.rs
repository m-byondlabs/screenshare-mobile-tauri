use serde::de::DeserializeOwned;
use tauri::{
    ipc::{Channel, InvokeResponseBody},
    plugin::{PluginApi, PluginHandle},
    AppHandle, Runtime,
};
use serde::{Deserialize, Serialize};


use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_mobile_screen_capture);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
    _app: &AppHandle<R>,
    api: PluginApi<R, C>,
) -> crate::Result<MobileScreenCapture<R>> {
    #[cfg(target_os = "android")]
    let handle =
        api.register_android_plugin("com.plugin.bl.mobile.screen.capture", "ScreenCapturePlugin")?;
    #[cfg(target_os = "ios")]
    let handle = api.register_ios_plugin(init_plugin_mobile_screen_capture)?;
    Ok(MobileScreenCapture(handle))
}

/// Access to the mobile-screen-capture APIs.
pub struct MobileScreenCapture<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> MobileScreenCapture<R> {
    pub fn start_screen_capture(&self) -> crate::Result<()> {
        self.0
            .run_mobile_plugin("startCapture", ())
            .map_err(Into::into)
    }

    pub fn stop_screen_capture(&self) -> crate::Result<()> {
        self.0
            .run_mobile_plugin("stopCapture", ())
            .map_err(Into::into)
    }
}

#[derive(Serialize)]
struct StartScreenCaptureRequest {
    channel: Channel,
}

use serde::{de::DeserializeOwned, Serialize};

use tauri::{
    ipc::{Channel, InvokeResponseBody},
    plugin::PluginApi,
    AppHandle, Runtime,
};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
    app: &AppHandle<R>,
    _api: PluginApi<R, C>,
) -> crate::Result<MobileScreenCapture<R>> {
    Ok(MobileScreenCapture(app.clone()))
}

/// Access to the mobile-screen-capture APIs.
pub struct MobileScreenCapture<R: Runtime>(AppHandle<R>);

impl<R: Runtime> MobileScreenCapture<R> {
    pub(crate) fn start_screen_capture(&self) -> crate::Result<()> {
        Ok(())
    }

    pub fn stop_screen_capture(&self) -> crate::Result<()> {
        Ok(())
    }
}

use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::MobileScreenCapture;
#[cfg(mobile)]
use mobile::MobileScreenCapture;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the mobile-screen-capture APIs.
pub trait MobileScreenCaptureExt<R: Runtime> {
    fn mobile_screen_capture(&self) -> &MobileScreenCapture<R>;
}

impl<R: Runtime, T: Manager<R>> crate::MobileScreenCaptureExt<R> for T {
    fn mobile_screen_capture(&self) -> &MobileScreenCapture<R> {
        self.state::<MobileScreenCapture<R>>().inner()
    }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("mobile-screen-capture")
        .invoke_handler(tauri::generate_handler![
            commands::start_screen_capture,
            commands::stop_screen_capture
        ])
        .setup(|app, api| {
            #[cfg(mobile)]
            let mobile_screen_capture = mobile::init(app, api)?;
            #[cfg(desktop)]
            let mobile_screen_capture = desktop::init(app, api)?;
            app.manage(mobile_screen_capture);
            Ok(())
        })
        .build()
}

use serde::{Deserialize, Serialize};
use tauri::ipc::Channel;

#[derive(Serialize)]
pub struct StartScreenCaptureRequest {
  channel: Channel,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum FrameEvent {
    Frame(String),
    Error(String),
}
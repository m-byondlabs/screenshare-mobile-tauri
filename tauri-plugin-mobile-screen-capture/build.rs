const COMMANDS: &[&str] = &["start_screen_capture", "stop_screen_capture"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}

import { useEffect, useRef, useState } from "react";
import reactLogo from "./assets/react.svg";
import { Channel, invoke } from "@tauri-apps/api/core";
import "./App.css";

export interface FrameData {
  width: number;
  height: number;
  frame: number[];
}

function App() {
  const [greetMsg, setGreetMsg] = useState("");
  const [channel, setChannel] = useState<Channel<FrameData> | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const newChannel = new Channel<FrameData>();

    newChannel.onmessage = async (message) => {
      // convert it to Frame type
      const { width, height, frame } = message;

      setGreetMsg(frame.length.toString());
      const ctx = canvasRef.current?.getContext("2d");
      if (ctx) {
        const blob = new Blob([new Uint8Array(frame)], {
          type: "image/webp",
        });
        const imageBitmap = await createImageBitmap(blob);
        ctx.canvas.width = width;
        ctx.canvas.height = height;
        ctx.drawImage(imageBitmap, 0, 0, width, height);
      } else {
        console.error("Canvas context is null");
      }
    };

    setChannel(newChannel);

    return () => {
      setChannel(null); // Clean up when component unmounts
    };
  }, []); 

  async function greet() {
    // Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
    setGreetMsg(await invoke("greet", { channel }));
  }

  async function stop() {
    // Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
    await invoke("stop_screen_capture", {});
  }

  return (
    <main className="container">
      <h1>Welcome to Tauri + React</h1>

      <div className="row">
        <a href="https://vitejs.dev" target="_blank">
          <img src="/vite.svg" className="logo vite" alt="Vite logo" />
        </a>
        <a href="https://tauri.app" target="_blank">
          <img src="/tauri.svg" className="logo tauri" alt="Tauri logo" />
        </a>
        <a href="https://reactjs.org" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <p>Click on the Tauri, Vite, and React logos to learn more.</p>

      <form
        className="row"
        onSubmit={(e) => {
          e.preventDefault();
          greet();
        }}
      >
       
        <button type="submit">Start Share</button>
      </form>
      <br></br>
      <button type="submit" onClick={() => stop()}>Stop Share</button>
      <p>{greetMsg}</p>
      <canvas ref={canvasRef} width="800" height="600"></canvas>
    </main>
  );
}

export default App;

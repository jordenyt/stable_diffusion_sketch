#  Stable Diffusion Sketch [![Version](https://img.shields.io/badge/Version-0.17.3-blue)](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)
Do more and simpler with your [ComfyUI](https://github.com/comfyanonymous/ComfyUI) on your Android device.  Inpainting / txt2img / img2img on your sketches and photos with just a few clicks.<br/><br/>
**[Download APK](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)**

### (Important) Version
Since A1111's sd-webui has not updated to support new model, this project is switched to use ComfyUI as the main SD platform. <br/>
For the A1111 version, please check out the branch [a1111](https://github.com/jordenyt/stable_diffusion_sketch/tree/a1111).

### Notes
- The ComfyUI version requires [ComfyUI Restful API Gateway](https://github.com/jordenyt/ComfyuiGW) to work.
- You can add your own workflows in the ComfyuiGW and use it in the app.


## Screenshots
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/50681a65-53a9-4368-87ec-571fc773b674" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/b7d8002c-700d-4055-9be5-17c59683ae5a" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/a83524c2-f12d-498b-8643-dccddcc89088" height="450">
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/7cebca43-0745-4547-8b1c-70a95a65bce5" height="450"> 

## Supported Features

- Autocomplete Phrase setup
- Autocomplete for Mode definition
- Select Style for prompt
- Sketch with color
- Create new paint from:
  - Blank Canvas
  - Capture from camera
  - Output of Stable Diffusion txt2img
  - shared image from other apps
- Enhance your sketch with Stable Diffusion ComfyUI workflows
- Painting Tools:
  - Palette
  - Paintbrush
  - Eyedropper
  - Eraser
  - Undo/redo
  - Zooming / Panning
- 4 Canvas aspect ratio: wide landscape, landscape, portrait and square
- Upscaler, tag and captioning button
- Long press image on Main Screen to delete
- Group related sketches
- Keep EXIF of shared content in your SD output
- Batch size
- Import and Export Settings

## Prerequisites
Before using Stable Diffusion Sketch, you need to install and setup [ComfyUI](https://github.com/comfyanonymous/ComfyUI) and [ComfyUI Restful API Gateway](https://github.com/jordenyt/ComfyuiGW).

## Usage
Here's how to use Stable Diffusion Sketch:

1. Start the [ComfyuiGW](https://github.com/jordenyt/ComfyuiGW) on your server.
2. Download and install the Stable Diffusion Sketch APK on your Android device.
3. Open the app and input the network address of your ComfyuiGW in the "SD Server Address" field, which is default set to `http://[SERVER_IP]:5000`.
   - If both of your Android device and Server are on the same intranet, you can use the intranet IP, i.e. 192.168.xxx.xxx / 10.xxx.xxx.xxx.  You can get this IP by running `ipconfig /all` on Windows or `ifconfig --all` on MacOS/Linux.
   - If your Android device is on public internet, and your server is on intranet, you need to config your router NAT/Firewall and DDNS. In this case, use the internet IP and translated port number as the server address.
   - You can test the server by checking `http://[serverIP:serverPort]/docs` on Android device's web browser.  If it is valid, then you will see FastAPI's Swagger UI. 
4. Start sketching and let Stable Diffusion do the magic!

## License
Stable Diffusion Sketch is licensed under the [GNU General Public License v3.0](https://github.com/jordenyt/stable_diffusion_sketch/blob/main/LICENSE).

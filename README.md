#  Stable Diffusion Sketch
Stable Diffusion Sketch is an Android app that enable you run Stable Diffusion on your own server with the sketching you made on your Android device.  

## Supported Features

- Sketch with color
- Enhance your sketch with Stable Diffusion
  - Preset Modes:
    - img2img with Scribble ControlNet Model
	- img2img with Depth ControlNet Model
	- img2img with Pose ControlNet Model
	- txt2img with Scribble ControlNet Model
	- txt2img with Depth ControlNet Model
	- txt2img with Canny ControlNet Model
	- basic inpaint
	- inpaint with sketch
	- inpaint with sketch and Depth
- Sketching Tools:
  - Palette
  - Paintbrush
  - Eyedropper
  - Eraser
  - Undo/redo
- Use Camera to capture base of your sketching
- Receive Share image from other apps and use it as base.
- Preset values for your prompt
- 3 Canvas aspect ratio: landscape, portrait and square
- 2x Upscaler

## Demo Video
https://user-images.githubusercontent.com/5007252/225839650-f55a1b4b-3fa3-4181-8989-c55af844440f.mp4

## Prerequisites
Before using Stable Diffusion Sketch, you need to install and set up the following on your server:

1. [Stable Diffusion Web UI](https://github.com/AUTOMATIC1111/stable-diffusion-webui) by AUTOMATIC1111
2. Install sd-webui-controlnet extension on Stable Diffusion Web UI
3. Enable the API and listen on all network interfaces by editing the running script webui-user.bat:
`set COMMANDLINE_ARGS=--api --listen`
4. Put your perferenced SD model under stable-diffusion-webui/models/Stable-diffusion folder.  You may selected one from [Civitai](https://civitai.com/).
5. Put your perferenced ControlNet Model under stable-diffusion-webui/extensions/sd-webui-controlnet/models folder.
   - Scribble, Canny, Depth and Pose model are needed.
   - Default supported model can be download from [lllyasviel's Hugging Face card](https://huggingface.co/lllyasviel/ControlNet/tree/main/models)
   - ControlNet Model needed to match with your SD model in order to get it working.  i.e. If your ControlNet model are build for SD1.5, then your SD model need to be SD1.5 based.

## Usage
Here's how to use Stable Diffusion Sketch:

1. Start the Stable Diffusion Web UI on your server.
2. Download and install the Stable Diffusion Sketch APK on your Android device.
3. Open the app and input the network address of your Stable Diffusion server in the "Stable Diffusion Server Address" field.
   - If both of your Android device and Server are on the same intranet, you can use the intranet IP, i.e. 192.168.xxx.xxx / 10.xxx.xxx.xxx.  You can get this IP by running `ipconfig /all` on Windows or `ifconfig --all` on MacOS/Linux.
   - If your Android device is on public internet, and your server is on intranet, you need to config your router NAT/Firewall and DDNS. In this case, use the internet IP and translated port number as the server address.
   - You can test the server address by using it on Android device's web browser.  If it is valid, then you will see automatic1111's webui running on your web browser. 
4. In the app, select SD Model, Inpaint Model, Sampler, Upscaler and ControlNet model.
5. Start sketching and let Stable Diffusion do the magic!

## License
Stable Diffusion Sketch is licensed under the [GNU General Public License v3.0](https://github.com/jordenyt/stable_diffusion_sketch/blob/main/LICENSE).

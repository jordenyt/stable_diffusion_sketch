#  Stable Diffusion Sketch v0.11.0
Stable Diffusion Sketch is an Android app that enable you run Stable Diffusion on your own server with the sketching you made on your Android device.  

## Supported Features

- Sketch with color
- Create new paint from:
  - Blank Canvas
  - Capture from camera
  - Output of Stable Diffusion txt2img
  - shared image from other apps
- Enhance your sketch with Stable Diffusion
  - Preset Modes:
    - img2img(sketch) + Scribble(sketch)
    - img2img(sketch) + Depth(sketch)
    - img2img(sketch) + Pose(sketch)
    - txt2img + Canny(sketch)
    - txt2img + Scribble(sketch)
    - txt2img + Depth(sketch)
    - Inpainting (background) 
    - Inpainting (sketch)
    - Partial Inpainting (background)
  - Special Modes:
    - Outpainting
    - Fill with Reference
    - Merge with Reference
  - 5 Custom Modes
- Painting Tools:
  - Palette
  - Paintbrush
  - Eyedropper
  - Eraser
  - Undo/redo
- Preset values for your prompt
  - Prompt Prefix
  - Prompt Postfix
  - Negative Prompt
- 3 Canvas aspect ratio: landscape, portrait and square
- Upscaler
- Long press image on Main Screen to delete
- Group related sketches
- Support multiple ControlNet

## Custom Modes
Custom mode can be defined in JSON format.<br/>
Below is an example which I use to enhance the details of inpainting area : <br/>
`{"type":"inpaint", "denoise":0.35, "cfgScale":7.0, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, "cn":[{"cnInputImage":"background", "cnModelKey":"cnTileModel", "cnModule":"tile_colorfix+sharp", "cnModuleParamA":5, "cnModuleParamB":0.3, "cnWeight":1.0}]}`

### Parameters for the mode definition JSON:
| Variable         | txt2img | img2img | inpainting | Value                                                                                                            |
|------------------|---------|---------|------------|------------------------------------------------------------------------------------------------------------------|
| `type`           | M       | M       | M          | `txt2img` - Text to Image <br /> `img2img` - Image to Image <br /> `inpaint` - Inpainting                        |
| `steps`          | M       | M       | M          | integer from 1 to 120, default value is 40                                                                       |
| `cfgScale`       | M       | M       | M          | decimal from 0 to 30, default value is 7.0                                                                       |
| `denoise`        | -       | M       | M          | decimal from 0 to 1                                                                                              |
| `baseImage`      | -       | M       | M          | `background` - background image under your drawing <br/> `sketch` - your drawing on the background image         |
| `inpaintFill`    | -       | -       | M          | `0` - fill (DEFAULT) <br/> `1` - original <br/> `2` - latent noise <br/> `3` - latent nothing                    |
| `inpaintPartial` | -       | -       | O          | `0` - Inpainting on whole image (DEFAULT) <br/> `1` - Inpainting on "painted" area and paste on original image   |
| `sdSize`         | O       | O       | O          | Output resolution of SD.  Default value is configured  in setting. <br/>Suggested value: 512 / 768 / 1024 / 1280 |
| `cn`             | O       | O       | O          | JSON Array for ControlNet Object                                                                                 |
(M - Mandatory; O - Optional)

### Parameters for ControlNet Object:
| Variable         | Value                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cnInputImage`   | `background` - background image under your drawing <br/> `sketch` - your drawing and the background image <br/> `reference` - reference image                                                                                                                                                                                                                                                   |
| `cnModelKey`     | `cnTileModel` - CN Tile Model <br/> `cnPoseModel` - CN Pose Model <br/> `cnCannyModel` - CN Canny Model <br/> `cnScribbleModel` - CN Scribble Model <br/> `cnDepthModel` - CN Depth Model <br/> `cnNormalModel` - CN Normal Model <br/> `cnMlsdModel` - CN MLSD Model <br/> `cnLineartModel` - CN Line Art Model <br/> `cnSoftedgeModel` - CN Soft Edge Model <br/> `cnSegModel` - CN Seg Model |
| `cnModule`       | CN Module that ControlNet provided.  Typical values are: `tile_resample` / `reference_only` / `openpose_full` / `canny` / `depth_midas` / `scribble_hed` <br/> For full list, please refer to the Automatic1111 web UI.                                                                                                                                                                         |
| `cnControlMode`  | `0` - Balanced (DEFAULT) <br/> `1` - My prompt is more important <br/> `2` - ControlNet is more important                                                                                                                                                                                                                                                                                       |
| `cnWeight`       | decimal from 0 to 1                                                                                                                                                                                                                                                                                                                                                                             |
| `cnModuleParamA` | First Parameter for ControlNet Module                                                                                                                                                                                                                                                                                                                                                           |
| `cnModuleParamB` | Second Parameter for ControlNet Module                                                                                                                                                                                                                                                                                                                                                          |
## Screenshots
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/50681a65-53a9-4368-87ec-571fc773b674" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/b7d8002c-700d-4055-9be5-17c59683ae5a" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/a83524c2-f12d-498b-8643-dccddcc89088" height="450"> 

<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/7cebca43-0745-4547-8b1c-70a95a65bce5" height="450"> 

## Demo Video (on outdated version)
https://user-images.githubusercontent.com/5007252/225839650-f55a1b4b-3fa3-4181-8989-c55af844440f.mp4

## Preset Modes Demo
| Mode                               | Config                                                                                                                                                              | Demo Input                                                                                                                   | Demo Output                                                                                                                                                                                                                                               |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| img2img(sketch) + Scribble(sketch) | `{"baseImage":"sketch", "cn":[{"cnInputImage":"sketch", "cnModelKey":"cnScribbleModel", "cnModule":"none", "cnWeight":0.7}], "denoise":0.8, "type":"img2img"}`      | <img src="https://user-images.githubusercontent.com/5007252/228425856-3600d997-4c4d-4b03-9727-7d90bde2a528.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228426672-466d23e2-730e-4186-ab84-1658750099bb.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228426679-8dbc6c20-1e99-473f-9109-b96b2a29a542.png" width="140"> |
| img2img(sketch) + Depth(sketch)    | `{"baseImage":"sketch", "cn":[{"cnInputImage":"sketch", "cnModelKey":"cnDepthModel", "cnModule":"depth_leres", "cnWeight":1.0}], "denoise":0.8, "type":"img2img"}`  | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228435631-6500ea57-f8e5-453d-97ec-be8783e66453.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228435664-77d27294-df71-472c-8f9a-38957f7c7046.png" width="140"> |
| img2img(sketch) + Pose(sketch)     | `{"baseImage":"sketch", "cn":[{"cnInputImage":"sketch", "cnModelKey":"cnPoseModel", "cnModule":"openpose_full", "cnWeight":1.0}], "denoise":0.8, "type":"img2img"}` | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436000-e6aec212-d912-42e0-821a-0df25683ee23.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436016-9cb7f092-99b7-445f-a06a-03beedea5d7f.png" width="140"> |
| txt2img + Canny(sketch)            | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnCannyModel", "cnModule":"canny", "cnWeight":1.0}], "type":"txt2img"}`                                             | <img src="https://user-images.githubusercontent.com/5007252/228436349-638047c0-97e9-43b1-8c5a-9f8a95d6d256.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436419-1252130c-166c-462b-b4ae-b1f643492a71.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436480-9c1112ff-2517-4c26-9875-0d51ad888d7e.png" width="140"> |
| txt2img + Scribble(sketch)         | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnScribbleModel", "cnModule":"scribble_hed", "cnWeight":0.7}], "type":"txt2img"}`                                   | <img src="https://user-images.githubusercontent.com/5007252/228436349-638047c0-97e9-43b1-8c5a-9f8a95d6d256.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436620-5263c004-851a-4b95-a19b-b808a8184257.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436637-f0898e2e-d884-4a25-942c-6b3cff1a1aad.png" width="140"> |
| txt2img + Depth(sketch)            | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnDepthModel", "cnModule":"depth_leres", "cnWeight":1.0}], "type":"txt2img"}`                                       | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436747-7ec3b80a-686d-47cf-a505-c0ec27230100.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436764-d7962b77-6006-49b1-aecb-59fc3941858b.png" width="140"> |
| txt2img + Pose(sketch)             | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnPoseModel", "cnModule":"openpose_full", "cnWeight":1.0}], "type":"txt2img"}`                                      | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> |                                                                                                                                                                                                                                                           |
| Inpainting(background)             | `{"baseImage":"background", "denoise":1.0, "inpaintFill":2, "type":"inpaint"}`                                                                                      | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228437109-942bd67f-1c05-4004-874f-61d818314765.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228437128-467737fe-850d-44d3-8eb2-3fbd6c136895.png" width="140"> |
| Inpainting(sketch)                 | `{"baseImage":"sketch", "denoise":0.8, "inpaintFill":1, "type":"inpaint"}`                                                                                          | <img src="https://user-images.githubusercontent.com/5007252/228435585-62fbe0f0-1cdf-42b3-821e-9dc12fc29a21.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228437282-6acad62a-49f9-45e4-98b5-141ee02215a3.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228437310-8cc54a11-f3fc-45aa-a846-3f080994cf1c.png" width="140"> |
| Partial Inpainting (background)    | `{"baseImage":"background", "denoise":1.0, "inpaintFill":2, "inpaintPartial":1, "type":"inpaint"}`                                                                  |                                                                                                                              |                                                                                                                                                                                                                                                           |
| Outpainting                        | `{"baseImage":"background", "denoise":1.0, "inpaintFill":2, "type":"inpaint", "cfgScale":10.0}`                                                                     |                                                                                                                              |                                                                                                                                                                                                                                                           |                                                                                                                                                                                                                                                          | | | 
| Merge with Reference               | `{"baseImage":"background", "denoise":0.75, "inpaintFill":1, "type":"inpaint"}`                                                                                     |                                                                                                                              |                                                                                                                                                                                                                                                           |

## Prerequisites
Before using Stable Diffusion Sketch, you need to install and set up the following on your server:

1. [Stable Diffusion Web UI](https://github.com/AUTOMATIC1111/stable-diffusion-webui) by AUTOMATIC1111
2. Install [sd-webui-controlnet extension](https://github.com/Mikubill/sd-webui-controlnet) on Stable Diffusion Web UI
3. Enable the API and listen on all network interfaces by editing the running script webui-user.bat:
`set COMMANDLINE_ARGS=--api --listen`
4. Put your perferenced SD model under stable-diffusion-webui/models/Stable-diffusion folder.  You may selected one from [Civitai](https://civitai.com/).
5. Put your perferenced ControlNet Model under stable-diffusion-webui/extensions/sd-webui-controlnet/models folder.
   - Scribble, Canny, Depth, Tile and Pose model are needed.
   - Default supported model can be download from [lllyasviel's ControlNet v1.1 Hugging Face card](https://huggingface.co/lllyasviel/ControlNet-v1-1/tree/main)
   - ControlNet Model needed to match with your SD model in order to get it working.  i.e. If your ControlNet model are build for SD1.5, then your SD model need to be SD1.5 based.

## Usage
Here's how to use Stable Diffusion Sketch:

1. Start the Stable Diffusion Web UI on your server.
2. Download and install the Stable Diffusion Sketch APK on your Android device.
3. Open the app and input the network address of your Stable Diffusion server in the "Stable Diffusion Server Address" field.
   - If both of your Android device and Server are on the same intranet, you can use the intranet IP, i.e. 192.168.xxx.xxx / 10.xxx.xxx.xxx.  You can get this IP by running `ipconfig /all` on Windows or `ifconfig --all` on MacOS/Linux.
   - If your Android device is on public internet, and your server is on intranet, you need to config your router NAT/Firewall and DDNS. In this case, use the internet IP and translated port number as the server address.
   - You can test the server address by using it on Android device's web browser.  If it is valid, then you will see automatic1111's webui running on your web browser. 
4. In the app, select SD Model, Inpainting Model, Sampler, Upscaler and ControlNet model.
5. Start sketching and let Stable Diffusion do the magic!

## License
Stable Diffusion Sketch is licensed under the [GNU General Public License v3.0](https://github.com/jordenyt/stable_diffusion_sketch/blob/main/LICENSE).

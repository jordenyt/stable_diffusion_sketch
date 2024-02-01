#  Stable Diffusion Sketch [![Version](https://img.shields.io/badge/Version-0.16.2-blue)](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)
Do more and simpler with your [A1111 SD-webui](https://github.com/AUTOMATIC1111/stable-diffusion-webui) on your Android device.  Inpainting / txt2img / img2img on your sketches and photos with just a few clicks.<br/><br/>
**NOTES: A1111 SD-webui 1.7.0 does not support SDXL Inpainiting model currently.  Please either use the [dev branch](https://github.com/AUTOMATIC1111/stable-diffusion-webui/tree/dev) or merge [this PR](https://github.com/AUTOMATIC1111/stable-diffusion-webui/pull/14390).**<br/>
**NOTES: There are several SDXL Inpainting models on [Civitai](https://civitai.com/). For your instance, [JuggerXL_inpaint](https://civitai.com/models/245423/juggerxlinpaint) and [RealVisXL V3.0](https://civitai.com/models/139562?modelVersionId=297320) may be a good choice.**<br/>
<br/>
**[Download APK](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)**

## Screenshots
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/50681a65-53a9-4368-87ec-571fc773b674" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/b7d8002c-700d-4055-9be5-17c59683ae5a" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/a83524c2-f12d-498b-8643-dccddcc89088" height="450">
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/7cebca43-0745-4547-8b1c-70a95a65bce5" height="450"> 

## Supported Features

- Support ControlNet v1.1
- Support SDXL
- Support SDXL Turbo
- Support SDXL Inpainting
- Autocomplete LORA tag in prompt
- Autocomplete Phrase setup
- Autocomplete for Custom Mode
- Select Style for prompt
- Sketch with color
- Create new paint from:
  - Blank Canvas
  - Capture from camera
  - Output of Stable Diffusion txt2img
  - shared image from other apps
- Enhance your sketch with Stable Diffusion
  - Preset Modes:  
    - img2img(sketch) + Scribble(sketch)
    - txt2img + Canny(sketch)
    - txt2img + Scribble(sketch)
    - SDXL (turbo) txt2img
    - SDXL img2img
    - Inpainting (background) 
    - Inpainting (sketch)
    - Partial Inpainting (background)
    - Partial Inpainting (sketch)
  - Special Modes:
    - Outpainting
    - Fill with Reference
    - Merge with Reference
  - 10 Custom Modes
- Painting Tools:
  - Palette
  - Paintbrush
  - Eyedropper
  - Eraser
  - Undo/redo
  - Zooming / Panning
- Preset values for your prompt
  - Prompt Prefix
  - Prompt Postfix
  - Negative Prompt
- 4 Canvas aspect ratio: wide landscape, landscape, portrait and square
- Upscaler
- Long press image on Main Screen to delete
- Group related sketches
- Support multiple ControlNet
- Keep EXIF of shared content in your SD output
- Batch size

## Custom Modes
Custom mode can be defined in JSON format.<br/>

### Examples
1. Partial inpaint with POSE <br/>
`{"type":"inpaint","denoise":0.75, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, "cn":[{"cnInputImage":"background", "cnModelKey":"cnPoseModel", "cnModule":"openpose_full", "cnWeight":1.0, "cnControlMode":0}], "sdSize":768}`
2. Color fix <br/>
`{"type":"inpaint","denoise":0.5, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, "cn":[{"cnInputImage":"background", "cnModelKey":"cnSoftedgeModel", "cnModule":"softedge_pidinet", "cnWeight":1.0, "cnControlMode":0}], "sdSize":1024}`
3. Mild Enhance <br/>
`{"type":"inpaint","denoise":0.15, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, "cn":[{"cnInputImage":"background", "cnModelKey":"cnTileModel", "cnModule":"tile_resample", "cnModuleParamA":1, "cnWeight":1.0, "cnControlMode":0}], "sdSize":1024}`
4. Heavy Enhance <br/>
`{"type":"inpaint","denoise":0.4, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, “cn”:[{"cnInputImage":"background", "cnModelKey":"cnTileModel", "cnModule":"tile_colorfix+sharp", "cnModuleParamA":5, "cnModuleParamB":0.2, "cnWeight":1.0, "cnControlMode":0}], "sdSize":1024}`
5. Partial Redraw <br/>
`{"type":"inpaint", "denoise":0.7, "baseImage":"background", "inpaintFill":1, "inpaintPartial":1, "sdSize":1024}`
6. Get similar image <br/>
`{"type":"txt2img", "cn":[{"cnInputImage":"background", "cnModelKey":"cnNoneModel", "cnModule":"reference_only", "cnWeight":1.0, "cnControlMode":2}]}`
7. Tiles Refiner <br/>
`{"type":"img2img", "denoise":0.4, "baseImage":"background", "cn":[{ "cnInputImage":"background", "cnModelKey":"cnTileModel", "cnModule":"tile_colorfix+sharp", "cnModuleParamA":4, "cnModuleParamB":0.1, "cnWeight":1.0}], "sdSize":1024}`
8. Partial Inpaint with LORA <br/>
`{"type":"inpaint", "denoise":0.9, "cfgScale":7, "inpaintFill":1, "baseImage":"background", "sdSize":1024, "model":"v1Model"}`

### Parameters for the mode definition JSON:
| Variable         | txt2img | img2img | inpainting | Value                                                                                                            |
|------------------|---------|---------|------------|------------------------------------------------------------------------------------------------------------------|
| `name`           | O       | O       | O          | Name of this custom mode.                                                                                        |
| `prompt`         | O       | O       | O          | Postfix for this mode on prompt.                                                                                 |
| `negPrompt`      | O       | O       | O          | Postfix for this mode on negative prompt.                                                                        |
| `type`           | M       | M       | M          | `txt2img` - Text to Image <br /> `img2img` - Image to Image <br /> `inpaint` - Inpainting                        |
| `steps`          | O       | O       | O          | integer from 1 to 120, default value is 40                                                                       |
| `cfgScale`       | O       | O       | O          | decimal from 0 to 30, default value is 7.0                                                                       |
| `model`          | O       | O       | O          | `v1Model` - Default for `type`=`txt2img` and `type`=`img2img` <br/> `v1Inpaint` - Default for `type`=`inpaint` <br/> `sdxlBase` - Default for SDXL txt2img mode <br/> `sdxlInpaint` <br/> `sdxlTurbo` - Default for SDXL Turbo txt2img mode|
| `sampler`        | O       | O       | O          | Can use all samplers available in your A1111 webui.                                                              |
| `denoise`        | -       | M       | M          | decimal from 0 to 1                                                                                              |
| `baseImage`      | -       | M       | M          | `background` - background image under your drawing <br/> `sketch` - your drawing on the background image         |
| `inpaintFill`    | -       | -       | O          | `0` - fill (DEFAULT) <br/> `1` - original <br/> `2` - latent noise <br/> `3` - latent nothing                    |
| `inpaintPartial` | -       | -       | O          | `0` - Inpainting on whole image (DEFAULT) <br/> `1` - Inpainting on "painted" area and paste on original image   |
| `sdSize`         | O       | O       | O          | Output resolution of SD.  Default value is configured in setting. <br/>Suggested value: 512 / 768 / 1024 / 1280  |
| `clipSkip`       | O       | O       | O          | Clip skip for v1.5 Model.  Default value is configured in setting. <br/>Suggested value: 1-2                     |
| `cn`             | O       | O       | O          | JSON Array for ControlNet Object                                                                                 |

(M - Mandatory; O - Optional)

### Parameters for ControlNet Object:
| Variable         | Value                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cnInputImage`   | `background` - background image under your drawing <br/> `sketch` - your drawing and the background image <br/> `reference` - reference image |
| `cnModelKey`     | `cnTileModel` - CN Tile Model <br/> `cnPoseModel` - CN Pose Model <br/> `cnCannyModel` - CN Canny Model <br/> `cnScribbleModel` - CN Scribble Model <br/> `cnDepthModel` - CN Depth Model <br/> `cnNormalModel` - CN Normal Model <br/> `cnMlsdModel` - CN MLSD Model <br/> `cnLineartModel` - CN Line Art Model <br/> `cnSoftedgeModel` - CN Soft Edge Model <br/> `cnSegModel` - CN Seg Model <br/> `cnIPAdapterModel` - CN IP-Adapter Model <br/> `cnxlIPAdapterModel` - CN IP-Adapter XL Model <br/> `cnOther1Model` - Other CN Model 1 <br/> `cnOther2Model` - Other CN Model 2 <br/> `cnOther3Model` - Other CN Model 3 |
| `cnModel`        | Alternative for `cnModelKey`. Value can be any valid CN models name with hash code. |
| `cnModule`       | CN Module that ControlNet provided.  Typical values are: `tile_resample` / `reference_only` / `openpose_full` / `canny` / `depth_midas` / `scribble_hed` <br/> For full list, please refer to the Automatic1111 web UI. |
| `cnControlMode`  | `0` - Balanced (DEFAULT) <br/> `1` - My prompt is more important <br/> `2` - ControlNet is more important |
| `cnWeight`       | decimal from 0 to 1 |
| `cnResizeMode` | `0` - Just Resize <br/> `1` - Crop and Resize <br/> `2` - Resize and Fill (default) |
| `cnModuleParamA` | First Parameter for ControlNet Module |
| `cnModuleParamB` | Second Parameter for ControlNet Module |
| `cnStart` | Starting Control Step (default 0.0) |
| `cnEnd` | Ending Control Step (default 1.0) |
## Demo Video (on outdated version)
https://user-images.githubusercontent.com/5007252/225839650-f55a1b4b-3fa3-4181-8989-c55af844440f.mp4

## Preset Modes Demo
| Mode                               | Config                                                                                                                                                              | Demo Input                                                                                                                   | Demo Output                                                                                                                                                                                                                                               |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| img2img(sketch) + Scribble(sketch) | `{"baseImage":"sketch", "cn":[{"cnInputImage":"sketch", "cnModelKey":"cnScribbleModel", "cnModule":"none", "cnWeight":0.7}], "denoise":0.8, "type":"img2img"}`      | <img src="https://user-images.githubusercontent.com/5007252/228425856-3600d997-4c4d-4b03-9727-7d90bde2a528.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228426672-466d23e2-730e-4186-ab84-1658750099bb.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228426679-8dbc6c20-1e99-473f-9109-b96b2a29a542.png" width="140"> |
| txt2img + Canny(sketch)            | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnCannyModel", "cnModule":"canny", "cnWeight":1.0}], "type":"txt2img"}`                                             | <img src="https://user-images.githubusercontent.com/5007252/228436349-638047c0-97e9-43b1-8c5a-9f8a95d6d256.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436419-1252130c-166c-462b-b4ae-b1f643492a71.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436480-9c1112ff-2517-4c26-9875-0d51ad888d7e.png" width="140"> |
| txt2img + Scribble(sketch)         | `{"cn":[{"cnInputImage":"sketch", "cnModelKey":"cnScribbleModel", "cnModule":"scribble_hed", "cnWeight":0.7}], "type":"txt2img"}`                                   | <img src="https://user-images.githubusercontent.com/5007252/228436349-638047c0-97e9-43b1-8c5a-9f8a95d6d256.png" width="140"> | <img src="https://user-images.githubusercontent.com/5007252/228436620-5263c004-851a-4b95-a19b-b808a8184257.png" width="140"> <img src="https://user-images.githubusercontent.com/5007252/228436637-f0898e2e-d884-4a25-942c-6b3cff1a1aad.png" width="140"> |
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

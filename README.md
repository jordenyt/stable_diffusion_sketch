# Stable Diffusion Sketch [![Version](https://img.shields.io/badge/Version-0.18.0-blue)](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)

Transform your sketches and photos with Stable Diffusion on Android. This app integrates with ComfyUI via a RESTful API, enabling powerful AI image generation capabilities right from your mobile device.

**[Download Latest APK](https://github.com/jordenyt/stable_diffusion_sketch/releases/latest)**

## Important Note
This version uses ComfyUI as the main Stable Diffusion platform. For the Automatic1111 (A1111) version, please check the [a1111 branch](https://github.com/jordenyt/stable_diffusion_sketch/tree/a1111).

## Key Features
- 🎨 **Sketch with color** using intuitive drawing tools
- ✨ **AI-powered enhancements** via ComfyUI workflows
- 🔄 **Multiple input sources**:
  - Blank canvas
  - Camera capture
  - Stable Diffusion outputs
  - Images from other apps
- 🛠️ **Advanced tools**:
  - Color palette & eyedropper
  - Brush & eraser
  - Undo/redo
  - Zoom/pan
- ⚙️ **Customization**:
  - 4 canvas aspect ratios
  - Upscaling
  - Tagging & captioning
  - Batch processing
  - Settings import/export
- 📁 **Organization**:
  - Sketch grouping
  - Long-press to delete
  - EXIF metadata preservation

## Screenshots
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/50681a65-53a9-4368-87ec-571fc773b674" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/b7d8002c-700d-4055-9be5-17c59683ae5a" height="450"> 
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/a83524c2-f12d-498b-8643-dccddcc89088" height="450">
<img src="https://github.com/jordenyt/stable_diffusion_sketch/assets/5007252/7cebca43-0745-4547-8b1c-70a95a65bce5" height="450"> 

## Demo Video
See how to create with Stable Diffusion Sketch:  
[![Demo Video](https://github.com/user-attachments/assets/141ff9c5-23f3-4f7e-bdc6-a31d03bdf073)](https://www.youtube.com/watch?v=5jq17L05kl8)

## Prerequisites
Before using this app, you need:
1. [ComfyUI](https://github.com/comfyanonymous/ComfyUI) installed on a server
2. [ComfyUI Restful API Gateway](https://github.com/jordenyt/ComfyuiGW) running

## Setup Guide
1. **Start API Gateway**: Launch ComfyuiGW on your server
2. **Install App**: Download and install the APK on your Android device
3. **Configure Connection**:
   - Open the app and enter your server address (default: `http://[SERVER_IP]:5000`)
   - For local networks: Use your server's internal IP (e.g., 192.168.x.x)
   - For remote access: Configure router NAT/firewall and DDNS
4. **Verify Connection**: Visit `http://[serverIP:serverPort]/docs` in a browser to confirm Swagger UI loads
5. **Start Creating**: Sketch and transform images with AI!

## Configuration Levels
Parameters can be set at multiple levels for flexible workflow control:

| Level        | Location | Description |
|--------------|----------|-------------|
| Workflow     | ComfyUI `.json` | Static parameters (models, VAEs, etc.) |
| Server       | `mode_config.json` | Server-wide defaults |
| App Defaults | App settings | Device-specific defaults |
| Mode         | App mode JSON | Per-mode configuration |
| Prompt       | `<key:value>` in prompt | Runtime overrides |

### Key Parameters
| Parameter    | Description | App | Mode | Prompt |
|--------------|-------------|-----|------|--------|
| `$size`      | Output image long side | ✓ | ✓ | ✓ |
| `$width`     | Output width |   | ✓ | ✓ |
| `$height`    | Output height |   | ✓ | ✓ |
| `$batchSize` | Outputs per run | ✓ | ✓ | ✓ |
| `$cfg`       | CFG scale | ✓ | ✓ | ✓ |
| `$steps`     | Sampling steps | ✓ | ✓ | ✓ |
| `$denoise`   | Denoise strength | ✓ | ✓ | ✓ |
| `$positive`  | Positive prompt |   |   | ✓ |

For complete configuration details, see the [ComfyuiGW documentation](https://github.com/jordenyt/ComfyuiGW).

## License
This project is licensed under the [GNU General Public License v3.0](https://github.com/jordenyt/stable_diffusion_sketch/blob/main/LICENSE).

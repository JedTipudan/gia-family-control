# Android Icon Setup Instructions

The icon.jpg has been copied to all mipmap density folders.

## To properly set up the launcher icon in Android Studio:

1. Open the project in Android Studio
2. Right-click on `app/src/main/res` → New → Image Asset
3. Select "Launcher Icons (Adaptive and Legacy)"
4. For "Source Asset" → choose "Image" → browse to `icon.jpg`
5. Android Studio will auto-generate all density variants (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
6. Click "Next" → "Finish"

This will generate:
- ic_launcher.png (legacy round/square)
- ic_launcher_foreground.png (adaptive foreground layer)
- ic_launcher_background.xml (adaptive background)

## Manual PNG conversion (alternative):
Use a tool like https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
to generate all density PNGs from icon.jpg and replace the files in each mipmap folder.

## Density sizes required:
- mipmap-mdpi:    48x48 px
- mipmap-hdpi:    72x72 px
- mipmap-xhdpi:   96x96 px
- mipmap-xxhdpi:  144x144 px
- mipmap-xxxhdpi: 192x192 px

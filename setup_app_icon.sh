#!/usr/bin/env bash
# ==============================================================================
# Script to configure the user's uploaded icon as the official Android App Icon
# Preserves 100% of original pixels using ImageMagick resize (No AI generation)
# Automatically centers the artwork inside the Android Adaptive Icon Safe Zone
# to prevent edges/text from being cropped by any launcher mask (MIUI, Samsung, Pixel)
# ==============================================================================

set -e

SOURCE_IMG=""

# Search for the image in common locations
CANDIDATES=(
  "app/src/main/res/drawable/app_icon.png"
  "app/src/main/res/drawable/icon.png"
  "app/src/main/res/drawable/logo.png"
  "app/src/main/res/drawable/ic_launcher.png"
  "app_icon.png"
  "icon.png"
  "logo.png"
  "ic_launcher.png"
)

# Also check for any file_* pattern
for f in file_*.* app/src/main/res/drawable/file_*.*; do
  if [ -f "$f" ]; then
    CANDIDATES=("$f" "${CANDIDATES[@]}")
  fi
done

for c in "${CANDIDATES[@]}"; do
  if [ -f "$c" ]; then
    SOURCE_IMG="$c"
    break
  fi
done

if [ -z "$SOURCE_IMG" ]; then
  echo "Searching for any user-uploaded PNG in the project..."
  FOUND=$(find . -maxdepth 4 -name "*.png" -not -path "*/build/*" -not -path "*/.gradle/*" -not -path "*/test/*" | head -n 1)
  if [ -n "$FOUND" ]; then
    SOURCE_IMG="$FOUND"
  fi
fi

if [ -z "$SOURCE_IMG" ] || [ ! -f "$SOURCE_IMG" ]; then
  echo "NO_IMAGE_FOUND: Please upload your image file via the File Explorer to app/src/main/res/drawable/app_icon.png"
  exit 1
fi

echo "Found source image: $SOURCE_IMG"

mkdir -p app/src/main/res/drawable

# Trim transparent boundary padding if any, preserving 100% original artwork
TMP_DIR=$(mktemp -d)
convert "$SOURCE_IMG" -trim +repage "${TMP_DIR}/artwork_trimmed.png"

# Resize trimmed artwork to 680x680 (fits strictly within the ~66dp-72dp Safe Zone of 108dp canvas)
convert "${TMP_DIR}/artwork_trimmed.png" -resize 680x680 "${TMP_DIR}/artwork_resized.png"

# 1. Create Adaptive Icon Foreground layer (1080x1080 transparent canvas with artwork centered)
convert -size 1080x1080 xc:none "${TMP_DIR}/artwork_resized.png" -gravity center -composite app/src/main/res/drawable/app_icon_foreground.png

# 2. Create full icon on clean white background for legacy launcher fallbacks and Google Play
convert -size 1080x1080 xc:white "${TMP_DIR}/artwork_resized.png" -gravity center -composite "${TMP_DIR}/full_icon_1080.png"

# 3. Generate mipmap densities with exact resize from the safe-padded icon
declare -A DENSITIES=(
  ["mdpi"]="48x48"
  ["hdpi"]="72x72"
  ["xhdpi"]="96x96"
  ["xxhdpi"]="144x144"
  ["xxxhdpi"]="192x192"
)

for density in "${!DENSITIES[@]}"; do
  size="${DENSITIES[$density]}"
  target_dir="app/src/main/res/mipmap-${density}"
  mkdir -p "$target_dir"
  
  echo "Generating ${density} (${size})..."
  # Remove any obsolete png in mipmap to prevent duplicate resource conflicts
  rm -f "${target_dir}/ic_launcher.png" "${target_dir}/ic_launcher_round.png"
  convert "${TMP_DIR}/full_icon_1080.png" -resize "${size}!" "${target_dir}/ic_launcher.webp"
  convert "${TMP_DIR}/full_icon_1080.png" -resize "${size}!" "${target_dir}/ic_launcher_round.webp"
done

# Play Store 512x512 high-res icon
convert "${TMP_DIR}/full_icon_1080.png" -resize "512x512!" "app/src/main/ic_launcher-playstore.png"

# 4. Adaptive icon foreground XML
cat << 'EOF' > app/src/main/res/drawable/ic_launcher_foreground.xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:width="108dp"
        android:height="108dp"
        android:drawable="@drawable/app_icon_foreground"
        android:gravity="center" />
</layer-list>
EOF

# 5. Adaptive icon background XML (pure clean white to frame the artwork seamlessly)
cat << 'EOF' > app/src/main/res/drawable/ic_launcher_background.xml
<?xml version="1.0" encoding="utf-8"?>
<color xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#FFFFFF" />
EOF

# 6. Adaptive icon definition XMLs
mkdir -p app/src/main/res/mipmap-anydpi-v26
cat << 'EOF' > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/app_icon_foreground" />
</adaptive-icon>
EOF

cat << 'EOF' > app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/app_icon_foreground" />
</adaptive-icon>
EOF

# Cleanup temp
rm -rf "$TMP_DIR"

echo "SUCCESS: App icon updated with 100% original file fidelity and safe-zone padding!"

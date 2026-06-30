import os
import zipfile
import math
import sys
import random

# Ensure Pillow is installed
try:
    from PIL import Image, ImageDraw
except ImportError:
    print("La librería 'Pillow' no está instalada. Instalándola automáticamente...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageDraw

# Color Palette: Paper & Graphite (B&W photocopy aesthetic)
BG_PAPER = (248, 246, 240)    # Warm off-white paper texture
LINE_CHARCOAL = (35, 35, 35)   # Dark graphite line
SHADE_LIGHT = (225, 222, 214)  # Light graphite shading
SHADE_MID = (195, 192, 183)    # Medium shading
SHADE_DARK = (155, 151, 142)   # Dark shading

def draw_clean_line(draw, x1, y1, x2, y2, width=2):
    # Draws a clean, straight line with no deviations
    draw.line([x1, y1, x2, y2], fill=LINE_CHARCOAL, width=width)

def draw_clean_polygon(draw, points, fill=None, outline_width=2):
    # Fills polygon with flat texture first
    if fill:
        draw.polygon(points, fill=fill)
    # Draws perfectly straight outline
    draw.polygon(points, outline=LINE_CHARCOAL, width=outline_width)

def apply_paper_photocopy_texture(img):
    # Adds photocopy toner dust, scan noise, and print grain
    pixels = img.load()
    width, height = img.size
    for y in range(height):
        for x in range(width):
            r, g, b = pixels[x, y]
            # Paper grain noise
            noise = random.randint(-5, 5)
            # Photocopy speckle noise
            if random.random() < 0.0006:
                noise -= 70 # dark toner dust
            elif random.random() < 0.0002:
                noise += 40 # light speckle
            
            r = max(0, min(255, r + noise))
            g = max(0, min(255, g + noise))
            b = max(0, min(255, b + noise))
            pixels[x, y] = (r, g, b)

def draw_s1a(draw, num):
    # S1A: 2D geometric shapes (perfect lines)
    center = (150, 150)
    radius = 60
    
    if num % 3 == 0:
        # Triangle
        points = [(150, 80), (80, 220), (220, 220)]
        draw_clean_polygon(draw, points, fill=SHADE_LIGHT)
    elif num % 3 == 1:
        # Circle
        draw.ellipse([150-radius, 150-radius, 150+radius, 150+radius], fill=SHADE_LIGHT, outline=LINE_CHARCOAL, width=2)
    else:
        # Pentagon / Hexagon
        points = []
        sides = 5 + (num % 2)
        for i in range(sides):
            angle = i * (2 * math.pi / sides) - math.pi / 2
            x = center[0] + radius * math.cos(angle)
            y = center[1] + radius * math.sin(angle)
            points.append((x, y))
        draw_clean_polygon(draw, points, fill=SHADE_LIGHT)

def draw_s2(draw, num):
    # S2: Rotated angles / Displacement (perfect lines)
    center = (150, 150)
    angle = (num * 15) * math.pi / 180
    
    x1 = center[0] - 80 * math.cos(angle)
    y1 = center[1] - 80 * math.sin(angle)
    x2 = center[0] + 80 * math.cos(angle)
    y2 = center[1] + 80 * math.sin(angle)
    
    draw_clean_line(draw, x1, y1, x2, y2, width=4)
    
    # Arrow head
    head_angle1 = angle + 5 * math.pi / 6
    head_angle2 = angle - 5 * math.pi / 6
    hx1 = x2 + 25 * math.cos(head_angle1)
    hy1 = y2 + 25 * math.sin(head_angle1)
    hx2 = x2 + 25 * math.cos(head_angle2)
    hy2 = y2 + 25 * math.sin(head_angle2)
    draw_clean_line(draw, x2, y2, hx1, hy1, width=4)
    draw_clean_line(draw, x2, y2, hx2, hy2, width=4)

def draw_cube(draw, cx, cy, size):
    # 3D isometric cube with perfect lines
    # Top face
    t_poly = [
        (cx, cy - size),
        (cx + size * 0.866, cy - size * 0.5),
        (cx, cy),
        (cx - size * 0.866, cy - size * 0.5)
    ]
    draw_clean_polygon(draw, t_poly, fill=SHADE_LIGHT, outline_width=1)
    
    # Left face
    l_poly = [
        (cx - size * 0.866, cy - size * 0.5),
        (cx, cy),
        (cx, cy + size),
        (cx - size * 0.866, cy + size * 0.5)
    ]
    draw_clean_polygon(draw, l_poly, fill=SHADE_MID, outline_width=1)
    
    # Right face
    r_poly = [
        (cx, cy),
        (cx + size * 0.866, cy - size * 0.5),
        (cx + size * 0.866, cy + size * 0.5),
        (cx, cy + size)
    ]
    draw_clean_polygon(draw, r_poly, fill=SHADE_DARK, outline_width=1)

def draw_s1b(draw, num):
    # S1B: 3D piles of bricks / cubes (perfect lines)
    size = 24
    positions = [
        (150, 200), # base center
        (150 - 41.5, 200 - 24), # base left
        (150 + 41.5, 200 - 24), # base right
        (150, 152), # middle
        (150 - 41.5, 152 - 24), # top left
        (150 + 41.5, 152 - 24), # top right
        (150, 104) # peak
    ]
    
    cubes_to_draw = 2 + (num % 6)
    for i in range(min(cubes_to_draw, len(positions))):
        cx, cy = positions[i]
        draw_cube(draw, cx, cy, size)

def main():
    zip_filename = "bfa_test_images.zip"
    print("Generando 110 imágenes de test con líneas de alta fidelidad y textura de fotocopia...")
    
    temp_dir = "temp_images"
    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs(os.path.join(temp_dir, "s1a"), exist_ok=True)
    os.makedirs(os.path.join(temp_dir, "s2"), exist_ok=True)
    os.makedirs(os.path.join(temp_dir, "s1b"), exist_ok=True)
    
    # 1. Generate S1A (27 items)
    for i in range(1, 28):
        img = Image.new("RGB", (300, 300), BG_PAPER)
        draw = ImageDraw.Draw(img)
        draw_s1a(draw, i)
        apply_paper_photocopy_texture(img)
        img.save(os.path.join(temp_dir, "s1a", f"item_{i}.png"))
        
    # 2. Generate S2 (34 items)
    for i in range(1, 35):
        img = Image.new("RGB", (300, 300), BG_PAPER)
        draw = ImageDraw.Draw(img)
        draw_s2(draw, i)
        apply_paper_photocopy_texture(img)
        img.save(os.path.join(temp_dir, "s2", f"item_{i}.png"))
        
    # 3. Generate S1B (49 items)
    for i in range(1, 50):
        img = Image.new("RGB", (300, 300), BG_PAPER)
        draw = ImageDraw.Draw(img)
        draw_s1b(draw, i)
        apply_paper_photocopy_texture(img)
        img.save(os.path.join(temp_dir, "s1b", f"item_{i}.png"))
        
    # Create ZIP file
    print(f"Empaquetando en archivo {zip_filename}...")
    with zipfile.ZipFile(zip_filename, 'w') as zipf:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                filepath = os.path.join(root, file)
                arcname = os.path.relpath(filepath, temp_dir)
                zipf.write(filepath, arcname)
                
    # Extra option: Extract directly into the project's static image folder
    target_path = os.path.join("src", "main", "resources", "static", "img")
    print(f"Extrayendo directamente en la carpeta del proyecto: {target_path} ...")
    os.makedirs(target_path, exist_ok=True)
    with zipfile.ZipFile(zip_filename, 'r') as zipf:
        zipf.extractall(target_path)
        
    # Cleanup temp folder
    import shutil
    shutil.rmtree(temp_dir)
    print("\n¡Listo! Las imágenes tienen líneas rectas de alta precisión y textura de fotocopia.")
    print("Se ha guardado 'bfa_test_images.zip' en la raíz.")

if __name__ == "__main__":
    main()

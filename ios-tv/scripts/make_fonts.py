"""Builds the font files the launcher expects into launcher/app/src/main/res/font/.

Apple's SF fonts are not redistributed in this repo (their license does not allow it). This script downloads them from
https://github.com/bradleyhodges/SFWindows, for your own use, and subsets them (Latin only, ~35 KB each) so the launcher
stays small. Needs: pip install fonttools

    python ios-tv/scripts/make_fonts.py
"""
import os, subprocess, sys, urllib.request

BASE = "https://github.com/bradleyhodges/SFWindows/raw/HEAD/SF%20Pro/"
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "launcher", "app", "src", "main", "res", "font")
TMP = os.path.join(os.path.dirname(__file__), "_sf_tmp")
UNI = "U+0020-007E,U+00A0-00FF,U+2013,U+2014,U+2018,U+2019,U+201C,U+201D,U+2022,U+2026,U+00B7"


def fetch(name):
    os.makedirs(TMP, exist_ok=True)
    path = os.path.join(TMP, name)
    if not os.path.exists(path):
        print("downloading", name)
        urllib.request.urlretrieve(BASE + name.replace(" ", "%20"), path)
    return path


def subset(src, dst, unicodes=UNI, features="kern,tnum"):
    subprocess.check_call([sys.executable, "-m", "fontTools.subset", src, f"--unicodes={unicodes}",
                           f"--layout-features={features}", f"--output-file={dst}"])


os.makedirs(OUT, exist_ok=True)
for weight, out in (("Ultralight", "sf_ultralight.otf"), ("Light", "sf_light.otf"), ("Regular", "sf_regular.otf"),
                    ("Medium", "sf_medium.otf"), ("Semibold", "sf_semibold.otf")):
    subset(fetch(f"SF-Pro-Display-{weight}.otf"), os.path.join(OUT, out))

# The clock: the variable SF Pro at width 47 (Compressed), weight 1000 (Black), digits only.
var = fetch("SF-Pro.ttf")
inst = os.path.join(TMP, "sfp_compressed_black.ttf")
subprocess.check_call([sys.executable, "-m", "fontTools.varLib.instancer", var, "wdth=47", "wght=1000", "opsz=28", "-o", inst, "-q"])
subset(inst, os.path.join(OUT, "sf_compressed_black.ttf"), unicodes="U+0030-003A,U+0020", features="")
print("done: fonts written to", os.path.abspath(OUT))

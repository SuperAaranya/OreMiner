import subprocess
import sys
from pathlib import Path

menu_script = Path(__file__).resolve().parent / "Menu" / "main.py"
if not menu_script.exists():
    raise SystemExit(f"Menu launcher not found: {menu_script}")

raise SystemExit(subprocess.run([sys.executable, str(menu_script)]).returncode)
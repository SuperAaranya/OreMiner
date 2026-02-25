import os
import subprocess
import sys
from pathlib import Path
import tkinter as tk
from tkinter import messagebox

RELEASES = [
    ("Snapshot 1.4.1", "Beta Snapshot"),
    ("Release 1.4", "Full Release"),
    ("Release Candidate", "Release Canidate"),
]


def get_project_root() -> Path:
    return Path(__file__).resolve().parent.parent


def get_release_dir(relative_name: str) -> Path:
    return get_project_root() / relative_name


def build_classpath() -> str:
    return os.pathsep.join([".", "json-simple-1.1.1.jar", "gson-2.10.1.jar"])


def launch_release(relative_name: str) -> None:
    release_dir = get_release_dir(relative_name)
    source_file = release_dir / "oreminer.java"
    if not release_dir.exists():
        messagebox.showerror("Missing Folder", f"Folder not found:\n{release_dir}")
        return
    if not source_file.exists():
        messagebox.showerror("Missing Source", f"Source file not found:\n{source_file}")
        return

    required_jars = [release_dir / "json-simple-1.1.1.jar", release_dir / "gson-2.10.1.jar"]
    missing_jars = [str(jar) for jar in required_jars if not jar.exists()]
    if missing_jars:
        messagebox.showerror("Missing Libraries", "Required jar files are missing:\n" + "\n".join(missing_jars))
        return

    classpath = build_classpath()
    try:
        compile_result = subprocess.run(
            ["javac", "-cp", classpath, "oreminer.java"],
            cwd=release_dir,
            capture_output=True,
            text=True,
            check=False,
        )
    except FileNotFoundError:
        messagebox.showerror("JDK Not Found", "javac was not found in PATH. Install JDK and try again.")
        return

    if compile_result.returncode != 0:
        error_text = compile_result.stderr.strip() or "Compilation failed with no stderr output."
        messagebox.showerror("Compilation Failed", error_text)
        return

    try:
        subprocess.Popen(["java", "-cp", classpath, "oreminer"], cwd=release_dir)
    except FileNotFoundError:
        messagebox.showerror("Java Not Found", "java was not found in PATH. Install JDK and try again.")
        return

    root.destroy()


def create_ui() -> tk.Tk:
    window = tk.Tk()
    window.title("OreMiner 1.4 Launcher")
    window.geometry("360x230")
    window.resizable(False, False)

    title = tk.Label(window, text="Choose a Build", font=("Arial", 16, "bold"))
    title.pack(pady=14)

    subtitle = tk.Label(window, text="Compile and run from this launcher")
    subtitle.pack(pady=(0, 14))

    for label, path_name in RELEASES:
        button = tk.Button(
            window,
            text=label,
            width=24,
            height=1,
            command=lambda p=path_name: launch_release(p),
        )
        button.pack(pady=5)

    return window


if __name__ == "__main__":
    root = create_ui()
    root.mainloop()
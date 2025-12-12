import tkinter as tk
import subprocess
import os
import sys

def run_java_project(release_path):
    root.destroy()
    try:
        current_dir = os.path.dirname(os.path.abspath(__file__))
        oreminer_root = os.path.dirname(current_dir)
        java_dir = os.path.join(oreminer_root, release_path)
        
        classpath = ".;json-simple-1.1.1.jar .;gson-2.10.1.jar"
        
        print("Compiling and Running Java Project...")
        
        compile_command = ["javac", "-cp", classpath, "oreminer.java"]
        
        compile_process = subprocess.run(compile_command, cwd=java_dir, check=True, capture_output=True, text=True)
        print("Compilation successful.")
        
        run_command = ["java", "-cp", classpath, "oreminer"]
        subprocess.Popen(run_command, cwd=java_dir)
        
    except subprocess.CalledProcessError as e:
        error_message = f"Java Compilation Failed in {java_dir}:\n{e.stderr}"
        tk.messagebox.showerror("Error", error_message)
        print(error_message)
        sys.exit(1)
    except FileNotFoundError:
        error_message = f"Java or Javac not found. Ensure Java Development Kit (JDK) is installed and in your PATH."
        tk.messagebox.showerror("Error", error_message)
        print(error_message)
        sys.exit(1)

def run_beta():
    run_java_project("Beta Snapshot")

def run_full():
    run_java_project("Full Release")

root = tk.Tk()
root.title("OreMiner Menu")
root.geometry("300x150")
root.resizable(False, False)

title_label = tk.Label(root, text="Select Release Version", font=("Arial", 14))
title_label.pack(pady=10)

beta_button = tk.Button(root, text="Beta Snapshot", width=20, command=run_beta)
beta_button.pack(pady=5)

full_button = tk.Button(root, text="Full Release", width=20, command=run_full)
full_button.pack(pady=5)

root.mainloop()
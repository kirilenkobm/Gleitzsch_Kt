#!/usr/bin/env python3
import os
import subprocess
from datetime import datetime as dt
import sys

INPUT_IMAGES_DIR = "input_images"
OUTPUT_IMAGES_DIR = "output_images"
JAR_PATH = "out/artifacts/Gleitzsch_Kt_jar/Gleitzsch_Kt.jar"


def main():
    """Entry point."""
    # if additional arguments to test on glitcher are provided:
    glitcher_args = sys.argv[1:] if len(sys.argv) > 1 else None
    input_filenames = [fname for fname in os.listdir(INPUT_IMAGES_DIR) if fname.endswith(".jpg")]
    print(f"# {len(input_filenames)} images to process")
    t0 = dt.now()

    for input_filename in input_filenames:
        print(f"# Processing image {input_filename}")
        start_time = dt.now()
        command = [
            "java",
            "-jar",
            JAR_PATH,
            os.path.join(INPUT_IMAGES_DIR, input_filename),
            os.path.join(OUTPUT_IMAGES_DIR, input_filename),
        ]
        if glitcher_args:
            command.extend(glitcher_args)

        process = subprocess.run(command, check=True, stdout=sys.stdout, stderr=sys.stderr)
        image_processing_time = dt.now() - start_time
        print(f"# Image processed in: {image_processing_time}")
    tot_runtime = dt.now() - t0
    print(f"### Total test runtime: {tot_runtime}")


if __name__ == "__main__":
    main()

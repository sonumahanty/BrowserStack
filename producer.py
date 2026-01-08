import time
import random

log_file = "test.log"

print(f"Appending logs to {log_file}...")
try:
    with open(log_file, "a") as f:
        # Initial burst
        for i in range(15):
            line = f"Initial Load {i} at {time.strftime('%H:%M:%S')}\n"
            f.write(line)
        f.flush()
        
        while True:
            line = f"Live Update {random.randint(1000, 9999)} at {time.strftime('%H:%M:%S')}\n"
            f.write(line)
            f.flush()
            time.sleep(1)
except KeyboardInterrupt:
    print("Stopped.")

"""Disposable integration fixture: exercise Java Console on a real pseudo-terminal, never a real password."""
import errno
import os
import select
import signal
import sys
import time

secret = b"synthetic-cli-password"
pid, terminal = os.forkpty()
if pid == 0:
    os.execvpe(sys.argv[1], sys.argv[1:], os.environ)
output = bytearray()
stage = 0
deadline = time.monotonic() + 60
status = None
try:
    while time.monotonic() < deadline:
        if select.select([terminal], [], [], 0.1)[0]:
            try:
                chunk = os.read(terminal, 8192)
            except OSError as error:
                if error.errno == errno.EIO:
                    break
                raise
            if not chunk:
                break
            output.extend(chunk)
            if stage == 0 and b"Password: " in output:
                os.write(terminal, secret + b"\n")
                stage = 1
            if stage == 1 and b"Confirm password: " in output:
                os.write(terminal, secret + b"\n")
                stage = 2
    else:
        os.kill(pid, signal.SIGKILL)
        raise RuntimeError("Private setup command timed out")
finally:
    os.close(terminal)
    _, status = os.waitpid(pid, 0)
if secret in output:
    raise RuntimeError("Password unexpectedly appeared in terminal output")
sys.stdout.buffer.write(output)
if stage != 2:
    raise RuntimeError("Private setup did not prompt twice")
sys.exit(os.waitstatus_to_exitcode(status))

import threading
import serial

class ReadingThread (threading.Thread):
    def __init__(self, threadID, name, serial_port, data_to_write, continue_loop):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.name = name
        self.serial_port = serial_port
        self.data_to_write = data_to_write
        self.continue_loop = continue_loop

    def run(self):
        with self.serial_port:
            while self.continue_loop:
                if self.serial_port.waiting() > 0:
                    line = self.serial_port.readline().decode()
                    self.data_to_write.append(line)

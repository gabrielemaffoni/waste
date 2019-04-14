import threading
from queue import Queue

data_to_read = []
global data_counter


class FirstThread(threading.Thread):

    def __init__(self, thread_id, name, data_input,):
        threading.Thread.__init__(self)
        self.thread_id = thread_id
        self.name = name
        self.data_input = data_input

    def run(self):
        global data_to_read
        global data_counter
        data_counter = 0
        while data_counter <= 20:
            self.data_input = input('Input a number: ')
            data_to_read.append(self.data_input)
            data_counter += 1
            print("data to read %s" % data_to_read)
            print("Length 1: %s" % len(data_to_read))


class SecondThread(threading.Thread):

    def __init__(self, thread_id, name, number_to_analyse, reader_counter):
        threading.Thread.__init__(self)
        self.thread_ide = thread_id
        self.name = name
        self.number_to_analyse = number_to_analyse
        self.reader_counter = reader_counter

    def run(self):
        global data_to_read
        while data_counter <= 20:
            if len(data_to_read) > 0:
                print("data_to_read" % data_to_read)
                print("data_counter %s" % data_counter)
                value = data_to_read[self.reader_counter]
                self.reader_counter += 1
                new_value = int(value) + self.number_to_analyse
                print("Input value was %s, New value is %s" % (value, new_value))


if __name__ == '__main__':
    data_counter = 0
    queue_3 = Queue
    thread_1 = FirstThread(1, 'First Thread', 'Hello world')
    thread_2 = SecondThread(2, 'Second thread', 750, 0)
    thread_1.start()
    thread_2.start()

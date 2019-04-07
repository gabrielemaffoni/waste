import platform
import time

import serial

from Item import *

CHECK_DATA = "data"
LUMINOSITY_CHECK = "luminosity"
TEMPERATURE_CHECK = "temperature"
DOOR_CLOSED = "door_closed"
QUANTITY_CHECK = "quantity"
RESET_BUTTON = "Reset mode"
LIGHT_ISSUE = "Light issue"
NO_ALARM = "No alarm"
EMERGENCY_MODE = "light_emergency"
EMERGENCY_MODE_OFF = "light_emergency_off"

config = {
    "apiKey": "AIzaSyBPp6hBybehlRJ7YFyN0ph_znlkV-1Xw1c",
    "authDomain": "waste-c86f8",
    "databaseURL": "https://waste-c86f8.firebaseio.com/",
    "storageBucket": "gs://waste-c86f8.appspot.com"
}

normal_seconds_check = 180
firebase = pyrebase.initialize_app(config)
database = firebase.database()
auth = firebase.auth()
user_id = ""
user = ""
user_email = ""
user_password = ""
counter_luminosity = 0
global light_issue
global data_request
global port
global serialPort
global tmp_item
global first_setup


def initialise_database_and_login():
    global user
    global user_email
    global user_password
    global user_id
    user_email = input('Please insert your email ')
    user_password = input('Please insert your password ')
    user = auth.sign_in_with_email_and_password(user_email, user_password)
    print("Logged")
    user_id = user['localId']
    print("Initialised!")


def set_check_time():
    last_check = datetime.datetime.now()
    print("It's now %s" % last_check)
    new_check = last_check + datetime.timedelta(seconds=30)
    print("I'm gonna check at %s" % new_check)
    return new_check


def send_request(message):
    print(message)
    serialPort.write(message.encode())


def read_all_startup_lines():
    startup_boolean = True
    print("Warming up the arduino")
    counter = 0
    while startup_boolean:
        print('Skipping %i lines' % counter, end='\r', flush=True)
        try:
            if serialPort.inWaiting() > 0:
                startup_line = serialPort.readline().decode()
                print(startup_line, flush=True)
                counter += 1
                if 'SETUP DONE' in startup_line:
                    print('Arduino is warm enough')
                    startup_boolean = False
        except serial.SerialException:
            print("Error %s" % serial.SerialException.strerror)


def upload_all_data():
    database.child('items').child(user_id).child(tmp_item.key).set(tmp_item.upload_all_data())


def update_item():
    database.child('items').child(user_id).child(tmp_item.key).update(tmp_item.data_to_update())


def upload_item():
    database.child('items').child(user_id).child(tmp_item.key).update(tmp_item.data_to_update())


def copy_data(data_to_copy):
    print("Copying data")
    for s_line in data_to_copy:
        column = s_line.split(":")[0]
        line_clean = s_line.split(":")[1]
        print("%s : %s" % (column, line_clean))
        if 'int_temperature' in column:
            tmp_item.current_temperature = float(line_clean.strip(","))
        if 'ext_temperature' in column:
            tmp_item.external_temperature = float(line_clean.strip(","))
        if 'quantity_left' in column:
            tmp_item.quantity_left = int(line_clean.strip(","))
        if 'luminosity' in column:
            tmp_item.luminosity = float(line_clean)
        if 'stream_number' in column:
            tmp_item.stream_number = int(line_clean.strip(","))


def first_setup_command():
    item_key = database.child('items').child(user_id).generate_key()
    new_product_data = {'new_product': 'yes', 'generated' : 'first_setup_command'}
    database.child(item_key).set(new_product_data)
    return item_key


def wait_for_data():
    keep_waiting = True
    while keep_waiting:
        database_to_check = database.child('items').child(user_id).child(tmp_item.key).get()
        print(database_to_check.val())
        is_it_placed = database_to_check.val()['new_product']
        # print(is_it_placed)
        if 'placed' in is_it_placed:
            keep_waiting = False
        else:
            time.sleep(1)


def set_optimal_temperature():
    optimal_tmp = float(database.child('items').child(user_id).child(tmp_item.key).child('opt_temperature').get().val())
    tmp_item.optimal_temperature = optimal_tmp


def read_data_emergency():
    print("Reading data")
    global line
    global first_setup
    global light_issue
    data_reading = []
    time.sleep(5)
    if serialPort.inWaiting() > 0:
        while 'LIGHT_ON' in line:
            line = serialPort.readline().decode()
            print("Line is: %s" % line)
            continue
        if "{" in line:
            start_counting = True
            while start_counting:
                new_line = serialPort.readline().decode()
                print("New line is: %s" % new_line)
                # Decodes data
                if "}" in new_line:
                    start_counting = False
                    break
                elif "RESET_MODE" in line:
                    first_setup = True
                    break
                else:
                    # Add the decoded data to a new type of line
                    data_for_item = new_line.strip('\r\n')
                    data_reading.append(data_for_item)

        if "RESET_MODE" in line:
            first_setup = True
            light_issue = False

        if "LIGHT_OFF" in line:
            light_issue = False
        else:
            print("No {")
    else:
        print("Serial not ready")
    return data_reading


def read_data():
    print("Reading data")
    global line
    global first_setup
    global light_issue
    data_reading = []
    time.sleep(5)
    if serialPort.inWaiting() > 0:
            line = serialPort.readline().decode()
            print("Line is: %s" % line)
            if "{" in line:
                start_counting = True
                while start_counting:
                    new_line = serialPort.readline().decode()
                    print("New line is: %s" % new_line)
                    # Decodes data
                    if "}" in new_line:
                        start_counting = False
                        break
                    elif "RESET_MODE" in line:
                        first_setup = True
                        break
                    else:
                        # Add the decoded data to a new type of line
                        data_for_item = new_line.strip('\r\n')
                        data_reading.append(data_for_item)

            if "RESET_MODE" in line:
                first_setup = True
                light_issue = False

            if "LIGHT_OFF" in line:
                light_issue = False
            else:
                print("No {")
    else:
        print("Serial not ready")
    return data_reading


def emergency_mode():
    global light_counter
    global light_issue
    while light_issue == True:
        print("Emergency mode")
        send_request(CHECK_DATA)
        data_to_upload = read_data_emergency()
        if light_issue:
            copy_data(data_to_upload)
            upload_all_data()
        time.sleep(5)
        if float(tmp_item.current_temperature) < tmp_item.optimal_temperature:
            light_counter = 0
            light_issue = False
            break


def set_expiration_date():
    expiration_date = database.child('items').child(user_id).child(tmp_item.key).child(EXPIRATION_DATE).get().val()
    exp_date = datetime.datetime.strptime(expiration_date.__str__(), '%d/%m/%Y')
    difference = (exp_date - datetime.datetime.now()).days
    tmp_item.expiration_date = exp_date
    send_request('expiration')
    time.sleep(10)
    send_request(str(difference))


if __name__ == '__main__':
    global line
    light_issue = False
    system = platform.system()
    port = ''
    light_counter = 0
    print(system)
    first_setup = False
    starting_point = datetime.datetime.now()
    print("Program started. It's %s" % starting_point.strftime('%d/%m/%y - %H:%M:%S'))
    new_check_time = set_check_time()
    initialise_database_and_login()

    if 'Linux' in system:
        port = '/dev/ttyACM0'
    elif 'Darwin' in system:
        print(system)
        port = '/dev/cu.usbmodem14201'
    serialPort = serial.Serial(port=port, baudrate=115200)
    first_setup = False
    counter = 0
    with serialPort:
        read_all_startup_lines()
        while True:
            line = ""
            #If the Arduino says something
            if serialPort.inWaiting() > 0:
                line = serialPort.readline().decode().strip('\r\n')
                print(line)
                #The raspberry will analyses it
                if 'FIRST_SETUP' in line:
                    first_setup = True
                #Door is open. Start counting if needed.
                if 'LIGHT_ON' in line:
                    light_counter += 1
                #Door has been closed again. No problem.
                if 'LIGHT_OFF' in line:
                    light_counter = 0

                if first_setup:
                    tmp_item = Item()
                    #Getting the key of the item
                    tmp_item.key = first_setup_command()
                    print("Key: %s" % tmp_item.key)
                    # Waits for the answer from the database
                    wait_for_data()
                    #Sends the requests of the data to the Arduino
                    send_request(CHECK_DATA)
                    #Reads the data requested
                    data_gotten = read_data()
                    #Copies the data on the Item object and uploads them on the database
                    copy_data(data_gotten)
                    update_item()
                    #Gets the optimal temperature from the database
                    set_optimal_temperature()
                    #Establishes when it is going to check the data next
                    new_check_time = set_check_time()
                    #Ends the setup
                    first_setup = False
                    print('Ended first setup!')
                    data_request = False
                    #Tells the Arduino how many days left to the expiration date
                    set_expiration_date()

            if 0 < light_counter < 5:
                time.sleep(1)

            elif light_counter >= 5:
                light_issue = True
                emergency_mode()
                light_counter = 0
                print("No more alarm")

            time_difference = new_check_time - datetime.datetime.now()
            if counter % 6 == 0:
                print(time_difference.total_seconds(), flush=True)
                counter += 1

            if time_difference.total_seconds() <= 0:
                send_request(CHECK_DATA)
                data_gotten = read_data()
                update_item()
                new_check_time = set_check_time()
                time.sleep(5)
"""
Coded with <3 by Gabriele Maffoni.
Ready for open source project, though please quote.
If there is any, no puns inteded.

This code is intended to be used with a raspberry pi with a USB Bluetooth Dongle by DFRobot called "BLUNO LINK".
It is the only one that works with Bluno Beetle and you can purchase it to this link: https://www.dfrobot.com/product-1220.html
Thanks to that dongle you don't have to manage bluetooth connection, but you will communicate through Serial monitor.

Requirements:
- Wi-Fi antenna
- Bluno Link
- Firebase account -> It has free packages, so don't worry.
- Python3

VERSION: 10/4/2019

CHANGELOG:
- Added documentation
- Variables polishing. Deleted unused methods.
- Added clearer printings
"""
import json
import threading # To use threads
import getpass # To securely input password
import pyrebase #Needed to connect to database
import platform  # Needed to find what the port is
import time  # Needed to find out the time lag between processes
import serial  # Needed to communicate with Bluno Beetle
from Item import *  # Needed to get constants from custom Item class

"""
CONSTANTS.
These are the ways Arduino and Raspberry will communicate.
The Arduino will interpret all Strings and act as consequence.
"""

CHECK_DATA = "data"
LUMINOSITY_CHECK = "luminosity"
TEMPERATURE_CHECK = "temperature"
DOOR_CLOSED = "door_closed"
QUANTITY_CHECK = "quantity"
RESET_BUTTON = "Reset mode"
LIGHT_ISSUE = "Light issue"
EXPIRATION_DAYS = "expiration_days"


"""
These are data required by Firebase to run. Use yours, please.
You can easily find them if you go on your Firebase console -> Tog on the upper left -> /
General -> Scroll to down and click on "Add app" -> Select "</>" -> Get the data inside "<script>"
"""


config = {
    "apiKey": "AIzaSyDNcuaodUY1vxhP2xG_VLdD6sZQadIZ67g",
    "authDomain": "waste-17fee",
    "databaseURL": "https://waste-17fee.firebaseio.com/",
    "storageBucket": "gs://waste-17fee.appspot.com"
}


# General variables
normal_seconds_check = 180  # The Raspberry will ask the data every 3 minutes if needed.

# Firebase variables
firebase = pyrebase.initialize_app(config)  # Initialising the authentication
database = firebase.database()  # Connecting to database
auth = firebase.auth()  # getting the authorization.
user_id = ""  # this string will be useful later on to store the user ID data once we log in.
user = ""  # this is going to become our user from which we will get the ID

# Login firebase. We'll leave the sign up to the app itself.
user_email = ""
user_password = ""

# Global variables which will be used by all methods.
global light_issue
global port  # The port where you attach your USB dongle
global serialPort  # Our way to connect to Arduino
global tmp_item  # Our single item
global first_setup  # Boolean
global check_time # The future time to check
global time_to_check # Boolean
global check_light  # Boolean to check whether it needs to check light or not
"""
This method allows the user to log in to the database. It is important because otherwise nothing else works.
It will ask users their email and password and it will store the userID inside the global variable,
which will be used to access the items later on.

IMPORTANT: MAKE SURE IT IS THE SAME ACCOUNT OF YOUR APP. If it is not, you won't see much.
"""
# TODO: handle errors


def initialise_database_and_login():
    global user
    global user_email
    global user_password
    global user_id
    correct_data = False

    while correct_data == False:
        user_email = input('Please, insert your email: ')
        user_password = getpass.getpass(prompt='Please, insert password: ')
        try:
            user = auth.sign_in_with_email_and_password(user_email, user_password)
            print("Logged")
            user_id = user['localId']
            print("Initialised!")
            correct_data = True
        except pyrebase.pyrebase.HTTPError as error:
            error_json = error.args[1]
            error_to_load = json.loads(error_json)['error']
            print(error_to_load['message'])
"""
This method will allow to set a new checking time after just doing the check.
The amount of check time can be changed at the upper variable "normal_seconds_check".

@:returns the next check time in datetime format.
"""


def set_check_time():
    global check_time
    print("Setting new check time.")
    last_check = datetime.datetime.now()
    print("It's now %s" % last_check)
    check_time = last_check + datetime.timedelta(seconds=normal_seconds_check)  # This is where people change check time.
    print("I'm gonna check at %s" % check_time)



"""
This method will write data on the serial port. To write them there it is necessary to encode them as byte.
I used the ".encode()" method inside str class.
"""


def send_request(message):
    print("Sending %s" % message)
    serialPort.write(message.encode())


"""
Allows the raspberry to start reading when it reads 'SETUP DONE' on the serial monitor. 

@:except any error coming from the serial monitor
"""


def read_all_startup_lines():
    still_startup_mode = True
    print("Warming up the arduino")
    counter = 0
    while still_startup_mode:
        # Raspberry will tell user how many lines its reading.
        print('Skipping %i lines' % counter, end='\r', flush=True)
        try:
            # It first checks if there is any new line in the Serial
            if serialPort.inWaiting() > 0:
                # If so, they'll start reading
                startup_line = serialPort.readline().decode()
                print(startup_line, flush=True)
                counter += 1
                if 'SETUP DONE' in startup_line:
                    # When they find the line "SETUP_DONE" they will start reading.
                    print('Arduino is warm enough')
                    still_startup_mode = False
        except serial.SerialException:
            print("Error %s" % serial.SerialException.strerror)


"""
This method will update the data on an existent Item in the database.
It uses ".update()" which doesn't overwrite ALL the data but just the ones on the lines.
"""


def update_item():
    database.child('items').child(user_id).child(tmp_item.key).update(tmp_item.data_to_update())


"""
Allows the data to be copied in the tmp_item.

@:parameter a map of data which is returned by read_data()  method.
"""


def copy_data(data_to_copy):
    print("Copying data")
    for s_line in data_to_copy:
        # We need to split the strings so we get a column AND a row.
        column = s_line.split(":")[0]
        line_clean = s_line.split(":")[1]
        print("Copying -> %s : %s" % (column, line_clean))
        if 'int_temperature' in column:
            tmp_item.current_temperature = float(line_clean.strip(","))
        if 'ext_temperature' in column:
            tmp_item.external_temperature = float(line_clean.strip(","))
        if 'quantity_mm' in column:
            tmp_item.quantity = int(line_clean.strip(","))
            # from the data structure, being the last in the JSON luminosity doesn't have any comma in the end.
            # Hence we won't take out the comma.
        if 'luminosity' in column:
            tmp_item.luminosity = float(line_clean)
        if 'stream_number' in column:
            tmp_item.stream_number = int(line_clean.strip(","))


"""
It sets on the database "new_product":"yes" line, which will be read by Firebase Functions and send a
notification to the user

@:returns the key of the item that has just been generated
"""


def first_setup_command():
    # Creates a new key on the database and returns it
    item_key = database.child('items').child(user_id).generate_key()
    # creates a dictionary
    new_product_data = {'new_product': 'yes'}
    # Set's the new product with parent the key
    database.child(item_key).set(new_product_data)
    return item_key


"""
Waits for the database to confirm that the user placed the product in the pantry.
Checks if "new_product" value changes to "placed". If so, it stops waiting.
"""


def wait_for_data():
    print("Waiting for the database to confirm")
    keep_waiting = True
    while keep_waiting:
        database_to_check = database.child('items').child(user_id).child(tmp_item.key).get()
        is_it_placed = database_to_check.val()['new_product']
        # print(is_it_placed)
        if 'placed' in is_it_placed:
            keep_waiting = False
        else:
            # Waits three seconds before checking again
            simple_counter(3)


"""
Asks the database what's the real optimal temperature. It is set by firebase functions.
"""


def set_optimal_temperature():
    optimal_tmp = float(database.child('items').child(user_id).child(tmp_item.key).child('opt_temperature').get().val())
    tmp_item.optimal_temperature = optimal_tmp


"""
Analyses the data from the database and converts the pints and liters to ml. If something else is there it just pastes it
"""


def analyse_total_quantity(quantity_to_analyse):
    global pack_to_return
    if "Pints" in tmp_item.pack_measure:
        pack_to_return = quantity_to_analyse*568
    elif "L" in tmp_item.pack_measure:
        pack_to_return = quantity_to_analyse*1000
    else:
        pack_to_return = quantity_to_analyse

    return pack_to_return


"""
Gets data from the database, makes it convert them and then uploads them on the dataabse again
"""


def set_total_quantity():
    print('updating total quantity')
    total_quantity_db = float(database.child('items').child(user_id).child(tmp_item.key).child(PACK_SIZE).get().val())
    print("Quantity got is %s " % total_quantity_db)
    tmp_item.pack_measure = database.child('items').child(user_id).child(tmp_item.key).child(PACK_MEASURE).get().val()
    print("Pack measure is %s" % tmp_item.pack_measure)
    tmp_item.pack_size = analyse_total_quantity(total_quantity_db)
    print("Final pack size is: %s" % tmp_item.pack_size)
    database.child('items').child(user_id).child(tmp_item.key).update(tmp_item.data_to_update_total_quantity())



"""
This is a key method.
It checks if there is any new data after requesting them. It reads it and if it is an actual value of the data required,
it stores it. Otherwhise it will do other actions, explained later.

@:returns JSON as dictionary.
"""


def read_data():
    print("Reading data")
    # Gets the global lines
    global line
    global first_setup
    global light_issue
    global check_light
    keep_checking = True
    data_reading = []
    serial_not_ready_counter = 1
    while keep_checking:
        if serialPort.inWaiting() > 0:  # If there is any new string in the serial. this check is important,
            # otherwise the code will just stay stuck.
            line = ""
            # Skips all the lines which are old and not the beginning of the JSON
            while "{" not in line:
                line = serialPort.readline().decode()  # .decode() is essential to decode from byte type.
                print("Line is: %s" % line)
                # If we restart the data we just break the code
                if "RESET_MODE" in line or "FIRST_SETUP" in line:
                    first_setup = True
                    light_issue = False
                    keep_checking = False
                    check_light = False
                    break
                # Same here
                elif "LIGHT_OFF" in line:
                    light_issue = False
                    keep_checking = False
                    print("Light is off")
                    break
                elif "{" in line:
                    break
                else:
                    print("No {")
                # If there is ANY other message
            # If the line has a new starting point we will start reading as JSON.
            if "{" in line:
                # Boolean required to use the while loop to read all data.
                start_counting = True
                while start_counting:
                    # We read a new line after the "{"
                    new_line = serialPort.readline().decode()
                    print("New line is: %s" % new_line)
                    # If we reach the end of the JSON, we stop checking.
                    if "}" in new_line:
                        keep_checking = False
                        start_counting = False
                    # The next check are really important for the code not to crash.
                    # If, in fact, stores some wrong data in the dictionary, the whole system will crash.
                    else:
                        # Add the decoded data to a dictionary
                        data_for_item = new_line.strip('\r\n')
                        data_reading.append(data_for_item)

        # IF in general there is no serial available, handle it by writing that the Serial Monitor is not ready.
        else:
            if serial_not_ready_counter == 1:
                print("Serial not ready")
                serial_not_ready_counter += 1
    # Returns a dictionary
    return data_reading


def analyse_temperature():
    global light_counter
    global light_issue
    if tmp_item.current_temperature is not 'int_temperature':
        if float(tmp_item.current_temperature) < tmp_item.optimal_temperature:
            light_counter = 0
            light_issue = False


"""
Keeps checking the data more frequently until told otherwise. If it keeps being in open space, we get a light issue.
"""

# TODO: send a reminder on the database after some time to place the item back in the fridge!


def emergency_mode():
    global light_counter
    global light_issue
    global line
    global first_setup
    global check_light
    print("Emergency mode START")
    # Wait 30 seconds to see whether the light is still on
    checker_counter(30)
    line = ""
    # Checks that the button hasn't been pressed in the last minute
    if serialPort.in_waiting > 0:
        line = serialPort.readline().decode()

    if "" in line:
        print("Line is empty")
        print(line)
        light_issue = True
        # Until told otherwise, keep checking the data every 10 seconds.
        while light_issue:
            send_request(CHECK_DATA)
            data_to_upload = read_data()
            if light_issue:
                copy_data(data_to_upload)
                update_thread = threading.Thread(name='update thread', target=update_item)
                update_thread.start()
            analyse_temperature_thread = threading.Thread(name='analyse_temperature', target=analyse_temperature)
            analyse_temperature_thread.start()
            if light_issue:
                checker_counter(10)
                if first_setup is True:
                    light_issue = False
                    break
            # If the current temperature is lower than the optimal one, then it can stay calmer.
    elif "FIRST_SETUP" in line:
        first_setup = True
    elif "LIGHT_OFF" in line:
        check_light = False
        print("Emergency mode ENDS")
        print(line)
    if light_issue is False:
        check_light = False

"""
Asks the expiration date to the Database and sends the number of days left to arduino.
"""


def set_expiration_date():
    # Gets the expiration date from the database
    expiration_date = database.child('items').child(user_id).child(tmp_item.key).child(EXPIRATION_DATE).get().val()
    # Converts the expiration date to datetime so we can do the math
    exp_date = datetime.datetime.strptime(expiration_date.__str__(), '%d/%m/%Y')
    # Doing a simple math and getting the days
    difference = (exp_date - datetime.datetime.now()).days
    # Storing the expiration date in our general item object
    tmp_item.expiration_date = exp_date
    # Sends the difference to the database
    database.child('items').child(user_id).child(tmp_item.key).child(EXPIRATION_DAYS).set(difference)
    # We are telling to the arduino that the next data we're sending is going to be the expiration days
    send_request('expiration')
    # Wait for 2 seconds to avoid problems
    simple_counter(2)
    # Sends the difference. Important converting it to string explicitly, as "encode" mode doesn't apply to integers.
    send_request(str(difference))


def timer_checking():
    global time_to_check
    global check_time
    while True:
        difference = check_time - datetime.datetime.now()
        if difference.total_seconds() <= 0:
            time_to_check = True


def simple_counter(seconds):
    while seconds > 0:
        print("Waiting for %s s" % seconds, flush=True, end='\r')
        seconds -= 1
        time.sleep(1)


def checker_counter(seconds):
    global check_light
    global light_issue
    global first_setup
    global line
    while seconds > 0:
        print("Waiting for %s s" % seconds, flush=True)
        seconds -= 1
        if serialPort.in_waiting > 0:
            line = serialPort.readline().decode()
            if 'FIRST_SETUP' in line:
                first_setup = True
                break
                # Door is open. Start counting if needed.
            elif 'LIGHT_ON' in line:
                check_light = True
                break
                # Door has been closed again. No problem.
            elif 'LIGHT_OFF' in line:
                print("Light is off")
                light_issue = False
                break
            else:
                print(line)
        time.sleep(1)
"""
This is the main method that will run all the time.
"""

if __name__ == '__main__':
    # Gets the general line
    global line

    # Initialising all the variables
    check_time = 0
    expiration_date_check = datetime.datetime.now()
    light_issue = False
    light_counter = 0
    first_setup = False
    time_to_check = False
    check_light = False
    update_thread = threading.Thread(target=update_item, name="updating")
    starting_point = datetime.datetime.now()
    tmp_item = Item()
    print("Program started. It's %s" % starting_point.strftime('%d/%m/%y - %H:%M:%S'))
    # Setting a new check time to initialise the variable
    check_time_thread = threading.Thread(target=set_check_time, name='Set check time start')
    check_time_thread.start()
    # Logging in
    initialise_database_and_login()
    # Checking which port i am going to use for the USB dongle
    system = platform.system()
    port = ''
    print("You are using %s" % system)
    if 'Linux' in system:
        port = '/dev/ttyACM0'
    elif 'Darwin' in system:
        port = '/dev/cu.usbmodem14201'

    # Opening the serial monitor
    serialPort = serial.Serial(port=port, baudrate=115200)
    # We set the first setup as false. If it changes will be later.
    first_setup = False
    counter = 0  # Debugging purposes: we're writing the amount of seconds left every time the counter % 6 == 0.
    # It's an arbitrary number.
    with serialPort:
        read_all_startup_lines()  # Skipping the startup lines
        while True:  # we start an infinite loop which will make avoid closing the program
            line = ""  # set line as empty
            # If the Arduino says something
            if serialPort.inWaiting() > 0:
                # Decode the line. We also strip it from the final \r\n that all the bytes decoded lines have
                line = serialPort.readline().decode().strip('\r\n')
                print(line)
                # The raspberry will analyses it
                if 'FIRST_SETUP' in line:
                    first_setup = True
                # Door is open. Start counting if needed.
                if 'LIGHT_ON' in line:
                    check_light = True
                # Door has been closed again. No problem.
                if 'LIGHT_OFF' in line:
                    light_issue = False

                if first_setup:
                    # Getting the key of the item
                    tmp_item.key = first_setup_command()
                    print("Key: %s" % tmp_item.key)
                    # Waits for the answer from the database
                    wait_for_data()
                    # Sends the requests of the data to the Arduino
                    send_request(CHECK_DATA)
                    # Reads the data requested
                    data_got = read_data()
                    # Copies the data on the Item object and uploads them on the database
                    copy_data(data_got)
                    # We open a new thread to update the database. Meanwhile the program will be running the optimal temperature
                    update_database = threading.Thread(target=update_item, name='update database')
                    update_database.start()
                    # Gets the optimal temperature from the database
                    optimal_temp_thread = threading.Thread(target=set_optimal_temperature, name='optimal_temp')
                    optimal_temp_thread.start()
                    # Establishes when it is going to check the data next
                    set_check_time_thread_setup = threading.Thread(target=set_check_time, name='set check time')
                    set_check_time_thread_setup.start()
                    # Ends the setup
                    first_setup = False
                    # Sets total quantity
                    set_total_quantity_thread = threading.Thread(target=set_total_quantity, name='set total quantity')
                    set_total_quantity_thread.start()
                    # Tells the Arduino how many days left to the expiration date
                    set_expiration_date()
                    expiration_date_check = datetime.datetime.now() + datetime.timedelta(days=1)
                    print('Ended first setup!')

            if check_light is True:
                emergency_mode()

            if time_to_check:
                send_request(CHECK_DATA)
                # Once we got the data we just upload them.
                data_got = read_data()
                copy_data(data_got)
                update_thread.start()
                check_time_thread.start()
                simple_counter(5)

            # Everyday it will update the database and arduino.
            if expiration_date_check.day == datetime.datetime.now().day:
                set_expiration_date()
                expiration_date_check = datetime.datetime.now() + datetime.timedelta(days=1)



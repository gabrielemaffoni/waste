#  Copyright (c) 2019. Gabriele Maffoni - All rights reserved.
import pyrebase
import datetime

PRODUCT_TYPE = "product_type"
QUANTITY = "quantity_mm"
QUANTITY_LEFT = "quantity_left"
EXPIRATION_DATE = "expiration_date"
OPTIMAL_TEMPERATURE = "opt_temperature"
EXTERNAL_TEMPERATURE = "ext_temperature"
CURRENT_TEMPERATURE = "int_temperature"
BRAND = "brand"
ALARM = "alarm"
STREAM_NUMBER = "stream_number"
LUMINOSITY = "luminosity"
LATEST_UPDATE = 'latest update'
KEY = 'key'
# Constant values for database
class Item:


    def __init__(self, product_type=PRODUCT_TYPE, quantity=QUANTITY, quantity_left=QUANTITY_LEFT,
                 expiration_date=EXPIRATION_DATE, optimal_temperature=OPTIMAL_TEMPERATURE,
                 current_temperature=CURRENT_TEMPERATURE,
                 brand=BRAND, alarm=ALARM, stream_number=STREAM_NUMBER, external_temperature=EXTERNAL_TEMPERATURE,
                 luminosity=LUMINOSITY, key=KEY):
        self.product_type = product_type
        self.quantity = quantity
        self.quantity_left = quantity_left
        self.expiration_date = expiration_date
        self.optimal_temperature = optimal_temperature
        self.current_temperature = current_temperature
        self.external_temperature = external_temperature
        self.brand = brand
        self.alarm = alarm
        self.stream_number = stream_number
        self.luminosity = luminosity
        self.key = key

    def get_data(self):
        data = {
                PRODUCT_TYPE: self.product_type,
                QUANTITY: self.quantity,
                QUANTITY_LEFT: self.quantity_left,
                EXPIRATION_DATE: self.expiration_date,
                OPTIMAL_TEMPERATURE: self.optimal_temperature,
                CURRENT_TEMPERATURE: self.current_temperature,
                BRAND: self.brand,
                ALARM: self.alarm,
                STREAM_NUMBER: self.stream_number,
                EXTERNAL_TEMPERATURE: self.external_temperature,
                'origin': 'get_data'

        }

        return data

    def get_type(self):
        return self.product_type

    #   Method to update data already existent in the database
    def data_to_update(self):
        data = {
                LATEST_UPDATE: datetime.datetime.now().strftime('%d/%m/%y - %H:%M:%S'),
                QUANTITY_LEFT: self.quantity_left,
                CURRENT_TEMPERATURE: self.current_temperature,
                EXTERNAL_TEMPERATURE: self.external_temperature,
                LUMINOSITY: self.luminosity,
                OPTIMAL_TEMPERATURE: 0,
                'origin': 'data_to_update'
        }

        return data

    def upload_all_data(self):

        data = {

                LATEST_UPDATE: datetime.datetime.now().strftime('%d/%m/%y - %H:%M:%S'),
                QUANTITY_LEFT: self.quantity_left,
                CURRENT_TEMPERATURE: self.current_temperature,
                STREAM_NUMBER: self.stream_number,
                EXTERNAL_TEMPERATURE: self.external_temperature,
                OPTIMAL_TEMPERATURE: 0,
                'origin': 'upload_all_data'

        }
        return data

    def download_all_data(self, database_reference):
        data_downloaded = database_reference.get()
        for data in data_downloaded:
            print(data)

    def assign_all_data(self, data_downloaded, item):
        for data in data_downloaded.each():
            if CURRENT_TEMPERATURE in data[0]:
                item.current_temperature = data[1]
            if EXTERNAL_TEMPERATURE in data[0]:
                item.external_temperature = data[1]
            if OPTIMAL_TEMPERATURE in data[0]:
                item.optimal_temperature = data[1]
            if STREAM_NUMBER in data[0]:
                item.stream_number = data[1]

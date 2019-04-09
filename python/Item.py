
"""
Copyright (c) 2019. Gabriele Maffoni - All rights reserved.
This class is a representation of the Item in the database. All the constants are the values that
are going to be uploaded on the db.

CHANGELOG:
- Deleted unused methods and variables
- Added documentation
"""
import datetime
# Constant values for database
PRODUCT_TYPE = "product_type"
QUANTITY = "quantity_mm"
QUANTITY_LEFT = "quantity_left"
EXPIRATION_DATE = "expiration_date"
OPTIMAL_TEMPERATURE = "opt_temperature"
EXTERNAL_TEMPERATURE = "ext_temperature"
CURRENT_TEMPERATURE = "int_temperature"
BRAND = "brand"
PACK_SIZE = "total_quantity"
PACK_MEASURE = "measure"
# A stream number is important in case someone wants to check earlier streams in the buffer.
# Won't use it anywhere at the moment, though.
STREAM_NUMBER = "stream_number"
LUMINOSITY = "luminosity"
LATEST_UPDATE = 'latest update'
KEY = 'key'


"""
The Item contains few methods so far, mainly to upload data. The rest is set easily by using variable.constant = value.
"""


class Item:
    """
    Empty definer to avoid crash

    """
    def __init__(self, product_type=PRODUCT_TYPE, quantity=QUANTITY, quantity_left=QUANTITY_LEFT,
                 expiration_date=EXPIRATION_DATE, optimal_temperature=OPTIMAL_TEMPERATURE,
                 current_temperature=CURRENT_TEMPERATURE,
                 brand=BRAND, stream_number=STREAM_NUMBER, external_temperature=EXTERNAL_TEMPERATURE,
                 luminosity=LUMINOSITY, key=KEY, pack_size=PACK_SIZE, pack_measure=PACK_MEASURE):
        """
        :param product_type: The type of product set on the database
        :param quantity: How much is left in mm
        :param quantity_left: How much is left converted
        :param expiration_date: What's the expiration date, in string format
        :param optimal_temperature: What is the optimal temperature from the database
        :param current_temperature: What is the current internal temperature value
        :param brand: What is the brand of the product
        :param stream_number: The stream number
        :param external_temperature: the temperature external of the product
        :param luminosity: Self explanatory
        :param key: The key assigned at the beginning of the setup from the database itself.
        """
        self.pack_size = pack_size
        self.product_type = product_type
        self.quantity = quantity
        self.quantity_left = quantity_left
        self.expiration_date = expiration_date
        self.optimal_temperature = optimal_temperature
        self.current_temperature = current_temperature
        self.external_temperature = external_temperature
        self.brand = brand
        self.stream_number = stream_number
        self.luminosity = luminosity
        self.key = key
        self.pack_measure = pack_measure

    """
    Returns a json format of all the data required to be sent  on the database.
    
    @:returns data used to update
    """
    #   Method to update data already existent in the database
    def data_to_update(self):
        data = {
                # Sets also a new last update.
                LATEST_UPDATE: datetime.datetime.now().strftime('%d/%m/%y - %H:%M:%S'),
                QUANTITY: self.quantity,
                CURRENT_TEMPERATURE: self.current_temperature,
                EXTERNAL_TEMPERATURE: self.external_temperature,
                LUMINOSITY: self.luminosity,
                OPTIMAL_TEMPERATURE: 0
        }

        return data

    """
    Returns a json format of total quantity defined
    """
    def data_to_update_total_quantity(self):
        data = {
            PACK_SIZE: self.pack_size
        }

        return data
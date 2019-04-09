/*
 * Copyright (c) 2019. Gabriele Maffoni.
 *
 * This code has several functions.
 * - It sends a temperature alarm
 * - Monitors the quantity, if there is a lack of something it sends a notification alert
 * - Sends a new notifiction product if needed
 * - Sends a notification that the product is expiring.
 *
 * CHANGELOG:
 * - Added comments
 * - Polished from unused variables
 * - Added constants
 */

/**
 * The database consists in two different fields so far.
 * 1) 'items'
 * 2) 'tokens'
 *
 * Each one has inside another container, confined by user IDs.
 * Checkout the rest of the database doc on github
 */

//CONSTANTS USED IN THE DATABASE
const opt_temperature = 'opt_temperature';
const int_temperature = 'int_temperature';
const ext_temperature = 'ext_temperature';
const quantity_mm = 'quantity_mm';
const quantity_left = 'quantity_left';
const product_type = 'product_type';
const expiration_days = 'expiration_days';
const pack_size = 'total_quantity';
//Database initialisation
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
var message;

/**
 * This method gets the temperature of the product and compares it against the optimal temperature.
 * If it is more than it, it will just send a notification to the user.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */
exports.temperature_alarm = functions.database.ref('/items/{userID}/{itemID}/').onUpdate((change, context)=>{
  console.log("Checking value");
  const data = change.after.val();
  const user_id = context.params.userID;
  console.log("Values checked");
  let internal_temperature = data.int_temperature;
  let optimal_temperature = data.opt_temperature;
  console.log("Internal temperature: "+data.int_temperature);

  message = "";

  if (internal_temperature > optimal_temperature){
    message = "TMP_ISSUE";
    const payload = {
      "notification": {
        "title": 'Your product is raising temperature!',
        "body": 'Consider putting it back in your pantry.',
      },
      "data": {
        "item_key": change.after.key,
        "intent_notification": message
      }
    };

    const token_snapshot = admin.database().ref(`/users/${user_id}/token/`).once('value').then((data_snapshot) => {
      console.log("Sending message", payload);
      return admin.messaging().sendToDevice(data_snapshot.val(), payload);
    }, (error) => {
      console.log(error);
    });


  } else {
    message = "TMP_OKAY";
    const payload = {
      "notification": {
        "title": 'Your product is now okay!',
        "body": 'Everything seems back to normal :)',
      },
      "data": {
        "item_key": change.after.key,
        "intent_notification": message
      }
    };
    const token_snapshot = admin.database().ref(`/users/${user_id}/token/`).once('value').then((data_snapshot) => {
      console.log("Sending message", payload);
      return admin.messaging().sendToDevice(data_snapshot.val(), payload);
    }, (error) => {
      console.log(error);
    });

  }

  return message;
});


/**
 * It checks the quantity against other data. If it is lower than a quarter of the total data,
 * it will send a notification saying that the product is finishing.
 * Otherwise, it will just send a notification that it is finished.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */


exports.monitor_quantity = functions.database.ref('items/{userID}/{itemID}/current_quantity').onUpdate((snapshot, context)=>{
  console.log("Checking quantity");
  const quantity_atm = snapshot.after.val();
  const product_id = context.params.itemID;
  const user_id = context.params.userID;
  const product_type_db = (admin.database().ref(`items/${user_id}/${product_id}/${product_type}`).once('value')).val();
  const total_quantity_db = admin.database().ref(`items/${user_id}/${product_id}/total_quantity`).once('value').then((quantity_snap)=>{
    return quantity_snap.val();
  }, (error) => {console.log(error)});
  const token = admin.database().ref(`users/${user_id}/token`).once('value').then((token_snap) => {
    return token_snap.val();
  },(error) => {
    console.log(error)
  });
  console.log("Token", token);
  console.log("Total quantity", total_quantity_db);
  var payload = "";
  if (quantity_atm < (total_quantity_db/4) && quantity_atm > 0){
    payload = {
      "notification": {
      "title" : `Your ${product_type_db} is almost finishing!`,
      "body"  : `Do you have it in your pantry?`
    },
    "data":{
      "item_key": product_id,
      "intent_notification": "PRODUCT_FINISHING"
    }
  }
  } else if (quantity_atm === 0){
    payload = {"notification":{
      "title": `Your ${product_type_db} is finished!`,
      "body": `Should I add it to the list?`
    },
  "data":{
    "item_key": product_id,
    "intent_notification" : "PRODUCT_FINISHED"
  }
}
  }

  return admin.messaging().sendToDevice(token.toString(), payload);
});

/**
 * Sets the optimal temperature based on arbitrary data and common knowledge.
 * Next task: put it real confirmed data in a part of the database from where people can also edit.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */


exports.define_optimal_temperature = functions.database.ref('/items/{userID}/{itemID}/').onUpdate((data_snapshot,context) => {
  console.log("Checking type of product");
  if (data_snapshot.after.child('new_product').val() === 'placed') {
    const product_type = data_snapshot.after.child('product_type').val();
    const product_id = data_snapshot.after.key;
    const user_id = context.params.userID;
    let optimal_temperature = 0;

    switch (product_type.toLowerCase()) {
      case 'milk':
        optimal_temperature = 4;
        break;
      case 'red wine':
        optimal_temperature = 12;
        break;
      case 'white wine':
        optimal_temperature = 6;
        break;
      case 'dairy product':
        optimal_temperature = 4;
        break;
      case 'wine':
        optimal_temperature = 10;
        break;
      default:
        optimal_temperature = 0;
    }
    console.log("Optimal temperature set: ", optimal_temperature);

    let data_to_upload = {'opt_temperature': optimal_temperature};


    return admin.database().ref(`/items/${user_id}/${product_id}`).update(data_to_upload);
  }
});

/**
 * Sends a notification to the user saying that the cap has been reset and to place it back to the pantry.
 * @type {CloudFunction<DataSnapshot>}
 */


exports.send_new_product_notification = functions.database.ref('/items/{userID}/{itemID}').onCreate( (snapshot, context) => {
  const user = {
    user_id: context.params.userID,
    token: 'token'
  };

  const product_key = snapshot.key;
  console.log("User ID", user.user_id);
  console.log("token", user.token  );
  console.log("Getting token snapshot");
  const tokenisation =  admin.database().ref(`/users/${user.user_id}/token/`).once('value').then((data) =>{
      user.token = data.val();
      const payload = {
        "notification": {
          "title": 'You just reset the cap!',
          "body": 'Place it in your pantry and then press "Placed"!',
        },
        "data": {
          "item_key": product_key,
          "intent_notification": "NEW_PRODUCT"
        }
      };

      return admin.messaging().sendToDevice(user.token, payload);
  }, (error) => {
    console.log(error);
  });

});

/**
 * Sends a notification to the user saying that a product is expiring or expired.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */



exports.product_expiring_notification = functions.database.ref('items/{userID}/{itemID}/').onUpdate((snapshot, context) =>{
  const expiration_days_db = snapshot.after.child('expiration_days').val();
  const user_id = context.params.userID;
  const product_key = context.params.itemID;
  if (expiration_days_db === 0){
    const token_snapshot = admin.database().ref(`/users/${user_id}/token/`).once('value').then((data_snapshot) => {
      const payload = {
        "notification": {
          "title": `Your product expires today!`,
          "body": 'Do you need some ideas on how to use it?',
        },
        "data": {
          "item_key": product_key,
          "intent_notification":"PRODUCT_EXPIRING"
        }
      };

      return admin.messaging().sendToDevice(data_snapshot.val(), payload);
    }, (error) => {
      console.log(error);
    });
  }
  else if (expiration_days_db < 4){
    const token_snapshot = admin.database().ref(`/users/${user_id}/token/`).once('value').then((data_snapshot) => {
      const payload = {
        "notification": {
          "title": `Your product is expiring in ${expiration_days_db} days!`,
          "body": 'Finish it quickly, before you have to throw it away. Do you need some ideas?',
        },
        "data": {
          "item_key": product_key,
          "intent_notification":"PRODUCT_EXPIRING"
        }
      };

      return admin.messaging().sendToDevice(data_snapshot.val(), payload);
    }, (error) => {
      console.log(error);
    });
  }
  return expiration_days_db
});


/**
 * This function converts the quantity I get on the database from the raspberry to the quantity in ml.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */
exports.convert_from_mm_to_ml = functions.database.ref('items/{userID}/{itemID}').onUpdate((snapshot, context)=>{
  const user_id = context.params.userID;
  //Stores all the values about the item
  const item = {
      id: context.params.itemID,
      quantity_mm_db: snapshot.after.child(quantity_mm).val(),
      quantity_converted: 0,
      total_quantity: snapshot.after.child('total_quantity').val(),
      ml_per_mm: 0
  };
  console.log(item);
  //All these values are not arbitrary. They have been measured specidifaclly.
  /**
   * 568 -> Milk bottle of 1 pint. | the threshold of most of the bottles here is 75 mm after that there is a new proportion
   * to do.
   * Empty 143
   * 1136 -> Milk bottle of 2 pints | The threshold is 125. After that most of the milk comes out.
   * empty 255
   */
  console.log('item_size', item.total_quantity);
  console.log('ietm quantity', item.quantity_mm_db);
    if (item.total_quantity === 568){

          if (item.quantity_mm_db <= 75){
            item.ml_per_mm = 200/75;
          } else {
            item.ml_per_mm = 368/68;
          }

    } else if (item.total_quantity === 1136){
        if (item.quantity_mm_db <= 125){
          item.ml_per_mm = 354/125;
        } else {
          item.ml_per_mm = 255/130;
        }
    }
    // Does the final math
  /**
   * To get what is remaining now it's kind of easy. We get the proportion of ml/mm calculated earlier,
   * and then we subtract the total quantity minus the lack of product
   */
    const quantity_to_multiply = item.ml_per_mm * item.quantity_mm_db;
    item.quantity_converted = item.total_quantity - quantity_to_multiply;
    console.log('Ml per mm', item.ml_per_mm);
    let data_to_upload = {'quantity_left': item.quantity_converted};
    console.log("data to upload", data_to_upload);
    //Sends everything to
    return admin.database().ref(`/items/${user_id}/${item.id}/`).update(data_to_upload)

});



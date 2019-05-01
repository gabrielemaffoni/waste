/*
 * Copyright (c) 2019. Gabriele Maffoni.
 *
 * This code has several functions.
 * - It sends a temperature alarm
 * - Monitors the quantity, if there is a lack of something it sends a notification alert
 * - Sends a new notification product if needed
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
    OPT_TEMPERATURE = 'opt_temperature';
    INT_TEMPERATURE = 'int_temperature';
    EXT_TEMPERATURE = 'ext_temperature';
    QUANTITY_MM = 'quantity_mm';
    QUANTITY_LEFT = 'quantity_left';
    TOTAL_QUANTITY = 'total_quantity';
    EXPIRATION_DAYS = 'expiration_days';
    PRODUCT_TYPE = 'product_type';
    TOKEN = 'token';
//Database initialisation
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * This method gets the temperature of the product and compares it against the optimal temperature.
 * If it is more than it, it will just send a notification to the user.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */
exports.temperature_alarm = functions.database.ref('/items/{userID}/{itemID}/int_temperature').onUpdate(async (change, context)=>{
  console.log("Checking value");
  const item = {
    id: context.params.itemID,
    int_temperature: change.after.val(),
    opt_temperature: 0
  };

  const user = {
    id : context.params.userID,
    token: ""
  };

  const payload = {
    notification: {
      title: "",
      body:"",
    },
    data: {
      item_key: item.id,
      intent_notification: ""
    }
  };

  console.log("Values checked");
  const opt_temperature_promise = admin.database().ref(`/items/${user.id}/${item.id}/opt_temperature`).once('value');
  const token_snapshot = admin.database().ref(`/users/${user.id}/token/`).once('value');
  console.log("Internal temperature: "+item.int_temperature);

  let promises = await Promise.all([token_snapshot, opt_temperature_promise]);
  user.token = promises[0].val();
  item.opt_temperature = promises[1].val();
  if (item.int_temperature > item.opt_temperature){
    payload.data.intent_notification = "TMP_ISSUE";
    payload.notification.title = 'Your product is raising temperature!';
    payload.notification.body ='Consider putting it back in your pantry.';
  } else {
    payload.data.intent_notification = "TMP_OKAY";
    payload.notification.title = 'Your product is now okay!';
    payload.notification.body = 'Everything seems back to normal :)'
  }

  return admin.messaging().sendToDevice(user.token, payload);
});


/**
 * It checks the quantity against other data. If it is lower than a quarter of the total data,
 * it will send a notification saying that the product is finishing.
 * Otherwise, it will just send a notification that it is finished.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */


exports.monitor_quantity = functions.database.ref('items/{userID}/{itemID}/quantity_left').onUpdate(async (snapshot, context)=>{
  console.log("Checking quantity");
  const user = {
    id: context.params.userID,
    token: ""
  };

  const item = {
    id: context.params.itemID,
    quantity: snapshot.after.val(),
    total_quantity: 0,
    type: ""
  };

  const payload = {
      notification: {
        title: '',
        body: '',
      },
    data:{
        item_key: item.id,
        intent_notification: ''
      }
  };
  const product_type_promise = admin.database().ref(`items/${user.id}/${item.id}/${PRODUCT_TYPE}`).once('value');
  const total_quantity_promise = admin.database().ref(`items/${user.id}/${item.id}/${TOTAL_QUANTITY}`).once('value');
  const token_promise = admin.database().ref(`users/${user.id}/${TOKEN}`).once('value');

  const promises = await Promise.all([product_type_promise, total_quantity_promise, token_promise]);

  item.type = promises[0].val();
  item.total_quantity = promises[1].val();
  user.token = promises[2].val();

  if (item.quantity < (item.total_quantity/4) && item.quantity > 0){
    payload.notification.title = `Your ${item.type} is almost finishing!`;
    payload.notification.body = `Do you have it in your pantry?`;
    payload.data.intent_notification = "PRODUCT_FINISHING";
  } else if (item.quantity === 0){
    payload.notification.title = `Your ${item.type} is finished!`;
    payload.notification.body = 'Should I add it to the list?';
    payload.data.intent_notification = "PRODUCT_FINISHED";
  }

  return admin.messaging().sendToDevice(user.token, payload);
});

/**
 * Sets the optimal temperature based on arbitrary data and common knowledge.
 * Next task: put it real confirmed data in a part of the database from where people can also edit.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */

exports.define_optimal_temperature = functions.database.ref('/items/{userID}/{itemID}/opt_temperature').onCreate(async (data_snapshot,context) => {
  console.log("Checking type of product");
    const user = {
      id: context.params.userID
    };

    const item = {
      id: context.params.itemID,
      opt_temperature: 0,
      type: ''
    };

    const item_type_promise = admin.database().ref(`items/${user.id}/${item.id}/${PRODUCT_TYPE}`).once('value');
    let type = await Promise.all([item_type_promise]);
    item.type = type[0].val();

    switch (item.type.toLowerCase()) {
      case 'milk':
        item.opt_temperature = 4;
        break;
      case 'red wine':
        item.opt_temperature = 12;
        break;
      case 'white wine':
        item.opt_temperature = 6;
        break;
      case 'dairy product':
        item.opt_temperature = 4;
        break;
      case 'wine':
        item.opt_temperature = 10;
        break;
      default:
        item.opt_temperature = 0;
    }
    console.log("Optimal temperature set: ", item.opt_temperature);




    return admin.database().ref(`/items/${user.id}/${item.id}/${OPT_TEMPERATURE}`).set(item.opt_temperature);

});

/**
 * Sends a notification to the user saying that the cap has been reset and to place it back to the pantry.
 * @type {CloudFunction<DataSnapshot>}
 */


exports.send_new_product_notification = functions.database.ref('/items/{userID}/{itemID}').onCreate( async (snapshot, context) => {
  const user = {
    id: context.params.userID,
    token: 'token'
  };
  const product_key = snapshot.key;

  const payload = {
    notification: {
      title: 'You just reset the cap!',
      body: 'Place it in your pantry and then press "Placed"!'
    },
    data :{
      item_key: product_key,
      intent_notification: "NEW_PRODUCT"
    }
  };
  console.log("Getting token snapshot");

  const token_snapshot = admin.database().ref(`/users/${user.id}/token/`).once('value');
  let tokens = await Promise.all([token_snapshot]);
  console.log(tokens);
  user.token = tokens[0].val();
  console.log("User ID", user.id);
  console.log("token", user.token);


  return admin.messaging().sendToDevice(user.token,payload);


});

/**
 * Sends a notification to the user saying that a product is expiring or expired.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */



exports.product_expiring_notification = functions.database.ref('items/{userID}/{itemID}/expiration_days').onUpdate(async (change, context) =>{
  const expiration_days_db = change.after.val();
  const product_key = context.params.itemID;

  const user = {
    id : context.params.userID,
    token: ""
  };
  const payload ={
    notification:{
      title: '',
      body: ''
    },
    data:{
      item_key: product_key,
      intent_notification: ""
    }
  };
  const token_snapshot = admin.database().ref(`/users/${user.id}/${TOKEN}/`).once('value');
  let token_array = await Promise.all([token_snapshot]);
  user.token = token_array[0].val();

  if (expiration_days_db === 0){
    payload.notification.title = `Your product expires today!`;
    payload.notification.body = 'Do you need some ideas on how to use it?';
    payload.data.intent_notification = "PRODUCT_EXPIRED";

  }  else if (expiration_days_db < 4){
    payload.notification.title = `Your product is expiring in ${expiration_days_db} days!`;
    payload.notification.body = 'Finish it quickly, before you have to throw it away. Do you need some ideas?';
    payload.data.intent_notification = "PRODUCT_EXPIRING";

    }
  return admin.messaging().sendToDevice(user.token, payload);
});


/**
 * This function converts the quantity I get on the database from the raspberry to the quantity in ml.
 * @type {CloudFunction<Change<DataSnapshot>>}
 */
exports.convert_from_mm_to_ml = functions.database.ref('items/{userID}/{itemID}/quantity_mm').onUpdate(async (snapshot, context)=>{
  const user_id = context.params.userID;
  //Stores all the values about the item
  const item = {
      id: context.params.itemID,
      quantity_mm_db: snapshot.after.val(),
      quantity_converted: 0,
      total_quantity: 0,
      ml_per_mm: 0
  };

  const total_quantity_promise = admin.database().ref(`/items/${user_id}/${item.id}/${TOTAL_QUANTITY}/`).once('value');
  let total_quantity_db = await Promise.all([total_quantity_promise]);
  item.total_quantity = total_quantity_db[0].val();
  console.log(item);
  //All these values are not arbitrary. They have been measured specifically.
  /**
   * 568 -> Milk bottle of 1 pint. | the threshold of most of the bottles here is 75 mm after that there is a new proportion
   * to do.
   * Empty 143
   * 1136 -> Milk bottle of 2 pints | The threshold is 125. After that most of the milk comes out.
   * empty 255
   */
  console.log('item_size', item.total_quantity);
  console.log('item quantity', item.quantity_mm_db);
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
    //Sends everything to
    return admin.database().ref(`/items/${user_id}/${item.id}/${QUANTITY_LEFT}`).set(item.quantity_converted);

});



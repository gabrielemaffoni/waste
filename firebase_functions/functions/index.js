const opt_temperature = 'opt_temperature';
const int_temperature = 'int_temperature';
const ext_temperature = 'ext_temperature';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
var message;

exports.get_user_token = functions.database.ref('/users/{userID}/token').onWrite(change =>{
    let user_token = change.after.val();
    return user_token;
});

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

  } else {
    message = "TMP_OKAY";
  }
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

  return message;
});

exports.monitor_quantity = functions.database.ref('items/{userID}/{itemID}/current_quantity').onUpdate((snapshot, context)=>{
  console.log("Checking quantity");
  const quantity_atm = snapshot.after.val();
  const product_type = admin.database.ref(`items/${user_id}/${product_id}/product_type`).once('value').then((d_snapshot) => {return d_snapshot.val()}, (error) => {console.log(error)});
  const product_id = context.params.itemID;
  const user_id = context.params.userID;
  const total_quantity = admin.database.ref(`items/${user_id}/${product_id}/total_quantity`).once('value').then((quantity_snap)=>{
    return quantity_snap.val();
  }, (error) => {console.log(error)});
  const token = admin.database.ref(`users/${user_id}/token`).once('value').then((token_snap) => {
    return token_snap.val();
  },(error) => {
    console.log(error)
  });
  console.log("Token", token);
  console.log("Total quantity", total_quantity);
  var payload = "";
  if (quantity_atm < (total_quantity/4) && quantity_atm > 0){
    payload = { "notification": {
      "title" : `Your ${product_type} is almost finishing!`,
      "body"  : `Do you have it in your pantry?`
    },
    "data":{
      "item_key": product_id,
      "intent_notification": "PRODUCT_FINISHING"
    }
  }
  } else if (quantity_atm === 0){
    payload = {"notification":{
      "title": `Your ${product_type} is finished!`,
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

    let data_to_upload = {opt_temperature: optimal_temperature};


    return admin.database().ref(`/items/${user_id}/${product_id}`).update(data_to_upload);
  }
});

exports.send_new_product_notification = functions.database.ref('/items/{userID}/{itemID}').onCreate((snapshot, context) => {
  const u_token = "token";
  const product_key = snapshot.key;
  const user_id = context.params.userID;
  console.log("User ID", user_id);
  let token_ID = "";
  console.log("Getting token snapshot");
  const token_snapshot = admin.database().ref(`/users/${user_id}/token/`).once('value').then((data_snapshot) => {
    console.log("New product created. Product key ", product_key);
    console.log("Sending to token ", token_ID);
    console.log("Token snapshot", token_snapshot);
    const payload = {
      "notification": {
        "title": 'You just reset the cap!',
        "body": 'Place it in your pantry and then press "Placed"!',
      },
      "data": {
        "item_key": product_key,
        "intent_notification":"NEW_PRODUCT"
      }
    };

    return admin.messaging().sendToDevice(data_snapshot.val(), payload);
  }, (error) => {
    console.log(error);
  });

});

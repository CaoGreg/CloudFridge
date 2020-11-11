const functions = require('firebase-functions');
const admin = require('firebase-admin');

var serviceAccount = require("./permissions.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://cloud-fridge.firebaseio.com"
});

const express = require('express');
const app = express();

const cors = require('cors');
app.use(cors({ origin:true }));


// Get request: reads a user from the database
app.get('/api/read/:name', async (req, res) => {    
    try {
        const snapshot = await admin.firestore().collection('Users').doc(req.params.name).get();
        const username = snapshot.name;
        const userData = snapshot.data();

        return res.status(200).send(JSON.stringify({name: username, ...userData}));
    }
    catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }
});

// Post request: creates a new user in the database
app.post('/api/create', async (req, res) => {    
    try {
        await admin.firestore().collection('Users').doc('/' + req.body.name + '/')
        .create({            
            password: req.body.password,
            fridge_contents: req.body.fridge_contents
        })

        return res.status(201).send();
    }
    catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }    
});

// Put request: updates a user in the database
app.put("/api/update/:name", async (req, res) => {    
    try {
        await admin.firestore().collection('Users').doc(req.params.name).update({
            password: req.body.password,
            fridge_contents: req.body.fridge_contents
        });

        return res.status(200).send();
    }
    catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }        
});

// Delete request: deletes a user from the database
app.delete("/api/delete/:name", async (req, res) => {    
    try {
        await admin.firestore().collection("Users").doc(req.params.name).delete();
        return res.status(200).send();
    }
    catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }    
});

// Export API to Firebase Cloud Functions
exports.app = functions.https.onRequest(app);

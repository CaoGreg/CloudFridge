const functions = require('firebase-functions');
const admin = require('firebase-admin');

var serviceAccount = require("./permissions.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://cloud-fridge.firebaseio.com"
});

const os = require("os");
const path = require("path");
const fs = require("fs");
const {Storage} = require('@google-cloud/storage');
const storage = new Storage({
    projectId: "cloud-fridge",
});
const Busboy = require('busboy');

const Clarifai = require('clarifai');
const UUID = require("uuid-v4");

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
    } catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }
});


// Get request: get recipes for user
app.get('/api/read/recipes/:name', async (req, res) => {   
    try {
        const user_snapshot = await admin.firestore().collection('Users').doc(req.params.name).get();
        const fridge_contents = user_snapshot.data().fridge_contents;

        const recipe_snapshot = await admin.firestore().collection('Recipes').doc('Recipes').get();       
        const recipes = recipe_snapshot.data();

        var today = new Date();
        var priority = {}
        
        Object.keys(recipes).forEach(function(key) {
            priority[key] = 8
            for (var i = 0; i < fridge_contents.length; i++) {
                var content = fridge_contents[i];
                Object.keys(content).forEach(function(c) {
                    if (c.toLowerCase() in recipes[key]) {
                        var date = new Date(content[c])
                        var expiry = Math.round(Math.abs(today - date)/(1000 * 60 * 60 * 24))
                        priority[key] = (expiry < priority[key]) ? expiry : priority[key]                       
                    }
                })
            }
        })
        
        var result = Object.keys(priority).sort(function(a, b) {
            return priority[a] - priority[b];
        })
        
        return res.status(200).send(recipes[result[0]]);
    } catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }
});


// Get request: get image from user
app.get('/api/read/dates/:name/:img_name', async (req, res) => {   
    try {      
        const tempFilePath = path.join(os.tmpdir(),req.params.img_name);
        const options = { destination: tempFilePath, };

        file = await storage.bucket('cloud-fridge.appspot.com').file(req.params.name + '/' + req.params.img_name).download(options);
        
        return res.status(200).sendFile(tempFilePath);
    } catch (error) {
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
            fridge_contents: req.body.fridge_contents,
            upload_date: req.body.upload_date
        })

        return res.status(201).send();
    } catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }    
});


// Post request: upload image
app.post("/api/upload/:name", async (req, res) => {
    const busboy = new Busboy({ headers: req.headers })
    let uploadData = null
    let f_name = null

    busboy.on('file', (fieldname, file, filename, encoding, mimetype) => {
        const filepath = path.join(os.tmpdir(), filename)
        f_name = filename
        uploadData = { file: filepath, type: mimetype }
        file.pipe(fs.createWriteStream(filepath))
    })

    busboy.on('finish', (filename) => {
        let uuid = UUID();
        const bucket = storage.bucket('cloud-fridge.appspot.com')
        bucket
            .upload(uploadData.file, {
                uploadType: 'media',
                destination: req.params.name + '/' + f_name,
                metadata: {
                    metadata: {
                        contentType: uploadData.type,
                        firebaseStorageDownloadTokens: uuid
                    },
                },
            })
            .then(async(data) => {         
                image_name = data[0]
                // if image upload succeeds, send the image to clarifai for processing
                let rawData = fs.readFileSync('clarifai.json');
                let key = JSON.parse(rawData)['private_key_id'];
            
                const clarifai_app = new Clarifai.App({apiKey: key});
                const image = "https://firebasestorage.googleapis.com/v0/b/cloud-fridge.appspot.com/o/" + encodeURIComponent(image_name.name) + "?alt=media&token=" + uuid;
                const response = await clarifai_app.models.predict(Clarifai.FOOD_MODEL, image);
                
                const items = response.outputs[0].data.concepts.reduce((accumulator, item)=>{
                    return accumulator + "|" +item.name + ":" + item.value;
                },"");

                res.status(200).json({message: items,})
                return null;
            })
            .catch(err => {
                res.status(500).json({
                    error: err,
                })
            })
    })
    busboy.end(req.rawBody)
});


// Put request: updates a user in the database
app.put("/api/update/:name", async (req, res) => {    
    try {
        await admin.firestore().collection('Users').doc(req.params.name).update({
            password: req.body.password,
            fridge_contents: req.body.fridge_contents,
            upload_date: req.body.upload_date
        });

        return res.status(200).send();
    } catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }        
});


// Delete request: deletes a user from the database
app.delete("/api/delete/:name", async (req, res) => {    
    try {
        await admin.firestore().collection("Users").doc(req.params.name).delete();
        return res.status(200).send();
    } catch (error) {
        console.log(error);
        return res.status(500).send(error);
    }    
});


// Export API to Firebase Cloud Functions
exports.app = functions.https.onRequest(app);

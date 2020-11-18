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


// Post request: creates a new user in the database
app.post('/api/create', async (req, res) => {    
    try {
        await admin.firestore().collection('Users').doc('/' + req.body.name + '/')
        .create({            
            password: req.body.password,
            fridge_contents: req.body.fridge_contents
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
                    return accumulator + " " +item.name + "probability: " + item.value;
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
            fridge_contents: req.body.fridge_contents
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

import { initializeApp } from 'firebase/app';
import { getDatabase, ref, onValue, off } from 'firebase/database';

const firebaseConfig = {
  apiKey: process.env.REACT_APP_FIREBASE_API_KEY,
  authDomain: process.env.REACT_APP_FIREBASE_AUTH_DOMAIN,
  databaseURL: process.env.REACT_APP_FIREBASE_DATABASE_URL,
  projectId: process.env.REACT_APP_FIREBASE_PROJECT_ID,
  storageBucket: process.env.REACT_APP_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.REACT_APP_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.REACT_APP_FIREBASE_APP_ID,
};

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

export const subscribeToDeviceLocation = (deviceId, callback) => {
  const locationRef = ref(db, `devices/${deviceId}/location`);
  onValue(locationRef, snapshot => {
    if (snapshot.exists()) callback(snapshot.val());
  });
  return () => off(locationRef);
};

export const subscribeToDeviceStatus = (deviceId, callback) => {
  const statusRef = ref(db, `devices/${deviceId}/status`);
  onValue(statusRef, snapshot => {
    if (snapshot.exists()) callback(snapshot.val());
  });
  return () => off(statusRef);
};

export default db;

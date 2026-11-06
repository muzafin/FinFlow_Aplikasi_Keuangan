import { createRequire } from 'node:module';
import { test } from 'node:test';
import assert from 'node:assert/strict';
const require=createRequire(process.env.FINFLOW_TEST_DEPENDENCIES || new URL('./package.json',import.meta.url));
const admin=createRequire(new URL('../functions/package.json',import.meta.url));
const {initializeApp,deleteApp}=require('firebase/app');
const {getAuth,connectAuthEmulator,createUserWithEmailAndPassword}=require('firebase/auth');
const {getFirestore,connectFirestoreEmulator,doc,setDoc}=require('firebase/firestore');
const {getStorage,connectStorageEmulator,ref,uploadBytes}=require('firebase/storage');
process.env.FIRESTORE_EMULATOR_HOST='127.0.0.1:8088';
process.env.FIREBASE_AUTH_EMULATOR_HOST='127.0.0.1:9098';
process.env.FIREBASE_STORAGE_EMULATOR_HOST='127.0.0.1:9198';
const adminApp=admin('firebase-admin/app').initializeApp({projectId:'demo-finflow',storageBucket:'demo-finflow.firebasestorage.app'},'deletion-test');
const db=admin('firebase-admin/firestore').getFirestore(adminApp);
const authAdmin=admin('firebase-admin/auth').getAuth(adminApp);
const bucket=admin('firebase-admin/storage').getStorage(adminApp).bucket();

test('callable deletion removes only the caller and blocks their old token from recreating data',{timeout:60000},async()=>{
  const app=initializeApp({apiKey:'fake-api-key',projectId:'demo-finflow',appId:'test'},`deletion-${Date.now()}`);
  try {
    const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9098',{disableWarnings:true});
    const client=getFirestore(app);connectFirestoreEmulator(client,'127.0.0.1',8088);
    const storage=getStorage(app,'gs://demo-finflow.firebasestorage.app');connectStorageEmulator(storage,'127.0.0.1',9198);
    const user=(await createUserWithEmailAndPassword(auth,`deletion-${Date.now()}@example.test`,'FinFlow-test-123!')).user;
    const uid=user.uid;const token=await user.getIdToken();const other=`untouched-${Date.now()}`;
    await db.doc(`users/${other}`).set({marker:'must survive'});
    await setDoc(doc(client,`users/${uid}`),{id:uid,name:'Disposable emulator account',email:user.email,baseCurrency:'IDR',schemaVersion:2});
    await setDoc(doc(client,`users/${uid}/notes/note`),{id:'note',title:'Disposable',body:'emulator only',pinned:false,createdAt:1,updatedAt:1});
    await uploadBytes(ref(storage,`users/${uid}/profile/avatar.jpg`),new Uint8Array([255,216,255,217]),{contentType:'image/jpeg'});
    const call=data=>fetch('http://127.0.0.1:5008/demo-finflow/asia-southeast2/requestAccountDeletion',{
      method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`},body:JSON.stringify({data}),
    });
    assert.equal((await call({confirmation:'HAPUS',uid:other})).status,400);
    assert.equal((await call({confirmation:'HAPUS'})).status,200);
    const deadline=Date.now()+40000;
    while((await db.doc(`_accountDeletions/${uid}`).get()).data()?.status!=='complete') {
      assert.ok(Date.now()<deadline,'Deletion job did not finish');await new Promise(resolve=>setTimeout(resolve,250));
    }
    assert.equal((await db.doc(`users/${uid}`).get()).exists,false);
    assert.equal((await db.doc(`users/${uid}/notes/note`).get()).exists,false);
    assert.equal((await bucket.getFiles({prefix:`users/${uid}/`}))[0].length,0);
    await assert.rejects(authAdmin.getUser(uid),{code:'auth/user-not-found'});
    assert.equal((await db.doc(`users/${other}`).get()).data().marker,'must survive');
    const recreate=await fetch(`http://127.0.0.1:8088/v1/projects/demo-finflow/databases/(default)/documents/users/${uid}`,{
      method:'PATCH',headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`},body:JSON.stringify({fields:{id:{stringValue:uid}}}),
    });
    assert.equal(recreate.status,403);
    await db.doc(`users/${other}`).delete();
  } finally { await deleteApp(app);await admin('firebase-admin/app').deleteApp(adminApp) }
});

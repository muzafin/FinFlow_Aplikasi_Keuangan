import {createRequire} from 'node:module';
import {test} from 'node:test';
import assert from 'node:assert/strict';
const require=createRequire(process.env.FINFLOW_TEST_DEPENDENCIES || new URL('./package.json',import.meta.url));
const admin=createRequire(new URL('../functions/package.json',import.meta.url));
const {initializeApp,deleteApp}=require('firebase/app');
const {getAuth,connectAuthEmulator,createUserWithEmailAndPassword}=require('firebase/auth');
const {getFirestore,connectFirestoreEmulator,doc,setDoc,getDoc}=require('firebase/firestore');
process.env.FIRESTORE_EMULATOR_HOST='127.0.0.1:8088';process.env.FIREBASE_AUTH_EMULATOR_HOST='127.0.0.1:9098';process.env.FIREBASE_STORAGE_EMULATOR_HOST='127.0.0.1:9198';
test('financial reset retains Auth, seeds defaults and old requests never delete newer data',{timeout:60000},async()=>{
  const app=initializeApp({apiKey:'fake-api-key',projectId:'demo-finflow',appId:'test'},`reset-${Date.now()}`);
  const adminApp=admin('firebase-admin/app').initializeApp({projectId:'demo-finflow'},`reset-admin-${Date.now()}`);
  try {
    const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9098',{disableWarnings:true});
    const db=getFirestore(app);connectFirestoreEmulator(db,'127.0.0.1',8088);
    const user=(await createUserWithEmailAndPassword(auth,`reset-${Date.now()}@example.test`,'Kasiva-test-123!')).user;
    const uid=user.uid;const token=await user.getIdToken();const root=doc(db,`users/${uid}`);
    await setDoc(root,{id:uid,name:'Tetap ada',email:user.email,baseCurrency:'IDR',schemaVersion:2});
    const note=doc(db,`users/${uid}/notes/note`);
    const record={id:'note',title:'Uji emulator',body:'Disposable',pinned:false,createdAt:1,updatedAt:1};
    await setDoc(note,record);
    const invoke=async requestId=>{
      const result=await fetch('http://127.0.0.1:5008/demo-finflow/asia-southeast2/resetFinancialData',{
        method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`},body:JSON.stringify({data:{confirmation:'HAPUS DATA',requestId}}),
      });
      assert.equal(result.status,200,await result.text());
    };
    await invoke('reset-request-first');assert.equal((await getDoc(note)).exists(),false);
    assert.equal((await getDoc(root)).data().name,'Tetap ada');
    assert.equal((await getDoc(doc(db,`users/${uid}/accounts/acc_cash`))).data().openingBalanceMinor,0);
    assert.equal((await admin('firebase-admin/auth').getAuth(adminApp).getUser(uid)).email,user.email);
    await setDoc(note,record);await invoke('reset-request-second');assert.equal((await getDoc(note)).exists(),false);
    await setDoc(note,{...record,body:'Baru setelah reset'});await invoke('reset-request-first');
    assert.equal((await getDoc(note)).data().body,'Baru setelah reset');
    assert.equal((await getDoc(doc(db,`users/${uid}/categories/food`))).data().type,'expense');
  }finally{await deleteApp(app);await admin('firebase-admin/app').deleteApp(adminApp)}
});

import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { before, after, beforeEach, test } from 'node:test';
const require=createRequire(process.env.FINFLOW_TEST_DEPENDENCIES || new URL('./package.json',import.meta.url));
const {initializeTestEnvironment,assertFails,assertSucceeds}=require('@firebase/rules-unit-testing');
const {ref,uploadBytes,getBytes,deleteObject}=require('firebase/storage');
const {doc,setDoc}=require('firebase/firestore');
let env;
before(async()=>{
  env=await initializeTestEnvironment({projectId:'demo-finflow',
    storage:{host:'127.0.0.1',port:9198,rules:readFileSync(new URL('../storage.rules',import.meta.url),'utf8')},
    firestore:{host:'127.0.0.1',port:8088,rules:readFileSync(new URL('../firestore.rules',import.meta.url),'utf8')}});
});
after(async()=>{await env?.cleanup()});
beforeEach(async()=>{await env.clearStorage();await env.clearFirestore()});
const photo=(client,uid='owner')=>ref(client,`users/${uid}/profile/avatar.jpg`);
test('photo is private, bounded and only JPEG metadata is accepted',async()=>{
  const owner=env.authenticatedContext('owner').storage(),other=env.authenticatedContext('other').storage();
  const data=new Uint8Array([255,216,255,217]);
  await assertSucceeds(uploadBytes(photo(owner),data,{contentType:'image/jpeg'}));
  await assertSucceeds(getBytes(photo(owner)));await assertFails(getBytes(photo(other)));
  await assertFails(getBytes(photo(env.unauthenticatedContext().storage())));
  await assertFails(uploadBytes(photo(other),data,{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(photo(owner),data,{contentType:'application/javascript'}));
  await assertFails(uploadBytes(photo(owner),new Uint8Array(2*1024*1024+1),{contentType:'image/jpeg'}));
  await assertFails(uploadBytes(ref(owner,'users/owner/unrecognized/file'),data,{contentType:'image/jpeg'}));
  await assertSucceeds(deleteObject(photo(owner)));
});
test('pending account deletion blocks stored photos and new uploads',async()=>{
  const owner=env.authenticatedContext('owner').storage();const data=new Uint8Array([255,216,255,217]);
  await assertSucceeds(uploadBytes(photo(owner),data,{contentType:'image/jpeg'}));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),'_accountDeletions/owner'),{status:'pending'}));
  await assertFails(getBytes(photo(owner)));
  await assertFails(uploadBytes(photo(owner),data,{contentType:'image/jpeg'}));
});

test('pending data reset blocks media while completed reset permits new photos',async()=>{
  const owner=env.authenticatedContext('reset-owner').storage();const data=new Uint8Array([255,216,255,217]);
  const target=ref(owner,'users/reset-owner/profile/avatar.jpg');
  await assertSucceeds(uploadBytes(target,data,{contentType:'image/jpeg'}));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),'_dataResets/reset-owner'),{status:'pending'}));
  await assertFails(getBytes(target));await assertFails(uploadBytes(target,data,{contentType:'image/jpeg'}));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),'_dataResets/reset-owner'),{status:'complete'}));
  await assertSucceeds(uploadBytes(target,data,{contentType:'image/jpeg'}));await assertSucceeds(getBytes(target));
});

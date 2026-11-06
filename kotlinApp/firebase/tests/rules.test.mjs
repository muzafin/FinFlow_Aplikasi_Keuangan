import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
// Tool-only dependencies may be installed in the ignored build folder.
const require = createRequire(process.env.FINFLOW_TEST_DEPENDENCIES || new URL('./package.json', import.meta.url));
const {initializeTestEnvironment,assertFails,assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,updateDoc,deleteDoc,writeBatch,setLogLevel} = require('firebase/firestore');
setLogLevel('silent');
let env, db;
before(async()=>{
  env=await initializeTestEnvironment({projectId:'demo-finflow',firestore:{host:'127.0.0.1',port:8088,rules:readFileSync(new URL('../firestore.rules',import.meta.url),'utf8')}});
  db=env.authenticatedContext('rules-owner').firestore();
});
after(async()=>{await env?.cleanup()});
beforeEach(async()=>{await env.clearFirestore()});
const note={id:'note',title:'Catatan uji',body:'Tidak ada data produksi',pinned:false,createdAt:1,updatedAt:1};
const card={id:'card',name:'Kartu uji',brand:'visa',lastFour:'1234',accountId:'',expiryMonth:0,expiryYear:0,note:'',createdAt:1};
test('owner can save, edit, read and delete their note',async()=>{
  const ref=doc(db,'users/rules-owner/notes/note');
  await assertSucceeds(setDoc(ref,note));await assertSucceeds(updateDoc(ref,{body:'Diubah',updatedAt:2}));
  await assertSucceeds(getDoc(ref));await assertSucceeds(deleteDoc(ref));
});
test('anonymous and other users cannot read or write owner data',async()=>{
  for(const client of [env.unauthenticatedContext().firestore(),env.authenticatedContext('other').firestore()]) {
    await assertFails(getDoc(doc(client,'users/rules-owner/notes/note')));
    await assertFails(setDoc(doc(client,'users/rules-owner/notes/note'),note));
  }
});
test('card accepts only last four digits and rejects PAN/CVV fields',async()=>{
  const ref=doc(db,'users/rules-owner/cards/card');
  await assertSucceeds(setDoc(ref,card));
  await assertFails(setDoc(ref,{...card,lastFour:'4111111111111111'}));
  await assertFails(setDoc(ref,{...card,cvv:'123'}));
});
test('oversize notes and invalid card expiry are denied',async()=>{
  await assertFails(setDoc(doc(db,'users/rules-owner/notes/note'),{...note,body:'x'.repeat(10001)}));
  await assertFails(setDoc(doc(db,'users/rules-owner/cards/card'),{...card,expiryMonth:13,expiryYear:2028}));
});
test('unsupported collections and direct bank connection writes remain denied',async()=>{
  await assertFails(setDoc(doc(db,'users/rules-owner/connections/fake'),{connected:true}));
  await assertFails(setDoc(doc(db,'users/rules-owner/unrecognized/data'),{anything:true}));
});

const path=(collection,id,client=db,uid='rules-owner')=>doc(client,`users/${uid}/${collection}/${id}`);
async function step(label,promise) { try { return await promise; } catch(error) { error.message=`${label}: ${error.message}`;throw error; } }
const account=(id='cash',overrides={})=>({id,name:'Cash',type:'cash',currency:'IDR',openingBalanceMinor:100000,archived:false,createdAt:1,
  excludedFromTotal:false,parentId:null,icon:'account_balance_wallet',color:0xff2196f3,goalId:null,...overrides});
const category=(id='food',overrides={})=>({id,name:'Makan',type:'expense',parentId:null,icon:'restaurant',color:null,...overrides});
const tx=(id,overrides={})=>({id,type:'expense',amountMinor:1000,currency:'IDR',accountId:'cash',categoryId:'food',title:'Uji',description:'',date:'2026-10-29',
  toAccountId:null,receivedAmountMinor:null,createdAt:1,updatedAt:1,source:'manual',sourceId:null,scheduled:false,subcategoryId:null,tags:[],...overrides});
const goal=(id='goal',overrides={})=>({id,name:'Target',targetMinor:10000,savedMinor:0,currency:'IDR',targetDate:'',bucketType:'standard',protection:'flexible',
  holdingAccountId:null,legacySavedMinor:0,archived:false,revision:0,lastEventId:null,...overrides});
const event=(id,kind,referenceId,overrides={})=>({id,kind,referenceId,title:'Uji',amountMinor:1000,currency:'IDR',accountId:'cash',date:'2026-10-29',periodDate:'2026-10-29',
  transactionId:id,createdAt:1,previousEventId:null,itemIds:[],...overrides});
const bundle=(id='bundle',count=1,overrides={})=>({...note,id,title:'Bundle uji',kind:'bundle',currency:'IDR',accountId:'cash',recordAs:'single',archived:false,revision:0,lastPaymentId:null,
  items:Array.from({length:count},(_,i)=>({id:`item-${i}`,name:`Item ${i}`,amountMinor:1000,checked:false,paymentId:null})),...overrides});
async function seedBasics() {
  await assertSucceeds(setDoc(path('accounts','cash'),account()));
  await assertSucceeds(setDoc(path('categories','food'),category()));
  await assertSucceeds(setDoc(path('categories','notes'),category('notes')));
}
async function writeGoalMovement(previous,id,amount=1000,withdraw=false) {
  const holdingId=previous.holdingAccountId || `goal-${previous.id}`;
  const after={...previous,savedMinor:previous.savedMinor+(withdraw?-amount:amount),holdingAccountId:holdingId,
    legacySavedMinor:previous.holdingAccountId===null?previous.savedMinor:previous.legacySavedMinor,revision:previous.revision+1,lastEventId:id};
  const kind=withdraw?'goal_withdrawal':'goal_deposit';
  const e=event(id,kind,previous.id,{amountMinor:amount,accountId:withdraw?holdingId:'cash'});
  const t=tx(id,{type:'transfer',amountMinor:amount,receivedAmountMinor:amount,accountId:e.accountId,toAccountId:withdraw?'cash':holdingId,
    categoryId:'transfer',source:kind,sourceId:previous.id});
  const batch=writeBatch(db);
  if(previous.holdingAccountId===null)batch.set(path('accounts',holdingId),account(holdingId,{name:previous.name,type:'savings',openingBalanceMinor:0,icon:'savings',goalId:previous.id}));
  batch.set(path('goals',previous.id),after);batch.set(path('financialEvents',id),e);batch.set(path('transactions',id),t);
  await assertSucceeds(batch.commit());return after;
}
async function reverseGoalMovement(previous,id,amount,wasWithdrawal=false) {
  const after={...previous,savedMinor:previous.savedMinor+(wasWithdrawal?amount:-amount),revision:previous.revision+1,lastEventId:id,archived:false};
  const batch=writeBatch(db);batch.set(path('goals',previous.id),after);batch.delete(path('financialEvents',id));batch.delete(path('transactions',id));
  await assertSucceeds(batch.commit());return after;
}
async function payBundle(previous,id,ids) {
  const selected=previous.items.filter(item=>ids.includes(item.id));
  const amount=selected.reduce((sum,item)=>sum+item.amountMinor,0);
  const after={...previous,items:previous.items.map(item=>ids.includes(item.id)?{...item,checked:true,paymentId:id}:item),lastPaymentId:id,revision:previous.revision+1,updatedAt:2};
  const batch=writeBatch(db);batch.set(path('notes',previous.id),after);
  batch.set(path('financialEvents',id),event(id,'bundle',previous.id,{amountMinor:amount,itemIds:ids,previousEventId:previous.lastPaymentId}));
  batch.set(path('transactions',id),tx(id,{amountMinor:amount,categoryId:'notes',source:'bundle',sourceId:previous.id}));
  await assertSucceeds(batch.commit());return after;
}

test('fresh Google users can atomically initialize profile, default categories and wallet',async()=>{
  const defaults=[['food','expense'],['transport','expense'],['shopping','expense'],['health','expense'],['bills','expense'],['education','expense'],
    ['entertainment','expense'],['other_expense','expense'],['salary','income'],['business','income'],['other_income','income']];
  for(const uid of ['new-first-account','new-second-account']) {
    const email=`${uid}@example.test`;const client=env.authenticatedContext(uid,{email}).firestore();const batch=writeBatch(client);
    batch.set(doc(client,`users/${uid}`),{id:uid,name:'Pengguna baru',email,baseCurrency:'IDR',schemaVersion:2});
    defaults.forEach(([id,type])=>batch.set(path('categories',id,client,uid),category(id,{type})));
    batch.set(path('accounts','cash',client,uid),account('cash',{openingBalanceMinor:0}));
    await assertSucceeds(batch.commit());await assertSucceeds(getDoc(doc(client,`users/${uid}`)));
    await assertFails(getDoc(path('accounts','cash',client,uid==='new-first-account'?'new-second-account':'new-first-account')));
  }
});

test('profile photos stay in the owner namespace and deletion tombstones block old sessions',async()=>{
  const uid='photo-owner';const email='photo@example.test';const client=env.authenticatedContext(uid,{email}).firestore();
  const ref=doc(client,`users/${uid}`);
  const profile={id:uid,name:'Pemilik foto',email,baseCurrency:'IDR',schemaVersion:2,photoPath:`users/${uid}/profile/avatar.jpg`,photoUpdatedAt:1};
  await assertSucceeds(setDoc(ref,profile));
  await assertFails(setDoc(ref,{...profile,photoPath:'users/other/profile/avatar.jpg'}));
  await assertFails(setDoc(ref,{...profile,photoPath:'https://example.test/tracking.jpg'}));
  await assertFails(setDoc(doc(client,`_accountDeletions/${uid}`),{status:'pending'}));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),`_accountDeletions/${uid}`),{status:'pending'}));
  await assertFails(getDoc(ref));await assertFails(setDoc(ref,profile));
  await assertFails(setDoc(path('accounts','cash',client,uid),account()));
});

test('pending financial reset denies user data and completed reset permits fresh writes',async()=>{
  const uid='reset-rule-owner';const email='reset-rule-owner@example.test';const client=env.authenticatedContext(uid,{email}).firestore();
  const ref=doc(client,`users/${uid}`);const profile={id:uid,name:'Pemilik',email,baseCurrency:'IDR',schemaVersion:2};
  await assertSucceeds(setDoc(ref,profile));
  await assertFails(setDoc(doc(client,`_dataResets/${uid}`),{status:'complete'}));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),`_dataResets/${uid}`),{status:'pending'}));
  await assertFails(getDoc(ref));await assertFails(setDoc(path('accounts','cash',client,uid),account()));
  await env.withSecurityRulesDisabled(async context=>setDoc(doc(context.firestore(),`_dataResets/${uid}`),{status:'complete'}));
  await assertSucceeds(getDoc(ref));await assertSucceeds(setDoc(path('accounts','cash',client,uid),account()));
});

test('pockets and subcategories enforce parent ownership, currency and a single level',async()=>{
  await seedBasics();
  await assertSucceeds(setDoc(path('accounts','pocket'),account('pocket',{parentId:'cash',excludedFromTotal:true})));
  await assertFails(setDoc(path('accounts','nested'),account('nested',{parentId:'pocket'})));
  await assertFails(setDoc(path('accounts','foreign'),account('foreign',{parentId:'cash',currency:'USD'})));
  await assertFails(setDoc(path('accounts','self'),account('self',{parentId:'self'})));
  await assertSucceeds(setDoc(path('categories','lunch'),category('lunch',{parentId:'food'})));
  await assertFails(setDoc(path('categories','nested'),category('nested',{parentId:'lunch'})));
  await assertFails(setDoc(path('categories','wrong'),category('wrong',{parentId:'food',type:'income'})));
  await assertFails(updateDoc(path('categories','lunch'),{parentId:null}));
  await assertSucceeds(setDoc(path('transactions','tagged'),tx('tagged',{subcategoryId:'lunch',tags:['Makan siang','Kantor']})));
  await assertFails(setDoc(path('transactions','bad-tags'),tx('bad-tags',{tags:['ganda','ganda']})));
  await assertFails(setDoc(path('transactions','bad-sub'),tx('bad-sub',{subcategoryId:'food'})));
  await assertFails(setDoc(path('transactions','bad-tags'),tx('bad-tags',{tags:[12]})));
});

test('legacy documents remain readable and can acquire new optional metadata',async()=>{
  const oldAccount={id:'cash',name:'Lama',type:'cash',currency:'IDR',openingBalanceMinor:0,archived:false,createdAt:1};
  await assertSucceeds(setDoc(path('accounts','cash'),oldAccount));
  await assertSucceeds(setDoc(path('accounts','cash'),account('cash',{openingBalanceMinor:0,name:'Diperbarui'})));
  await assertSucceeds(setDoc(path('categories','food'),{id:'food',name:'Makan',type:'expense'}));
  await assertSucceeds(setDoc(path('categories','food'),category()));
  const legacyGoal={id:'old-goal',name:'Progres manual lama',targetMinor:10000,savedMinor:2000,currency:'IDR',targetDate:'2027-01-01'};
  await assertSucceeds(setDoc(path('goals',legacyGoal.id),legacyGoal));
  await assertSucceeds(setDoc(path('goals',legacyGoal.id),goal(legacyGoal.id,{...legacyGoal,revision:1})));
  const budget={id:'monthly',categoryId:'food',limitMinor:10000,currency:'IDR',month:'2026-10'};
  await assertSucceeds(setDoc(path('budgets','monthly'),budget));
  await assertSucceeds(setDoc(path('budgets','weekly'),{...budget,id:'weekly',title:'Makan minggu ini',period:'weekly',month:'2026-10-26'}));
  await assertFails(setDoc(path('budgets','invalid'),{...budget,id:'invalid',period:'weekly'}));
});

test('bank and e-wallet records accept account details while card records still reject full PAN',async()=>{
  const bank={...card,id:'bank',lastFour:'',brand:'other',recordType:'bank',accountNumber:'1234567890',ownerName:'Nama sendiri',label:'Utama',branch:'Jakarta',
    ownership:'personal',category:'main',pinned:true,color:0xff2196f3,design:6};
  await assertSucceeds(setDoc(path('cards','bank'),bank));
  await assertSucceeds(setDoc(path('cards','ewallet'),{...bank,id:'ewallet',recordType:'ewallet',accountNumber:'081234567890'}));
  await assertFails(setDoc(path('cards','card'),{...card,recordType:'card',accountNumber:'4111111111111111'}));
  await assertFails(setDoc(path('cards','bank'),{...bank,design:7}));
});

test('goal deposits and withdrawals require matching transfers and parent updates',async()=>{
  await seedBasics();let current=goal();await assertSucceeds(setDoc(path('goals','goal'),current));
  await assertFails(updateDoc(path('goals','goal'),{savedMinor:1000,revision:1}));
  await assertFails(setDoc(path('financialEvents','orphan'),event('orphan','goal_deposit','goal')));
  current=await writeGoalMovement(current,'deposit');
  await assertFails(setDoc(path('transactions','manual-savings'),tx('manual-savings',{type:'transfer',toAccountId:current.holdingAccountId,receivedAmountMinor:1000})));
  await assertFails(deleteDoc(path('goals','goal')));
  await assertFails(deleteDoc(path('financialEvents','deposit')));
  current=await writeGoalMovement(current,'withdraw',400,true);
  await assertFails(reverseGoalMovement({...current,savedMinor:0},'deposit',1000));
  current=await reverseGoalMovement(current,'withdraw',400,true);
  current=await reverseGoalMovement(current,'deposit',1000);
  const archived={...current,archived:true,revision:current.revision+1};const batch=writeBatch(db);
  batch.set(path('goals','goal'),archived);batch.update(path('accounts',current.holdingAccountId),{archived:true});await assertSucceeds(batch.commit());
});

test('legacy goal progress becomes a floor and never mints a wallet opening balance',async()=>{
  await seedBasics();let current=goal('legacy',{savedMinor:2000});await assertSucceeds(setDoc(path('goals','legacy'),current));
  current=await writeGoalMovement(current,'legacy-deposit',500);
  const holding=(await getDoc(path('accounts',current.holdingAccountId))).data();
  if(holding.openingBalanceMinor!==0 || current.legacySavedMinor!==2000)throw new Error('Legacy progress minted money');
  await assertFails(updateDoc(path('goals','legacy'),{legacySavedMinor:0,revision:2}));
  current=await writeGoalMovement(current,'legacy-withdraw',500,true);
  await assertFails(updateDoc(path('goals','legacy'),{savedMinor:1500,revision:3}));
});

test('new zero-funded goals create only their canonical zero-balance savings wallet atomically',async()=>{
  await seedBasics();const holdingId='goal-empty';let current=goal('empty',{holdingAccountId:holdingId});
  await assertFails(setDoc(path('goals','empty'),current));
  await assertFails(setDoc(path('accounts',holdingId),account(holdingId,{type:'savings',goalId:'empty',openingBalanceMinor:0})));
  const invalid=writeBatch(db);invalid.set(path('goals','empty'),current);
  invalid.set(path('accounts',holdingId),account(holdingId,{type:'savings',goalId:'empty',openingBalanceMinor:1000}));
  await assertFails(invalid.commit());
  const batch=writeBatch(db);batch.set(path('goals','empty'),current);
  batch.set(path('accounts',holdingId),account(holdingId,{type:'savings',goalId:'empty',openingBalanceMinor:0}));
  await assertSucceeds(batch.commit());
  current=await writeGoalMovement(current,'empty-deposit',500);
  current=await reverseGoalMovement(current,'empty-deposit',500);
  const archived=writeBatch(db);archived.set(path('goals','empty'),{...current,archived:true,revision:current.revision+1});
  archived.update(path('accounts',holdingId),{archived:true});await assertSucceeds(archived.commit());
});

test('bundle payments are paired, immutable and reversible with exact item totals',async()=>{
  await seedBasics();let current=bundle('bundle',2);await assertSucceeds(setDoc(path('notes','bundle'),current));
  await assertFails(updateDoc(path('notes','bundle'),{items:current.items.map(item=>({...item,paymentId:'fake'})),revision:1}));
  current=await payBundle(current,'bundle-pay',['item-0']);
  await assertFails(deleteDoc(path('notes','bundle')));await assertFails(deleteDoc(path('financialEvents','bundle-pay')));
  await assertFails(updateDoc(path('notes','bundle'),{items:current.items.map(item=>({...item,amountMinor:1})),revision:2}));
  const after={...current,items:current.items.map(item=>item.paymentId==='bundle-pay'?{...item,paymentId:null,checked:false}:item),lastPaymentId:null,revision:2,updatedAt:3};
  const batch=writeBatch(db);batch.set(path('notes','bundle'),after);batch.delete(path('financialEvents','bundle-pay'));batch.delete(path('transactions','bundle-pay'));
  await assertSucceeds(batch.commit());await assertSucceeds(deleteDoc(path('notes','bundle')));
});

test('five-item bundles support first, last and partial payments within rules limits; six are denied',async()=>{
  await seedBasics();let current=bundle('long-bundle',5);await assertSucceeds(setDoc(path('notes',current.id),current));
  await assertFails(setDoc(path('notes','oversize-bundle'),bundle('oversize-bundle',6)));
  current=await payBundle(current,'pay-first',['item-0']);
  current={...current,items:current.items.map((item,index)=>index===4?{...item,name:'Item belum dibayar diubah',amountMinor:1500,checked:true}:item),revision:current.revision+1,updatedAt:3};
  await step('edit unpaid item while another is paid',assertSucceeds(setDoc(path('notes',current.id),current)));
  current=await payBundle(current,'pay-last',['item-4']);
  current=await payBundle(current,'pay-middle',Array.from({length:3},(_,i)=>`item-${i+1}`));
  const stored=(await getDoc(path('notes',current.id))).data();
  if(stored.items.some(item=>item.paymentId===null))throw new Error('Unpaid item after completing bundle');
  current={...current,title:'Judul yang diperbarui',archived:true,revision:current.revision+1,updatedAt:4};
  await step('archive paid bundle',assertSucceeds(setDoc(path('notes',current.id),current)));
  // Undo an older payment as well as the latest, without dropping other paid items.
  for(const [payment,ids,previous] of [['pay-first',['item-0'],null],['pay-middle',['item-1','item-2','item-3'],'pay-last'],['pay-last',['item-4'],'pay-first']]) {
    current={...current,archived:false,revision:current.revision+1,updatedAt:current.updatedAt+1,
      lastPaymentId:current.lastPaymentId===payment?previous:current.lastPaymentId,
      items:current.items.map(item=>ids.includes(item.id)?{...item,paymentId:null,checked:false}:item)};
    const batch=writeBatch(db);batch.set(path('notes',current.id),current);batch.delete(path('financialEvents',payment));batch.delete(path('transactions',payment));
    await step(`reverse ${payment}`,assertSucceeds(batch.commit()));
  }
  current={...current,items:current.items.map((item,index)=>index===4?{...item,checked:true,name:'Item diubah',amountMinor:2500}:item),revision:current.revision+1,updatedAt:8};
  await step('edit unpaid item',assertSucceeds(setDoc(path('notes',current.id),current)));await assertSucceeds(deleteDoc(path('notes',current.id)));
});

test('bundle schema and event pairing reject malformed items, forged amounts and partial deletion',async()=>{
  await seedBasics();const original=bundle('safe-bundle',5);
  for(const changed of [
    {...original,items:original.items.map((item,index)=>index===4?{...item,amountMinor:-1}:item)},
    {...original,items:original.items.map((item,index)=>index===4?{...item,name:42}:item)},
    {...original,items:original.items.map((item,index)=>index===4?{...item,extra:'forbidden'}:item)},
    {...original,items:original.items.map((item,index)=>index===4?{...item,id:'item-0'}:item)},
    {...original,items:original.items.map((item,index)=>index===4?{...item,paymentId:'forged'}:item)},
  ])await assertFails(setDoc(path('notes',original.id),changed));
  await assertSucceeds(setDoc(path('notes',original.id),original));
  const id='forged-total';const after={...original,revision:1,lastPaymentId:id,items:original.items.map(item=>({...item,checked:true,paymentId:id}))};
  const batch=writeBatch(db);batch.set(path('notes',original.id),after);
  batch.set(path('financialEvents',id),event(id,'bundle',original.id,{amountMinor:1,itemIds:original.items.map(item=>item.id)}));
  batch.set(path('transactions',id),tx(id,{amountMinor:1,source:'bundle',sourceId:original.id}));
  await assertFails(batch.commit());
  const current=await payBundle(original,'full-pay',original.items.map(item=>item.id));
  await assertFails(deleteDoc(path('transactions','full-pay')));
  await assertFails(updateDoc(path('notes',current.id),{items:current.items.slice(0,4),revision:current.revision+1}));
});
test('AI consent, AI usage and Premium entitlements are owner-readable but only server-writable',async()=>{
  await env.withSecurityRulesDisabled(async context=>{
    const admin=context.firestore();
    await setDoc(doc(admin,'users/rules-owner/ai/usage'),{month:'2026-11',chat:1});
    await setDoc(doc(admin,'users/rules-owner/entitlements/premium'),{active:true,expiresAt:null});
  });
  await assertSucceeds(getDoc(doc(db,'users/rules-owner/ai/usage')));
  await assertSucceeds(getDoc(doc(db,'users/rules-owner/entitlements/premium')));
  await assertFails(setDoc(doc(db,'users/rules-owner/ai/usage'),{month:'2026-11',chat:0}));
  await assertFails(setDoc(doc(db,'users/rules-owner/ai/consent'),{chat:true}));
  await assertFails(setDoc(doc(db,'users/rules-owner/entitlements/premium'),{active:true,expiresAt:null}));
  await assertFails(getDoc(doc(db,'users/rules-owner/ai/other')));
  await assertFails(getDoc(doc(env.authenticatedContext('other').firestore(),'users/rules-owner/entitlements/premium')));
});

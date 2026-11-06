const { test } = require('node:test');
const assert = require('node:assert/strict');
const { deletionUid, deleteAccountData } = require('../account-deletion');

test('only the recently authenticated caller can request their own deletion', () => {
  const auth = { uid: 'caller', token: { auth_time: 900 } };
  assert.equal(deletionUid(auth, { confirmation: 'HAPUS' }, 1000), 'caller');
  for (const [identity, data] of [[null, {confirmation:'HAPUS'}], [auth, {}],
    [auth,{confirmation:'HAPUS',uid:'victim'}], [{...auth,token:{auth_time:699}},{confirmation:'HAPUS'}],
    [{...auth,token:{auth_time:2000}},{confirmation:'HAPUS'}]]) assert.throws(() => deletionUid(identity,data,1000));
});

test('cleanup is scoped, retryable after failure and idempotent after completion', async () => {
  const calls = []; let status='pending'; let failStorage=true;
  const missing = () => Object.assign(new Error('already removed'),{code:'auth/user-not-found'});
  const job={get:async()=>({data:()=>({status})}),update:async data=>{status=data.status;calls.push('complete')}};
  const services={
    db:{collection:name=>({doc:uid=>name==='_accountDeletions'?job:`${name}/${uid}`}),recursiveDelete:async path=>calls.push(path)},
    auth:{revokeRefreshTokens:async uid=>{calls.push(`revoke:${uid}`);throw missing()},deleteUser:async uid=>{calls.push(`auth:${uid}`);throw missing()}},
    bucket:{deleteFiles:async({prefix})=>{calls.push(prefix);if(failStorage)throw new Error('network');}},
  };
  await assert.rejects(deleteAccountData('owner',services));
  assert.equal(status,'pending');assert.ok(!calls.includes('users/owner'));
  failStorage=false;await deleteAccountData('owner',services);
  assert.equal(status,'complete');assert.deepEqual(calls.slice(-5),['users/owner/','users/owner','_dataResets/owner','auth:owner','complete']);
  const count=calls.length;await deleteAccountData('owner',services);assert.equal(calls.length,count);
});

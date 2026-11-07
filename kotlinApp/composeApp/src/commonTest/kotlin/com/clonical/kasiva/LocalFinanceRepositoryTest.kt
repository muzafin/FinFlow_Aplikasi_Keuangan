package com.clonical.kasiva

import com.clonical.kasiva.core.data.*
import com.clonical.kasiva.core.model.*
import com.clonical.kasiva.core.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class LocalFinanceRepositoryTest {
    private class Memory:LocalFinancePersistence {
        override val id="test-${newId()}"
        var content:String?=null
        var fail=false
        override fun load()=LocalFinanceLoad(content)
        override fun save(content:String):String? {if(fail)return "Disk penuh";this.content=content;return null}
    }
    private suspend fun FinanceRepository.snapshot()=observe().first{it.featuresReady}

    @Test fun offlineInitializationAndConcurrentTransactionsSurviveRepositoryRecreation()=runTest {
        val memory=Memory();val repo=FinanceRepository(local=memory)
        repo.initializeProfile("Lokal","IDR","Tunai",20000)
        assertNull(repo.currentUser);assertEquals("local",repo.ownerId)
        val writes=(1..2).map { index->async{repo.saveTransaction(Transaction(id="tx-$index",type="expense",amountMinor=1000,currency="IDR",accountId="acc_cash",categoryId="food",title="Makan"))} }
        writes.forEach{it.await()}
        val data=FinanceRepository(local=memory).snapshot()
        assertEquals("Lokal",data.profile?.name);assertEquals(2,data.transactions.size)
        assertEquals(18000L,data.balance(data.accounts.single()))
        repo.initializeProfile();assertEquals(18000L,repo.snapshot().balance(data.accounts.single()))
        assertFailsWith<IllegalArgumentException>{repo.saveTransaction(Transaction(id="invalid",type="income",amountMinor=1000,currency="IDR",accountId="acc_cash",categoryId="food",title="Tidak sesuai"))}
    }

    @Test fun failedWriteAndAbortedTransactionNeverPublishPartialMoney()=runTest {
        val memory=Memory();val repo=FinanceRepository(local=memory)
        repo.initializeProfile("Lokal","IDR","Kas",20000)
        val before=memory.content
        memory.fail=true
        assertFailsWith<IllegalStateException>{repo.saveTransaction(Transaction("fail","expense",1000,"IDR","acc_cash","food","Makan"))}
        assertEquals(before,memory.content);assertTrue(repo.snapshot().transactions.isEmpty())
        memory.fail=false
        val db=FinanceDatabase(local=memory)
        assertFailsWith<IllegalStateException>{db.runTransaction{set(db.document("users/local/accounts/acc_cash"),Account("acc_cash","Batal",openingBalanceMinor=0));error("batal")}}
        assertEquals(before,memory.content);assertEquals(20000L,repo.snapshot().balance(repo.snapshot().accounts.single()))
    }

    @Test fun existingLocalAccountGetsMissingDefaultSubcategoriesOnInitialize()=runTest {
        val memory=Memory();val repo=FinanceRepository(local=memory)
        repo.initializeProfile("Lokal","IDR","Tunai",0)
        assertEquals(defaultCategoryTree.size,repo.snapshot().categories.size)
        // Simulasikan akun lama yang dibuat sebelum subkategori bawaan ada.
        val db=FinanceDatabase(local=memory)
        db.runTransaction { defaultSubcategories.forEach { sub -> delete(db.document("users/local/categories/" + sub.id)) } }
        assertEquals(defaultCategories.size,repo.snapshot().categories.size)
        repo.initializeProfile()
        val restored=repo.snapshot().categories
        assertEquals(defaultCategoryTree.size,restored.size)
        defaultSubcategories.forEach{sub->assertEquals(sub.parentId,restored.single{it.id==sub.id}.parentId)}
    }

    @Test fun corruptVersionAndForeignNamespacesAreRejectedWithoutOverwrite()=runTest {
        val memory=Memory().apply{content="{\"version\":99,\"records\":{}}"}
        val before=memory.content
        assertFailsWith<IllegalStateException>{FinanceRepository(local=memory).initializeProfile()}
        assertEquals(before,memory.content)
        val db=FinanceDatabase(local=Memory())
        assertFailsWith<IllegalArgumentException>{db.document("users/someone-else")}
        assertFailsWith<IllegalArgumentException>{db.document("users/local/accounts/../other")}
    }

    @Test fun localGoalFundingAndBillPaymentsKeepAtomicHistoryAndBalances()=runTest {
        val memory=Memory();val repo=FinanceRepository(local=memory);repo.initializeProfile("Lokal","IDR","Kas",20000)
        repo.saveGoal(Goal("goal","Tabungan",10000))
        repo.planner.moveGoal("goal",5000,"acc_cash",today().toString(),"deposit",false)
        var data=repo.snapshot();assertEquals(20000L,data.includedAccounts().sumOf{data.balance(it)})
        assertEquals(5000L,data.goals.single().savedMinor)
        repo.planner.reverseEvent(data.financialEvents.single().id)
        data=repo.snapshot();assertEquals(0L,data.goals.single().savedMinor);assertEquals(20000L,data.balance(data.accounts.first{it.id=="acc_cash"}))
        repo.planner.saveBill(Bill("bill","Tagihan",1000,accountId="acc_cash",dueDate=today().toString()))
        repo.planner.payBill("bill",today().toString(),today().toString());repo.planner.payBill("bill",today().toString(),today().toString())
        data=repo.snapshot();assertEquals(1,data.transactions.size);assertEquals(19000L,data.balance(data.accounts.first{it.id=="acc_cash"}))
        repo.planner.reverseEvent(data.financialEvents.single().id);assertTrue(repo.snapshot().transactions.isEmpty())
    }

    @Test fun localBackupRestorePreservesMoneyAndRejectsNonemptyDestination()=runTest {
        val source=FinanceRepository(local=Memory());source.initializeProfile("Asal","IDR","Kas",20000)
        source.saveTransaction(Transaction("income","income",1000,"IDR","acc_cash","salary","Gaji"))
        val backup=FinanceBackup(ownerId="local",data=source.snapshot())
        val destination=FinanceRepository(local=Memory());destination.initializeProfile()
        destination.restoreBackup(BackupCodec.decode(BackupCodec.encode(backup)),false,{}, {})
        val restored=destination.snapshot();assertEquals(21000L,restored.includedAccounts().sumOf{restored.balance(it)})
        assertEquals("Asal",restored.profile?.name)
        assertFailsWith<IllegalArgumentException>{destination.restoreBackup(backup,false,{}, {})}
        destination.restoreBackup(backup,true,{}, {})
        assertEquals(1,destination.snapshot().transactions.size)
    }

    @Test fun localResetPreservesIdentityAndAtomicallyRestoresEmptyDefaults()=runTest {
        val memory=Memory();val repo=FinanceRepository(local=memory)
        repo.initializeProfile("Nama Tetap","USD","Kas",20000)
        repo.saveTransaction(Transaction("old","expense",1000,"USD","acc_cash","food","Lama"))
        repo.planner.saveNote(FinanceNote("note","Catatan lama"))
        val before=memory.content
        memory.fail=true
        assertFailsWith<IllegalStateException>{repo.clearLocalData()}
        assertEquals(before,memory.content)
        memory.fail=false
        repo.clearLocalData()
        val data=FinanceRepository(local=memory).snapshot()
        assertEquals("Nama Tetap",data.profile?.name);assertEquals("USD",data.profile?.baseCurrency)
        assertTrue(data.transactions.isEmpty());assertTrue(data.notes.isEmpty())
        assertEquals("USD",data.accounts.single().currency);assertEquals(0L,data.balance(data.accounts.single()))
        assertEquals(defaultCategoryTree,data.categories.sortedBy{it.id}.let{sorted->defaultCategoryTree.map{category->sorted.single{it.id==category.id}}})
    }
}

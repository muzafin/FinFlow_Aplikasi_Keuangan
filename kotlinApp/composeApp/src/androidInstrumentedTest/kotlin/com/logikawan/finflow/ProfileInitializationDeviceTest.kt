package com.logikawan.finflow

import androidx.test.platform.app.InstrumentationRegistry
import com.logikawan.finflow.core.data.FinanceRepository
import com.logikawan.finflow.core.model.Account
import com.logikawan.finflow.core.model.Profile
import com.logikawan.finflow.core.model.defaultCategories
import dev.gitlive.firebase.*
import dev.gitlive.firebase.auth.*
import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class ProfileInitializationDeviceTest {
    private lateinit var app: FirebaseApp
    @Before fun setup() {
        app = Firebase.initialize(InstrumentationRegistry.getInstrumentation().targetContext,
            FirebaseOptions(applicationId="1:123:android:test", apiKey="fake-api-key", projectId="demo-finflow"), "profile-${UUID.randomUUID()}")
        Firebase.auth(app).useEmulator("127.0.0.1",9098)
        Firebase.firestore(app).useEmulator("127.0.0.1",8088)
        Firebase.firestore(app).settings = firestoreSettings { cacheSettings=memoryCacheSettings{} }
    }
    @After fun teardown() = runBlocking { if (::app.isInitialized) app.delete() }

    @Test fun twoNewAccountsInitializeAndStayIsolated() = runBlocking {
        withTimeout(60000) {
            val auth=Firebase.auth(app); val db=Firebase.firestore(app); val repo=FinanceRepository(app)
            var firstUid=""
            repeat(2) { index ->
                val user=auth.createUserWithEmailAndPassword("profile-${UUID.randomUUID()}@example.test","FinFlow-test-123!").user!!
                repo.initializeProfile("Akun $index")
                val data=repo.observe().first { it.featuresReady }
                assertEquals(user.uid,data.profile?.id)
                assertEquals("Akun $index",data.profile?.name)
                assertEquals(defaultCategories.size,data.categories.size)
                assertEquals(0L,data.balance(data.accounts.single()))
                assertTrue(data.transactions.isEmpty())
                if (index==0) firstUid=user.uid else {
                    try { db.collection("users").document(firstUid).get(Source.SERVER); fail("Cross-account read allowed") }
                    catch (expected: FirebaseFirestoreException) { assertEquals(FirestoreExceptionCode.PERMISSION_DENIED,expected.code) }
                }
                auth.signOut()
            }
        }
    }

    @Test fun retryPreservesExistingProfileAndWallet() = runBlocking {
        withTimeout(60000) {
            val auth=Firebase.auth(app); val db=Firebase.firestore(app); val repo=FinanceRepository(app)
            val user=auth.createUserWithEmailAndPassword("profile-${UUID.randomUUID()}@example.test","FinFlow-test-123!").user!!
            repo.initializeProfile("Nama pilihan")
            val root=db.collection("users").document(user.uid)
            val original=root.collection("accounts").document("acc_cash").get().data<Account>()
            repo.initializeProfile()
            assertEquals("Nama pilihan",root.get().data<Profile>().name)
            assertEquals(original,root.collection("accounts").document("acc_cash").get().data<Account>())
        }
    }
}

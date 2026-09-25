package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthSessionManager
import com.example.data.model.AuthProvider
import com.example.data.model.UserAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAuthSecurityTest {

    private lateinit var context: Context
    private lateinit var sessionManager: AuthSessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = AuthSessionManager(context)
        sessionManager.clearSession()
    }

    @Test
    fun `test admin email without custom claim does not grant super admin role`() {
        val userWithoutClaim = UserAccount(
            id = "test_uid_1",
            email = "m.k3shka@gmail.com",
            displayName = "Mohamed Keshka",
            role = "USER"
        )

        sessionManager.saveUserSession(userWithoutClaim)
        val retrieved = sessionManager.getUserSession()

        assertFalse("User with admin email but no admin claim must not be super admin", retrieved?.isSuperAdmin == true)
        assertEquals("Role must remain USER", "USER", retrieved?.role)
    }

    @Test
    fun `test admin claim grants super admin role`() {
        val userWithAdminClaim = UserAccount(
            id = "test_uid_2",
            email = "m.k3shka@gmail.com",
            displayName = "Mohamed Keshka",
            role = "SUPER_ADMIN"
        )

        sessionManager.saveUserSession(userWithAdminClaim)
        val retrieved = sessionManager.getUserSession()

        assertTrue("User with SUPER_ADMIN role from custom claim must be super admin", retrieved?.isSuperAdmin == true)
        assertEquals("Role must be SUPER_ADMIN", "SUPER_ADMIN", retrieved?.role)
    }

    @Test
    fun `test sign out clears session and admin privileges immediately`() {
        val adminUser = UserAccount(
            id = "test_uid_3",
            email = "m.k3shka@gmail.com",
            displayName = "Mohamed Keshka",
            role = "SUPER_ADMIN"
        )

        sessionManager.saveUserSession(adminUser)
        assertTrue(sessionManager.getUserSession()?.isSuperAdmin == true)

        sessionManager.clearSession()
        val retrievedAfterSignOut = sessionManager.getUserSession()

        assertFalse("Session must be cleared and user must not be admin", retrievedAfterSignOut?.isSuperAdmin == true)
    }
}

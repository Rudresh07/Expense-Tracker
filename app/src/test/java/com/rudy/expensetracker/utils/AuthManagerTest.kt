package com.rudy.expensetracker.utils

import com.rudy.expensetracker.database.CategoryDao
import com.rudy.expensetracker.database.ExpenseDao
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthManagerTest {

    private lateinit var prefs: PreferenceManager
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var authManager: AuthManager

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
        expenseDao = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        authManager = AuthManager(prefs, expenseDao, categoryDao)
    }

    @Test
    fun `login sets login status to true`() {
        authManager.login("test@example.com", "Alice")

        verify { prefs.setUserLoginStatus(true) }
    }

    @Test
    fun `login stores email in prefs`() {
        authManager.login("test@example.com", "Alice")

        verify { prefs.setUserEmail("test@example.com") }
    }

    @Test
    fun `login stores name in prefs`() {
        authManager.login("test@example.com", "Alice")

        verify { prefs.setUserName("Alice") }
    }

    @Test
    fun `login uses default name User when name is not provided`() {
        authManager.login("test@example.com")

        verify { prefs.setUserName("User") }
    }

    @Test
    fun `logout sets login status to false`() {
        authManager.logout()

        verify { prefs.setUserLoginStatus(false) }
    }

    @Test
    fun `logout clears all user data from prefs`() {
        authManager.logout()

        verify { prefs.clearUserData() }
    }

    @Test
    fun `isUserLoggedIn returns true when user is logged in`() {
        every { prefs.getUserLoginStatus() } returns true

        assertTrue(authManager.isUserLoggedIn())
    }

    @Test
    fun `isUserLoggedIn returns false when user is not logged in`() {
        every { prefs.getUserLoginStatus() } returns false

        assertFalse(authManager.isUserLoggedIn())
    }

    @Test
    fun `getUserEmail returns email stored in prefs`() {
        every { prefs.getUserEmail() } returns "alice@example.com"

        assertEquals("alice@example.com", authManager.getUserEmail())
    }

    @Test
    fun `getUserName returns stored name when available`() {
        every { prefs.getUserName() } returns "Alice"

        assertEquals("Alice", authManager.getUserName())
    }

    @Test
    fun `getUserName derives capitalized name from email when stored name is empty`() {
        every { prefs.getUserName() } returns ""
        every { prefs.getUserEmail() } returns "alice@example.com"

        assertEquals("Alice", authManager.getUserName())
    }

    @Test
    fun `getUserName uses email username part before @ symbol`() {
        every { prefs.getUserName() } returns ""
        every { prefs.getUserEmail() } returns "john.doe@example.com"

        assertEquals("John.doe", authManager.getUserName())
    }

    @Test
    fun `getUserName does not alter already capitalized first letter`() {
        every { prefs.getUserName() } returns ""
        every { prefs.getUserEmail() } returns "Bob@example.com"

        assertEquals("Bob", authManager.getUserName())
    }
}

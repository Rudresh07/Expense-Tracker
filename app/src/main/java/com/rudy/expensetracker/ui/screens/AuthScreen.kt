package com.rudy.expensetracker.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.rudy.expensetracker.analytics.FirebaseAnalytics
import com.rudy.expensetracker.ui.theme.Orange
import com.rudy.expensetracker.utils.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.compose.getKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var userName by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLogin by rememberSaveable { mutableStateOf(true) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val isDarkTheme = isSystemInDarkTheme()

    val authManager: AuthManager = getKoin().get()
    val firebaseEvents: FirebaseAnalytics = getKoin().get()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp


    // Auto-generate username from email
    LaunchedEffect(email) {
        userName = if (email.isNotEmpty() && email.contains("@")) {
            email.substringBefore("@").replace(".", "_").replace("-", "_")
        } else {
            ""
        }
    }

    // Theme colors
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textSecondaryColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color.Gray
    val errorBackgroundColor = if (isDarkTheme) Color(0xFF3D1A1A) else Color(0xFFFFEBEE)
    val errorTextColor = if (isDarkTheme) Color(0xFFFF6B6B) else Color(0xFFD32F2F)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        if (isLandscape) {
            // Landscape Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Branding
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BrandingSection(isDarkTheme, textColor, textSecondaryColor, isLogin)
                }

                // Right side - Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthForm(
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        userName = userName,
                        passwordVisible = passwordVisible,
                        confirmPasswordVisible = confirmPasswordVisible,
                        isLogin = isLogin,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        isDarkTheme = isDarkTheme,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        errorBackgroundColor = errorBackgroundColor,
                        errorTextColor = errorTextColor,
                        onEmailChange = {
                            email = it
                            errorMessage = ""
                        },
                        onPasswordChange = {
                            password = it
                            errorMessage = ""
                        },
                        onConfirmPasswordChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        onAuthModeToggle = {
                            isLogin = !isLogin
                            errorMessage = ""
                            password = ""
                            confirmPassword = ""
                        },
                        onAuthClick = {
                            handleAuthentication(
                                email, password, confirmPassword, userName, isLogin,
                                onLoadingChange = { isLoading = it },
                                onErrorChange = { errorMessage = it },
                                authManager = authManager,
                                onSuccess = onLoginSuccess
                            )
                            firebaseEvents.logEvent("auth_attempt", mapOf("mode" to if (isLogin) "login" else "register"))

                        },
                        onUseDemoCredentials = {
                            email = "demo@example.com"
                            password = "password123"
                            confirmPassword = "password123"
                            errorMessage = ""
                        }
                    )
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                BrandingSection(isDarkTheme, textColor, textSecondaryColor, isLogin)

                Spacer(modifier = Modifier.height(40.dp))

                AuthForm(
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    userName = userName,
                    passwordVisible = passwordVisible,
                    confirmPasswordVisible = confirmPasswordVisible,
                    isLogin = isLogin,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    isDarkTheme = isDarkTheme,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    errorBackgroundColor = errorBackgroundColor,
                    errorTextColor = errorTextColor,
                    onEmailChange = {
                        email = it
                        errorMessage = ""
                    },
                    onPasswordChange = {
                        password = it
                        errorMessage = ""
                    },
                    onConfirmPasswordChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                    onAuthModeToggle = {
                        isLogin = !isLogin
                        errorMessage = ""
                        password = ""
                        confirmPassword = ""
                    },
                    onAuthClick = {
                        handleAuthentication(
                            email, password, confirmPassword, userName, isLogin,
                            onLoadingChange = { isLoading = it },
                            onErrorChange = { errorMessage = it },
                            authManager = authManager,
                            onSuccess = onLoginSuccess
                        )
                        firebaseEvents.logEvent("auth_attempt", mapOf("mode" to if (isLogin) "login" else "register"))
                    },
                    onUseDemoCredentials = {
                        email = "demo@example.com"
                        password = "password123"
                        confirmPassword = "password123"
                        errorMessage = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun BrandingSection(
    isDarkTheme: Boolean,
    textColor: Color,
    textSecondaryColor: Color,
    isLogin: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /*Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = textSecondaryColor
                )
            }
        }*/

        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(containerColor = Orange),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Expense Tracker",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLogin) "Welcome back!" else "Create your account",
            fontSize = 16.sp,
            color = textSecondaryColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthForm(
    email: String,
    password: String,
    confirmPassword: String,
    userName: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    isLogin: Boolean,
    isLoading: Boolean,
    errorMessage: String,
    isDarkTheme: Boolean,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    errorBackgroundColor: Color,
    errorTextColor: Color,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onAuthModeToggle: () -> Unit,
    onAuthClick: () -> Unit,
    onUseDemoCredentials: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error Message
        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = errorBackgroundColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = errorTextColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email", color = textSecondaryColor) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = Orange)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMessage.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.5f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = Orange
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Username Field (only for registration and auto-generated)
        if (!isLogin) {
            OutlinedTextField(
                value = userName,
                onValueChange = { /* Read-only, auto-generated */ },
                label = { Text("Username (auto-generated)", color = textSecondaryColor) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Username", tint = Orange)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = textSecondaryColor.copy(alpha = 0.3f),
                    disabledTextColor = textSecondaryColor,
                    disabledLabelColor = textSecondaryColor.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password", color = textSecondaryColor) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password", tint = Orange)
            },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = textSecondaryColor
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMessage.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.5f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = Orange
            )
        )

        // Confirm Password Field (only for registration)
        if (!isLogin) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password", color = textSecondaryColor) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = Orange)
                },
                trailingIcon = {
                    IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = textSecondaryColor
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = errorMessage.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Orange,
                    unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.5f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = Orange
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login/Register Button
        Button(
            onClick = onAuthClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                disabledContainerColor = Orange.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isLogin) "Login" else "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch between Login/Register
        TextButton(onClick = onAuthModeToggle) {
            Text(
                text = if (isLogin) "Don't have an account? Register" else "Already have an account? Login",
                color = Orange,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Credentials Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Demo Credentials",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Orange
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email: demo@example.com\nPassword: password123",
                    fontSize = 12.sp,
                    color = textSecondaryColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onUseDemoCredentials) {
                    Text(
                        "Use Demo Credentials",
                        color = Orange,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
private fun handleAuthentication(
    email: String,
    password: String,
    confirmPassword: String,
    userName: String,
    isLogin: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String) -> Unit,
    authManager: AuthManager,
    onSuccess: () -> Unit
) {
    // Validation
    if (email.isEmpty() || password.isEmpty()) {
        onErrorChange("Please fill in all required fields")
        return
    }

    if (!isLogin && confirmPassword.isEmpty()) {
        onErrorChange("Please confirm your password")
        return
    }

    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        onErrorChange("Please enter a valid email address")
        return
    }

    if (password.length < 6) {
        onErrorChange("Password must be at least 6 characters long")
        return
    }

    if (!isLogin && password != confirmPassword) {
        onErrorChange("Passwords do not match")
        return
    }

    onLoadingChange(true)
    onErrorChange("")

    val firebaseAuth = FirebaseAuth.getInstance()

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            if (isLogin) {
                // LOGIN
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = firebaseAuth.currentUser
                user?.let {
                    authManager.login(it.email ?: email, userName.ifEmpty { "User" })
                }
                onSuccess()
            } else {
                // REGISTER
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = firebaseAuth.currentUser
                user?.let {
                    // Optionally update displayName
                    val generatedUserName = userName.ifEmpty {
                        email.substringBefore("@").replace(".", "_").replace("-", "_")
                    }
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(generatedUserName)
                        .build()
                    it.updateProfile(profileUpdates).await()

                    authManager.login(it.email ?: email, generatedUserName)
                }
                onSuccess()
            }
        } catch (e: Exception) {
            onErrorChange(e.message ?: "Authentication failed")
        } finally {
            onLoadingChange(false)
        }
    }
}

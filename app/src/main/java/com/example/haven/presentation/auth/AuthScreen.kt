package com.example.haven.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haven.R
import com.example.haven.data.repository.FamilyRepository
import com.example.haven.ui.theme.*
import kotlinx.coroutines.launch

enum class AuthScreenState {
    WELCOME,
    SIGN_IN,
    REGISTER
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    var screenState by remember { mutableStateOf(AuthScreenState.WELCOME) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = HavenBackgroundWash
    ) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "auth_screen_transition"
        ) { state ->
            when (state) {
                AuthScreenState.WELCOME -> {
                    WelcomeContent(
                        onGetStarted = { screenState = AuthScreenState.REGISTER },
                        onAlreadyHaveAccount = { screenState = AuthScreenState.SIGN_IN }
                    )
                }
                AuthScreenState.SIGN_IN -> {
                    SignInContent(
                        onBack = { screenState = AuthScreenState.WELCOME },
                        onNavigateToRegister = { screenState = AuthScreenState.REGISTER },
                        onAuthSuccess = onAuthSuccess,
                        repository = repository
                    )
                }
                AuthScreenState.REGISTER -> {
                    RegisterContent(
                        onBack = { screenState = AuthScreenState.WELCOME },
                        onNavigateToSignIn = { screenState = AuthScreenState.SIGN_IN },
                        onAuthSuccess = onAuthSuccess,
                        repository = repository
                    )
                }
            }
        }
    }
}

/**
 * Data model representing each slide in the Get Started Onboarding Carousel
 */
private data class OnboardingSlide(
    val title: String,
    val description: String,
    val drawableRes: Int
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        title = "Safer families.\nHappier home lives.",
        description = "Keep your loved ones safe, organized\nand connected — all in one place.",
        drawableRes = R.drawable.ic_haven_welcome_scenic
    ),
    OnboardingSlide(
        title = "Keep everyone safe",
        description = "Real-time location, check-ins and\ninstant alerts when it matters most.",
        drawableRes = R.drawable.ic_haven_onboarding_safety
    ),
    OnboardingSlide(
        title = "Stay organized together",
        description = "Tasks, routines, calendars and\nmore — all in one place.",
        drawableRes = R.drawable.ic_haven_onboarding_organized
    ),
    OnboardingSlide(
        title = "A happier home",
        description = "Different days.\nA stronger tomorrow.",
        drawableRes = R.drawable.ic_haven_onboarding_happier_home
    )
)

/**
 * 1. Welcome / Onboarding Carousel Screen — UI Board 01/40
 */
@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onAlreadyHaveAccount: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()
    val activePageIndex by remember { derivedStateOf { pagerState.currentPage } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Logo on left/center & Skip button on right (always reserved space to prevent relayout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_haven_logo),
                    contentDescription = "Haven Logo",
                    modifier = Modifier.size(34.dp),
                    tint = HavenPrimaryTeal
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Haven",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HavenPrimaryTeal,
                        letterSpacing = (-0.5).sp
                    )
                )
            }

            TextButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(onboardingSlides.size - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        alpha = if (activePageIndex < onboardingSlides.size - 1) 1f else 0f
                    },
                enabled = activePageIndex < onboardingSlides.size - 1
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = HavenTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Dedicated Slide Title & Subtitle Area (Fixed height: exactly 96.dp so text never shifts or remeasures)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            val slide = onboardingSlides[activePageIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = HavenTextPrimary,
                        lineHeight = 28.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HavenTextSecondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Horizontal Pager ONLY for the center illustrations (Lightweight swiping)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) { pageIndex ->
            val slide = onboardingSlides[pageIndex]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = slide.drawableRes),
                    contentDescription = slide.title,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(220.dp)
                )
            }
        }

        // Bottom Controls: Fixed height containers
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                for (iteration in onboardingSlides.indices) {
                    val isSelected = activePageIndex == iteration
                    Box(
                        modifier = Modifier
                            .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                            .clip(if (isSelected) RoundedCornerShape(4.dp) else CircleShape)
                            .background(if (isSelected) HavenPrimaryTeal else Color(0xFFCBD5E1))
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(iteration)
                                }
                            }
                    )
                }
            }

            // Fixed-Height Button Container (56.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                when {
                    activePageIndex == 0 -> {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(1)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HavenPrimaryTeal,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Get Started",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    activePageIndex < onboardingSlides.size - 1 -> {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(activePageIndex + 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(56.dp)
                                .background(HavenPrimaryTeal, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onGetStarted,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HavenPrimaryTeal,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary: I already have an account
            TextButton(
                onClick = onAlreadyHaveAccount,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "I already have an account",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = HavenPrimaryTeal
                    )
                )
            }
        }
    }
}

/**
 * 2. Sign In Screen — UI Board 02/40
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInContent(
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onAuthSuccess: () -> Unit,
    repository: FamilyRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("lucix910@gmail.com") }
    var password by remember { mutableStateOf("SecurePassword123!") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var keepSignedIn by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HavenSurfaceWhite)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = HavenTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Heading
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = HavenTextPrimary
            )
        )
        Text(
            text = "Glad to see you again.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = HavenTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim(); errorMessage = null },
            label = { Text("Email Address") },
            placeholder = { Text("your@email.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = HavenTextSecondary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenSurfaceWhite,
                unfocusedContainerColor = HavenSurfaceWhite,
                focusedBorderColor = HavenPrimaryTeal,
                unfocusedBorderColor = HavenBorderLight,
                focusedTextColor = HavenTextPrimary,
                unfocusedTextColor = HavenTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = HavenTextSecondary
                )
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = HavenTextSecondary
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenSurfaceWhite,
                unfocusedContainerColor = HavenSurfaceWhite,
                focusedBorderColor = HavenPrimaryTeal,
                unfocusedBorderColor = HavenBorderLight,
                focusedTextColor = HavenTextPrimary,
                unfocusedTextColor = HavenTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Keep me signed in & Forgot password row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { keepSignedIn = !keepSignedIn }
            ) {
                Checkbox(
                    checked = keepSignedIn,
                    onCheckedChange = { keepSignedIn = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = HavenPrimaryTeal,
                        uncheckedColor = HavenBorderLight
                    )
                )
                Text(
                    text = "Keep me signed in",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HavenTextPrimary
                    )
                )
            }

            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = HavenPrimaryTeal
                ),
                modifier = Modifier.clickable { /* Reset password dialog */ }
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFF991B1B),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Sign In Button (Deep Teal)
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter your email and password."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val res = repository.login(email, password)
                    isLoading = false
                    if (res.isSuccess) {
                        onAuthSuccess()
                    } else {
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Invalid email or password."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HavenPrimaryTeal,
                contentColor = Color.White
            ),
            enabled = !isLoading && !isGoogleLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "or continue with" Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = HavenBorderLight)
            Text(
                text = "  or continue with  ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HavenTextSecondary
                )
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = HavenBorderLight)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Sign-In Button (Elevated White Card with 4-Color Logo)
        Button(
            onClick = {
                isGoogleLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val result = repository.signInWithGoogle(context)
                    isGoogleLoading = false
                    if (result.isSuccess) {
                        onAuthSuccess()
                    } else {
                        val err = result.exceptionOrNull()
                        if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                            errorMessage = err?.localizedMessage ?: "Google Sign-In failed."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
            enabled = !isLoading && !isGoogleLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = HavenPrimaryTeal,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(28.dp))

        // Footer: Don't have an account? Create account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = HavenTextSecondary
                )
            )
            Text(
                text = "Create account",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HavenPrimaryTeal
                ),
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

/**
 * 3. Register Screen — UI Board 02/40 (Register Variant)
 */
@Composable
private fun RegisterContent(
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onAuthSuccess: () -> Unit,
    repository: FamilyRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HavenSurfaceWhite)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = HavenTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Heading
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = HavenTextPrimary
            )
        )
        Text(
            text = "Start protecting and organizing your family.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = HavenTextSecondary
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Full Name Field
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; errorMessage = null },
            label = { Text("Full / Display Name") },
            placeholder = { Text("e.g. Sahan Pramuditha") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = HavenTextSecondary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenSurfaceWhite,
                unfocusedContainerColor = HavenSurfaceWhite,
                focusedBorderColor = HavenPrimaryTeal,
                unfocusedBorderColor = HavenBorderLight,
                focusedTextColor = HavenTextPrimary,
                unfocusedTextColor = HavenTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim(); errorMessage = null },
            label = { Text("Email Address") },
            placeholder = { Text("your@email.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = HavenTextSecondary
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenSurfaceWhite,
                unfocusedContainerColor = HavenSurfaceWhite,
                focusedBorderColor = HavenPrimaryTeal,
                unfocusedBorderColor = HavenBorderLight,
                focusedTextColor = HavenTextPrimary,
                unfocusedTextColor = HavenTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password (min 8 characters)") },
            placeholder = { Text("Create strong password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = HavenTextSecondary
                )
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = HavenTextSecondary
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HavenSurfaceWhite,
                unfocusedContainerColor = HavenSurfaceWhite,
                focusedBorderColor = HavenPrimaryTeal,
                unfocusedBorderColor = HavenBorderLight,
                focusedTextColor = HavenTextPrimary,
                unfocusedTextColor = HavenTextPrimary
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFF991B1B),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Create Account Button (Deep Teal)
        Button(
            onClick = {
                if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all required fields."
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val res = repository.register(email, password, displayName)
                    isLoading = false
                    if (res.isSuccess) {
                        onAuthSuccess()
                    } else {
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Registration failed."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HavenPrimaryTeal,
                contentColor = Color.White
            ),
            enabled = !isLoading && !isGoogleLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "or continue with" Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = HavenBorderLight)
            Text(
                text = "  or continue with  ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HavenTextSecondary
                )
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = HavenBorderLight)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Sign-In Button
        Button(
            onClick = {
                isGoogleLoading = true
                errorMessage = null
                coroutineScope.launch {
                    val result = repository.signInWithGoogle(context)
                    isGoogleLoading = false
                    if (result.isSuccess) {
                        onAuthSuccess()
                    } else {
                        val err = result.exceptionOrNull()
                        if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                            errorMessage = err?.localizedMessage ?: "Google Sign-In failed."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
            enabled = !isLoading && !isGoogleLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = HavenPrimaryTeal,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1F1F)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(28.dp))

        // Footer: Already have an account? Sign In
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = HavenTextSecondary
                )
            )
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HavenPrimaryTeal
                ),
                modifier = Modifier.clickable { onNavigateToSignIn() }
            )
        }
    }
}

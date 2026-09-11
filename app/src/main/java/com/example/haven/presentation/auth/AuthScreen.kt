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
import androidx.compose.ui.layout.ContentScale
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
        drawableRes = R.drawable.img_haven_welcome_scenery
    ),
    OnboardingSlide(
        title = "Family First",
        description = "Stay connected, support each other\nand be there, always.",
        drawableRes = R.drawable.img_haven_onboarding_family
    ),
    OnboardingSlide(
        title = "Greater Safety",
        description = "Real-time locations, safe zones\nand instant alerts for peace of mind.",
        drawableRes = R.drawable.img_haven_onboarding_safety
    ),
    OnboardingSlide(
        title = "More Organization",
        description = "Manage tasks, routines, calendars\nand everything your family needs.",
        drawableRes = R.drawable.img_haven_onboarding_organization
    ),
    OnboardingSlide(
        title = "A Brighter Tomorrow",
        description = "Healthier, safer and happier\nhome lives together.",
        drawableRes = R.drawable.img_haven_onboarding_tomorrow
    )
)

/**
 * 1. Welcome / Onboarding Carousel Screen — Matching Haven Onboarding UI Specification Board
 */
@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onAlreadyHaveAccount: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()
    val activePageIndex by remember { derivedStateOf { pagerState.currentPage } }

    // Preload & memoize drawable painters so swiping between pages does zero bitmap decoding on main thread
    val welcomePainter = painterResource(id = R.drawable.img_haven_welcome_scenery)
    val slidePainters = onboardingSlides.map { slide ->
        if (slide.drawableRes != 0) painterResource(id = slide.drawableRes) else null
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        if (pageIndex == 0) {
            // Screen 01: Welcome
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HavenBackgroundWash)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Skip button + Haven Logo + Title + Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Top row with Skip button on right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(onboardingSlides.size - 1)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = HavenTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Centered Haven Stacked Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_haven_stacked_logo),
                        contentDescription = "Haven Logo",
                        modifier = Modifier.height(78.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title and Subtitle
                    Text(
                        text = "Safer families.\nHappier home lives.",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HavenTextPrimary,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keep your loved ones safe, organized\nand connected — all in one place.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = HavenTextSecondary,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Full-bleed scenery illustration matching spec board
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = welcomePainter,
                        contentDescription = "Haven Home Welcome",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                }

                // Bottom Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                ) {
                    // 4 Dot Indicators (first is active)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        for (dotIndex in 0 until 4) {
                            val isSelected = dotIndex == 0
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) HavenTextPrimary else Color(0xFFCBD5E1))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(if (dotIndex == 0) 0 else dotIndex + 1)
                                        }
                                    }
                            )
                        }
                    }

                    // Get Started Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(1)
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
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub-caption
                    Text(
                        text = "A safer, more organized tomorrow\nstarts together.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HavenTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        } else {
            // Screens 02, 03, 04, 05: Feature slides with top full-bleed illustration & wavy transition
            val slide = onboardingSlides[pageIndex]
            val featureIndex = pageIndex - 1 // 0, 1, 2, 3

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HavenBackgroundWash)
                    .navigationBarsPadding()
            ) {
                // Top Half: Vector artwork with wavy transition + Skip button overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.25f)
                ) {
                    val currentPainter = slidePainters.getOrNull(pageIndex)
                    if (currentPainter != null) {
                        Image(
                            painter = currentPainter,
                            contentDescription = slide.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.BottomCenter)
                        )
                    }

                    // Skip button on top right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp, end = 16.dp)
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(onboardingSlides.size - 1)
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = HavenTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Bottom Half: Clean information section matching spec board
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.75f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title and Description
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HavenTextPrimary,
                                lineHeight = 32.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = slide.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = HavenTextSecondary,
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom: 4 Dot Indicators + Action Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 4 Dot Indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 22.dp)
                        ) {
                            for (dotIndex in 0 until 4) {
                                val isSelected = dotIndex == featureIndex
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) HavenTextPrimary else Color(0xFFCBD5E1))
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(dotIndex + 1)
                                            }
                                        }
                                )
                            }
                        }

                        // Button: "Next →" for 02, 03, 04; "Get Started →" for 05
                        Button(
                            onClick = {
                                if (pageIndex < onboardingSlides.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pageIndex + 1)
                                    }
                                } else {
                                    onGetStarted()
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
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (pageIndex == onboardingSlides.size - 1) "Get Started" else "Next",
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
                }
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

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

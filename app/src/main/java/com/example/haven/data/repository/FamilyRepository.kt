package com.example.haven.data.repository

import com.example.haven.core.network.FamilyDto
import com.example.haven.core.network.HavenApiClient
import com.example.haven.data.model.FamilyMemberUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class ActiveFamilyState(
    val id: String = "",
    val name: String = "",
    val invitationCode: String = "",
    val primaryHousehold: String = "",
    val members: List<FamilyMemberUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CurrentUserState(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val token: String = ""
)

class FamilyRepository(
    private val apiClient: HavenApiClient = HavenApiClient()
) {
    private val _currentUser = MutableStateFlow<CurrentUserState?>(null)
    val currentUser: StateFlow<CurrentUserState?> = _currentUser.asStateFlow()

    private val _currentFamily = MutableStateFlow<ActiveFamilyState?>(null)
    val currentFamily: StateFlow<ActiveFamilyState?> = _currentFamily.asStateFlow()

    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
        get() = com.google.firebase.auth.FirebaseAuth.getInstance()

    suspend fun register(email: String, password: String, displayName: String): Result<com.example.haven.core.network.AuthTokenDto> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Create account in Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = authResult.user ?: throw IllegalStateException("Firebase user was null after creation")

            // 2. Set Firebase user display name profile
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            fbUser.updateProfile(profileUpdates).await()

            // 3. Obtain real Firebase ID Token
            val idTokenResult = fbUser.getIdToken(true).await()
            val idToken = idTokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token")

            // 4. Authenticate with Cloud Run backend /auth/me to provision user in Neon PostgreSQL
            apiClient.setAuthToken(idToken)
            val profileRes = apiClient.getCurrentUserProfile()
            if (profileRes.isFailure) {
                return@withContext Result.failure(profileRes.exceptionOrNull() ?: Exception("Failed to provision user profile on backend"))
            }
            val profile = profileRes.getOrThrow()

            val tokenDto = com.example.haven.core.network.AuthTokenDto(
                access_token = idToken,
                user_id = profile.id,
                email = profile.email,
                display_name = profile.display_name,
                family_id = profile.family_id,
                family_name = profile.family_name
            )

            _currentUser.value = CurrentUserState(
                userId = tokenDto.user_id,
                email = tokenDto.email,
                displayName = tokenDto.display_name,
                token = tokenDto.access_token
            )
            saveAuth(tokenDto.access_token, tokenDto.user_id, tokenDto.email, tokenDto.display_name)
            Result.success(tokenDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<com.example.haven.core.network.AuthTokenDto> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Sign in with Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val fbUser = authResult.user ?: throw IllegalStateException("Firebase user was null after login")

            // 2. Obtain real Firebase ID Token
            val idTokenResult = fbUser.getIdToken(true).await()
            val idToken = idTokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token")

            // 3. Authenticate with Cloud Run backend /auth/me to fetch profile from Neon PostgreSQL
            apiClient.setAuthToken(idToken)
            val profileRes = apiClient.getCurrentUserProfile()
            if (profileRes.isFailure) {
                return@withContext Result.failure(profileRes.exceptionOrNull() ?: Exception("Failed to fetch user profile from backend"))
            }
            val profile = profileRes.getOrThrow()

            val tokenDto = com.example.haven.core.network.AuthTokenDto(
                access_token = idToken,
                user_id = profile.id,
                email = profile.email,
                display_name = profile.display_name,
                family_id = profile.family_id,
                family_name = profile.family_name
            )

            _currentUser.value = CurrentUserState(
                userId = tokenDto.user_id,
                email = tokenDto.email,
                displayName = tokenDto.display_name,
                token = tokenDto.access_token
            )
            saveAuth(tokenDto.access_token, tokenDto.user_id, tokenDto.email, tokenDto.display_name)
            refreshFamilies()
            Result.success(tokenDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: android.content.Context): Result<com.example.haven.core.network.AuthTokenDto> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val webClientId = "203542231273-prbft4vg81eevvrn1bt6i7vlh1t3ba4d.apps.googleusercontent.com"

            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Sign in to Firebase with the Google ID token
            val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val fbUser = authResult.user ?: throw IllegalStateException("Firebase user was null after Google sign in")

            // Obtain real Firebase ID Token
            val fbTokenResult = fbUser.getIdToken(true).await()
            val fbIdToken = fbTokenResult.token ?: throw IllegalStateException("Failed to obtain Firebase ID token")

            // Authenticate with Cloud Run backend /auth/me to provision/fetch user profile in Neon PostgreSQL
            apiClient.setAuthToken(fbIdToken)
            val profileRes = apiClient.getCurrentUserProfile()
            if (profileRes.isFailure) {
                return@withContext Result.failure(profileRes.exceptionOrNull() ?: Exception("Failed to sync Google user with backend"))
            }
            val profile = profileRes.getOrThrow()

            val tokenDto = com.example.haven.core.network.AuthTokenDto(
                access_token = fbIdToken,
                user_id = profile.id,
                email = profile.email,
                display_name = profile.display_name,
                family_id = profile.family_id,
                family_name = profile.family_name
            )

            _currentUser.value = CurrentUserState(
                userId = tokenDto.user_id,
                email = tokenDto.email,
                displayName = tokenDto.display_name,
                token = tokenDto.access_token
            )
            saveAuth(tokenDto.access_token, tokenDto.user_id, tokenDto.email, tokenDto.display_name)
            refreshFamilies()
            Result.success(tokenDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        _currentFamily.value = null
        apiClient.setAuthToken("")
        prefs?.edit()?.clear()?.apply()
    }

    suspend fun createFamily(
        familyName: String,
        displayName: String,
        householdName: String
    ): Result<FamilyDto> {
        val result = apiClient.createFamily(familyName, displayName, householdName)
        result.onSuccess { dto ->
            val household = dto.households.firstOrNull()?.name ?: householdName
            _currentFamily.value = ActiveFamilyState(
                id = dto.id,
                name = dto.name,
                invitationCode = dto.invitation_code,
                primaryHousehold = household,
                members = dto.members.map { m ->
                    FamilyMemberUiModel(
                        id = m.id,
                        displayName = m.display_name,
                        role = m.role,
                        colorHex = m.color_code ?: "#4E6058",
                        statusText = "At Home",
                        isOnline = true
                    )
                }
            )
            saveActiveFamily(dto.id, dto.name, dto.invitation_code, household, displayName)
        }
        return result
    }

    suspend fun joinFamily(
        inviteCode: String,
        displayName: String
    ): Result<Unit> {
        val result = apiClient.joinFamily(inviteCode, displayName)
        return if (result.isSuccess) {
            // Refresh families
            refreshFamilies()
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to join family"))
        }
    }

    suspend fun refreshFamilies(): Result<List<FamilyDto>> {
        val result = apiClient.listMyFamilies()
        result.onSuccess { list ->
            val first = list.firstOrNull()
            if (first != null) {
                _currentFamily.value = ActiveFamilyState(
                    id = first.id,
                    name = first.name,
                    invitationCode = first.invitation_code,
                    primaryHousehold = first.households.firstOrNull()?.name ?: "Main Home",
                    members = first.members.map { m ->
                        FamilyMemberUiModel(
                            id = m.id,
                            displayName = m.display_name,
                            role = m.role,
                            colorHex = m.color_code ?: "#4E6058",
                            statusText = "At Home",
                            isOnline = true
                        )
                    }
                )
            }
        }
        return result
    }

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        prefs = context.getSharedPreferences("haven_prefs", android.content.Context.MODE_PRIVATE)
        val savedToken = prefs?.getString("auth_token", null)
        val savedUserId = prefs?.getString("auth_user_id", null)
        val savedEmail = prefs?.getString("auth_email", null)
        val savedDisplayName = prefs?.getString("auth_display_name", null)

        if (savedToken != null && savedUserId != null) {
            _currentUser.value = CurrentUserState(
                userId = savedUserId,
                email = savedEmail ?: "",
                displayName = savedDisplayName ?: "User",
                token = savedToken
            )
            apiClient.setAuthToken(savedToken)
        }

        val savedId = prefs?.getString("family_id", null)
        val savedName = prefs?.getString("family_name", null)
        val savedCode = prefs?.getString("family_code", null)
        val savedHousehold = prefs?.getString("family_household", null)
        val savedMemberName = prefs?.getString("family_member_name", null)
        if (savedId != null && savedName != null) {
            _currentFamily.value = ActiveFamilyState(
                id = savedId,
                name = savedName,
                invitationCode = savedCode ?: "HAVEN888",
                primaryHousehold = savedHousehold ?: "Main Home",
                members = listOf(
                    FamilyMemberUiModel("1", savedMemberName ?: "User", "Admin", statusText = "At Home", isOnline = true)
                )
            )
        }
    }

    private fun saveAuth(token: String, userId: String, email: String, displayName: String) {
        prefs?.edit()
            ?.putString("auth_token", token)
            ?.putString("auth_user_id", userId)
            ?.putString("auth_email", email)
            ?.putString("auth_display_name", displayName)
            ?.apply()
    }

    private fun saveActiveFamily(id: String, name: String, code: String, household: String, memberName: String) {
        prefs?.edit()
            ?.putString("family_id", id)
            ?.putString("family_name", name)
            ?.putString("family_code", code)
            ?.putString("family_household", household)
            ?.putString("family_member_name", memberName)
            ?.apply()
    }

    companion object {
        val instance = FamilyRepository()
    }
}

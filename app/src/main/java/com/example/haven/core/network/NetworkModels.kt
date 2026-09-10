package com.example.haven.core.network

import org.json.JSONArray
import org.json.JSONObject

data class CreateFamilyApiRequest(
    val family_name: String,
    val my_display_name: String,
    val primary_household_name: String = "Main Home"
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("family_name", family_name)
            put("my_display_name", my_display_name)
            put("primary_household_name", primary_household_name)
        }.toString()
    }
}

data class JoinFamilyApiRequest(
    val invitation_code: String,
    val my_display_name: String
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("invitation_code", invitation_code)
            put("my_display_name", my_display_name)
        }.toString()
    }
}

data class FamilyMemberDto(
    val id: String,
    val family_id: String,
    val user_id: String? = null,
    val display_name: String,
    val role: String,
    val avatar_url: String? = null,
    val color_code: String? = null,
    val is_active: Boolean = true
) {
    companion object {
        fun fromJson(json: JSONObject): FamilyMemberDto {
            return FamilyMemberDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                user_id = if (json.isNull("user_id")) null else json.optString("user_id"),
                display_name = json.optString("display_name", ""),
                role = json.optString("role", "viewer"),
                avatar_url = if (json.isNull("avatar_url")) null else json.optString("avatar_url"),
                color_code = if (json.isNull("color_code")) null else json.optString("color_code"),
                is_active = json.optBoolean("is_active", true)
            )
        }
    }
}

data class HouseholdDto(
    val id: String,
    val family_id: String,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius_meters: Int = 100
) {
    companion object {
        fun fromJson(json: JSONObject): HouseholdDto {
            return HouseholdDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                name = json.optString("name", ""),
                address = if (json.isNull("address")) null else json.optString("address"),
                latitude = if (json.isNull("latitude")) null else json.optDouble("latitude"),
                longitude = if (json.isNull("longitude")) null else json.optDouble("longitude"),
                radius_meters = json.optInt("radius_meters", 100)
            )
        }
    }
}

data class FamilyDto(
    val id: String,
    val name: String,
    val invitation_code: String,
    val created_by: String,
    val created_at: String,
    val members: List<FamilyMemberDto> = emptyList(),
    val households: List<HouseholdDto> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): FamilyDto {
            val membersList = mutableListOf<FamilyMemberDto>()
            val membersArray = json.optJSONArray("members") ?: JSONArray()
            for (i in 0 until membersArray.length()) {
                val memberObj = membersArray.optJSONObject(i)
                if (memberObj != null) {
                    membersList.add(FamilyMemberDto.fromJson(memberObj))
                }
            }

            val householdsList = mutableListOf<HouseholdDto>()
            val householdsArray = json.optJSONArray("households") ?: JSONArray()
            for (i in 0 until householdsArray.length()) {
                val hObj = householdsArray.optJSONObject(i)
                if (hObj != null) {
                    householdsList.add(HouseholdDto.fromJson(hObj))
                }
            }

            return FamilyDto(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                invitation_code = json.optString("invitation_code", ""),
                created_by = json.optString("created_by", ""),
                created_at = json.optString("created_at", ""),
                members = membersList,
                households = householdsList
            )
        }
    }
}

data class ChannelDto(
    val id: String,
    val family_id: String,
    val name: String,
    val channel_type: String,
    val is_encrypted: Boolean,
    val last_message: String,
    val last_message_time: String,
    val unread_count: Int
) {
    companion object {
        fun fromJson(json: JSONObject): ChannelDto {
            return ChannelDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                name = json.optString("name", ""),
                channel_type = json.optString("channel_type", "general"),
                is_encrypted = json.optBoolean("is_encrypted", true),
                last_message = json.optString("last_message", ""),
                last_message_time = json.optString("last_message_time", ""),
                unread_count = json.optInt("unread_count", 0)
            )
        }
    }
}

data class MessageDto(
    val id: String,
    val channel_id: String,
    val sender_id: String,
    val sender_name: String,
    val content: String,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): MessageDto {
            return MessageDto(
                id = json.optString("id", ""),
                channel_id = json.optString("channel_id", ""),
                sender_id = json.optString("sender_id", ""),
                sender_name = json.optString("sender_name", ""),
                content = json.optString("content", ""),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class TaskDto(
    val id: String,
    val family_id: String,
    val title: String,
    val assigned_to: String,
    val is_completed: Boolean,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): TaskDto {
            return TaskDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                title = json.optString("title", ""),
                assigned_to = json.optString("assigned_to", ""),
                is_completed = json.optBoolean("is_completed", false),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class CalendarEventDto(
    val id: String,
    val family_id: String,
    val title: String,
    val start_time: String,
    val end_time: String? = null,
    val date: String,
    val location: String = "",
    val attendee: String = "All Family",
    val category: String = "Routine",
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): CalendarEventDto {
            return CalendarEventDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                title = json.optString("title", ""),
                start_time = json.optString("start_time", ""),
                end_time = if (json.isNull("end_time")) null else json.optString("end_time"),
                date = json.optString("date", ""),
                location = json.optString("location", ""),
                attendee = json.optString("attendee", "All Family"),
                category = json.optString("category", "Routine"),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class SafetyZoneDto(
    val id: String,
    val family_id: String,
    val name: String,
    val subtitle: String,
    val zone_type: String,
    val is_active: Boolean,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): SafetyZoneDto {
            return SafetyZoneDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                name = json.optString("name", ""),
                subtitle = json.optString("subtitle", ""),
                zone_type = json.optString("zone_type", "home"),
                is_active = json.optBoolean("is_active", true),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class SosAlertDto(
    val id: String,
    val family_id: String,
    val sender_name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): SosAlertDto {
            return SosAlertDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                sender_name = json.optString("sender_name", ""),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                status = json.optString("status", "ACTIVE"),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class FamilyExpenseDto(
    val id: String,
    val family_id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val paid_by: String,
    val split_type: String,
    val date: String,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): FamilyExpenseDto {
            return FamilyExpenseDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                title = json.optString("title", ""),
                amount = json.optDouble("amount", 0.0),
                category = json.optString("category", "General"),
                paid_by = json.optString("paid_by", ""),
                split_type = json.optString("split_type", "Equal"),
                date = json.optString("date", "Today"),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class VaultDocumentDto(
    val id: String,
    val family_id: String,
    val title: String,
    val doc_type: String,
    val encrypted_blob: String,
    val uploaded_by: String,
    val created_at: String
) {
    companion object {
        fun fromJson(json: JSONObject): VaultDocumentDto {
            return VaultDocumentDto(
                id = json.optString("id", ""),
                family_id = json.optString("family_id", ""),
                title = json.optString("title", ""),
                doc_type = json.optString("doc_type", "General"),
                encrypted_blob = json.optString("encrypted_blob", ""),
                uploaded_by = json.optString("uploaded_by", ""),
                created_at = json.optString("created_at", "")
            )
        }
    }
}

data class AuthTokenDto(
    val access_token: String,
    val user_id: String,
    val email: String,
    val display_name: String,
    val token_type: String = "bearer",
    val family_id: String? = null,
    val family_name: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): AuthTokenDto {
            return AuthTokenDto(
                access_token = json.optString("access_token", ""),
                user_id = json.optString("user_id", ""),
                email = json.optString("email", ""),
                display_name = json.optString("display_name", ""),
                token_type = json.optString("token_type", "bearer"),
                family_id = if (json.isNull("family_id")) null else json.optString("family_id"),
                family_name = if (json.isNull("family_name")) null else json.optString("family_name")
            )
        }
    }
}

data class UserProfileDto(
    val id: String,
    val email: String,
    val display_name: String,
    val family_id: String? = null,
    val family_name: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): UserProfileDto {
            return UserProfileDto(
                id = json.optString("id", ""),
                email = json.optString("email", ""),
                display_name = json.optString("display_name", ""),
                family_id = if (json.isNull("family_id")) null else json.optString("family_id"),
                family_name = if (json.isNull("family_name")) null else json.optString("family_name")
            )
        }
    }
}




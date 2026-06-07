package com.example.diplom.data

import com.google.gson.annotations.SerializedName

data class ApiErrorBody(
    val error: ApiError? = null,
)

data class ApiError(
    val code: String? = null,
    val message: String? = null,
)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val token: String, val user: UserDto)

data class UserDto(val id: String, val email: String, val role: String)

data class MeResponse(
    val user: MeUser,
    val profile: ProfileDto?,
    val assignment: AssignmentDto?,
    val achievements: List<AchievementDto>? = null,
)

data class MeUser(
    val id: String,
    val email: String,
    @SerializedName("displayName") val displayName: String? = null,
    val role: String,
    @SerializedName("premiumUntil") val premiumUntil: String?,
    @SerializedName("hasPremium") val hasPremium: Boolean,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

data class AchievementDto(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

data class AvatarUploadResponse(
    @SerializedName("avatarUrl") val avatarUrl: String,
)

data class ProfileDto(
    @SerializedName("surveyCompletedAt") val surveyCompletedAt: String?,
    @SerializedName("heightCm") val heightCm: Int?,
    @SerializedName("weightKg") val weightKg: Double?,
    val age: Int?,
    val sex: String?,
    @SerializedName("experienceLevel") val experienceLevel: String?,
    @SerializedName("trainingGoal") val trainingGoal: String? = null,
    @SerializedName("gymAccess") val gymAccess: Boolean? = null,
)

data class AssignmentDto(
    @SerializedName("programId") val programId: String,
    @SerializedName("programTitle") val programTitle: String?,
)

data class SurveyRequest(
    @SerializedName("heightCm") val heightCm: Int,
    @SerializedName("weightKg") val weightKg: Double,
    val age: Int,
    val sex: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String,
    @SerializedName("trainingGoal") val trainingGoal: String,
    @SerializedName("gymAccess") val gymAccess: Boolean,
)

data class ForYouResponse(
    @SerializedName("needsSurvey") val needsSurvey: Boolean,
    val message: String? = null,
    @SerializedName("trainingGoal") val trainingGoal: String? = null,
    @SerializedName("goalLabel") val goalLabel: String? = null,
    val summary: String? = null,
    val tips: List<String>? = null,
    @SerializedName("primaryProgramId") val primaryProgramId: String? = null,
    @SerializedName("primaryProgramTitle") val primaryProgramTitle: String? = null,
    @SerializedName("recommendedPrograms") val recommendedPrograms: List<RecommendedProgramDto>? = null,
    @SerializedName("otherPrograms") val otherPrograms: List<ForYouProgramDto>? = null,
)

data class RecommendedProgramDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("coverImageUrl") val coverImageUrl: String?,
    val locked: Boolean,
    @SerializedName("isPrimary") val isPrimary: Boolean,
)

data class ForYouProgramDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("coverImageUrl") val coverImageUrl: String? = null,
    val locked: Boolean,
)

data class SurveyResponse(
    @SerializedName("assignedProgramId") val assignedProgramId: String,
    @SerializedName("assignmentKey") val assignmentKey: String,
)

data class ProgramsListResponse(val programs: List<ProgramSummaryDto>)

data class ProgramSummaryDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("coverImageUrl") val coverImageUrl: String? = null,
    @SerializedName("isPremium") val isPremium: Boolean,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("dayCount") val dayCount: Int,
    val locked: Boolean,
)

data class ProgramDetailResponse(val program: ProgramDetailDto)

data class ProgramDetailDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("coverImageUrl") val coverImageUrl: String? = null,
    @SerializedName("isPremium") val isPremium: Boolean,
    val days: List<ProgramDayDto>,
)

data class ProgramDayDto(
    val id: String,
    @SerializedName("dayIndex") val dayIndex: Int,
    val title: String?,
    @SerializedName("bodyFocus") val bodyFocus: String? = null,
    val exercises: List<ExerciseDto>,
)

data class ExerciseDto(
    val id: String,
    @SerializedName("orderIndex") val orderIndex: Int,
    val name: String,
    val sets: Int,
    val reps: Int,
    @SerializedName("restSeconds") val restSeconds: Int,
    @SerializedName("exerciseSeconds") val exerciseSeconds: Int,
    @SerializedName("gifUrl") val gifUrl: String? = null,
    @SerializedName("libraryExerciseId") val libraryExerciseId: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
)

data class WorkoutCompleteRequest(
    @SerializedName("programId") val programId: String,
    @SerializedName("programDayId") val programDayId: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
)

data class WorkoutCompleteResponse(val session: WorkoutSessionSummary)

data class WorkoutSessionSummary(val id: String)

data class HistoryResponse(val items: List<HistoryItemDto>)

data class HistoryItemDto(
    val id: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("completedAt") val completedAt: String,
    val program: TitleDto,
    @SerializedName("programDay") val programDay: DayTitleDto,
)

data class TitleDto(val id: String, val title: String)

data class DayTitleDto(
    val id: String,
    @SerializedName("dayIndex") val dayIndex: Int,
    val title: String?,
)

data class StreakResponse(val streak: Int)

data class PremiumDemoResponse(
    @SerializedName("premiumUntil") val premiumUntil: String,
    @SerializedName("hasPremium") val hasPremium: Boolean,
)

data class WeightHistoryResponse(val items: List<WeightLogItem>)

data class WeightLogItem(
    val id: String,
    @SerializedName("weightKg") val weightKg: Double,
    @SerializedName("recordedAt") val recordedAt: String,
)

data class WeightPostRequest(
    @SerializedName("weightKg") val weightKg: Double,
    @SerializedName("recordedAt") val recordedAt: String? = null,
)

data class WeightPostResponse(val ok: Boolean = true)

data class WorkoutCatalogResponse(val groups: List<WorkoutGroupDto>)

data class WorkoutGroupDto(
    @SerializedName("bodyFocus") val bodyFocus: String,
    val label: String?,
    val items: List<CatalogWorkoutItemDto>,
)

data class CatalogWorkoutItemDto(
    @SerializedName("programId") val programId: String,
    @SerializedName("programTitle") val programTitle: String,
    @SerializedName("programDayId") val programDayId: String,
    @SerializedName("dayIndex") val dayIndex: Int,
    @SerializedName("dayTitle") val dayTitle: String?,
    @SerializedName("workoutTitle") val workoutTitle: String? = null,
    @SerializedName("bodyFocus") val bodyFocus: String,
    @SerializedName("coverImageUrl") val coverImageUrl: String?,
    @SerializedName("exerciseCount") val exerciseCount: Int,
    @SerializedName("locked") val locked: Boolean = false,
)

data class PatchProfileRequest(
    @SerializedName("heightCm") val heightCm: Int,
    @SerializedName("weightKg") val weightKg: Double,
    val age: Int,
    val sex: String? = null,
    @SerializedName("experienceLevel") val experienceLevel: String,
    @SerializedName("trainingGoal") val trainingGoal: String,
    @SerializedName("gymAccess") val gymAccess: Boolean,
    @SerializedName("displayName") val displayName: String,
)

data class PatchProfileResponse(
    val ok: Boolean = true,
    @SerializedName("assignedProgramId") val assignedProgramId: String? = null,
)

data class FriendsListResponse(val friends: List<FriendUserDto>)

data class FriendUserDto(
    val id: String,
    val email: String,
    val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
)

data class FriendSearchResponse(
    val user: FriendUserDto?,
    val relationship: String,
)

data class FriendRequestsIncomingResponse(val requests: List<FriendIncomingRequestDto>)

data class FriendIncomingRequestDto(
    val id: String,
    @SerializedName("createdAt") val createdAt: String,
    val from: FriendUserDto,
)

data class SendFriendRequestBody(
    val email: String? = null,
    @SerializedName("toUserId") val toUserId: String? = null,
)

data class SendFriendRequestResponse(
    val ok: Boolean,
    @SerializedName("becameFriends") val becameFriends: Boolean? = null,
)

data class OkSimpleResponse(val ok: Boolean)

data class FriendPublicProfileResponse(
    val user: FriendUserDto,
    @SerializedName("workoutCount") val workoutCount: Int,
    @SerializedName("maxStreakDays") val maxStreakDays: Int,
    val achievements: List<AchievementDto>? = null,
)

data class FriendsLeaderboardResponse(
    @SerializedName("sortBy") val sortBy: String,
    val items: List<FriendLeaderboardRowDto>,
)

data class FriendLeaderboardRowDto(
    val user: FriendUserDto,
    @SerializedName("workoutCount") val workoutCount: Int,
    @SerializedName("maxStreakDays") val maxStreakDays: Int,
    @SerializedName("isMe") val isMe: Boolean = false,
)

data class FriendsFeedResponse(val items: List<FriendFeedItemDto>)

data class FriendFeedItemDto(
    @SerializedName("friendId") val friendId: String,
    @SerializedName("friendDisplayName") val friendDisplayName: String,
    @SerializedName("workoutTitle") val workoutTitle: String,
    @SerializedName("completedAt") val completedAt: String,
)

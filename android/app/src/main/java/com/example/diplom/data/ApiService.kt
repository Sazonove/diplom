package com.example.diplom.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/profile/me")
    suspend fun me(): MeResponse

    @PUT("api/profile/survey")
    suspend fun submitSurvey(@Body body: SurveyRequest): SurveyResponse

    @GET("api/profile/for-you")
    suspend fun forYou(): ForYouResponse

    @GET("api/profile/weight-history")
    suspend fun weightHistory(): WeightHistoryResponse

    @POST("api/profile/weight")
    suspend fun postWeight(@Body body: WeightPostRequest): WeightPostResponse

    @PATCH("api/profile")
    suspend fun patchProfile(@Body body: PatchProfileRequest): PatchProfileResponse

    @Multipart
    @POST("api/profile/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): AvatarUploadResponse

    @GET("api/programs")
    suspend fun programs(): ProgramsListResponse

    @GET("api/programs/{id}")
    suspend fun program(@Path("id") id: String): ProgramDetailResponse

    @POST("api/workouts/complete")
    suspend fun completeWorkout(@Body body: WorkoutCompleteRequest): WorkoutCompleteResponse

    @GET("api/workouts/catalog")
    suspend fun workoutCatalog(): WorkoutCatalogResponse

    @GET("api/workouts/history")
    suspend fun history(): HistoryResponse

    @GET("api/workouts/streak")
    suspend fun streak(): StreakResponse

    @POST("api/premium/demo")
    suspend fun premiumDemo(): PremiumDemoResponse

    @GET("api/friends")
    suspend fun friendsList(): FriendsListResponse

    @GET("api/friends/search")
    suspend fun friendsSearch(@Query("email") email: String): FriendSearchResponse

    @GET("api/friends/requests/incoming")
    suspend fun friendsIncoming(): FriendRequestsIncomingResponse

    @POST("api/friends/requests")
    suspend fun friendsSendRequest(@Body body: SendFriendRequestBody): SendFriendRequestResponse

    @POST("api/friends/requests/{requestId}/accept")
    suspend fun friendsAcceptRequest(@Path("requestId") requestId: String): OkSimpleResponse

    @POST("api/friends/requests/{requestId}/decline")
    suspend fun friendsDeclineRequest(@Path("requestId") requestId: String): OkSimpleResponse

    @GET("api/friends/profile/{userId}")
    suspend fun friendProfile(@Path("userId") userId: String): FriendPublicProfileResponse

    @GET("api/friends/leaderboard")
    suspend fun friendsLeaderboard(@Query("by") by: String): FriendsLeaderboardResponse

    @GET("api/friends/feed")
    suspend fun friendsFeed(): FriendsFeedResponse
}

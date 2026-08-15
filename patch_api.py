with open('app/src/main/java/com/example/data/TelegramService.kt', 'r') as f:
    text = f.read()

patch = """
    @GET("getMyDescription")
    suspend fun getMyDescription(): retrofit2.Response<com.example.data.GetMyDescriptionResponse>

    @POST("setMyDescription")
    suspend fun setMyDescription(@Body request: com.example.data.SetMyDescriptionRequest): retrofit2.Response<Any>
"""

text = text.replace('interface TelegramApi {', 'interface TelegramApi {' + patch)

# Add data classes
dataclasses = """
data class GetMyDescriptionResponse(val ok: Boolean, val result: BotDescription?)
data class BotDescription(val description: String)
data class SetMyDescriptionRequest(val description: String)
"""

text = text.replace('data class SendMessageRequest', dataclasses + '\ndata class SendMessageRequest')

with open('app/src/main/java/com/example/data/TelegramService.kt', 'w') as f:
    f.write(text)

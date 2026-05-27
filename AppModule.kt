package com.nova.assistant.core.di
import android.content.Context
import androidx.room.Room
import com.nova.assistant.BuildConfig
import com.nova.assistant.data.local.database.NovaDatabase
import com.nova.assistant.data.remote.api.OpenAIApi
import com.nova.assistant.data.remote.api.WhisperApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton fun provideOpenAIApi(r: Retrofit): OpenAIApi = r.create(OpenAIApi::class.java)
    @Provides @Singleton fun provideWhisperApi(r: Retrofit): WhisperApi = r.create(WhisperApi::class.java)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): NovaDatabase =
        Room.databaseBuilder(ctx, NovaDatabase::class.java, "nova_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideConversationDao(db: NovaDatabase) = db.conversationDao()
    @Provides fun provideMemoryDao(db: NovaDatabase) = db.memoryDao()
    @Provides fun provideRoutineDao(db: NovaDatabase) = db.routineDao()
}

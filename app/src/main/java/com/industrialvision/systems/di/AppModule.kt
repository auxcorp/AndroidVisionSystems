package com.industrialvision.systems.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.industrialvision.ai.llm.AnthropicClient
import com.industrialvision.ai.llm.LLMConfiguration
import com.industrialvision.ai.llm.LLMProvider
import com.industrialvision.ai.llm.OpenAIClient
import com.industrialvision.core.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App Module - Main dependency injection configuration
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IndustrialVisionDatabase {
        return Room.databaseBuilder(
            context,
            IndustrialVisionDatabase::class.java,
            IndustrialVisionDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideInspectionDao(database: IndustrialVisionDatabase): InspectionDao {
        return database.inspectionDao()
    }

    @Provides
    fun provideProfileDao(database: IndustrialVisionDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideAnalyticsDao(database: IndustrialVisionDatabase): AnalyticsDao {
        return database.analyticsDao()
    }

    // JSON
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }

    // LLM Configuration
    @Provides
    @Singleton
    fun provideLLMConfiguration(): LLMConfiguration {
        return LLMConfiguration(
            preferredProvider = LLMProvider.ANTHROPIC,
            model = "claude-sonnet-4-20250514",
            maxTokens = 4096,
            temperature = 0.3f
        )
    }

    @Provides
    @Singleton
    fun provideAnthropicClient(config: LLMConfiguration): AnthropicClient {
        return AnthropicClient(config)
    }

    @Provides
    @Singleton
    fun provideOpenAIClient(config: LLMConfiguration): OpenAIClient {
        return OpenAIClient(config)
    }
}

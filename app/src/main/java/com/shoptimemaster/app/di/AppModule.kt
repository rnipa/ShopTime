package com.shoptimemaster.app.di

import android.content.Context
import com.shoptimemaster.app.data.local.dao.*
import com.shoptimemaster.app.data.local.database.AppDatabase
import com.shoptimemaster.app.data.local.preferences.UserPreferencesDataStore
import com.shoptimemaster.app.location.GeofenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides fun provideShopDao(db: AppDatabase): ShopDao = db.shopDao()
    @Provides fun provideTimeSessionDao(db: AppDatabase): TimeSessionDao = db.timeSessionDao()
    @Provides fun provideOutsideDao(db: AppDatabase): OutsideDao = db.outsideDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideBusinessDao(db: AppDatabase): BusinessDataDao = db.businessDataDao()

    @Provides
    @Singleton
    fun providePrefs(@ApplicationContext context: Context): UserPreferencesDataStore =
        UserPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideGeofenceManager(@ApplicationContext context: Context): GeofenceManager =
        GeofenceManager(context)
}

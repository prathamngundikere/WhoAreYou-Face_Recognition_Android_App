package com.prathamngundikere.whoareyou.core.di

import android.app.Application
import com.prathamngundikere.whoareyou.faceClassifier.data.repository.FaceClassifierHelper
import com.prathamngundikere.whoareyou.faceClassifier.domain.repository.FaceClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFaceClassifier(app: Application): FaceClassifier {
        return FaceClassifierHelper(
            context = app
        )
    }
}
package com.example.jumpmeasure.di

import com.example.jumpmeasure.core.vision.MediaPipePoseEstimator
import com.example.jumpmeasure.core.vision.PoseEstimator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class VisionModule {

    @Binds
    abstract fun bindPoseEstimator(
        mediaPipePoseEstimator: MediaPipePoseEstimator
    ): PoseEstimator
}

package com.manspower.jumpmeasure.di

import com.manspower.jumpmeasure.core.vision.MediaPipePoseEstimator
import com.manspower.jumpmeasure.core.vision.PoseEstimator
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

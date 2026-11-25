package com.learnmate.ai

import android.app.Application
import com.learnmate.ai.data.local.LearnMateDatabase
import com.learnmate.ai.data.repository.LearningRepository
import com.learnmate.ai.domain.engine.ExecuTorchLlmEngine
import com.learnmate.ai.domain.engine.ModelAssetManager

class LearnMateApplication : Application() {

    lateinit var database: LearnMateDatabase
        private set

    lateinit var repository: LearningRepository
        private set

    lateinit var modelAssetManager: ModelAssetManager
        private set

    lateinit var llmEngine: ExecuTorchLlmEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = LearnMateDatabase.getDatabase(this)
        repository = LearningRepository(database, this)
        modelAssetManager = ModelAssetManager(this)
        llmEngine = ExecuTorchLlmEngine(this)
    }
}

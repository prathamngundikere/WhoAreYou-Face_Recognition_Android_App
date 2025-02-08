package com.prathamngundikere.whoareyou.faceClassifier.util

import com.prathamngundikere.whoareyou.faceClassifier.domain.model.CombinedResult

data class ResultState(
    val isLoading: Boolean = false,
    val combinedResult: CombinedResult? = null,
    val error: String? = null
)

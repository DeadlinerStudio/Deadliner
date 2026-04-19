package com.aritxonly.deadliner.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubTask(
    val id: String,
    val content: String,
    @SerializedName("is_completed")
    val isCompleted: Int = 0,
    @SerializedName("sort_order")
    val sortOrder: Int = 0,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Parcelable

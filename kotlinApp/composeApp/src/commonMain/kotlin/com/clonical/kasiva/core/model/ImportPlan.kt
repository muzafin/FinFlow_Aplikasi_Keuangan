package com.clonical.kasiva.core.model

data class ImportPlan(
    val accounts: List<Account>, val categories: List<Category>, val transactions: List<Transaction>,
    val errors: List<String>, val warnings: List<String>, val duplicateCount: Int,
)

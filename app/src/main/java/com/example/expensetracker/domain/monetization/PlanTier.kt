package com.example.expensetracker.domain.monetization

enum class PlanTier(val displayName: String, val monthlyPriceUsd: Double, val annualPriceUsd: Double) {
    FREE("Free", 0.0, 0.0),
    PRO("Pro", 2.99, 24.99),
    PRO_PLUS("Pro+", 4.99, 39.99)
}

enum class FeatureGate(val requiredTier: PlanTier, val featureName: String) {
    UNLIMITED_CATEGORIES(PlanTier.PRO, "Unlimited Categories & Subcategories"),
    DEPOSIT_VAULT(PlanTier.PRO, "Deposit Vault & Protected Reserves"),
    MULTI_SPLIT(PlanTier.PRO, "Split Transactions & Receipts"),
    SPENDING_PACING(PlanTier.PRO, "Pacing Analysis & Health Score"),
    CATEGORY_COMPARISON(PlanTier.PRO, "Category & Subcategory Comparison"),
    EXECUTIVE_EXPORTS(PlanTier.PRO, "Executive PDF & Advanced Exports"),
    MULTI_CURRENCY(PlanTier.PRO_PLUS, "Multi-Currency & Travel Mode"),
    GOLD_ZAKAH_FEED(PlanTier.PRO_PLUS, "Gold Register & Auto-Zakah"),
    INFLATION_TRACKER(PlanTier.PRO_PLUS, "Smart Price Memory & Inflation Tracker"),
    FINANCIAL_RUNWAY(PlanTier.PRO_PLUS, "Financial Runway Stress-Tester"),
    MURABAHA_SIMULATOR(PlanTier.PRO_PLUS, "Murabaha Islamic Loan Simulator"),
    SADAQAH_JAR(PlanTier.PRO_PLUS, "Virtual Sadaqah Jar")
}


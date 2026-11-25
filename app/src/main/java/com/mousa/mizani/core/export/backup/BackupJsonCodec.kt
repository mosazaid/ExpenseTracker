package com.mousa.mizani.core.export.backup

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.AppAlert
import com.mousa.mizani.data.database.entities.Budget
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.ConfiguredLoan
import com.mousa.mizani.data.database.entities.CurrencyExchange
import com.mousa.mizani.data.database.entities.ItemPriceRecord
import com.mousa.mizani.data.database.entities.MonthlyLoanPayment
import com.mousa.mizani.data.database.entities.PendingSmsTransaction
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.SubCategory
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionSplit
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.database.entities.Trip
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Converts a [BackupSnapshot] to and from the backup JSON file. Dates are epoch milliseconds.
 *
 * Format history:
 *  - 1: the original nine tables (written by the app before the Mizani rename).
 *  - 2: adds trips, currency exchanges, item price history, pending bank SMS, and the
 *       transaction fields excludeFromMonthExpenses, currency, originalAmount,
 *       exchangeRateUsed and tripId.
 * Decoding accepts both; every field added later is optional and falls back to the entity default.
 */
object BackupJsonCodec {

    const val FORMAT_VERSION = 2

    private const val KEY_CATEGORIES = "categories"
    private const val KEY_SUB_CATEGORIES = "subCategories"
    private const val KEY_BUDGETS = "budgets"
    private const val KEY_LOANS = "configuredLoans"
    private const val KEY_LOAN_PAYMENTS = "monthlyLoanPayments"
    private const val KEY_RECURRING = "recurringTransactions"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_SPLITS = "transactionSplits"
    private const val KEY_ALERTS = "appAlerts"
    private const val KEY_TRIPS = "trips"
    private const val KEY_EXCHANGES = "currencyExchanges"
    private const val KEY_PRICES = "itemPriceHistory"
    private const val KEY_PENDING_SMS = "pendingSmsTransactions"

    // ---------------------------------------------------------------- encode

    fun encode(
        snapshot: BackupSnapshot,
        preferences: Map<String, Any?>,
        databaseVersion: Int,
        exportedAt: String
    ): JSONObject = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("databaseVersion", databaseVersion)
        put("exportedAt", exportedAt)
        put("preferences", JSONObject().apply { preferences.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } })
        put(KEY_CATEGORIES, snapshot.categories.toJsonArray(::encodeCategory))
        put(KEY_SUB_CATEGORIES, snapshot.subCategories.toJsonArray(::encodeSubCategory))
        put(KEY_BUDGETS, snapshot.budgets.toJsonArray(::encodeBudget))
        put(KEY_LOANS, snapshot.configuredLoans.toJsonArray(::encodeLoan))
        put(KEY_LOAN_PAYMENTS, snapshot.monthlyLoanPayments.toJsonArray(::encodePayment))
        put(KEY_RECURRING, snapshot.recurringTransactions.toJsonArray(::encodeRecurring))
        put(KEY_TRANSACTIONS, snapshot.transactions.toJsonArray(::encodeTransaction))
        put(KEY_SPLITS, snapshot.transactionSplits.toJsonArray(::encodeSplit))
        put(KEY_ALERTS, snapshot.appAlerts.toJsonArray(::encodeAlert))
        put(KEY_TRIPS, snapshot.trips.orEmpty().toJsonArray(::encodeTrip))
        put(KEY_EXCHANGES, snapshot.currencyExchanges.orEmpty().toJsonArray(::encodeExchange))
        put(KEY_PRICES, snapshot.itemPriceHistory.orEmpty().toJsonArray(::encodePrice))
        put(KEY_PENDING_SMS, snapshot.pendingSmsTransactions.orEmpty().toJsonArray(::encodePendingSms))
    }

    private fun encodeCategory(c: Category) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("icon", c.icon); put("color", c.color)
        put("type", c.type.name); put("isDefault", c.isDefault); put("createdAt", c.createdAt.time)
    }

    private fun encodeSubCategory(s: SubCategory) = JSONObject().apply {
        put("id", s.id); put("categoryId", s.categoryId); put("name", s.name); put("createdAt", s.createdAt.time)
    }

    private fun encodeBudget(b: Budget) = JSONObject().apply {
        put("id", b.id); put("categoryId", b.categoryId); put("amount", b.amount)
        put("periodStart", b.periodStart.time); put("periodEnd", b.periodEnd.time); put("createdAt", b.createdAt.time)
    }

    private fun encodeLoan(l: ConfiguredLoan) = JSONObject().apply {
        put("id", l.id); put("name", l.name); put("defaultAmount", l.defaultAmount)
        put("accountType", l.accountType.name); put("deductFromIncome", l.deductFromIncome)
        put("isActive", l.isActive); put("createdAt", l.createdAt.time)
    }

    private fun encodePayment(p: MonthlyLoanPayment) = JSONObject().apply {
        put("id", p.id); put("loanConfigId", p.loanConfigId); put("monthKey", p.monthKey)
        put("amount", p.amount); put("accountType", p.accountType.name); put("isPaid", p.isPaid)
        putNullable("paidDate", p.paidDate?.time); putNullable("transactionId", p.transactionId)
        put("isDismissed", p.isDismissed); put("createdAt", p.createdAt.time)
    }

    private fun encodeRecurring(r: RecurringTransaction) = JSONObject().apply {
        put("id", r.id); put("amount", r.amount); put("description", r.description); put("type", r.type.name)
        putNullable("categoryId", r.categoryId); put("accountType", r.accountType.name)
        put("frequency", r.frequency.name); put("nextDueDate", r.nextDueDate.time)
        put("isActive", r.isActive); put("createdAt", r.createdAt.time)
    }

    private fun encodeTransaction(t: Transaction) = JSONObject().apply {
        put("id", t.id); put("amount", t.amount); put("description", t.description)
        putNullable("subDescription", t.subDescription); put("date", t.date.time); put("type", t.type.name)
        putNullable("categoryId", t.categoryId); put("accountType", t.accountType.name)
        putNullable("toAccountType", t.toAccountType?.name); put("startsNewPeriod", t.startsNewPeriod)
        putNullable("carriedForwardBalance", t.carriedForwardBalance)
        put("allowNegativeBalance", t.allowNegativeBalance); putNullable("linkedExpenseId", t.linkedExpenseId)
        putNullable("debtorNote", t.debtorNote); put("awaitingReimbursement", t.awaitingReimbursement)
        putNullable("debtType", t.debtType); put("isDebtSettled", t.isDebtSettled)
        putNullable("recurringId", t.recurringId); putNullable("receiptImagePath", t.receiptImagePath)
        put("excludeFromMonthExpenses", t.excludeFromMonthExpenses); put("currency", t.currency)
        putNullable("originalAmount", t.originalAmount); putNullable("exchangeRateUsed", t.exchangeRateUsed)
        putNullable("tripId", t.tripId); put("createdAt", t.createdAt.time); put("updatedAt", t.updatedAt.time)
    }

    private fun encodeSplit(s: TransactionSplit) = JSONObject().apply {
        put("id", s.id); put("transactionId", s.transactionId); putNullable("subCategoryId", s.subCategoryId)
        putNullable("subCategoryName", s.subCategoryName); put("amount", s.amount); putNullable("note", s.note)
        put("isDebt", s.isDebt); putNullable("debtPersonName", s.debtPersonName); put("isDebtSettled", s.isDebtSettled)
    }

    private fun encodeAlert(a: AppAlert) = JSONObject().apply {
        put("id", a.id); put("type", a.type); put("title", a.title); put("message", a.message)
        putNullable("relatedId", a.relatedId); put("periodKey", a.periodKey)
        put("isDismissed", a.isDismissed); put("createdAt", a.createdAt.time)
    }

    private fun encodeTrip(t: Trip) = JSONObject().apply {
        put("id", t.id); put("title", t.title); put("destination", t.destination)
        put("baseCurrency", t.baseCurrency); put("homeCurrency", t.homeCurrency); put("totalBudget", t.totalBudget)
        put("startDate", t.startDate.time); put("endDate", t.endDate.time); put("isActive", t.isActive)
        putNullable("notes", t.notes); put("createdAt", t.createdAt.time)
    }

    private fun encodeExchange(e: CurrencyExchange) = JSONObject().apply {
        put("id", e.id); put("soldCurrency", e.soldCurrency); put("soldAmount", e.soldAmount)
        put("boughtCurrency", e.boughtCurrency); put("boughtAmount", e.boughtAmount)
        put("exchangeRate", e.exchangeRate); put("fee", e.fee); put("feeCurrency", e.feeCurrency)
        putNullable("location", e.location); put("date", e.date.time); putNullable("notes", e.notes)
        put("createdAt", e.createdAt.time)
    }

    private fun encodePrice(p: ItemPriceRecord) = JSONObject().apply {
        put("id", p.id); put("itemName", p.itemName); put("normalizedItemName", p.normalizedItemName)
        put("unitPrice", p.unitPrice); put("currency", p.currency); putNullable("subCategoryId", p.subCategoryId)
        putNullable("transactionId", p.transactionId); putNullable("storeOrVendor", p.storeOrVendor)
        put("date", p.date.time); putNullable("notes", p.notes); put("createdAt", p.createdAt.time)
    }

    private fun encodePendingSms(s: PendingSmsTransaction) = JSONObject().apply {
        put("id", s.id); put("sender", s.sender); put("rawBody", s.rawBody); put("amount", s.amount)
        put("currency", s.currency); put("merchant", s.merchant); putNullable("cardOrAccount", s.cardOrAccount)
        putNullable("suggestedCategory", s.suggestedCategory); put("timestamp", s.timestamp)
        put("isProcessed", s.isProcessed); put("isDismissed", s.isDismissed)
    }

    // ---------------------------------------------------------------- decode

    /** Parses a backup file. Throws [org.json.JSONException] or [IllegalArgumentException] naming the bad row. */
    fun decode(json: String): DecodedBackup {
        val root = JSONObject(json)
        val snapshot = BackupSnapshot(
            categories = root.decodeList(KEY_CATEGORIES, ::decodeCategory),
            subCategories = root.decodeList(KEY_SUB_CATEGORIES, ::decodeSubCategory),
            budgets = root.decodeList(KEY_BUDGETS, ::decodeBudget),
            configuredLoans = root.decodeList(KEY_LOANS, ::decodeLoan),
            monthlyLoanPayments = root.decodeList(KEY_LOAN_PAYMENTS, ::decodePayment),
            recurringTransactions = root.decodeList(KEY_RECURRING, ::decodeRecurring),
            transactions = root.decodeList(KEY_TRANSACTIONS, ::decodeTransaction),
            transactionSplits = root.decodeList(KEY_SPLITS, ::decodeSplit),
            appAlerts = root.decodeList(KEY_ALERTS, ::decodeAlert),
            trips = root.decodeOptionalList(KEY_TRIPS, ::decodeTrip),
            currencyExchanges = root.decodeOptionalList(KEY_EXCHANGES, ::decodeExchange),
            itemPriceHistory = root.decodeOptionalList(KEY_PRICES, ::decodePrice),
            pendingSmsTransactions = root.decodeOptionalList(KEY_PENDING_SMS, ::decodePendingSms)
        )
        val preferences = root.optJSONObject("preferences")?.let { prefs ->
            prefs.keys().asSequence().associateWith { prefs.optString(it, "") }
        }
        return DecodedBackup(root.optInt("version", 1), snapshot, preferences)
    }

    private fun decodeCategory(o: JSONObject) = Category(
        id = o.getLong("id"), name = o.getString("name"), icon = o.getString("icon"),
        color = o.getString("color"), type = TransactionType.valueOf(o.getString("type")),
        isDefault = o.optBoolean("isDefault", false), createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeSubCategory(o: JSONObject) = SubCategory(
        id = o.getLong("id"), categoryId = o.getLong("categoryId"), name = o.getString("name"),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeBudget(o: JSONObject) = Budget(
        id = o.getLong("id"), categoryId = o.getLong("categoryId"), amount = o.getDouble("amount"),
        periodStart = Date(o.getLong("periodStart")), periodEnd = Date(o.getLong("periodEnd")),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeLoan(o: JSONObject) = ConfiguredLoan(
        id = o.getLong("id"), name = o.getString("name"), defaultAmount = o.getDouble("defaultAmount"),
        accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
        deductFromIncome = o.optBoolean("deductFromIncome", true), isActive = o.optBoolean("isActive", true),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodePayment(o: JSONObject) = MonthlyLoanPayment(
        id = o.getLong("id"), loanConfigId = o.getLong("loanConfigId"), monthKey = o.getString("monthKey"),
        amount = o.getDouble("amount"), accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
        isPaid = o.optBoolean("isPaid", false), paidDate = o.longOrNull("paidDate")?.let(::Date),
        transactionId = o.longOrNull("transactionId"), isDismissed = o.optBoolean("isDismissed", false),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeRecurring(o: JSONObject) = RecurringTransaction(
        id = o.getLong("id"), amount = o.getDouble("amount"), description = o.getString("description"),
        type = TransactionType.valueOf(o.getString("type")), categoryId = o.longOrNull("categoryId"),
        accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
        frequency = RecurrenceFrequency.valueOf(o.optString("frequency", "MONTHLY")),
        nextDueDate = Date(o.getLong("nextDueDate")), isActive = o.optBoolean("isActive", true),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeTransaction(o: JSONObject) = Transaction(
        id = o.getLong("id"), amount = o.getDouble("amount"), description = o.getString("description"),
        subDescription = o.stringOrNull("subDescription"), date = Date(o.getLong("date")),
        type = TransactionType.valueOf(o.getString("type")), categoryId = o.longOrNull("categoryId"),
        accountType = AccountType.valueOf(o.getString("accountType")),
        toAccountType = o.stringOrNull("toAccountType")?.let(AccountType::valueOf),
        startsNewPeriod = o.optBoolean("startsNewPeriod", false),
        carriedForwardBalance = o.doubleOrNull("carriedForwardBalance"),
        allowNegativeBalance = o.optBoolean("allowNegativeBalance", false),
        linkedExpenseId = o.longOrNull("linkedExpenseId"), debtorNote = o.stringOrNull("debtorNote"),
        awaitingReimbursement = o.optBoolean("awaitingReimbursement", false),
        debtType = o.stringOrNull("debtType"), isDebtSettled = o.optBoolean("isDebtSettled", false),
        recurringId = o.longOrNull("recurringId"), receiptImagePath = o.stringOrNull("receiptImagePath"),
        excludeFromMonthExpenses = o.optBoolean("excludeFromMonthExpenses", false),
        currency = o.stringOrNull("currency") ?: "JOD",
        originalAmount = o.doubleOrNull("originalAmount"), exchangeRateUsed = o.doubleOrNull("exchangeRateUsed"),
        tripId = o.longOrNull("tripId"), createdAt = o.dateOrNow("createdAt"), updatedAt = o.dateOrNow("updatedAt")
    )

    private fun decodeSplit(o: JSONObject) = TransactionSplit(
        id = o.getLong("id"), transactionId = o.getLong("transactionId"),
        subCategoryId = o.longOrNull("subCategoryId"), subCategoryName = o.stringOrNull("subCategoryName"),
        amount = o.getDouble("amount"), note = o.stringOrNull("note"), isDebt = o.optBoolean("isDebt", false),
        debtPersonName = o.stringOrNull("debtPersonName"), isDebtSettled = o.optBoolean("isDebtSettled", false)
    )

    private fun decodeAlert(o: JSONObject) = AppAlert(
        id = o.getLong("id"), type = o.getString("type"), title = o.getString("title"),
        message = o.getString("message"), relatedId = o.longOrNull("relatedId"),
        periodKey = o.optString("periodKey", ""), isDismissed = o.optBoolean("isDismissed", false),
        createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeTrip(o: JSONObject) = Trip(
        id = o.getLong("id"), title = o.getString("title"), destination = o.optString("destination", ""),
        baseCurrency = o.getString("baseCurrency"), homeCurrency = o.optString("homeCurrency", "JOD"),
        totalBudget = o.getDouble("totalBudget"), startDate = Date(o.getLong("startDate")),
        endDate = Date(o.getLong("endDate")), isActive = o.optBoolean("isActive", false),
        notes = o.stringOrNull("notes"), createdAt = o.dateOrNow("createdAt")
    )

    private fun decodeExchange(o: JSONObject) = CurrencyExchange(
        id = o.getLong("id"), soldCurrency = o.getString("soldCurrency"), soldAmount = o.getDouble("soldAmount"),
        boughtCurrency = o.getString("boughtCurrency"), boughtAmount = o.getDouble("boughtAmount"),
        exchangeRate = o.getDouble("exchangeRate"), fee = o.optDouble("fee", 0.0),
        feeCurrency = o.optString("feeCurrency", "JOD"), location = o.stringOrNull("location"),
        date = o.dateOrNow("date"), notes = o.stringOrNull("notes"), createdAt = o.dateOrNow("createdAt")
    )

    private fun decodePrice(o: JSONObject) = ItemPriceRecord(
        id = o.getLong("id"), itemName = o.getString("itemName"),
        normalizedItemName = o.stringOrNull("normalizedItemName") ?: o.getString("itemName").trim().lowercase(),
        unitPrice = o.getDouble("unitPrice"), currency = o.optString("currency", "JOD"),
        subCategoryId = o.longOrNull("subCategoryId"), transactionId = o.longOrNull("transactionId"),
        storeOrVendor = o.stringOrNull("storeOrVendor"), date = o.dateOrNow("date"),
        notes = o.stringOrNull("notes"), createdAt = o.dateOrNow("createdAt")
    )

    private fun decodePendingSms(o: JSONObject) = PendingSmsTransaction(
        id = o.getLong("id"), sender = o.optString("sender", ""), rawBody = o.getString("rawBody"),
        amount = o.getDouble("amount"), currency = o.optString("currency", "JOD"),
        merchant = o.optString("merchant", ""), cardOrAccount = o.stringOrNull("cardOrAccount"),
        suggestedCategory = o.stringOrNull("suggestedCategory"),
        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
        isProcessed = o.optBoolean("isProcessed", false), isDismissed = o.optBoolean("isDismissed", false)
    )

    // ---------------------------------------------------------------- helpers

    private fun <T> List<T>.toJsonArray(encode: (T) -> JSONObject): JSONArray =
        JSONArray().also { array -> forEach { array.put(encode(it)) } }

    private fun <T> JSONObject.decodeList(key: String, decode: (JSONObject) -> T): List<T> =
        decodeOptionalList(key, decode).orEmpty()

    private fun <T> JSONObject.decodeOptionalList(key: String, decode: (JSONObject) -> T): List<T>? {
        val array = optJSONArray(key) ?: return null
        return (0 until array.length()).map { index ->
            try {
                decode(array.getJSONObject(index))
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid entry in '$key' at index $index: ${e.message}", e)
            }
        }
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.dateOrNow(key: String): Date = Date(optLong(key, System.currentTimeMillis()))
}

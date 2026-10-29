package com.example.core.reportcard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.network.FirebaseRealtimeService
import com.example.domain.model.ReportCard
import com.example.domain.model.SubjectGrade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object ReportCardManager {

    private const val PREFS_NAME = "nezamyar_report_cards"
    private const val KEY_CARDS = "saved_report_cards_json"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _reportCards = MutableStateFlow<List<ReportCard>>(emptyList())
    val reportCards: StateFlow<List<ReportCard>> = _reportCards.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (prefs == null) {
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPreferences()
            syncWithFirebase()

            // Continuous background sync for real-time report card updates
            managerScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    try {
                        syncWithFirebase()
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun loadFromPreferences() {
        try {
            val jsonStr = prefs?.getString(KEY_CARDS, null)
            if (!jsonStr.isNullOrBlank()) {
                val list = parseJsonToReportCards(jsonStr)
                _reportCards.value = list
                return
            }
        } catch (e: Exception) {
            Log.e("ReportCardManager", "Error loading report cards: ${e.message}")
        }

        // NO default mock report cards if none entered by staff
        _reportCards.value = emptyList()
    }

    private fun saveToPreferences(cards: List<ReportCard>) {
        try {
            val jsonArray = JSONArray()
            cards.forEach { card ->
                jsonArray.put(reportCardToJson(card))
            }
            prefs?.edit()?.putString(KEY_CARDS, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            Log.e("ReportCardManager", "Error saving report cards to prefs: ${e.message}")
        }
    }

    private fun syncWithFirebase() {
        managerScope.launch {
            try {
                val remoteCards = FirebaseRealtimeService.fetchReportCards()
                if (remoteCards != null) {
                    _reportCards.value = remoteCards
                    saveToPreferences(remoteCards)
                }
            } catch (e: Exception) {
                Log.w("ReportCardManager", "Firebase sync warning: ${e.message}")
            }
        }
    }

    fun getReportCardForStudent(studentCode: String, studentId: String = ""): ReportCard? {
        val trimmedCode = studentCode.trim()
        val trimmedId = studentId.trim()
        return _reportCards.value.firstOrNull { card ->
            (trimmedCode.isNotBlank() && card.studentCode == trimmedCode) ||
            (trimmedCode.isNotBlank() && card.nationalId == trimmedCode) ||
            (trimmedId.isNotBlank() && card.studentId == trimmedId)
        }
    }

    fun hasReportCard(studentCode: String, studentId: String = ""): Boolean {
        return getReportCardForStudent(studentCode, studentId) != null
    }

    fun saveReportCard(card: ReportCard): Boolean {
        return try {
            val current = _reportCards.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == card.id || it.studentCode == card.studentCode }
            if (existingIndex >= 0) {
                current[existingIndex] = card
            } else {
                current.add(0, card)
            }
            _reportCards.value = current
            saveToPreferences(current)

            // Save to Firebase Realtime Database and immediately refresh
            managerScope.launch {
                FirebaseRealtimeService.saveReportCard(card)
                syncWithFirebase()
            }
            true
        } catch (e: Exception) {
            Log.e("ReportCardManager", "Failed to save report card: ${e.message}")
            false
        }
    }

    fun deleteReportCard(cardId: String): Boolean {
        return try {
            val targetCard = _reportCards.value.firstOrNull { it.id == cardId }
            val updated = _reportCards.value.filter { it.id != cardId }
            _reportCards.value = updated
            saveToPreferences(updated)
            managerScope.launch {
                FirebaseRealtimeService.deleteReportCard(cardId)
                if (targetCard != null && targetCard.studentCode.isNotBlank()) {
                    FirebaseRealtimeService.deleteReportCard("rc_${targetCard.studentCode}")
                }
                syncWithFirebase()
            }
            true
        } catch (e: Exception) {
            Log.e("ReportCardManager", "Failed to delete report card: ${e.message}")
            false
        }
    }

    private fun reportCardToJson(card: ReportCard): JSONObject {
        return JSONObject().apply {
            put("id", card.id)
            put("studentId", card.studentId)
            put("studentCode", card.studentCode)
            put("nationalId", card.nationalId)
            put("studentFirstName", card.studentFirstName)
            put("studentLastName", card.studentLastName)
            put("fatherName", card.fatherName)
            put("gender", card.gender)
            put("birthDate", card.birthDate)
            put("birthPlace", card.birthPlace)
            put("studentPhoto", card.studentPhoto ?: "")

            put("cardType", card.cardType)
            put("selectedMonth", card.selectedMonth)

            put("province", card.province)
            put("region", card.region)
            put("schoolName", card.schoolName)
            put("schoolCode", card.schoolCode)
            put("grade", card.grade)
            put("className", card.className)
            put("schoolType", card.schoolType)
            put("schoolLocation", card.schoolLocation)
            put("schoolFeature", card.schoolFeature)
            put("educationalPeriod", card.educationalPeriod)
            put("academicYear", card.academicYear)
            put("semesterTitle", card.semesterTitle)
            put("issueDate", card.issueDate)

            put("totalTerm1", card.totalTerm1)
            put("totalAnnual", card.totalAnnual)
            put("gpaTerm1", card.gpaTerm1)
            put("gpaAnnual", card.gpaAnnual)
            put("gpaInWords", card.gpaInWords)
            put("conductTerm1", card.conductTerm1)
            put("conductTerm2", card.conductTerm2)
            put("finalStatus", card.finalStatus)

            put("highestClassGpa", card.highestClassGpa)
            put("monthlyStudentGpa", card.monthlyStudentGpa)
            put("lowestClassGpa", card.lowestClassGpa)
            put("overallClassRank", card.overallClassRank)

            put("principalName", card.principalName)
            put("executiveDeputyName", card.executiveDeputyName)
            put("certificationText", card.certificationText)
            put("indicatorNumber", card.indicatorNumber)
            put("createdBy", card.createdBy)
            put("createdAt", card.createdAt)
            put("updatedAt", card.updatedAt)

            val subjArr = JSONArray()
            card.subjects.forEach { s ->
                val sObj = JSONObject().apply {
                    put("rowNumber", s.rowNumber)
                    put("subjectName", s.subjectName)
                    put("term1Continuous", s.term1Continuous)
                    put("term1Final", s.term1Final)
                    put("term1Score", s.term1Score)
                    put("term2Continuous", s.term2Continuous)
                    put("term2Final", s.term2Final)
                    put("term2ScoreWithCoefficient", s.term2ScoreWithCoefficient)
                    put("annualScore", s.annualScore)
                    put("summerFinal", s.summerFinal)
                    put("summerAverage", s.summerAverage)
                    put("summerAnnual", s.summerAnnual)
                    put("majorFieldSelection", s.majorFieldSelection)
                    put("result", s.result)
                    put("notes", s.notes)
                    put("isContinuousTerm1Hatched", s.isContinuousTerm1Hatched)
                    put("isContinuousTerm2Hatched", s.isContinuousTerm2Hatched)

                    put("monthlyMyScore", s.monthlyMyScore)
                    put("monthlyClassAvg", s.monthlyClassAvg)
                    put("monthlyClassRank", s.monthlyClassRank)
                }
                subjArr.put(sObj)
            }
            put("subjects", subjArr)
        }
    }

    private fun parseJsonToReportCards(jsonStr: String): List<ReportCard> {
        val list = mutableListOf<ReportCard>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val cardType = obj.optString("cardType", "TERM")
            val subjArr = obj.optJSONArray("subjects") ?: JSONArray()
            val subjectsList = mutableListOf<SubjectGrade>()
            for (j in 0 until subjArr.length()) {
                val sObj = subjArr.getJSONObject(j)
                subjectsList.add(
                    SubjectGrade(
                        rowNumber = sObj.optInt("rowNumber", j + 1),
                        subjectName = sObj.optString("subjectName", ""),
                        term1Continuous = sObj.optString("term1Continuous", "۲۰"),
                        term1Final = sObj.optString("term1Final", "۲۰"),
                        term1Score = sObj.optString("term1Score", "۲۰"),
                        term2Continuous = sObj.optString("term2Continuous", "۲۰"),
                        term2Final = sObj.optString("term2Final", "۲۰"),
                        term2ScoreWithCoefficient = sObj.optString("term2ScoreWithCoefficient", "۴۰"),
                        annualScore = sObj.optString("annualScore", "۲۰"),
                        summerFinal = sObj.optString("summerFinal", ""),
                        summerAverage = sObj.optString("summerAverage", ""),
                        summerAnnual = sObj.optString("summerAnnual", ""),
                        majorFieldSelection = sObj.optString("majorFieldSelection", ""),
                        result = sObj.optString("result", "قبول"),
                        notes = sObj.optString("notes", ""),
                        isContinuousTerm1Hatched = sObj.optBoolean("isContinuousTerm1Hatched", false),
                        isContinuousTerm2Hatched = sObj.optBoolean("isContinuousTerm2Hatched", false),
                        monthlyMyScore = sObj.optString("monthlyMyScore", "۲۰"),
                        monthlyClassAvg = sObj.optString("monthlyClassAvg", "۱۸.۵۰"),
                        monthlyClassRank = sObj.optString("monthlyClassRank", "۱")
                    )
                )
            }

            val defaultSubjs = if (cardType == "MONTHLY") ReportCard.defaultMonthlySubjectsList() else ReportCard.defaultSubjectsList()

            list.add(
                ReportCard(
                    id = obj.optString("id", "rc_${System.currentTimeMillis()}"),
                    studentId = obj.optString("studentId", ""),
                    studentCode = obj.optString("studentCode", ""),
                    nationalId = obj.optString("nationalId", ""),
                    studentFirstName = obj.optString("studentFirstName", ""),
                    studentLastName = obj.optString("studentLastName", ""),
                    fatherName = obj.optString("fatherName", ""),
                    gender = obj.optString("gender", "پسر"),
                    birthDate = obj.optString("birthDate", "۱۳۹۱/۱۲/۱۶"),
                    birthPlace = obj.optString("birthPlace", "کنگان"),
                    studentPhoto = obj.optString("studentPhoto").ifBlank { null },
                    cardType = cardType,
                    selectedMonth = obj.optString("selectedMonth", "مهر"),
                    province = obj.optString("province", "بوشهر"),
                    region = obj.optString("region", "دیر"),
                    schoolName = obj.optString("schoolName", "دبیرستان شهید رجایی"),
                    schoolCode = obj.optString("schoolCode", "۹۵۰۹۶۸۷۳"),
                    grade = obj.optString("grade", "هفتم"),
                    className = obj.optString("className", "هفتم ۱"),
                    schoolType = obj.optString("schoolType", "دولتی"),
                    schoolLocation = obj.optString("schoolLocation", "شهری"),
                    schoolFeature = obj.optString("schoolFeature", "عادی"),
                    educationalPeriod = obj.optString("educationalPeriod", "دوره اول متوسطه"),
                    academicYear = obj.optString("academicYear", "۱۴۰۵-۱۴۰۶"),
                    semesterTitle = obj.optString("semesterTitle", "ضمن سال"),
                    issueDate = obj.optString("issueDate", "۱۴۰۵/۰۷/۳۰"),
                    subjects = if (subjectsList.isEmpty()) defaultSubjs else subjectsList,
                    totalTerm1 = obj.optString("totalTerm1", "۲۸۰"),
                    totalAnnual = obj.optString("totalAnnual", "۵۶۰"),
                    gpaTerm1 = obj.optString("gpaTerm1", "۲۰"),
                    gpaAnnual = obj.optString("gpaAnnual", "۲۰"),
                    gpaInWords = obj.optString("gpaInWords", "بیست"),
                    conductTerm1 = obj.optString("conductTerm1", "عالی"),
                    conductTerm2 = obj.optString("conductTerm2", "عالی"),
                    finalStatus = obj.optString("finalStatus", "قبول خرداد"),
                    highestClassGpa = obj.optString("highestClassGpa", "۱۹.۲۵"),
                    monthlyStudentGpa = obj.optString("monthlyStudentGpa", "۱۹.۰۰"),
                    lowestClassGpa = obj.optString("lowestClassGpa", "۱۳.۰۸"),
                    overallClassRank = obj.optString("overallClassRank", "۲"),
                    principalName = obj.optString("principalName", "مهدی فاریابی"),
                    executiveDeputyName = obj.optString("executiveDeputyName", "یاسین دراهکی"),
                    certificationText = obj.optString("certificationText", ""),
                    indicatorNumber = obj.optString("indicatorNumber", ""),
                    createdBy = obj.optString("createdBy", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }
}

package com.example.core.voting

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.domain.model.PtaCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object PtaVotingManager {
    private const val PREFS_NAME = "nezamyar_pta_voting_prefs"
    private const val KEY_VOTES_MAP = "pta_votes_clean_v2"
    private const val KEY_USER_VOTE_PREFIX = "pta_user_vote_"

    private var prefs: SharedPreferences? = null

    val initialCandidates = listOf(
        PtaCandidate(
            id = "cand_1",
            name = "سرکار خانم مریم صادقی",
            childRelation = "مادر علی صادقی (پایه هشتم)",
            education = "کارشناس ارشد روانشناسی بالینی",
            bio = "مشاور خانواده و فعال کارگاه‌های تربیت فرزند و مهارت‌های زندگی",
            imageRes = R.drawable.img_mother_candidate_1,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_2",
            name = "سرکار خانم فاطمه حسینی",
            childRelation = "مادر امیرمحمد حسینی (پایه نهم)",
            education = "کارشناس ارشد علوم تربیتی",
            bio = "دبیر بازنشسته آموزش و پرورش و مدرس دوره‌های آموزشی و پرورشی",
            imageRes = R.drawable.img_mother_candidate_2,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_3",
            name = "سرکار خانم زهرا رضایی",
            childRelation = "مادر حسین رضایی (پایه هفتم)",
            education = "دکترای داروسازی",
            bio = "فعال بهداشت و سلامت مدارس و عضو کارگروه تغذیه سالم دانش‌آموزی",
            imageRes = R.drawable.img_mother_candidate_3,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_4",
            name = "سرکار خانم نرگس احمدی",
            childRelation = "مادر رضا احمدی (پایه هشتم)",
            education = "کارشناس مهندسی فناوری اطلاعات",
            bio = "مدرس سواد رسانه‌ای و فضای مجازی، پیگیر هوشمندسازی کلاس‌های درس",
            imageRes = R.drawable.img_mother_candidate_4,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_5",
            name = "سرکار خانم معصومه کریمی",
            childRelation = "مادر پارسا کریمی (پایه نهم)",
            education = "دکترای مدیریت آموزشی",
            bio = "استاد دانشگاه و پژوهشگر حوزه‌های تربیتی، انضباطی و انگیزشی نوجوانان",
            imageRes = R.drawable.img_mother_candidate_5,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_6",
            name = "سرکار خانم زینب موسوی",
            childRelation = "مادر محمد موسوی (پایه هفتم)",
            education = "کارشناس ارشد حقوق",
            bio = "مشاور امور حقوقی و خانواده، همیار برنامه‌های فرهنگی و مذهبی مدرسه",
            imageRes = R.drawable.img_mother_candidate_6,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_7",
            name = "سرکار خانم الهام کاظمی",
            childRelation = "مادر متین کاظمی (پایه هشتم)",
            education = "پزشک عمومی",
            bio = "فعال داوطلب حوزه سلامت جسمی و روانی دانش‌آموزان و بهداشت مدارس",
            imageRes = R.drawable.img_mother_candidate_7,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_8",
            name = "سرکار خانم سمیه ابراهیمی",
            childRelation = "مادر امیرعلی ابراهیمی (پایه هفتم)",
            education = "کارشناس مدیریت برنامه‌ریزی",
            bio = "سابقه سه دوره همکاری موفق با انجمن اولیا و مربیان و توانمندسازی اردوها",
            imageRes = R.drawable.img_mother_candidate_8,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_9",
            name = "سرکار خانم مهدیه رحمانی",
            childRelation = "مادر سجاد رحمانی (پایه نهم)",
            education = "کارشناس هنرهای تجسمی",
            bio = "مربی استعدادیابی هنری و مشارکت‌کننده در زیباسازی فضای آموزشی دبیرستان",
            imageRes = R.drawable.img_mother_candidate_9,
            baseVotes = 0
        ),
        PtaCandidate(
            id = "cand_10",
            name = "سرکار خانم ناهید محمدی",
            childRelation = "مادر طاها محمدی (پایه هشتم)",
            education = "کارشناس ارشد ادبیات فارسی",
            bio = "مسئول تجهیز کتابخانه آموزشگاه و برگزارکننده مسابقات کتابخوانی اولیا",
            imageRes = R.drawable.img_mother_candidate_10,
            baseVotes = 0
        )
    )

    private val _votesMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val votesMap: StateFlow<Map<String, Int>> = _votesMap.asStateFlow()

    private val _currentUserVoteId = MutableStateFlow<String?>(null)
    val currentUserVoteId: StateFlow<String?> = _currentUserVoteId.asStateFlow()

    fun init(context: Context, currentUserId: String = "guest_user") {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        prefs?.let { p ->
            // Purge old mock votes if present from version 1
            if (p.contains("pta_votes_map_v1")) {
                p.edit().remove("pta_votes_map_v1").apply()
            }
        }
        loadVotes(currentUserId)
    }

    @Synchronized
    private fun loadVotes(currentUserId: String) {
        val p = prefs ?: return
        val map = mutableMapOf<String, Int>()
        val savedJson = p.getString(KEY_VOTES_MAP, null)

        if (savedJson != null) {
            try {
                val obj = JSONObject(savedJson)
                initialCandidates.forEach { candidate ->
                    map[candidate.id] = obj.optInt(candidate.id, 0)
                }
            } catch (_: Exception) {
                initialCandidates.forEach { map[it.id] = 0 }
            }
        } else {
            initialCandidates.forEach { map[it.id] = 0 }
            saveVotesMap(map)
        }

        _votesMap.value = map
        _currentUserVoteId.value = p.getString("${KEY_USER_VOTE_PREFIX}$currentUserId", null)
    }

    private fun saveVotesMap(map: Map<String, Int>) {
        val p = prefs ?: return
        try {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            p.edit().putString(KEY_VOTES_MAP, json.toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun castVote(currentUserId: String, candidateId: String): Boolean {
        val p = prefs ?: return false
        val currentMap = _votesMap.value.toMutableMap()
        val previousVote = p.getString("${KEY_USER_VOTE_PREFIX}$currentUserId", null)

        if (previousVote == candidateId) {
            // Unvote if tapping the same candidate
            val count = currentMap[candidateId] ?: 0
            if (count > 0) currentMap[candidateId] = count - 1
            p.edit().remove("${KEY_USER_VOTE_PREFIX}$currentUserId").apply()
            _currentUserVoteId.value = null
        } else {
            // Decrement previous vote if any
            if (previousVote != null) {
                val prevCount = currentMap[previousVote] ?: 0
                if (prevCount > 0) currentMap[previousVote] = prevCount - 1
            }
            // Increment new vote
            val newCount = (currentMap[candidateId] ?: 0) + 1
            currentMap[candidateId] = newCount
            p.edit().putString("${KEY_USER_VOTE_PREFIX}$currentUserId", candidateId).apply()
            _currentUserVoteId.value = candidateId
        }

        _votesMap.value = currentMap
        saveVotesMap(currentMap)
        return true
    }

    fun getTotalVotes(): Int {
        return _votesMap.value.values.sum()
    }

    fun getCandidatePercentage(candidateId: String): Float {
        val total = getTotalVotes()
        if (total == 0) return 0f
        val votes = _votesMap.value[candidateId] ?: 0
        return (votes.toFloat() / total.toFloat()) * 100f
    }

    @Synchronized
    fun resetAllVotesToZero() {
        val p = prefs ?: return
        val map = mutableMapOf<String, Int>()
        initialCandidates.forEach { map[it.id] = 0 }
        _votesMap.value = map
        saveVotesMap(map)

        // Clear all user vote records
        val allKeys = p.all.keys.toList()
        val editor = p.edit()
        allKeys.filter { it.startsWith(KEY_USER_VOTE_PREFIX) }.forEach {
            editor.remove(it)
        }
        editor.apply()
        _currentUserVoteId.value = null
    }
}

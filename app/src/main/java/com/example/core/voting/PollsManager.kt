package com.example.core.voting

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.domain.model.Poll
import com.example.domain.model.PollOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PollsManager {
    private const val PREFS_NAME = "nezamyar_polls_prefs_v1"
    private const val KEY_POLLS_LIST = "polls_json_list"
    private const val KEY_USER_VOTE_PREFIX = "poll_user_vote_"
    private const val KEY_OPTION_VOTES_PREFIX = "poll_option_votes_"

    private var prefs: SharedPreferences? = null

    private val _polls = MutableStateFlow<List<Poll>>(emptyList())
    val polls: StateFlow<List<Poll>> = _polls.asStateFlow()

    // Map: pollId -> optionId voted by current user
    private val _userVotesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userVotesMap: StateFlow<Map<String, String>> = _userVotesMap.asStateFlow()

    // Map: pollId -> Map(optionId -> totalVotes)
    private val _votesCountMap = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val votesCountMap: StateFlow<Map<String, Map<String, Int>>> = _votesCountMap.asStateFlow()

    private var currentUserId: String = ""

    fun init(context: Context, userId: String = "") {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        currentUserId = userId.ifBlank { "anonymous" }

        loadPolls()
        loadVotes()
    }

    private fun getInitialSamplePolls(): List<Poll> {
        return emptyList()
    }

    private fun loadPolls() {
        val jsonStr = prefs?.getString(KEY_POLLS_LIST, null)
        if (jsonStr.isNullOrBlank()) {
            val samples = getInitialSamplePolls()
            savePollsToPrefs(samples)
            _polls.value = samples
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Poll>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val optsArray = obj.getJSONArray("options")
                    val options = mutableListOf<PollOption>()
                    for (j in 0 until optsArray.length()) {
                        val optObj = optsArray.getJSONObject(j)
                        options.add(
                            PollOption(
                                id = optObj.optString("id", UUID.randomUUID().toString()),
                                title = optObj.optString("title", ""),
                                description = optObj.optString("description", ""),
                                imageUrl = if (optObj.has("imageUrl") && !optObj.isNull("imageUrl")) optObj.optString("imageUrl") else null,
                                imageRes = if (optObj.has("imageRes") && !optObj.isNull("imageRes")) optObj.optInt("imageRes") else null,
                                baseVotes = optObj.optInt("baseVotes", 0)
                            )
                        )
                    }
                    list.add(
                        Poll(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            creatorName = obj.optString("creatorName", "مدیریت مدرسه"),
                            creatorRole = obj.optString("creatorRole", "مدیر"),
                            options = options,
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true),
                            targetRole = obj.optString("targetRole", "همه"),
                            isPermanent = obj.optBoolean("isPermanent", true),
                            expiryDate = obj.optString("expiryDate", "")
                        )
                    )
                }
                _polls.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                val samples = getInitialSamplePolls()
                _polls.value = samples
            }
        }
    }

    private fun savePollsToPrefs(list: List<Poll>) {
        val array = JSONArray()
        for (poll in list) {
            val obj = JSONObject()
            obj.put("id", poll.id)
            obj.put("title", poll.title)
            obj.put("description", poll.description)
            obj.put("creatorName", poll.creatorName)
            obj.put("creatorRole", poll.creatorRole)
            obj.put("createdAtMillis", poll.createdAtMillis)
            obj.put("isActive", poll.isActive)
            obj.put("targetRole", poll.targetRole)
            obj.put("isPermanent", poll.isPermanent)
            obj.put("expiryDate", poll.expiryDate)

            val optsArray = JSONArray()
            for (opt in poll.options) {
                val optObj = JSONObject()
                optObj.put("id", opt.id)
                optObj.put("title", opt.title)
                optObj.put("description", opt.description)
                optObj.put("imageUrl", opt.imageUrl ?: JSONObject.NULL)
                if (opt.imageRes != null) optObj.put("imageRes", opt.imageRes)
                optObj.put("baseVotes", opt.baseVotes)
                optsArray.put(optObj)
            }
            obj.put("options", optsArray)
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_POLLS_LIST, array.toString())?.apply()
    }

    private fun loadVotes() {
        val uVotes = mutableMapOf<String, String>()
        val counts = mutableMapOf<String, MutableMap<String, Int>>()

        val currentPolls = _polls.value
        for (poll in currentPolls) {
            // Load user's vote for this poll
            val keyUserVote = "${KEY_USER_VOTE_PREFIX}${poll.id}_${currentUserId}"
            val userOptionVoted = prefs?.getString(keyUserVote, null)
            if (!userOptionVoted.isNullOrBlank()) {
                uVotes[poll.id] = userOptionVoted
            }

            // Load options votes counts
            val optionMap = mutableMapOf<String, Int>()
            for (opt in poll.options) {
                val keyOptVote = "${KEY_OPTION_VOTES_PREFIX}${poll.id}_${opt.id}"
                val countInPrefs = prefs?.getInt(keyOptVote, opt.baseVotes) ?: opt.baseVotes
                optionMap[opt.id] = countInPrefs
            }
            counts[poll.id] = optionMap
        }

        _userVotesMap.value = uVotes
        _votesCountMap.value = counts
    }

    fun castVote(pollId: String, optionId: String): Boolean {
        val p = prefs ?: return false
        val keyUserVote = "${KEY_USER_VOTE_PREFIX}${pollId}_${currentUserId}"
        val previousVote = p.getString(keyUserVote, null)

        val pollMap = _votesCountMap.value[pollId]?.toMutableMap() ?: mutableMapOf()

        // If previously voted, decrement old option count
        if (previousVote != null && previousVote != optionId) {
            val prevCount = pollMap[previousVote] ?: 1
            pollMap[previousVote] = (prevCount - 1).coerceAtLeast(0)
            val keyPrevOptVote = "${KEY_OPTION_VOTES_PREFIX}${pollId}_${previousVote}"
            p.edit().putInt(keyPrevOptVote, pollMap[previousVote] ?: 0).apply()
        }

        // Increment new option count
        val currentCount = pollMap[optionId] ?: 0
        val newCount = currentCount + 1
        pollMap[optionId] = newCount

        p.edit()
            .putString(keyUserVote, optionId)
            .putInt("${KEY_OPTION_VOTES_PREFIX}${pollId}_${optionId}", newCount)
            .apply()

        // Update Flows
        val updatedUserVotes = _userVotesMap.value.toMutableMap()
        updatedUserVotes[pollId] = optionId
        _userVotesMap.value = updatedUserVotes

        val updatedCounts = _votesCountMap.value.toMutableMap()
        updatedCounts[pollId] = pollMap
        _votesCountMap.value = updatedCounts

        return true
    }

    fun addPoll(poll: Poll) {
        val list = _polls.value.toMutableList()
        list.add(0, poll)
        _polls.value = list
        savePollsToPrefs(list)

        // Initialize option counts
        val counts = _votesCountMap.value.toMutableMap()
        val optMap = mutableMapOf<String, Int>()
        for (opt in poll.options) {
            optMap[opt.id] = opt.baseVotes
        }
        counts[poll.id] = optMap
        _votesCountMap.value = counts
    }

    fun deletePoll(pollId: String) {
        val list = _polls.value.filter { it.id != pollId }
        _polls.value = list
        savePollsToPrefs(list)

        val uVotes = _userVotesMap.value.toMutableMap()
        uVotes.remove(pollId)
        _userVotesMap.value = uVotes

        val counts = _votesCountMap.value.toMutableMap()
        counts.remove(pollId)
        _votesCountMap.value = counts
    }

    fun togglePollActive(pollId: String) {
        val list = _polls.value.map {
            if (it.id == pollId) it.copy(isActive = !it.isActive) else it
        }
        _polls.value = list
        savePollsToPrefs(list)
    }
}

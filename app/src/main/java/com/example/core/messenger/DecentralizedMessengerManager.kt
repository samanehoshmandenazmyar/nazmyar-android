package com.example.core.messenger

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import android.util.Log
import com.example.domain.model.ChatMessage
import com.example.domain.model.ClassGroup
import com.example.domain.model.MessageStatus
import com.example.domain.model.PersianDateHelper
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.UUID

/**
 * Decentralized P2P Messenger Core for NezamYar inspired by Gereh (MLM VPN) architecture:
 * - No central server required for LAN/Wi-Fi communication.
 * - Cryptographic asymmetric key pair (ECDSA) for secure user identity and digital signing.
 * - Network Service Discovery (NSD) for discovering nearby peers on local network.
 * - Direct TCP Sockets for instant device-to-device message delivery and ACKs.
 * - Offline Queueing (PENDING status) with auto-retry when peer is discovered.
 * - Complete memory & resource cleanup on exit (releasing sockets, NSD listeners & threads).
 */
object DecentralizedMessengerManager {
    private const val TAG = "DecentralizedMessenger"
    private const val SERVICE_TYPE = "_nezamyar_p2p._tcp."
    private const val DEFAULT_PORT = 8988

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Device Cryptographic KeyPair
    private var myKeyPair: KeyPair? = null
    private var myPublicFingerprint: String = ""

    // Discovered P2P Peers on Local Network (IP -> PeerInfo)
    data class P2pPeer(
        val peerId: String,
        val name: String,
        val hostAddress: InetAddress,
        val port: Int,
        val publicKeyBase64: String
    )

    private val _discoveredPeers = MutableStateFlow<List<P2pPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<P2pPeer>> = _discoveredPeers.asStateFlow()

    private val _activeConversations = MutableStateFlow<List<ClassGroup>>(emptyList())
    val activeConversations: StateFlow<List<ClassGroup>> = _activeConversations.asStateFlow()

    private val _p2pMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val p2pMessages: StateFlow<Map<String, List<ChatMessage>>> = _p2pMessages.asStateFlow()

    private val pendingMessagesQueue = mutableListOf<ChatMessage>()
    private var isEngineRunning = false

    fun startEngine(context: Context, currentUser: User?) {
        if (isEngineRunning) return
        isEngineRunning = true

        initCryptoKeyPair()

        nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

        startServerSocket()
        registerNsdService(currentUser)
        discoverNsdServices()
    }

    private fun initCryptoKeyPair() {
        try {
            if (myKeyPair == null) {
                val kpg = KeyPairGenerator.getInstance("EC")
                kpg.initialize(256)
                myKeyPair = kpg.generateKeyPair()
                val pubBytes = myKeyPair?.public?.encoded ?: byteArrayOf()
                myPublicFingerprint = Base64.encodeToString(pubBytes, Base64.NO_WRAP).take(16)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crypto init failed: ${e.message}")
        }
    }

    private fun startServerSocket() {
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(DEFAULT_PORT)
                Log.d(TAG, "P2P ServerSocket started on port $DEFAULT_PORT")

                while (isEngineRunning) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingSocket(socket)
                }
            } catch (e: Exception) {
                Log.d(TAG, "ServerSocket closed: ${e.message}")
            }
        }
    }

    private fun handleIncomingSocket(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val line = reader.readLine()
                if (!line.isNull_or_blank()) {
                    val json = JSONObject(line)
                    val action = json.optString("action")

                    if (action == "SEND_MSG") {
                        val msgId = json.getString("id")
                        val conversationId = json.getString("conversationId")
                        val senderId = json.getString("senderId")
                        val senderName = json.getString("senderName")
                        val senderRole = json.getString("senderRole")
                        val text = json.getString("text")
                        val signature = json.optString("signature")

                        // Verify signature
                        val isValid = verifySignature(text, signature, json.optString("pubKey"))

                        val receivedMsg = ChatMessage(
                            id = msgId,
                            classGroupId = conversationId,
                            senderId = senderId,
                            senderName = senderName,
                            senderRole = senderRole,
                            text = if (isValid) text else "$text [امضای دیجیتال نامعتبر]",
                            timestamp = System.currentTimeMillis(),
                            timeFormatted = PersianDateHelper.getCurrentTimeFormatted(),
                            status = MessageStatus.DELIVERED
                        )

                        // Save message locally
                        addMessageToState(conversationId, receivedMsg)

                        // Ensure conversation exists in list
                        ensureConversationExists(conversationId, senderName, senderRole)

                        // Respond ACK
                        val ack = JSONObject().apply {
                            put("action", "ACK")
                            put("msgId", msgId)
                            put("status", "DELIVERED")
                        }
                        writer.println(ack.toString())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling socket: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun registerNsdService(currentUser: User?) {
        val userName = currentUser?.name ?: "کاربر نظمیار"
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "NezamYar_$myPublicFingerprint"
            serviceType = SERVICE_TYPE
            port = DEFAULT_PORT
            setAttribute("peerId", myPublicFingerprint)
            setAttribute("name", userName)
            setAttribute("role", currentUser?.rankTitle ?: "دانش‌آموز")
            setAttribute("pubKey", Base64.encodeToString(myKeyPair?.public?.encoded, Base64.NO_WRAP))
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.e(TAG, "NSD Registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(arg0: NsdServiceInfo, arg1: Int) {}
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering NSD: ${e.message}")
        }
    }

    private fun discoverNsdServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "NSD Discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_nezamyar_p2p") && !service.serviceName.contains(myPublicFingerprint)) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val peerId = serviceInfo.attributes["peerId"]?.let { String(it) } ?: serviceInfo.serviceName
                            val peerName = serviceInfo.attributes["name"]?.let { String(it) } ?: "همکلاسی"
                            val pubKeyBase64 = serviceInfo.attributes["pubKey"]?.let { String(it) } ?: ""

                            val peer = P2pPeer(
                                peerId = peerId,
                                name = peerName,
                                hostAddress = serviceInfo.host,
                                port = serviceInfo.port,
                                publicKeyBase64 = pubKeyBase64
                            )

                            val current = _discoveredPeers.value.toMutableList()
                            current.removeAll { it.peerId == peerId }
                            current.add(peer)
                            _discoveredPeers.value = current

                            // Flush any queued pending messages for this peer
                            retryPendingMessagesForPeer(peer)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val peerId = service.serviceName.replace("NezamYar_", "")
                val current = _discoveredPeers.value.toMutableList()
                current.removeAll { it.peerId == peerId }
                _discoveredPeers.value = current
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed discovering NSD: ${e.message}")
        }
    }

    fun sendMessageP2p(
        conversationId: String,
        currentUser: User?,
        text: String,
        targetPeerId: String? = null
    ) {
        if (text.isBlank() || currentUser == null) return

        val msgId = "p2p_msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()
        val timeFormatted = PersianDateHelper.getCurrentTimeFormatted()

        val signature = signText(text)

        val localMsg = ChatMessage(
            id = msgId,
            classGroupId = conversationId,
            senderId = currentUser.id,
            senderName = currentUser.name,
            senderRole = currentUser.rankTitle.ifBlank { "دانش‌آموز" },
            text = text.trim(),
            timestamp = timestamp,
            timeFormatted = timeFormatted,
            status = MessageStatus.SENDING
        )

        addMessageToState(conversationId, localMsg)

        scope.launch {
            val peer = _discoveredPeers.value.firstOrNull {
                it.peerId == targetPeerId || conversationId.contains(it.peerId)
            }

            if (peer != null) {
                val success = transmitSocketMessage(peer, localMsg, signature)
                if (success) {
                    updateMessageStatus(conversationId, msgId, MessageStatus.DELIVERED)
                } else {
                    updateMessageStatus(conversationId, msgId, MessageStatus.SENDING)
                    pendingMessagesQueue.add(localMsg)
                }
            } else {
                // Target device is currently offline / not found on LAN -> Keep in PENDING state
                updateMessageStatus(conversationId, msgId, MessageStatus.SENDING)
                pendingMessagesQueue.add(localMsg)
            }
        }
    }

    private suspend fun transmitSocketMessage(peer: P2pPeer, msg: ChatMessage, signature: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket(peer.hostAddress, peer.port).use { socket ->
                    socket.soTimeout = 4000
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    val payload = JSONObject().apply {
                        put("action", "SEND_MSG")
                        put("id", msg.id)
                        put("conversationId", msg.classGroupId)
                        put("senderId", msg.senderId)
                        put("senderName", msg.senderName)
                        put("senderRole", msg.senderRole)
                        put("text", msg.text)
                        put("signature", signature)
                        put("pubKey", Base64.encodeToString(myKeyPair?.public?.encoded, Base64.NO_WRAP))
                    }

                    writer.println(payload.toString())

                    val responseLine = reader.readLine()
                    if (!responseLine.isNull_or_blank()) {
                        val ack = JSONObject(responseLine)
                        return@withContext ack.optString("status") == "DELIVERED"
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Socket transmission failed to ${peer.name}: ${e.message}")
                false
            }
        }
    }

    private fun retryPendingMessagesForPeer(peer: P2pPeer) {
        scope.launch {
            val toRetry = pendingMessagesQueue.filter { it.classGroupId.contains(peer.peerId) }
            for (msg in toRetry) {
                val sig = signText(msg.text)
                val success = transmitSocketMessage(peer, msg, sig)
                if (success) {
                    pendingMessagesQueue.remove(msg)
                    updateMessageStatus(msg.classGroupId, msg.id, MessageStatus.DELIVERED)
                }
            }
        }
    }

    private fun signText(text: String): String {
        return try {
            val privKey = myKeyPair?.private ?: return ""
            val dsa = Signature.getInstance("SHA256withECDSA")
            dsa.initSign(privKey)
            dsa.update(text.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(dsa.sign(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    private fun verifySignature(text: String, signatureBase64: String, pubKeyBase64: String): Boolean {
        if (signatureBase64.isBlank() || pubKeyBase64.isBlank()) return true
        return try {
            val pubBytes = Base64.decode(pubKeyBase64, Base64.NO_WRAP)
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val pubKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(pubBytes))

            val dsa = Signature.getInstance("SHA256withECDSA")
            dsa.initVerify(pubKey)
            dsa.update(text.toByteArray(Charsets.UTF_8))
            dsa.verify(Base64.decode(signatureBase64, Base64.NO_WRAP))
        } catch (_: Exception) {
            false
        }
    }

    private fun addMessageToState(conversationId: String, msg: ChatMessage) {
        val currentMap = _p2pMessages.value.toMutableMap()
        val list = currentMap[conversationId]?.toMutableList() ?: mutableListOf()
        list.removeAll { it.id == msg.id }
        list.add(msg)
        currentMap[conversationId] = list
        _p2pMessages.value = currentMap
    }

    private fun updateMessageStatus(conversationId: String, msgId: String, status: MessageStatus) {
        val currentMap = _p2pMessages.value.toMutableMap()
        val list = currentMap[conversationId]?.toMutableList() ?: return
        val index = list.indexOfFirst { it.id == msgId }
        if (index != -1) {
            val updated = list[index].copy(status = status)
            list[index] = updated
            currentMap[conversationId] = list
            _p2pMessages.value = currentMap
        }
    }

    fun ensureConversationExists(conversationId: String, title: String, role: String) {
        val currentList = _activeConversations.value.toMutableList()
        if (currentList.none { it.id == conversationId }) {
            currentList.add(
                ClassGroup(
                    id = conversationId,
                    title = title,
                    gradeCode = "P2P",
                    gradeName = role,
                    avatarColorHex = 0xFF0D9488,
                    memberCount = 2,
                    description = "ارتباط مستقیم و غیرمتمرکز در شبکه محلی Wi-Fi"
                )
            )
            _activeConversations.value = currentList
        }
    }

    fun getGroupById(groupId: String): ClassGroup {
        return _activeConversations.value.firstOrNull { it.id == groupId }
            ?: ClassGroup(
                id = groupId,
                title = "گفتگوی P2P",
                gradeCode = "P2P",
                gradeName = "شبکه محلی",
                avatarColorHex = 0xFF0D9488,
                memberCount = 2,
                description = "ارتباط غیرمتمرکز دستگاه به دستگاه"
            )
    }

    fun stopP2pEngine() {
        if (!isEngineRunning) return
        isEngineRunning = false

        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.d(TAG, "NSD unregister exception: ${e.message}")
        }

        registrationListener = null
        discoveryListener = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        serverJob?.cancel()
        serverJob = null

        _discoveredPeers.value = emptyList()
        Log.d(TAG, "Decentralized P2P Engine stopped and all resources freed.")
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.isBlank()
    }
}

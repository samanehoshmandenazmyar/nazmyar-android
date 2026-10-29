package com.example.core.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PointF
import android.media.FaceDetector
import com.example.domain.model.Student
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class FaceMatchResult {
    data class Match(val student: Student, val similarity: Float, val confidencePercentage: Int) : FaceMatchResult()
    data class Uncertain(val student: Student, val similarity: Float, val confidencePercentage: Int) : FaceMatchResult()
    data class Unknown(val highestSimilarity: Float) : FaceMatchResult()
    data class NoFaceDetected(val reason: String) : FaceMatchResult()
}

data class FaceEmbedding(
    val studentId: String,
    val features: FloatArray,
    val registeredAt: Long = System.currentTimeMillis(),
    val sampleCount: Int = 1,
    val qualityScore: Int = 100
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEmbedding
        if (studentId != other.studentId) return false
        return features.contentEquals(other.features)
    }

    override fun hashCode(): Int {
        var result = studentId.hashCode()
        result = 31 * result + features.contentHashCode()
        return result
    }
}

data class CandidateMatch(
    val student: Student,
    val similarity: Float,
    val confidencePct: Int,
    val isTopMatch: Boolean
)

object FaceRecognitionManager {

    private const val PREFS_NAME = "nezamyar_face_embeddings"
    private const val KEY_EMBEDDINGS = "enrolled_face_embeddings"

    // Model constants
    private const val NORMALIZED_WIDTH = 96
    private const val NORMALIZED_HEIGHT = 96
    private const val EMBEDDING_DIM = 128

    // Thresholds
    // High confidence: definitive identity match
    const val MATCH_THRESHOLD = 0.72f
    // Uncertain range: potential match, but needs explicit manual confirmation
    const val UNCERTAIN_THRESHOLD = 0.58f

    private val cachedEmbeddings = mutableMapOf<String, FaceEmbedding>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_EMBEDDINGS, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val studentId = obj.getString("studentId")
                    val featuresJson = obj.getJSONArray("features")
                    val features = FloatArray(featuresJson.length())
                    for (j in 0 until featuresJson.length()) {
                        features[j] = featuresJson.getDouble(j).toFloat()
                    }
                    val registeredAt = obj.optLong("registeredAt", System.currentTimeMillis())
                    val sampleCount = obj.optInt("sampleCount", 1)
                    val qualityScore = obj.optInt("qualityScore", 100)
                    cachedEmbeddings[studentId] = FaceEmbedding(studentId, features, registeredAt, sampleCount, qualityScore)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isInitialized = true
    }

    private fun persist(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        cachedEmbeddings.values.forEach { emb ->
            val obj = JSONObject()
            obj.put("studentId", emb.studentId)
            val featuresJson = JSONArray()
            emb.features.forEach { f -> featuresJson.put(f.toDouble()) }
            obj.put("features", featuresJson)
            obj.put("registeredAt", emb.registeredAt)
            obj.put("sampleCount", emb.sampleCount)
            obj.put("qualityScore", emb.qualityScore)
            array.put(obj)
        }
        prefs.edit().putString(KEY_EMBEDDINGS, array.toString()).apply()
    }

    fun isFaceEnrolled(studentId: String): Boolean {
        return cachedEmbeddings.containsKey(studentId)
    }

    fun getEmbedding(studentId: String): FaceEmbedding? = cachedEmbeddings[studentId]

    fun getEnrolledCount(): Int = cachedEmbeddings.size

    fun getAllEnrolledStudentIds(): Set<String> = cachedEmbeddings.keys.toSet()

    fun deleteFace(context: Context, studentId: String): Boolean {
        val removed = cachedEmbeddings.remove(studentId) != null
        if (removed) {
            persist(context)
        }
        return removed
    }

    /**
     * Enrolls the student's face from a single captured bitmap.
     */
    fun enrollFace(context: Context, studentId: String, bitmap: Bitmap): Boolean {
        return enrollMultiShotFace(context, studentId, listOf(bitmap))
    }

    /**
     * Enrolls or updates the student's face from 1-3 captured bitmaps at varied angles.
     * Extracts normalized LBP-gradient vectors, averages them, and normalizes to unit length.
     * This creates a robust primary face template allowing the app to reliably differentiate
     * and find the true face among all students.
     */
    fun enrollMultiShotFace(context: Context, studentId: String, bitmaps: List<Bitmap>): Boolean {
        if (bitmaps.isEmpty()) return false
        val validEmbeddings = bitmaps.mapNotNull { extractFaceEmbedding(it, studentId) }
        if (validEmbeddings.isEmpty()) return false

        val avgVector = FloatArray(EMBEDDING_DIM)
        for (emb in validEmbeddings) {
            for (i in 0 until EMBEDDING_DIM) {
                avgVector[i] += emb.features[i]
            }
        }
        val count = validEmbeddings.size.toFloat()
        for (i in 0 until EMBEDDING_DIM) {
            avgVector[i] /= count
        }

        // L2 re-normalization
        var sumSquares = 0f
        for (v in avgVector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares).coerceAtLeast(1e-6f)
        for (i in 0 until EMBEDDING_DIM) {
            avgVector[i] /= norm
        }

        val quality = when (validEmbeddings.size) {
            1 -> 80
            2 -> 92
            else -> 98
        }

        cachedEmbeddings[studentId] = FaceEmbedding(
            studentId = studentId,
            features = avgVector,
            registeredAt = System.currentTimeMillis(),
            sampleCount = validEmbeddings.size,
            qualityScore = quality
        )
        persist(context)
        return true
    }

    /**
     * Finds the top matching students in descending order of similarity.
     * Useful for testing and verifying which student's enrolled face matches best.
     */
    fun findTopMatches(bitmap: Bitmap, students: List<Student>, topK: Int = 3): List<CandidateMatch> {
        if (cachedEmbeddings.isEmpty()) return emptyList()
        val probeEmbedding = extractFaceEmbedding(bitmap, "probe") ?: return emptyList()

        val studentMap = students.associateBy { it.id }
        val results = mutableListOf<CandidateMatch>()

        for ((studentId, enrolled) in cachedEmbeddings) {
            val student = studentMap[studentId] ?: continue
            val similarity = computeCosineSimilarity(probeEmbedding.features, enrolled.features)
            val confidencePct = ((similarity.coerceIn(0.5f, 0.95f) - 0.5f) / 0.45f * 100f).toInt().coerceIn(20, 99)
            results.add(
                CandidateMatch(
                    student = student,
                    similarity = similarity,
                    confidencePct = confidencePct,
                    isTopMatch = similarity >= MATCH_THRESHOLD
                )
            )
        }

        return results.sortedByDescending { it.similarity }.take(topK)
    }

    /**
     * Identifies a face from an image against the enrolled database of students.
     */
    fun identifyFace(bitmap: Bitmap, students: List<Student>): FaceMatchResult {
        if (cachedEmbeddings.isEmpty()) {
            return FaceMatchResult.Unknown(0.0f)
        }

        val probeEmbedding = extractFaceEmbedding(bitmap, "probe")
            ?: return FaceMatchResult.NoFaceDetected("چهره واضحی در کادر تصویر تشخیص داده نشد. لطفاً در نور مناسب و رو به دوربین قرار بگیرید.")

        var bestSimilarity = -1.0f
        var bestStudentId: String? = null

        for ((studentId, enrolled) in cachedEmbeddings) {
            val similarity = computeCosineSimilarity(probeEmbedding.features, enrolled.features)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestStudentId = studentId
            }
        }

        if (bestStudentId == null || bestSimilarity < UNCERTAIN_THRESHOLD) {
            return FaceMatchResult.Unknown(bestSimilarity.coerceAtLeast(0f))
        }

        val matchedStudent = students.firstOrNull { it.id == bestStudentId }
            ?: return FaceMatchResult.Unknown(bestSimilarity)

        // Convert cosine similarity to 0-100% confidence scale
        val confidencePct = ((bestSimilarity.coerceIn(0.5f, 0.95f) - 0.5f) / 0.45f * 100f).toInt().coerceIn(30, 99)

        return if (bestSimilarity >= MATCH_THRESHOLD) {
            FaceMatchResult.Match(matchedStudent, bestSimilarity, confidencePct)
        } else {
            FaceMatchResult.Uncertain(matchedStudent, bestSimilarity, confidencePct)
        }
    }

    /**
     * Extracts a normalized 128-D facial feature vector from the given bitmap.
     */
    fun extractFaceEmbedding(sourceBitmap: Bitmap, identifier: String): FaceEmbedding? {
        val croppedFace = detectAndCropFace(sourceBitmap) ?: return null
        val normalized = normalizeFaceBitmap(croppedFace)
        val features = computeMultiScaleLbpAndGradients(normalized)
        return FaceEmbedding(identifier, features)
    }

    /**
     * Uses Android FaceDetector to isolate face region, or crops the central square if detector misses.
     */
    private fun detectAndCropFace(source: Bitmap): Bitmap? {
        val width = source.width
        val height = source.height
        if (width < 32 || height < 32) return null

        try {
            // Android FaceDetector requires even width and Bitmap.Config.RGB_565
            val evenWidth = if (width % 2 == 0) width else width - 1
            val rgb565Bitmap = Bitmap.createBitmap(evenWidth, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(rgb565Bitmap)
            canvas.drawBitmap(source, 0f, 0f, null)

            val detector = FaceDetector(evenWidth, height, 1)
            val faces = arrayOfNulls<FaceDetector.Face>(1)
            val numFaces = detector.findFaces(rgb565Bitmap, faces)

            if (numFaces > 0 && faces[0] != null) {
                val face = faces[0]!!
                val midPoint = PointF()
                face.getMidPoint(midPoint)
                val eyeDistance = face.eyesDistance()

                // Bounding box estimated from eye distance
                val boxSize = (eyeDistance * 2.8f).toInt().coerceAtLeast(48)
                val left = (midPoint.x - boxSize / 2f).toInt().coerceIn(0, width - 1)
                val top = (midPoint.y - boxSize * 0.45f).toInt().coerceIn(0, height - 1)
                val actualW = boxSize.coerceAtMost(width - left)
                val actualH = boxSize.coerceAtMost(height - top)

                if (actualW > 20 && actualH > 20) {
                    return Bitmap.createBitmap(source, left, top, actualW, actualH)
                }
            }
        } catch (_: Throwable) {
            // Fall through to center-crop if detector fails or crashes
        }

        // Viewfinder center crop: The UI provides a circular target reticle at center
        val minDim = minOf(width, height)
        val cropSize = (minDim * 0.72f).toInt().coerceAtLeast(32)
        val startX = (width - cropSize) / 2
        val startY = (height - cropSize) / 2

        return Bitmap.createBitmap(source, startX, startY, cropSize, cropSize)
    }

    /**
     * Resizes to 96x96 grayscale and applies contrast enhancement.
     */
    private fun normalizeFaceBitmap(face: Bitmap): Bitmap {
        val scaled = Bitmap.createScaledBitmap(face, NORMALIZED_WIDTH, NORMALIZED_HEIGHT, true)
        val grayBitmap = Bitmap.createBitmap(NORMALIZED_WIDTH, NORMALIZED_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return grayBitmap
    }

    /**
     * Extracts multi-scale Local Binary Patterns (LBP) and directional gradient histograms
     * across a 4x4 spatial grid of cells (16 cells total).
     * Output is a 128-dimensional L2-normalized feature vector.
     */
    private fun computeMultiScaleLbpAndGradients(bitmap: Bitmap): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Extract luminance (0..255)
        val lum = IntArray(w * h)
        for (i in pixels.indices) {
            lum[i] = pixels[i] and 0xFF
        }

        // Apply contrast normalization / histogram stretch
        var minL = 255
        var maxL = 0
        for (v in lum) {
            if (v < minL) minL = v
            if (v > maxL) maxL = v
        }
        val range = (maxL - minL).coerceAtLeast(1)
        for (i in lum.indices) {
            lum[i] = ((lum[i] - minL) * 255) / range
        }

        // 4x4 spatial grid cells -> 16 cells
        val gridRows = 4
        val gridCols = 4
        val cellW = w / gridCols
        val cellH = h / gridRows

        // Each cell has 8 features (4 LBP bins + 4 directional gradient bins) = 16 * 8 = 128 dims
        val featureVector = FloatArray(EMBEDDING_DIM)

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val cellIndex = r * gridCols + c
                val vectorOffset = cellIndex * 8

                val startX = c * cellW
                val startY = r * cellH
                val endX = (startX + cellW).coerceAtMost(w - 2)
                val endY = (startY + cellH).coerceAtMost(h - 2)

                var grad0 = 0f  // Horizontal gradient
                var grad45 = 0f // Diagonal gradient
                var grad90 = 0f // Vertical gradient
                var grad135 = 0f

                var lbpLow = 0f
                var lbpMid1 = 0f
                var lbpMid2 = 0f
                var lbpHigh = 0f

                for (y in (startY + 1) until endY) {
                    for (x in (startX + 1) until endX) {
                        val center = lum[y * w + x]

                        // 8-neighbor LBP
                        var pattern = 0
                        if (lum[(y - 1) * w + (x - 1)] >= center) pattern = pattern or 1
                        if (lum[(y - 1) * w + x] >= center) pattern = pattern or 2
                        if (lum[(y - 1) * w + (x + 1)] >= center) pattern = pattern or 4
                        if (lum[y * w + (x + 1)] >= center) pattern = pattern or 8
                        if (lum[(y + 1) * w + (x + 1)] >= center) pattern = pattern or 16
                        if (lum[(y + 1) * w + x] >= center) pattern = pattern or 32
                        if (lum[(y + 1) * w + (x - 1)] >= center) pattern = pattern or 64
                        if (lum[y * w + (x - 1)] >= center) pattern = pattern or 128

                        when (pattern) {
                            in 0..63 -> lbpLow += 1f
                            in 64..127 -> lbpMid1 += 1f
                            in 128..191 -> lbpMid2 += 1f
                            else -> lbpHigh += 1f
                        }

                        // Gradient magnitudes
                        val dx = (lum[y * w + (x + 1)] - lum[y * w + (x - 1)]).toFloat()
                        val dy = (lum[(y + 1) * w + x] - lum[(y - 1) * w + x]).toFloat()
                        val mag = sqrt(dx * dx + dy * dy)
                        val angle = (atan2(dy, dx) * 180.0 / Math.PI + 180.0).toFloat() % 180f

                        when (angle) {
                            in 0f..45f -> grad0 += mag
                            in 45f..90f -> grad45 += mag
                            in 90f..135f -> grad90 += mag
                            else -> grad135 += mag
                        }
                    }
                }

                // Place in feature vector
                featureVector[vectorOffset] = lbpLow
                featureVector[vectorOffset + 1] = lbpMid1
                featureVector[vectorOffset + 2] = lbpMid2
                featureVector[vectorOffset + 3] = lbpHigh
                featureVector[vectorOffset + 4] = grad0
                featureVector[vectorOffset + 5] = grad45
                featureVector[vectorOffset + 6] = grad90
                featureVector[vectorOffset + 7] = grad135
            }
        }

        // L2 normalize vector
        var sumSquares = 0f
        for (v in featureVector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares).coerceAtLeast(1e-6f)
        for (i in featureVector.indices) {
            featureVector[i] /= norm
        }

        return featureVector
    }

    /**
     * Cosine similarity between two normalized vectors: sum(a_i * b_i).
     */
    fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot.coerceIn(-1f, 1f)
    }
}

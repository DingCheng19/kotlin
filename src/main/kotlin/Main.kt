import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

fun startChallenge(nickname: String): JSONObject {
    val endpoint = "http://challenge.z2o.cloud/challenges?nickname=$nickname"
    val url = URL(endpoint)
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            return JSONObject(response)
        }
    }
}

fun makeCall(challengeId: String): JSONObject {
    val endpoint = "http://challenge.z2o.cloud/challenges"
    val url = URL(endpoint)
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = "PUT"
        setRequestProperty("X-Challenge-Id", challengeId)
        inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            return JSONObject(response)
        }
    }
}

fun calculateServerReturnTime(
    localTimeBeforeRequest: Long,
    calledAt: Long,
    localTimeAfterRequest: Long
): Long {
    // ミリ秒タイムスタンプを Instant オブジェクトに変換する
    val beforeRequestInstant = Instant.ofEpochMilli(localTimeBeforeRequest)
    val calledAtInstant = Instant.ofEpochMilli(calledAt)
    val afterRequestInstant = Instant.ofEpochMilli(localTimeAfterRequest)

    // 往復時間（RTT）を計算する
    val rtt = Duration.between(beforeRequestInstant, afterRequestInstant)

    // サーバー上での処理時間を計算する
    val timeOnServer = Duration.between(calledAtInstant, afterRequestInstant).minus(rtt.dividedBy(2))

    // サーバーが返答する時間の Instant オブジェクトを計算する
    val serverReturnInstant = calledAtInstant.plus(timeOnServer).plus(rtt.dividedBy(2))

    // Instant オブジェクトをミリ秒タイムスタンプに変換する
    return serverReturnInstant.toEpochMilli()
}

fun calculateServerTime(
    localTimeBeforeRequestMillis: Long,
    calledAtMillis: Long,
    localTimeAfterRequestMillis: Long
): Long {


    // 將毫秒轉換為 Instant
    val localTimeBeforeRequest = Instant.ofEpochMilli(localTimeBeforeRequestMillis)
    val calledAt = Instant.ofEpochMilli(calledAtMillis)
    val localTimeAfterRequest = Instant.ofEpochMilli(localTimeAfterRequestMillis)

    // 計算請求的往返時間
    val roundTripTime = Duration.between(localTimeBeforeRequest, localTimeAfterRequest)
    // 計算半程時間
    val halfRoundTripTime = roundTripTime.dividedBy(2)
    // 計算服務器的時間
    val serverTime = calledAt.plus(halfRoundTripTime)

    // 將 Instant 轉換回毫秒
    return serverTime.toEpochMilli()
}

fun main(args: Array<String>) = runBlocking {
    val nickname = "nikeDing"
//    var activesAt: Long = 0
//    var calledAt: Long = 0
    var waitTime: Long = 0
    try {
        // チャレンジの開始
        var localTimeBeforeRequest  = System.nanoTime()
        val challenge = startChallenge(nickname)
        var localTimeAfterRequest   = System.nanoTime()
        val challengeId = challenge.getString("id")
        println("チャレンジ開始、チャレンジID: $challengeId")
//        activesAt = challenge.getLong("actives_at")
//        calledAt = calculateServerTime(localTimeBeforeRequest, challenge.getLong("called_at"),localTimeAfterRequest)
//        val str1 = formatUnixTimestamp(localTimeBeforeRequest)
//        val str2 = formatUnixTimestamp(challenge.getLong("called_at"))
//        val str3 = formatUnixTimestamp(localTimeAfterRequest)
//        val str4 = formatUnixTimestamp(activesAt)
//        val str5 = formatUnixTimestamp(calledAt)
         waitTime = challenge.getLong("actives_at") - challenge.getLong("called_at") - (localTimeAfterRequest -localTimeBeforeRequest ) / 2 / 1_000_000 - 10
//        waitTime = calculateWaitTime(challenge.getLong("actives_at"),challenge.getLong("called_at"),localTimeBeforeRequest,localTimeAfterRequest)
        while (true) {




            if (waitTime > 0) {
                println("次の呼び出し時間まで ${waitTime / 1000.0} 秒待機")
//                Thread.sleep(waitTime)
                delay(waitTime )
            }

            // 呼び出しの実行
            localTimeBeforeRequest  = System.nanoTime()
            val result = makeCall(challengeId)

//            println("呼び出し成功、現在の時間: ${result.getLong("called_at")}, 予定時間: ${result.getLong("actives_at")}, 総差分: ${result.getLong("total_diff")}ms")

            val calledAtStr = if (result.has("called_at")) result.getLong("called_at") else "不明"
            val activesAtStr = if (result.has("actives_at")) result.getLong("actives_at") else "不明"
            val totalDiffStr = if (result.has("total_diff")) result.getLong("total_diff") else "不明"

            println("呼び出し成功、現在の時間: $calledAtStr, 予定時間: $activesAtStr, 総差分: $totalDiffStr ms")

            // チャレンジが終了したかどうかの確認
            if (result.has("result")) {
                val resultObject = result.getJSONObject("result")
                println("チャレンジ終了、呼び出し回数: ${resultObject.getInt("attempts")}")
                if (resultObject.isNull("url")) {
                    println("チャレンジ失敗")
                } else {
                    println("応募キーワードページのURL: ${resultObject.getString("url")}")
                }
                break
            }

//            // 更新
            localTimeAfterRequest   = System.nanoTime()
            waitTime = result.getLong("actives_at") - result.getLong("called_at") - (localTimeAfterRequest -localTimeBeforeRequest ) / 2 / 1_000_000- 11

//            waitTime = calculateWaitTime(result.getLong("actives_at"),result.getLong("called_at"),localTimeBeforeRequest,localTimeAfterRequest) //result.getLong("actives_at") - result.getLong("called_at")- (localTimeAfterRequest -localTimeBeforeRequest ) / 2 - 10
//            activesAt = result.getLong("actives_at")
//            calledAt = calculateServerReturnTime(localTimeBeforeRequest, result.getLong("called_at"),localTimeAfterRequest)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}

fun calculateWaitTime(activesAt: Long, calledAt: Long, localTimeBeforeRequest: Long, localTimeAfterRequest: Long): Long {
    // 使用纳秒时间来计算waitTime
    return activesAt - calledAt - (localTimeAfterRequest - localTimeBeforeRequest) / 2 -10 //- 10_000_000 // 10ms的缓冲
}

fun formatUnixTimestamp(unixTimestamp: Long): String {
    // 创建 DateTimeFormatter 对象，指定所需的格式
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault()) // 使用系统默认时区

    // 将 Unix 时间戳转换为 Instant 对象
    val instant = Instant.ofEpochMilli(unixTimestamp)

    // 格式化 Instant 对象为指定格式的字符串
    return formatter.format(instant)
}
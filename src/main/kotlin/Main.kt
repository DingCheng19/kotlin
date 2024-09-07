import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

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

fun main(args: Array<String>){
    val nickname = "nikeDing"
    var waitTime: Long = 0
    try {
        // チャレンジの開始
        var localTimeBeforeRequest  = System.currentTimeMillis()
        val challenge = startChallenge(nickname)
        var localTimeAfterRequest   = System.currentTimeMillis()
        val challengeId = challenge.getString("id")
        println("チャレンジ開始、チャレンジID: $challengeId")
        waitTime = challenge.getLong("actives_at") - challenge.getLong("called_at") - (localTimeAfterRequest -localTimeBeforeRequest ) +70
        while (true) {
            if (waitTime > 0) {
                println("次の呼び出し時間まで ${waitTime / 1000.0} 秒待機")
                Thread.sleep(waitTime)
            }

            // 呼び出しの実行
            localTimeBeforeRequest  = System.currentTimeMillis()
            val result = makeCall(challengeId)

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
            localTimeAfterRequest   = System.currentTimeMillis()
            waitTime = result.getLong("actives_at") - result.getLong("called_at")- (localTimeAfterRequest -localTimeBeforeRequest )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}
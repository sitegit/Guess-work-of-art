package com.example.guessworkofart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.guessworkofart.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var descriptions: List<String>
    private lateinit var urlsImg: List<String>
    private lateinit var buttons: List<Button>
    private var numberOfQuestion = 0
    private var numberOfRightQuestion = 0
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        start()
    }

    private fun start() {
        val baseUrl = getString(R.string.url_work_of_art)

        descriptions = mutableListOf()
        urlsImg = mutableListOf()
        buttons = listOf(
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4
        )

        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            getContent(baseUrl)
        } else {
            Toast.makeText(this@MainActivity, getString(R.string.lost_network), Toast.LENGTH_SHORT).show()
        }
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                getContent(baseUrl)
            }

            override fun onLost(network: Network) {
                Toast.makeText(this@MainActivity, getString(R.string.lost_network), Toast.LENGTH_SHORT).show()
                binding.layout.isInvisible = true
            }
        })
    }
    private fun getContent(baseUrl: String) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                downloadText(baseUrl)
            }
            createPattern(text)
            playGame()
        }
    }
    private fun playGame() {
        lifecycleScope.launch {
            generateQuestion()
            val bitMap = withContext(Dispatchers.IO) {
                downloadImage(urlsImg[numberOfQuestion])
            }
            bitMap?.let {
                binding.imageView.setImageBitmap(it)
                buttons.forEachIndexed { index, button ->
                    val tempList = mutableListOf<String>()
                    val buttonText: String
                    if (index == numberOfRightQuestion) {
                        buttonText = descriptions[numberOfQuestion]
                    } else {
                        while (true) {
                            val wrongAnswer = descriptions.indices.random()
                            if (wrongAnswer != numberOfQuestion && !tempList.contains(
                                    descriptions[wrongAnswer]
                                )
                            ) {
                                buttonText = descriptions[wrongAnswer]
                                tempList.add(descriptions[wrongAnswer])
                                break
                            }
                        }
                    }
                    button.text = buttonText
                }
            }
            binding.layout.isVisible = true
        }
    }

    private fun generateQuestion() {
        try {
            numberOfQuestion = descriptions.indices.random()
            numberOfRightQuestion = buttons.indices.random()
        } catch (e: NoSuchElementException) {
            Toast.makeText(this@MainActivity, getString(R.string.failed_data), Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPattern(text: String) {
        val patternBaseUrl = Regex(getString(R.string.start_pattern_url) + getString(R.string.finish_pattern_url))
        val splitContent = patternBaseUrl.findAll(text).map { it.groupValues[1] }.toList().toString()
        val patternImg = Regex(getString(R.string.pattern_img))
        val patternText = Regex(getString(R.string.pattern_text))
        descriptions = patternText.findAll(splitContent).map { it.groupValues[1] }.toList()
        urlsImg = patternImg.findAll(splitContent).map { getString(R.string.https) + it.groupValues[1] }.toList()
    }

    private suspend fun downloadText(stringUrl: String): String {
        return withContext(Dispatchers.IO) {
            val url: URL?
            var urlConnection: HttpURLConnection? = null
            val result = StringBuilder()
            try {
                url = URL(stringUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                val inputStream = urlConnection.inputStream
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var line = bufferedReader.readLine()
                while (line != null) {
                    result.append(line)
                    line = bufferedReader.readLine()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                urlConnection?.disconnect()
            }
            result.toString()
        }
    }

    private suspend fun downloadImage(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            val urlConnection = URL(urlString).openConnection() as? HttpURLConnection
            urlConnection?.let {
                try {
                    it.connect()
                    val inputStream = it.inputStream
                    bitmap = BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    urlConnection.disconnect()
                }
            }
            bitmap
        }
    }

    fun onClickAnswer(view: View) {
        val button = view as Button
        val tag = button.tag.toString()
        if (tag.toInt() == numberOfRightQuestion) {
            toast?.cancel()
            toast = Toast.makeText(this, getString(R.string.right_answer), Toast.LENGTH_SHORT)
            toast?.show()
        } else {
            toast?.cancel()
            toast = Toast.makeText(
                this,
                "${getString(R.string.incorrect_answer)} ${descriptions[numberOfQuestion]}",
                Toast.LENGTH_SHORT
            )
            toast?.show()
        }
        playGame()
    }
}
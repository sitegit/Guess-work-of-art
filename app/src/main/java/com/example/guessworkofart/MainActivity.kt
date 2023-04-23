package com.example.guessworkofart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        val baseUrl = getString(R.string.url_work_of_art)

        descriptions = mutableListOf()
        urlsImg = mutableListOf()
        buttons = listOf(
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4
        )

        getContent(baseUrl)
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
                    val buttonText = if (index == numberOfRightQuestion) {
                        descriptions[numberOfQuestion]
                    } else {
                        descriptions[generateWrongAnswer()]
                    }
                    button.text = buttonText
                }
            }
            binding.layout.isVisible = true
        }
    }

    private fun generateQuestion() {
        numberOfQuestion = descriptions.indices.random()
        numberOfRightQuestion = buttons.indices.random()
    }

    private fun generateWrongAnswer() = descriptions.indices.random()

    private fun getContent(baseUrl: String) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                downloadText(baseUrl)
            }
            createPattern(text)
            playGame()
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
                // Ensure that the connection is properly closed
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
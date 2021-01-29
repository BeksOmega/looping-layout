package com.bekawestberg.loopinglayout.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val simpleAdapter = SimpleAdapter()

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LoopingLayoutManager(
                    this@MainActivity,
                    LoopingLayoutManager.HORIZONTAL,
                    false
            )
            adapter = simpleAdapter
        }

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val api = retrofit.create(Api::class.java)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                val result = api.runCatching { getPhotos() }.onFailure {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                    }
                } .getOrDefault(emptyList()).let { it.map { SimpleAdapter.DataItem("${BASE_URL}id/${it.id}/100") } }
                withContext(Dispatchers.Main) {
                    simpleAdapter.items = result
                }
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://picsum.photos/"
    }
}
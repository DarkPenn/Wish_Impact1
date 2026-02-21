package com.example.wishimpacttest

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

// 1. Định nghĩa dữ liệu
enum class Rarity(val stars: Int, val colorHex: String) {
    THREE_STAR(3, "#42A5F5"), // Xanh
    FOUR_STAR(4, "#AB47BC"),  // Tím
    FIVE_STAR(5, "#FFD700")   // Vàng
}

data class GachaItem(val name: String, val rarity: Rarity)

class MainActivity : AppCompatActivity() {

    // 2. Khai báo biến giao diện
    private lateinit var tvLabel: TextView
    private lateinit var resultContainer: LinearLayout // <--- THAY ĐỔI: Dùng Container thay vì TextView
    private lateinit var tvHistory: TextView
    private lateinit var btnWish1: Button
    private lateinit var btnWish10: Button

    // 3. Database & Logic
    private val pool3 = listOf("Kiếm Cùi", "Sách Cũ", "Gậy Gỗ", "Cung Tập Sự")
    private val pool4 = listOf("Amber", "Kaeya", "Lisa", "Barbara", "Bennett")
    private val pool5 = listOf("Diluc", "Jean", "Keqing", "Mona", "Qiqi")

    private var pity5 = 0
    private var pity4 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showChooseBanner() // Chạy hàm sử lý trang chọn banner khi start
    }

    private fun showChooseBanner() {
        setContentView(R.layout.choose_banner) // Hiển thị trang chọn banner

        // 4. Ánh xạ (Kết nối code với XML choose_banner)
        val bannerIDs=listOf(R.id.Banner1,R.id.Banner2,R.id.Banner3,R.id.Banner4)

        //Lặp từng ID để thiết lập click listener
        bannerIDs.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                setupMainActivity() // Chạy hàm thiết lập giao diện khi chọn banner
            } }
    }

    private fun setupMainActivity(){
        setContentView(R.layout.activity_main)


        tvHistory = findViewById(R.id.tvHistory)
        btnWish1 = findViewById(R.id.btnWish1)
        btnWish10 = findViewById(R.id.btnWish10)

        // 5. Sự kiện bấm nút
        btnWish1.setOnClickListener {
            val item = pullOne()
            showResultInBannerLayout(listOf(item))
        }

        btnWish10.setOnClickListener {
            val items = List(10) { pullOne() }
            showResultInBannerLayout(items)
        }
    }

    private fun RollHistory(item: GachaItem) {

    }

    // Hàm quay 1 lần (có tính toán bảo hiểm)
    private fun pullOne(): GachaItem {

        pity5++
        pity4++
        val rate = Random.nextDouble(0.0, 100.0)

        // Logic 5 sao (0.2% hoặc pity 90)
        if (pity5 >= 150 || rate <= 0.001) {
            pity5 = 0; pity4 = 0
            return GachaItem(pool5.random(), Rarity.FIVE_STAR)
        }
        // Logic 4 sao (5.1% hoặc pity 10)
        if (pity4 >= 10 || rate <= 5.7) {
            pity4 = 0
            return GachaItem(pool4.random(), Rarity.FOUR_STAR)
        }
        return GachaItem(pool3.random(), Rarity.THREE_STAR)
    }

    // Hàm cập nhật màn hình (LOGIC MỚI CHO THANH NGANG)
    // MÀN HÌNH 3: HIỂN THỊ KẾT QUẢ QUAY (banner_layout)
    private fun showResultInBannerLayout(items: List<GachaItem>) {
        setContentView(R.layout.banner_layout)

        val tvLabel: TextView = findViewById(R.id.tvLabel)
        val resultContainer: LinearLayout = findViewById(R.id.resultContainer)
        val btnBack: Button = findViewById(R.id.btnBackFromResultToBanner)

        tvLabel.text = if (items.size > 1) "Kết quả 10 lần quay:" else "Bạn nhận được:"

        // Hiển thị các vật phẩm quay được vào Horizontal View
        items.forEach { item ->
            val itemView = TextView(this)
            val stars = "★".repeat(item.rarity.stars)
            itemView.text = "${item.name}\n$stars"
            itemView.setTextColor(Color.parseColor(item.rarity.colorHex))
            itemView.textSize = 16f
            itemView.typeface = Typeface.DEFAULT_BOLD
            itemView.gravity = Gravity.CENTER
            itemView.setBackgroundColor(Color.parseColor("#3E3E55"))

            val params = LinearLayout.LayoutParams(215, LinearLayout.LayoutParams.MATCH_PARENT)
            params.setMargins(5, 0, 5, 0)
            itemView.layoutParams = params

            resultContainer.addView(itemView)
        }
        btnBack.setOnClickListener {
            setupMainActivity()
        }
    }

}
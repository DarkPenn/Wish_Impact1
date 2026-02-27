package com.example.wishimpacttest

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
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

data class GachaItem(val name: String, val rarity: Rarity) //khởi tạo 1 loại lớp dùng để lưu trữ dữ liệu

class MainActivity : AppCompatActivity() {

    // 2. Khai báo biến giao diện
    private lateinit var tvLabel: TextView  //lateinit var: dùng để khai báo biến nhưng chưa gán giá trị
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvHistory: TextView
    private lateinit var btnWish1: Button
    private lateinit var btnWish10: Button
    private lateinit var btnHistory: Button



    // 3. Database & Logic
    private val pool3 = listOf("Kiếm Cùi", "Sách Cũ", "Gậy Gỗ", "Cung Tập Sự") //khai báo danh sách vật phấm sẽ rơi ra trong 3*
    private val pool4 = listOf("Amber", "Kaeya", "Lisa", "Barbara", "Bennett") //khai báo danh sách vật phấm sẽ rơi ra trong 4*
    private val pool5 = listOf("Diluc", "Jean", "Keqing", "Mona", "Qiqi") //khai báo danh sách vật phấm sẽ rơi ra trong 5*

    private var pity5 = 0 //pity luôn bắt đầu từ 0
    private var pity4 = 0 //pity luôn bắt đầu từ 0
    private var currentBannerImage: Int = R.drawable.banner1 //dòng dùng để lưu trữ ID của tấm hình banner được chọn
    // Lưu lịch sử cho từng loại BANNER
    private val bannerHistories = mutableMapOf<Int, MutableList<GachaItem>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showChooseBanner() // Chạy hàm sử lý trang chọn banner khi start
    }

    private fun showChooseBanner() {
        setContentView(R.layout.choose_banner)

        // Tạo danh sách các cặp (Nút bấm - Hình ảnh tương ứng)
        val bannerButtons = mapOf(                   //mapof: giúp quản lí nút nào đi với hình nào
            R.id.Banner1 to R.drawable.banner1,     //kết nối id banner với ảnh banner
            R.id.Banner2 to R.drawable.banner2,
            R.id.Banner3 to R.drawable.banner3,
            R.id.Banner4 to R.drawable.banner4
        )

        // Duyệt qua từng cặp để thiết lập sự kiện
        bannerButtons.forEach { (btnId, resId) ->
            findViewById<Button>(btnId).setOnClickListener {
                currentBannerImage = resId // Dòng dùng để lưu lại hình ảnh được chọn
                if(!bannerHistories.containsKey(currentBannerImage))
                    bannerHistories[currentBannerImage] = mutableListOf()

                setupMainActivity()        // Sau đó mới chuyển sang màn hình chính
            }
        }
    }

    private fun setupMainActivity() {
        setContentView(R.layout.activity_main)

        //Tìm ImageView trong activity_main và gán hình ảnh tương ứng
        val imageView: ImageView = findViewById(R.id.imageView)

        // Đây chính là dòng giúp app hiển thị đúng Banner
        imageView.setImageResource(currentBannerImage)

        btnWish1 = findViewById(R.id.btnWish1)
        btnWish10 = findViewById(R.id.btnWish10)
        btnHistory = findViewById(R.id.btnHistory)

        // 5. Sự kiện bấm nút
        btnWish1.setOnClickListener {
            val item = pullOne()  //Gọi hàm logic để lấy ra 1 món đồ ngẫu nhiên
            bannerHistories[currentBannerImage]?.add(0, item) // Thêm lên đầu danh sách lịch sử
            // listOf(item) là biến 1 món đồ đơn lẻ thành 1 danh sách để hàm hiển thị xử
            showResultInBannerLayout(listOf(item))
        }

        btnWish10.setOnClickListener {
            val items = List(10) { pullOne() }  //Gọi hàm logic để lấy ra 10 món đồ ngẫu nhiên
            bannerHistories[currentBannerImage]?.addAll(0, items) // Thêm lên đầu danh sách lịch sử
            showResultInBannerLayout(items)  //Chuyển sang màn hình kết quả để hiển thị toàn bộ danh sách 10 món đồ
        }

        btnHistory.setOnClickListener {
            showHistory()
        }
    }

    private fun showHistory() {
        setContentView(R.layout.banner_history)
        val tvTitle: TextView = findViewById(R.id.tvHistoryTitle)
        val tvContent: TextView = findViewById(R.id.tvHistoryContent)
        val btnBack: Button = findViewById(R.id.btnBackFromHistory)

        val historyList = bannerHistories[currentBannerImage] ?: mutableListOf()
        if(historyList.isEmpty()) {
            tvContent.text = "Chưa có lịch sử quay"
        } else {
            val item = StringBuilder()
            historyList.forEach {
                item.append("[${it.rarity.stars}★] ${it.name}\n")
                item.setTextColor(Color.parseColor(item.rarity.colorHex)) //Lấy mã màu đã định nghĩa ở phần Rarity để tô màu cho món đồ
                item. = 16f
                item.typeface = Typeface.DEFAULT_BOLD
            }
            tvContent.text = item.toString()
        }
        btnBack.setOnClickListener {
            setupMainActivity()
        }
    }

    // Hàm quay 1 lần (có tính toán bảo hiểm)
    private fun pullOne(): GachaItem {

        pity5++
        pity4++
        val rate = Random.nextDouble(0.0, 100.0)


        // Logic 5 sao (0.001% hoặc pity 90)
        if (pity5 >= 150 || rate <= 0.001) {
            pity5 = 0; pity4 = 0
            return GachaItem(pool5.random(), Rarity.FIVE_STAR)
        }
        // Logic 4 sao (5.7% hoặc pity 10)
        if (pity4 >= 10 || rate <= 5.7) {
            pity4 = 0
            return GachaItem(pool4.random(), Rarity.FOUR_STAR)
        }
        return GachaItem(pool3.random(), Rarity.THREE_STAR)
    }

    // Hàm cập nhật màn hình (LOGIC MỚI CHO THANH NGANG)
    // MÀN HÌNH 3: Hiển thị kết quả quay (banner_layout)
    private fun showResultInBannerLayout(items: List<GachaItem>) {
        setContentView(R.layout.banner_layout)

        val tvLabel: TextView = findViewById(R.id.tvLabel)
        val resultContainer: LinearLayout = findViewById(R.id.resultContainer)
        val btnBack: Button = findViewById(R.id.btnBackFromResultToBanner)

        // 4. Kiểm tra số lượng đồ: Nếu quay > 1 món (quay x10) thì hiện "Kết quả...", còn lại hiện "Bạn nhận được:"
        tvLabel.text = if (items.size > 1) "Kết quả 10 lần quay:" else "Bạn nhận được:"

        // Hiển thị các vật phẩm quay được vào Horizontal View thông qua vòng lặp
        items.forEach { item ->
            val itemView = TextView(this)
            val stars = "★".repeat(item.rarity.stars) //Tạo hình ngôi sao tương ứng với độ hiếm
            itemView.text = "${item.name}\n$stars" //Hiển thị tên vật phẩm và hình ngôi sao
            itemView.setTextColor(Color.parseColor(item.rarity.colorHex)) //Lấy mã màu đã định nghĩa ở phần Rarity để tô màu cho món đồ
            itemView.textSize = 16f
            itemView.typeface = Typeface.DEFAULT_BOLD
            itemView.gravity = Gravity.CENTER
            itemView.setBackgroundColor(Color.parseColor("#3E3E55"))

            //Thiết lập kích thước cho ô kết quả: Rộng 215 pixel
            val params = LinearLayout.LayoutParams(215, LinearLayout.LayoutParams.MATCH_PARENT)
            // Tạo khoảng cách 5 pixel bên trái và phải để các món đồ không dính sát nhau
            params.setMargins(5, 0, 5, 0)
            // Áp dụng kích thước vừa thiết lập cho thẻ chữ
            itemView.layoutParams = params

            //Trả kết quả về khung (LinearLayout) hiển thi kết quả
            resultContainer.addView(itemView)
        }

        //Cài đặt nút quay lại màn hình quay
        btnBack.setOnClickListener {
            setupMainActivity()
        }
    }

}
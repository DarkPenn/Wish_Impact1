package com.example.wishimpacttest

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.random.Random

// 1. Định nghĩa dữ liệu
enum class Rarity(val stars: Int, val colorHex: String) {
    THREE_STAR(3, "#42A5F5"), // Xanh
    FOUR_STAR(4, "#AB47BC"),  // Tím
    FIVE_STAR(5, "#FFD700")   // Vàng
}

data class GachaItem(val name: String, val rarity: Rarity) //khởi tạo 1 loại lớp dùng để lưu trữ dữ liệu

data class WishHistory(
    val stt: Int,         // Số thứ tự
    val name: String,     // Tên vật phẩm
    val rarity: Rarity,   // Độ hiếm
    val time: String      // Thời gian quay
)
class MainActivity : AppCompatActivity() {

    // Khai báo biến giao diện
    private lateinit var tvLabel: TextView  //lateinit var: dùng để khai báo biến nhưng chưa gán giá trị
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvHistory: TextView

    private lateinit var btnHistory: Button
    private lateinit var imgbtnback : ImageButton
    private lateinit var btnWish1: Button
    private lateinit var btnWish10: Button

    // 3. Database & Logic
    private val pool3 = listOf("Kiếm Cùi", "Sách Cũ", "Gậy Gỗ", "Cung Tập Sự") //khai báo danh sách vật phấm sẽ rơi ra trong 3*
    private val pool4 = listOf("Amber", "Kaeya", "Lisa", "Barbara", "Bennett") //khai báo danh sách vật phấm sẽ rơi ra trong 4*
    private val pool5 = listOf("Diluc", "Jean", "Keqing", "Mona", "Qiqi") //khai báo danh sách vật phấm sẽ rơi ra trong 5*

    private var pity5 = 0 //pity luôn bắt đầu từ 0
    private var pity4 = 0 //pity luôn bắt đầu từ 0
    private var currentBannerImage: Int = R.drawable.banner1 //dòng dùng để lưu trữ ID của tấm hình banner được chọn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showChooseBanner() // Chạy hàm sử lý trang chọn banner khi start
    }

    private fun showChooseBanner() {
        setContentView(R.layout.choose_banner)

        val tvNameHeader: TextView = findViewById(R.id.tvUserNameHeader)
        val imgUser: ImageView = findViewById(R.id.imgUserIcon)
        tvNameHeader.text = UserManager.getDisplayName(this)

        //Hiển thị nút icon và đăng xuất nếu đã đăng nhập ở trên góc bên phải sử dụng popup
        imgUser.setOnClickListener { view ->
            if (UserManager.isLoggedIn(this)) {
                val popup = PopupMenu(this, view)
                popup.menu.add("Trang cá nhân")
                popup.menu.add("Đăng xuất")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Trang cá nhân" -> showProfile()
                        "Đăng xuất" -> {
                            UserManager.logout(this)
                            showChooseBanner()
                            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                popup.show()
            } else {
                showLogin()
            }
        }

        // Tạo danh sách các cặp (Nút bấm - Hình ảnh tương ứng)
        val bannerConfig = mapOf(                   //mapof: giúp quản lí nút nào đi với hình nào
            R.id.Banner1 to R.drawable.banner1,     //kết nối id banner với ảnh banner
            R.id.Banner2 to R.drawable.banner2,
            R.id.Banner3 to R.drawable.banner3,
            R.id.Banner4 to R.drawable.banner4
        )

        // Duyệt qua từng cặp để thiết lập sự kiện
        bannerConfig.forEach { (btnId, resId) ->
            findViewById<Button>(btnId).setOnClickListener {
                currentBannerImage = resId // Dòng dùng để lưu lại hình ảnh được chọn
                setupMainActivity()        // Sau đó mới chuyển sang màn hình chính
            }
        }
    }

    private fun setupMainActivity() {
        setContentView(R.layout.activity_main)

        //Hiển thị Tiền và tên Customer ở góc phải
        if(UserManager.isLoggedIn(this)==false) {
            findViewById<TextView>(R.id.tvTotalWishes).text = 0.toString()
            findViewById<TextView>(R.id.tvUserNameMain).text = "Customer"
        } else {
            findViewById<TextView>(R.id.tvTotalWishes).text = UserManager.getWishes(this).toString()
            findViewById<TextView>(R.id.tvUserNameMain).text = UserManager.getDisplayName(this)
        }

        //Hiển thị nút icon và đăng xuất nếu đã đăng nhập ở trên góc bên phải sử dụng popup
        findViewById<ImageView>(R.id.imgUserIconMain).setOnClickListener { view ->
            if (UserManager.isLoggedIn(this)) {
                val popup = PopupMenu(this, view)
                popup.menu.add("Trang cá nhân")
                popup.menu.add("Đăng xuất")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Trang cá nhân" -> showProfile()
                        "Đăng xuất" -> {
                            UserManager.logout(this)
                            setupMainActivity()
                            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                popup.show()
            } else {
                showLogin()
            }
        }

        //Tìm ImageView trong activity_main và gán hình ảnh tương ứng
        val imageView: ImageView = findViewById(R.id.imageView)

        // Đây chính là dòng giúp app hiển thị đúng Banner
        imageView.setImageResource(currentBannerImage)

        //Nút quay lại màn hình khi chọn banner
        val btnBackToChoose: Button = findViewById(R.id.btnBackToChoose)
        btnBackToChoose.setOnClickListener {
            showChooseBanner()
        }


//        tvHistory = findViewById(R.id.tvHistory)
        btnWish1 = findViewById(R.id.btnWish1)
        btnWish10 = findViewById(R.id.btnWish10)
        btnHistory = findViewById(R.id.btnHistory)

        // 5. Sự kiện bấm nút
        btnHistory.setOnClickListener {
            showHistory()
        }

        btnWish1.setOnClickListener {
            if(UserManager.getWishes(this) >= 0) {  //Kiểm tra đủ 1 Tiền tệ để quay 1 lần không
                val item = pullOne()  //Gọi hàm logic để lấy ra 1 món đồ ngẫu nhiên
                UserManager.removeWishes(this,1)    //Trừ 1 Tiền tệ sau khi quay 1 lần
                // listOf(item) là biến 1 món đồ đơn lẻ thành 1 danh sách để hàm hiển thị xử
                showResultInBannerLayout(listOf(item))
            }  else{
                Toast.makeText(this, "Bạn không đủ tiền để quay!", Toast.LENGTH_SHORT).show()
            }
        }

        btnWish10.setOnClickListener {
            if (UserManager.getWishes(this) >= 10) {    //Kiểm tra đủ 10 Tiền tệ để quay 10 lần không
                val items = List(10) { pullOne() }  //Gọi hàm logic để lấy ra 10 món đồ ngẫu nhiên
                UserManager.removeWishes(this,10)   //Trừ 10 Tiền tệ sau khi quay 10 lần
                showResultInBannerLayout(items)  //Chuyển sang màn hình kết quả để hiển thị toàn bộ danh sách 10 món đồ
            } else{
                Toast.makeText(this, "Bạn không đủ tiền để quay!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Các hàm xử lý đăng nhập, đăng ký, profile
    //Trang đăng nhập
    private fun showLogin() {
        setContentView(R.layout.layout_login)
        val edtUser = findViewById<EditText>(R.id.etLoginUsername)
        val edtPass = findViewById<EditText>(R.id.etLoginPassword)
        findViewById<Button>(R.id.btnLoginSubmit).setOnClickListener {
            val user = edtUser.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            val pass = edtPass.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            if (UserManager.login(this, user, pass)) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                showChooseBanner()
            } else {
                Toast.makeText(this, "Tài khoản hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener { showRegister() }
        findViewById<Button>(R.id.btnLoginBack).setOnClickListener { showChooseBanner() }
    }

    //Trang đăng ký
    private fun showRegister() {
        setContentView(R.layout.layout_register)
        val edtName = findViewById<EditText>(R.id.etRegDisplayName) //Tên hiển thị
        val edtUser = findViewById<EditText>(R.id.etRegUsername) //Tên đăng nhập
        val edtPass = findViewById<EditText>(R.id.etRegPassword) //Mật khẩu
        findViewById<Button>(R.id.btnRegisterSubmit).setOnClickListener {
            val name = edtName.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            val user = edtUser.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            val pass = edtPass.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show()
            } else if (name.contains(" ") || user.contains(" ") || pass.contains(" ")) {
                Toast.makeText(this, "Không được có khoảng trắng!", Toast.LENGTH_SHORT).show()
            } else {
                UserManager.register(this, name, user, pass)
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                showChooseBanner() // Đăng ký xong tự động vào luôn
            }
        }
        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener { showLogin() }
    }

    //Trang cá nhân
    private fun showProfile() {
        setContentView(R.layout.layout_profile)
        val edtName = findViewById<EditText>(R.id.etProfDisplayName)
        val edtNewPass = findViewById<EditText>(R.id.etProfNewPassword)
        val edtConfirm = findViewById<EditText>(R.id.etProfConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnProfileSave)
        val btnBackTop = findViewById<ImageButton>(R.id.btnProfileBackTop)

        val oldName = UserManager.getDisplayName(this)
        edtName.setText(oldName)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentName = edtName.text.toString().trim()
                val currentPass = edtNewPass.text.toString()
                btnSave.isEnabled = (currentName != oldName && currentName.isNotEmpty()) || currentPass.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        edtName.addTextChangedListener(watcher)
        edtNewPass.addTextChangedListener(watcher)

        btnSave.setOnClickListener {
            val newName = edtName.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            val newPass = edtNewPass.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            val confirm = edtConfirm.text.toString().trim() //dùng để lấy dữ liệu từ EditText cũng như tránh việc để khoảng trắng
            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass.isNotEmpty()) {
                if (newPass != confirm) {
                    Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass.contains(" ")) {
                    Toast.makeText(this, "Mật khẩu không được có khoảng trắng!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            UserManager.updateProfile(this, newName, newPass)
            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
            showChooseBanner()
        }
        btnBackTop.setOnClickListener { showChooseBanner() }
    }

    // Hàm quay 1 lần (có tính toán bảo hiểm)
    private fun pullOne(): GachaItem {

        pity5++
        pity4++
        val rate = Random.nextDouble(0.0, 100.0)


        // Logic 5 sao (0.001% hoặc pity 90)
        if (pity5 >= 90 || rate <= 0.001) {
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
            // Tăng STT (Số thứ tự)
            HistoryManager.totalWishes++

            // Lấy thời gian hiện tại chính xác lúc vật phẩm rơi ra
            val currentTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            // Định dạng lại chuỗi Độ hiếm (Ví dụ: "5 ★")
            val rarityString = "${item.rarity.stars} ★"

            // Gom cả 4 dữ liệu vào Object và lưu vào kho
            val historyRecord = WishHistory(
                stt = HistoryManager.totalWishes,  // Dữ liệu cột 1
                name = item.name,                  // Dữ liệu cột 2
                rarity = item.rarity,             // Dữ liệu cột 3
                time = currentTime                 // Dữ liệu cột 4
            )
            HistoryManager.historyList.add(historyRecord)

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

    object HistoryManager {
        // Danh sách này sẽ lưu toàn bộ lịch sử quay
        var totalWishes = 0
        val historyList = mutableListOf<WishHistory>()
    }
    private fun showHistory(){
        setContentView(R.layout.reward_history)

        tvHistory = findViewById(R.id.tvHistory)
        imgbtnback = findViewById(R.id.imgbtnBack)

        val allData = HistoryManager.historyList.reversed()

        // Ánh xạ RecyclerView và Cài đặt Adapter

        val rcvHistory: RecyclerView = findViewById(R.id.rcvHistory)// Tìm RecyclerView trên giao diện XML để chuẩn bị hiển thị dữ liệu

        val historyAdapter = HistoryAdapter(emptyList()) // Ban đầu để rỗng, sẽ cập nhật qua hàm loadPage
        rcvHistory.layoutManager = LinearLayoutManager(this)// Quy định cách sắp xếp các dòng xếp theo dạng danh sách cuộn dọc từ trên xuống
        rcvHistory.adapter = historyAdapter

        //Tự động vẽ thêm các đường kẻ ngang ngăn cách giữa các dòng để tạo thành hình cái bảng
        rcvHistory.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))

        // Xử lý Logic Phân trang (Pagination)
        val btnPrevPage: Button = findViewById(R.id.btnDecrease)
        val btnNextPage: Button = findViewById(R.id.btnIncrease)
        val tvCurrentPage: TextView = findViewById(R.id.tvNumber)

        var currentPage = 1
        val itemsPerPage = 5 // Mỗi trang hiện 5 dòng

        // Tính tổng số trang (Nếu chưa quay gì thì mặc định là 1)
        val totalPages = if (allData.isNotEmpty()) {
            Math.ceil(allData.size / itemsPerPage.toDouble()).toInt()
        } else {
            1
        }

        // Hàm nội bộ để cắt dữ liệu và hiển thị đúng trang
        fun loadPage(page: Int) {
            tvCurrentPage.text = page.toString()

            if (allData.isEmpty()) return // Tránh lỗi nếu danh sách trống

            // Tính toán vị trí bắt đầu và kết thúc của danh sách con
            val startIndex = (page - 1) * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, allData.size)

            // Cắt lấy 5 vật phẩm của trang hiện tại và đẩy vào Adapter
            val pageData = allData.subList(startIndex, endIndex)
            historyAdapter.updateData(pageData)
        }

        // Load dữ liệu trang 1 ngay khi vừa mở màn hình
        loadPage(currentPage)

        // Xử lý khi bấm nút Trang tiếp theo (>)
        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                loadPage(currentPage)
            }
        }

        // Xử lý khi bấm nút Trang trước đó (<)
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadPage(currentPage)
            }
        }

        imgbtnback.setOnClickListener {
            setupMainActivity()
        }
    }
}
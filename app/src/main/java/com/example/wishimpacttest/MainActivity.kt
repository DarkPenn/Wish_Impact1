package com.example.wishimpacttest

import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.wishimpacttest.MainActivity.PriceManager.getBasePrice
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
    val time: String,   // Thời gian quay
    var customPrice: Int = 0, // có thể thay đổi được giá item
    var isSold: Boolean = false,// nếu true thì item đã bán ra (getbasePrice)
    var isListedOnShop: Boolean = false// nếu true thì item đã trên kệ của shop (getPrice)
)

// Tạo một class mới để chứa nhóm vật phẩm
data class ItemsGroup(
    val sampleItem: WishHistory,        // Lấy 1 món làm đại diện lấy tên, sao, hình ảnh
    val totalCount: Int,                // Tổng số lượng
    val rawItems: List<WishHistory>     // Danh sách các món đồ thực sự bên trong
)

data class ItemPrice(
    val name: String,     // Tên vật phẩm
    val rarity: Rarity,   // Độ hiếm
    val sellPrice: Int // Giá tiền của item
)
class MainActivity : AppCompatActivity() {

    // Khai báo biến giao diện
    private lateinit var tvLabel: TextView  //lateinit var: dùng để khai báo biến nhưng chưa gán giá trị
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvHistory: TextView

    private lateinit var btnHistory: Button
    private lateinit var imgbtnback : ImageButton

    private lateinit var imgbtnInventory : ImageButton
    private lateinit var imgbtnshop : ImageButton
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
        // LOAD DỮ LIỆU TỪ KHO KHI VỪA MỞ APP (Không cần gọi loadItems nữa vì SQLite load trực tiếp)
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
            findViewById<ImageButton>(btnId).setOnClickListener {
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
        val btnBackToChoose: ImageButton = findViewById(R.id.btnBackToChoose)
        btnBackToChoose.setOnClickListener {
            showChooseBanner()
        }

//        tvHistory = findViewById(R.id.tvHistory)
        btnWish1 = findViewById(R.id.btnWish1)
        btnWish10 = findViewById(R.id.btnWish10)
        btnHistory = findViewById(R.id.btnHistory)
        imgbtnInventory = findViewById(R.id.imgbtn_Inventory)
        imgbtnshop= findViewById(R.id.imgbtn_Shop)



        // 5. Sự kiện bấm nút
        imgbtnInventory.setOnClickListener {
            showInventory()
        }

        imgbtnshop.setOnClickListener {
            showShop()
        }

        btnHistory.setOnClickListener {
            showHistory()
        }

        btnWish1.setOnClickListener {
            if(UserManager.getWishes(this) >= 1) {  //Kiểm tra đủ 1 Tiền tệ để quay 1 lần không
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
        val btnGoToChangePass = findViewById<Button>(R.id.btnGoToChangePass)
        val layoutChangePass = findViewById<LinearLayout>(R.id.layoutChangePassword)
        val edtCurrentPass = findViewById<EditText>(R.id.etProfCurrentPass)
        val edtNewPass = findViewById<EditText>(R.id.etProfNewPassword)
        val edtConfirm = findViewById<EditText>(R.id.etProfConfirmPassword)
        val btnSave = findViewById<Button>(R.id.btnProfileSave)
        val btnBackTop = findViewById<ImageButton>(R.id.btnProfileBackTop)

        val oldName = UserManager.getDisplayName(this)
        edtName.setText(oldName)

        // Nhấn nút "Đổi mật khẩu"
        btnGoToChangePass.setOnClickListener {
            layoutChangePass.visibility = View.VISIBLE  // Ẩn khung thay đổi mật khẩu
            btnGoToChangePass.visibility = View.GONE    // Hiện khung thay đổi mật khẩu
            btnSave.isEnabled = true // Khi hiện đổi mật khẩu thì bật nút lưu luôn
        }

        // Xem coi người sở hữu tài khoản có thay đổi mật khẩu không để hiển thị nút lưu lên
        edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentName = edtName.text.toString().trim()
                btnSave.isEnabled = (currentName != oldName && currentName.isNotEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSave.setOnClickListener {
            val newName = edtName.text.toString().trim()
            val currentPassInput = edtCurrentPass.text.toString().trim()
            val newPass = edtNewPass.text.toString().trim()
            val confirm = edtConfirm.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Xử lý logic nếu đang mở phần đổi mật khẩu
            if (layoutChangePass.visibility == View.VISIBLE) {
                val realCurrentPass = UserManager.getPassword(this) // Đây là lệnh truy vấn dữ liệu trong SQLite dùng để tìm mật khẩu hiện tại dùng để so sánh mật khẩu nhập vào
                if (currentPassInput != realCurrentPass) {
                    Toast.makeText(this, "Mật khẩu hiện tại không đúng!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập mật khẩu mới!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass != confirm) {
                    Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPass.contains(" ")) {
                    Toast.makeText(this, "Mật khẩu không được có khoảng trắng!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            UserManager.updateProfile(this, newName, newPass)  //Cập nhật mật khẩu mới đã thành công đưa vào danh sách SQLite
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
            // Lấy thời gian hiện tại chính xác lúc vật phẩm rơi ra
            val currentTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            // Lưu vào SQLite (HISTORY)
            if (UserManager.isLoggedIn(this)) {
                val userId = UserManager.getCurrentUserId(this)
                val db = DatabaseHelper(this).writableDatabase
                
                // Tìm ID của vật phẩm trong bảng VatPham
                val cursor = db.rawQuery("SELECT ID FROM VatPham WHERE TenVatPham=?", arrayOf(item.name))
                if (cursor.moveToFirst()) {
                    val vpId = cursor.getInt(0)
                    val v = ContentValues()
                    v.put("UserID", userId)
                    v.put("VatPhamID", vpId)
                    v.put("ThoiGian", currentTime)
                    db.insert("History", null, v)
                }
                cursor.close()
            }

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

    // Nơi quản lý toàn bộ vật phẩm trong game
    object ItemsManager {
        // Biến này sẽ bị xóa dần khi chuyển hẳn sang SQLite
        val historyList = mutableListOf<WishHistory>() 
    }

    object PriceManager {
        val priceList = mutableListOf<ItemPrice>()

        fun getBasePrice(stars: Int): Int {
            return when (stars) {
                5 -> 10
                4 -> 3
                3 -> 1
                else -> 0
            }
        }
        fun getPrice(item: WishHistory): Int {
            val foundItem = priceList.find { it.name == item.name }
            return foundItem?.sellPrice ?: getBasePrice(item.rarity.stars)
        }
    }
    
    // Lấy Thông Tin Lịch Sử từ SQLite
    private fun showHistory(){
        setContentView(R.layout.reward_history)     // Mở màn hình lịch sử quay

        val tvHistory: TextView = findViewById(R.id.tvHistory)
        val imgbtnback: ImageButton = findViewById(R.id.imgbtnBack)

        // Truy vấn dữ liệu JOIN vào bảng HISTORY từ SQLite
        val allData = mutableListOf<WishHistory>()
        if (UserManager.isLoggedIn(this)) {
            val userId = UserManager.getCurrentUserId(this)
            val db = DatabaseHelper(this).readableDatabase
            val sql = """
                SELECT History.ID, VatPham.TenVatPham, VatPham.SoSao, History.ThoiGian 
                FROM History 
                JOIN VatPham ON History.VatPhamID = VatPham.ID 
                WHERE History.UserID = ? 
                ORDER BY History.ID DESC
            """
            val cursor = db.rawQuery(sql, arrayOf(userId.toString()))
            var stt = cursor.count  // Lấy tổng số món đồ hiển thị để đánh số
            if (cursor.moveToFirst()) {    //Nếu tìm thấy món đồ
                do {
                    val name = cursor.getString(1)  // Tên vật phẩm
                    val star = cursor.getInt(2)     // Số sao
                    val time = cursor.getString(3)  // Thời gian quay ra
                    val rarity = Rarity.entries.first { it.stars == star }  // Độ hiếm để hiển thị màu sắc dựa vào số sao
                    allData.add(WishHistory(stt--, name, rarity, time)) // Thêm vào danh sách tạm thời
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        val rcvHistory: RecyclerView = findViewById(R.id.rcvHistory)
        val historyAdapter = HistoryAdapter(emptyList<WishHistory>())
        rcvHistory.layoutManager = LinearLayoutManager(this)
        rcvHistory.adapter = historyAdapter
        rcvHistory.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))

        val btnPrevPage: Button = findViewById(R.id.btnDecrease)
        val btnNextPage: Button = findViewById(R.id.btnIncrease)
        val tvCurrentPage: TextView = findViewById(R.id.tvNumber)

        var currentPage = 1 // Trang hiển thị đầu tiên bắt đâầu từ 1
        val itemsPerPage = 6 // Số lượng món đồ hiển thị được trên 1 trang là 6 món
        val totalPages = if (allData.isNotEmpty()) Math.ceil(allData.size / itemsPerPage.toDouble()).toInt() else 1 // Giúp trang nhận biết được việc chỉ chứa đc 6 món nên khi có món thứ 7 thì sang trang thứ 2

        fun loadPage(page: Int) {
            tvCurrentPage.text = page.toString()
            if (allData.isEmpty()) return
            val startIndex = (page - 1) * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, allData.size)
            historyAdapter.updateData(allData.subList(startIndex, endIndex))
        }

        loadPage(currentPage)
        btnNextPage.setOnClickListener { if (currentPage < totalPages) { currentPage++; loadPage(currentPage) } }
        btnPrevPage.setOnClickListener { if (currentPage > 1) { currentPage--; loadPage(currentPage) } }
        imgbtnback.setOnClickListener { setupMainActivity() }
    }


    private fun showInventory() {
        setContentView(R.layout.inventory)
        val imgbtnback: ImageButton = findViewById(R.id.imgbtnBack)

        val btnFilterAll: TextView = findViewById(R.id.btnFilterAll)
        val btnFilter5: TextView = findViewById(R.id.btnFilter5)
        val btnFilter4: TextView = findViewById(R.id.btnFilter4)
        val btnFilter3: TextView = findViewById(R.id.btnFilter3)
        val btnSortStarDesc: TextView = findViewById(R.id.btnSortStarDesc)
        val btnSortStarAsc: TextView = findViewById(R.id.btnSortStarAsc)
        val layoutFilterSort: LinearLayout = findViewById(R.id.layoutFilterSort)

        val tvTotalItems: TextView = findViewById(R.id.tvTotalItems)
        val tvEmptyInventory: TextView = findViewById(R.id.tvEmptyInventory)
        val rcvInventory: RecyclerView = findViewById(R.id.rcvInventory)

        // Cột phải (Bảng chi tiết)
        val panelDetail: LinearLayout = findViewById(R.id.panelDetail)
        val btnClosePanel: ImageButton = findViewById(R.id.btnClosePanel)
        val tvSelectedName: TextView = findViewById(R.id.tvSelectedName)
        val tvDetailStars: TextView = findViewById(R.id.tvDetailStars)
        val tvDetailPrice: TextView = findViewById(R.id.tvDetailPrice)
        val btnMinus: Button = findViewById(R.id.btnMinus)
        val tvCurrentQuantity: TextView = findViewById(R.id.tvCurrentQuantity)
        val btnPlus: Button = findViewById(R.id.btnPlus)
        val tvTotalEarn: TextView = findViewById(R.id.tvTotalEarn)
        val btnSell: Button = findViewById(R.id.btnSell)
        val btnPushToShop: Button = findViewById(R.id.btnPushToShop)
        val edtCustomPrice: EditText = findViewById(R.id.edtCustomPrice)
        val layoutFilterfollowstar: LinearLayout = findViewById(R.id.layoutFilterfollowstar)

        var currentFilterStar = 0
        var currentSortMode = "STAR_DESC" 
        var currentSelectedGroup: ItemsGroup? = null
        var sellQuantity = 1

        fun updateQuantityUI() {
            val group = currentSelectedGroup ?: return
            tvCurrentQuantity.text = sellQuantity.toString()
            val singlePrice = PriceManager.getBasePrice(group.sampleItem.rarity.stars)
            tvTotalEarn.text = "Thu về: +${singlePrice * sellQuantity} Tiền"
            btnMinus.isEnabled = sellQuantity > 1
            btnPlus.isEnabled = sellQuantity < group.totalCount
        }

        fun loadGroupedInventory() {
            // Lấy toàn bộ items từ SQLite và gom nhóm
            val inventoryList = mutableListOf<WishHistory>()
            if (UserManager.isLoggedIn(this)) {
                val userId = UserManager.getCurrentUserId(this)
                val db = DatabaseHelper(this).readableDatabase
                val sql = "SELECT VatPham.TenVatPham, VatPham.SoSao FROM History JOIN VatPham ON History.VatPhamID = VatPham.ID WHERE History.UserID = ?"
                val cursor = db.rawQuery(sql, arrayOf(userId.toString()))
                if (cursor.moveToFirst()) {
                    do {
                        val name = cursor.getString(0)
                        val star = cursor.getInt(1)
                        val rarity = Rarity.entries.first { it.stars == star }
                        inventoryList.add(WishHistory(0, name, rarity, ""))
                    } while (cursor.moveToNext())
                }
                cursor.close()
            }

            val grouped = inventoryList.groupBy { it.name }.map { (_, items) -> ItemsGroup(items.first(), items.size, items) }
            var processData = grouped.toList()

            if (currentFilterStar != 0) processData = processData.filter { it.sampleItem.rarity.stars == currentFilterStar }
            processData = if (currentSortMode == "STAR_ASC") processData.sortedBy { it.sampleItem.rarity.stars } else processData.sortedByDescending { it.sampleItem.rarity.stars }

            rcvInventory.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
            if (processData.isEmpty()) {
                tvEmptyInventory.visibility = View.VISIBLE
                rcvInventory.visibility = View.GONE
                panelDetail.visibility = View.GONE
            } else {
                tvEmptyInventory.visibility = View.GONE
                rcvInventory.visibility = View.VISIBLE
                rcvInventory.adapter = InventoryAdapter(processData) { clickedGroup ->
                    currentSelectedGroup = clickedGroup; sellQuantity = 1; panelDetail.visibility = View.VISIBLE
                    tvSelectedName.text = clickedGroup.sampleItem.name
                    tvDetailStars.text = "★".repeat(clickedGroup.sampleItem.rarity.stars)
                    tvDetailStars.setTextColor(Color.parseColor(clickedGroup.sampleItem.rarity.colorHex))
                    tvDetailPrice.text = "Giá trị gốc: ${getBasePrice(clickedGroup.sampleItem.rarity.stars)} Tiền"
                    updateQuantityUI()
                }
            }
            tvTotalItems.text = "Tổng số vật phẩm: ${inventoryList.size}"
        }

        btnClosePanel.setOnClickListener { panelDetail.visibility = View.GONE; currentSelectedGroup = null }
        btnFilterAll.setOnClickListener { currentFilterStar = 0; loadGroupedInventory() }
        btnFilter5.setOnClickListener { currentFilterStar = 5; loadGroupedInventory() }
        btnFilter4.setOnClickListener { currentFilterStar = 4; loadGroupedInventory() }
        btnFilter3.setOnClickListener { currentFilterStar = 3; loadGroupedInventory() }
        btnSortStarAsc.setOnClickListener { currentSortMode = "STAR_ASC"; loadGroupedInventory() }
        btnSortStarDesc.setOnClickListener { currentSortMode = "STAR_DESC"; loadGroupedInventory() }
        btnPlus.setOnClickListener { if (sellQuantity < currentSelectedGroup!!.totalCount) { sellQuantity++; updateQuantityUI() } }
        btnMinus.setOnClickListener { if (sellQuantity > 1) { sellQuantity--; updateQuantityUI() } }
        imgbtnback.setOnClickListener { setupMainActivity() }
        loadGroupedInventory()
    }

    private fun showShop() {
        // Hiện tại Shop cũng sẽ được cập nhật tương tự bằng cách query từ bảng GiaBanVP trong SQLite
        Toast.makeText(this, "Tính năng Shop đang được SQLite hóa...", Toast.LENGTH_SHORT).show()
        setupMainActivity()
    }
}


// Nhiệm vụ của nó là lấy dữ liệu từ mảng List<WishHistory> và mang đi hiển thị lên RecyclerView.
class HistoryAdapter(private var list: List<WishHistory>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    //Lớp ViewHolder lưu trữ các thành phần giao diện của 1 dòng
    // Việc này giúp ứng dụng không phải tốn tài nguyên đi tìm lại View (findViewById) liên tục khi người dùng cuộn lên xuống.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSTT: TextView = view.findViewById(R.id.tvSTT)       // Ô chứa Số thứ tự
        val tvName: TextView = view.findViewById(R.id.tvName)     // Ô chứa Tên vật phẩm
        val tvRarity: TextView = view.findViewById(R.id.tvRarity) // Ô chứa Độ hiếm (Số sao)
        val tvTime: TextView = view.findViewById(R.id.tvTime)     // Ô chứa Thời gian quay
    }

    // Hàm này được hệ thống gọi khi cần tạo ra một dòng mới (khung)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Nạp items_history để biến nó thành một View thật trên màn hình
        val view = LayoutInflater.from(parent.context).inflate(R.layout.items_history, parent, false)
        return ViewHolder(view) // Nhét View vừa tạo vào ViewHolder
    }

    //Hàm này được gọi để thêm dữ liệu thật vào ViewHolder đã được tạo trước đó
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Lấy món đồ ở đúng vị trí hiện tại đang được cuộn tới
        val item = list[position]
        holder.tvSTT.text = item.stt.toString()
        holder.tvName.text = item.name
        holder.tvTime.text = item.time
        holder.tvRarity.text = "★".repeat(item.rarity.stars)
        // tô màu cho dòng chữ hiển thị số sao đó.
        holder.tvRarity.setTextColor(Color.parseColor(item.rarity.colorHex))
    }

    //Báo cáo cho hệ thống biết mảng dữ liệu này có tổng cộng bao nhiêu món
    override fun getItemCount() = list.size

    // Hàm này dùng để cập nhật lại nội dung của bảng
    fun updateData(newList: List<WishHistory>) {
        list = newList
        notifyDataSetChanged()
    }
}

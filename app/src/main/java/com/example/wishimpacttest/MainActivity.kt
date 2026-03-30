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
    var isListedOnShop: Boolean = false,// nếu true thì item đã trên kệ của shop (getPrice)
    var listedBy: String = "",
    var historyId: Int = 0  // ID trong SQLite để update đúng hàng
)

// Tạo một class mới để chứa nhóm vật phẩm
data class ItemsGroup(
    val sampleItem: WishHistory,        // Lấy 1 món làm đại diện lấy tên, sao, hình ảnh
    val totalCount: Int,                // Tổng số lượng
    val rawItems: List<WishHistory>     // Danh sách các món đồ thực sự bên trong
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

    // 3. Database & Logic & Danh sách nhân vật , vật phẩm
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

    //Hàm tính softPity 5s
    private fun caculatorPity5(currentPity: Int): Double {
        return when {
            currentPity >= 90 -> 1.0 //nếu pity5s =90 chắc chắn ra 5s
            currentPity >= 74 ->   {    //nếu pity5s >=74 thì tăng tỉ lệ ra 5s lên 6% mỗi lượt
                0.006 + (currentPity-73)*0.06
            }
            else -> 0.006
        }
    }
    //Hàm tính softPity 4s
    private fun caculatorPity4(currentPity: Int): Double {
        return when {
            currentPity >= 10 -> 1.0 //nếu pity4s =10 chắc chắn ra 4s
            currentPity >= 9 ->  0.561  //nếu pity4s >=9 thì tăng tỉ lệ ra 4s thành 56.1%
            else -> 0.057
        }
    }
    // Hàm quay 1 lần (có tính toán bảo hiểm)
    private fun pullOne(): GachaItem {

        pity5++
        pity4++
        val rate = Random.nextDouble(0.0, 1.0)
        val rate4 = caculatorPity4(pity4)
        val rate5 = caculatorPity5(pity5)
        // Logic 5 sao (0.6% hoặc pity 90)
        if(rate <= rate5)
        {
            pity5 = 0
            return GachaItem(pool5.random(), Rarity.FIVE_STAR)
        }
        else if(rate <= rate4 + rate5)  // Logic 4 sao (5.7% hoặc pity 10)
        {
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
                    v.put("isSold", 0)
                    v.put("isListedOnShop", 0)
                    v.put("customPrice", 0)
                    v.put("listedBy", "")
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

    object PriceManager {
        fun getBasePrice(stars: Int): Int = when (stars) {
            5 -> 10
            4 -> 3
            3 -> 1
            else -> 0
        }
    }


    private fun showHistory() {
        setContentView(R.layout.reward_history)
        val imgbtnback: ImageButton = findViewById(R.id.imgbtnBack)

        // Lấy toàn bộ lịch sử của user từ SQLite
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
            var stt = cursor.count
            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(1)
                    val star = cursor.getInt(2)
                    val time = cursor.getString(3)
                    val rarity = Rarity.entries.first { it.stars == star }
                    allData.add(WishHistory(stt--, name, rarity, time))
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        val rcvHistory: RecyclerView = findViewById(R.id.rcvHistory)
        val historyAdapter = HistoryAdapter(emptyList())
        rcvHistory.layoutManager = LinearLayoutManager(this)
        rcvHistory.adapter = historyAdapter
        rcvHistory.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            )
        )

        val btnPrevPage: Button = findViewById(R.id.btnDecrease)
        val btnNextPage: Button = findViewById(R.id.btnIncrease)
        val tvCurrentPage: TextView = findViewById(R.id.tvNumber)

        var currentPage = 1
        val itemsPerPage = 5
        val totalPages = if (allData.isNotEmpty())
            Math.ceil(allData.size / itemsPerPage.toDouble()).toInt() else 1

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
        val layoutFilterfollowstar: LinearLayout = findViewById(R.id.layoutFilterfollowstar)

        val tvTotalItems: TextView = findViewById(R.id.tvTotalItems)
        val tvEmptyInventory: TextView = findViewById(R.id.tvEmptyInventory)
        val rcvInventory: RecyclerView = findViewById(R.id.rcvInventory)

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

        var currentFilterStar = 0
        var currentSortMode = "STAR_DESC"
        var currentSelectedGroup: ItemsGroup? = null
        var sellQuantity = 1

        // Lấy đồ chưa bán, chưa lên shop từ SQLite
        fun fetchInventoryFromDB(): List<WishHistory> {
            val list = mutableListOf<WishHistory>()
            if (!UserManager.isLoggedIn(this)) return list
            val userId = UserManager.getCurrentUserId(this)
            val db = DatabaseHelper(this).readableDatabase
            val sql = """
                SELECT History.ID, VatPham.TenVatPham, VatPham.SoSao, History.ThoiGian,
                       History.customPrice, History.isSold, History.isListedOnShop, History.listedBy
                FROM History
                JOIN VatPham ON History.VatPhamID = VatPham.ID
                WHERE History.UserID = ? AND History.isSold = 0 AND History.isListedOnShop = 0
                ORDER BY History.ID DESC
            """
            val cursor = db.rawQuery(sql, arrayOf(userId.toString()))
            var stt = 1
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        WishHistory(
                            stt = stt++,
                            name = cursor.getString(1),
                            rarity = Rarity.entries.first { it.stars == cursor.getInt(2) },
                            time = cursor.getString(3),
                            customPrice = cursor.getInt(4),
                            isSold = cursor.getInt(5) == 1,
                            isListedOnShop = cursor.getInt(6) == 1,
                            listedBy = cursor.getString(7) ?: "",
                            historyId = cursor.getInt(0)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            return list
        }

        fun updateQuantityUI() {
            val group = currentSelectedGroup ?: return
            tvCurrentQuantity.text = sellQuantity.toString()
            val singlePrice = getBasePrice(group.sampleItem.rarity.stars)
            tvTotalEarn.text = "Thu về: +${singlePrice * sellQuantity} Tiền"
            btnMinus.isEnabled = sellQuantity > 1
            btnPlus.isEnabled = sellQuantity < group.totalCount
        }

        fun loadGroupedInventory() {
            val inventoryList = fetchInventoryFromDB()

            // Ẩn/hiện bộ lọc
            layoutFilterfollowstar.visibility = if (inventoryList.isEmpty()) View.GONE else View.VISIBLE
            layoutFilterSort.visibility = if (inventoryList.isEmpty()) View.GONE else View.VISIBLE

            val has5 = inventoryList.any { it.rarity.stars == 5 }
            val has4 = inventoryList.any { it.rarity.stars == 4 }
            val has3 = inventoryList.any { it.rarity.stars == 3 }
            btnFilter5.visibility = if (has5) View.VISIBLE else View.GONE
            btnFilter4.visibility = if (has4) View.VISIBLE else View.GONE
            btnFilter3.visibility = if (has3) View.VISIBLE else View.GONE

            // Reset bộ lọc nếu loại sao đó không còn trong túi
            if (currentFilterStar == 5 && !has5) currentFilterStar = 0
            if (currentFilterStar == 4 && !has4) currentFilterStar = 0
            if (currentFilterStar == 3 && !has3) currentFilterStar = 0

            // Gom nhóm → lọc → sắp xếp
            var grouped = inventoryList
                .groupBy { it.name }
                .map { (_, items) -> ItemsGroup(items.first(), items.size, items) }

            if (currentFilterStar != 0)
                grouped = grouped.filter { it.sampleItem.rarity.stars == currentFilterStar }

            grouped = if (currentSortMode == "STAR_ASC")
                grouped.sortedBy { it.sampleItem.rarity.stars }
            else
                grouped.sortedByDescending { it.sampleItem.rarity.stars }

            rcvInventory.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)

            if (grouped.isEmpty()) {
                tvEmptyInventory.visibility = View.VISIBLE
                rcvInventory.visibility = View.GONE
                panelDetail.visibility = View.GONE
            } else {
                tvEmptyInventory.visibility = View.GONE
                rcvInventory.visibility = View.VISIBLE
                panelDetail.visibility = View.GONE
                currentSelectedGroup = null

                rcvInventory.adapter = InventoryAdapter(grouped) { clickedGroup ->
                    currentSelectedGroup = clickedGroup
                    sellQuantity = 1
                    panelDetail.visibility = View.VISIBLE
                    tvSelectedName.text = clickedGroup.sampleItem.name
                    tvDetailStars.text = "★".repeat(clickedGroup.sampleItem.rarity.stars)
                    tvDetailStars.setTextColor(Color.parseColor(clickedGroup.sampleItem.rarity.colorHex))
                    tvDetailPrice.text = "Giá trị gốc: ${getBasePrice(clickedGroup.sampleItem.rarity.stars)} Tiền"
                    updateQuantityUI()
                }
            }
            tvTotalItems.text = "Tổng số vật phẩm: ${inventoryList.size}"
        }

        // Nút bán lấy tiền
        btnSell.setOnClickListener {
            val group = currentSelectedGroup ?: return@setOnClickListener
            val pricePerItem = getBasePrice(group.sampleItem.rarity.stars)
            val totalEarned = pricePerItem * sellQuantity
            val itemsToSell = group.rawItems.take(sellQuantity)

            val db = DatabaseHelper(this).writableDatabase
            for (item in itemsToSell) {
                val v = ContentValues()
                v.put("isSold", 1)
                db.update("History", v, "ID=?", arrayOf(item.historyId.toString()))
            }
            UserManager.addWishes(this, totalEarned)
            Toast.makeText(this, "Đã bán $sellQuantity món, nhận $totalEarned Tiền!", Toast.LENGTH_SHORT).show()
            panelDetail.visibility = View.GONE
            loadGroupedInventory()
        }

        // Nút đưa lên shop
        btnPushToShop.setOnClickListener {
            val group = currentSelectedGroup ?: return@setOnClickListener
            val priceStr = edtCustomPrice.text.toString()
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập giá muốn bán!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userCustomPrice = priceStr.toInt()
            val itemsToPush = group.rawItems.take(sellQuantity)
            val currentUser = UserManager.getUsername(this)

            val db = DatabaseHelper(this).writableDatabase
            for (item in itemsToPush) {
                val v = ContentValues()
                v.put("isListedOnShop", 1)
                v.put("customPrice", userCustomPrice)
                v.put("listedBy", currentUser)
                db.update("History", v, "ID=?", arrayOf(item.historyId.toString()))
            }
            Toast.makeText(this, "Đã đưa $sellQuantity món lên Shop giá $userCustomPrice!", Toast.LENGTH_SHORT).show()
            edtCustomPrice.text.clear()
            panelDetail.visibility = View.GONE
            loadGroupedInventory()
        }

        btnClosePanel.setOnClickListener { panelDetail.visibility = View.GONE; currentSelectedGroup = null }
        btnFilterAll.setOnClickListener { currentFilterStar = 0; loadGroupedInventory() }
        btnFilter5.setOnClickListener { currentFilterStar = 5; loadGroupedInventory() }
        btnFilter4.setOnClickListener { currentFilterStar = 4; loadGroupedInventory() }
        btnFilter3.setOnClickListener { currentFilterStar = 3; loadGroupedInventory() }
        btnSortStarAsc.setOnClickListener { currentSortMode = "STAR_ASC"; loadGroupedInventory() }
        btnSortStarDesc.setOnClickListener { currentSortMode = "STAR_DESC"; loadGroupedInventory() }
        btnPlus.setOnClickListener {
            if (sellQuantity < (currentSelectedGroup?.totalCount ?: 1)) {
                sellQuantity++; updateQuantityUI()
            }
        }
        btnMinus.setOnClickListener { if (sellQuantity > 1) { sellQuantity--; updateQuantityUI() } }
        imgbtnback.setOnClickListener { setupMainActivity() }

        loadGroupedInventory()
    }

    private fun showShop() {
        setContentView(R.layout.shop)
        val imgbtnback: ImageButton = findViewById(R.id.imgbtnBack)

        val btnFilterAll: TextView = findViewById(R.id.btnShopFilterAll)
        val btnFilter5: TextView = findViewById(R.id.btnShopFilter5)
        val btnFilter4: TextView = findViewById(R.id.btnShopFilter4)
        val btnFilter3: TextView = findViewById(R.id.btnShopFilter3)
        val btnSortStarDesc: TextView = findViewById(R.id.btnShopSortStarDesc)
        val btnSortStarAsc: TextView = findViewById(R.id.btnShopSortStarAsc)
        val btnSortPriceDesc: TextView = findViewById(R.id.btnShopSortPriceDesc)
        val btnSortPriceAsc: TextView = findViewById(R.id.btnShopSortPriceAsc)
        val tvShopEmpty: TextView = findViewById(R.id.tvShopEmpty)
        val rcvShopItems: RecyclerView = findViewById(R.id.rcvShopItems)

        val panelBuyDetail: LinearLayout = findViewById(R.id.panelBuyDetail)
        val btnCloseShopPanel: ImageButton = findViewById(R.id.btnCloseShopPanel)
        val tvBuyName: TextView = findViewById(R.id.tvBuyName)
        val tvBuyStars: TextView = findViewById(R.id.tvBuyStars)
        val tvUnitPrice: TextView = findViewById(R.id.tvUnitPrice)
        val btnBuyMinus: Button = findViewById(R.id.btnBuyMinus)
        val tvBuyQuantity: TextView = findViewById(R.id.tvBuyQuantity)
        val btnBuyPlus: Button = findViewById(R.id.btnBuyPlus)
        val tvTotalPrice: TextView = findViewById(R.id.tvTotalPrice)
        val btnConfirmBuy: Button = findViewById(R.id.btnConfirmBuy)
        val btnBackInventory: Button = findViewById(R.id.btnBackInventory)

        var currentFilterStar = 0
        var currentSortMode = "STAR_DESC"
        var currentSelectedGroup: ItemsGroup? = null
        var buyQuantity = 1

        findViewById<TextView>(R.id.tvTotalWishes).text =
            if (UserManager.isLoggedIn(this)) UserManager.getWishes(this).toString() else "0"

        // Lấy toàn bộ đồ đang trên shop từ SQLite
        fun fetchShopFromDB(): List<WishHistory> {
            val list = mutableListOf<WishHistory>()
            val db = DatabaseHelper(this).readableDatabase
            val sql = """
                SELECT History.ID, VatPham.TenVatPham, VatPham.SoSao, History.ThoiGian,
                       History.customPrice, History.listedBy
                FROM History
                JOIN VatPham ON History.VatPhamID = VatPham.ID
                WHERE History.isSold = 0 AND History.isListedOnShop = 1
                ORDER BY History.customPrice ASC
            """
            val cursor = db.rawQuery(sql, null)
            var stt = 1
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        WishHistory(
                            stt = stt++,
                            name = cursor.getString(1),
                            rarity = Rarity.entries.first { it.stars == cursor.getInt(2) },
                            time = cursor.getString(3),
                            customPrice = cursor.getInt(4),
                            isListedOnShop = true,
                            listedBy = cursor.getString(5) ?: "",
                            historyId = cursor.getInt(0)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            return list
        }

        fun updateBuyUI() {
            val group = currentSelectedGroup ?: return
            tvBuyQuantity.text = buyQuantity.toString()
            tvTotalPrice.text = "Tổng thanh toán: ${group.sampleItem.customPrice * buyQuantity} Tiền"
            btnBuyMinus.isEnabled = buyQuantity > 1
            btnBuyPlus.isEnabled = buyQuantity < group.totalCount
        }

        fun loadShopData() {
            val shopList = fetchShopFromDB()

            val has5 = shopList.any { it.rarity.stars == 5 }
            val has4 = shopList.any { it.rarity.stars == 4 }
            val has3 = shopList.any { it.rarity.stars == 3 }
            btnFilter5.visibility = if (has5) View.VISIBLE else View.GONE
            btnFilter4.visibility = if (has4) View.VISIBLE else View.GONE
            btnFilter3.visibility = if (has3) View.VISIBLE else View.GONE

            if (currentFilterStar == 5 && !has5) currentFilterStar = 0
            if (currentFilterStar == 4 && !has4) currentFilterStar = 0
            if (currentFilterStar == 3 && !has3) currentFilterStar = 0

            var grouped = shopList
                .groupBy { "${it.name}_${it.customPrice}" }
                .map { (_, items) -> ItemsGroup(items.first(), items.size, items) }

            if (currentFilterStar != 0)
                grouped = grouped.filter { it.sampleItem.rarity.stars == currentFilterStar }

            grouped = when (currentSortMode) {
                "STAR_ASC"   -> grouped.sortedBy { it.sampleItem.rarity.stars }
                "STAR_DESC"  -> grouped.sortedByDescending { it.sampleItem.rarity.stars }
                "PRICE_ASC"  -> grouped.sortedBy { it.sampleItem.customPrice }
                "PRICE_DESC" -> grouped.sortedByDescending { it.sampleItem.customPrice }
                else -> grouped
            }

            if (grouped.isEmpty()) {
                tvShopEmpty.visibility = View.VISIBLE
                rcvShopItems.visibility = View.GONE
                panelBuyDetail.visibility = View.GONE
            } else {
                tvShopEmpty.visibility = View.GONE
                rcvShopItems.visibility = View.VISIBLE
                panelBuyDetail.visibility = View.GONE
                currentSelectedGroup = null

                rcvShopItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
                rcvShopItems.adapter = InventoryAdapter(grouped) { clickedGroup ->
                    currentSelectedGroup = clickedGroup
                    buyQuantity = 1
                    panelBuyDetail.visibility = View.VISIBLE
                    tvBuyName.text = clickedGroup.sampleItem.name
                    tvBuyStars.text = "★".repeat(clickedGroup.sampleItem.rarity.stars)
                    tvBuyStars.setTextColor(Color.parseColor(clickedGroup.sampleItem.rarity.colorHex))
                    tvUnitPrice.text = "Đơn giá: ${clickedGroup.sampleItem.customPrice} Tiền"

                    //  Kiểm tra người dùng hiện tại có phải người đăng bán không
                    val currentUser = UserManager.getUsername(this)
                    val isOwner = clickedGroup.sampleItem.listedBy == currentUser

                    btnConfirmBuy.visibility  = if (isOwner) View.GONE else View.VISIBLE
                    btnBackInventory.visibility = if (isOwner) View.VISIBLE else View.GONE

                    updateBuyUI()
                }
            }
        }

        // Nút mua
        btnConfirmBuy.setOnClickListener {
            val group = currentSelectedGroup ?: return@setOnClickListener
            if (!UserManager.isLoggedIn(this)) {
                Toast.makeText(this, "Vui lòng đăng nhập để mua!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val totalCost = group.sampleItem.customPrice * buyQuantity
            if (UserManager.getWishes(this) < totalCost) {
                Toast.makeText(this, "Không đủ tiền! Cần $totalCost Tiền.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = DatabaseHelper(this).writableDatabase
            val buyerId = UserManager.getCurrentUserId(this)
            val currentTime = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
            ).format(java.util.Date())

            for (item in group.rawItems.take(buyQuantity)) {
                //  Đánh dấu đồ của người bán là đã bán
                val v = ContentValues()
                v.put("isSold", 1)
                v.put("isListedOnShop", 0)
                db.update("History", v, "ID=?", arrayOf(item.historyId.toString()))

                //  Tạo bản ghi mới trong History cho người mua
                val cursor = db.rawQuery(
                    "SELECT VatPhamID FROM History WHERE ID=?",
                    arrayOf(item.historyId.toString())
                )
                if (cursor.moveToFirst()) {
                    val vatPhamId = cursor.getInt(0)
                    val newItem = ContentValues()
                    newItem.put("UserID", buyerId)
                    newItem.put("VatPhamID", vatPhamId)
                    newItem.put("ThoiGian", currentTime)
                    newItem.put("isSold", 0)
                    newItem.put("isListedOnShop", 0)
                    newItem.put("customPrice", 0)
                    newItem.put("listedBy", "")
                    db.insert("History", null, newItem)
                }
                cursor.close()
            }

            UserManager.removeWishes(this, totalCost)
            Toast.makeText(this, "Giao dịch thành công $buyQuantity ${group.sampleItem.name}!", Toast.LENGTH_SHORT).show()
            loadShopData()
        }
        // Nút rút về túi
        btnBackInventory.setOnClickListener {
            val group = currentSelectedGroup ?: return@setOnClickListener
            val currentUser = UserManager.getUsername(this)
            val isOwner = group.rawItems.take(buyQuantity).all { it.listedBy == currentUser }
            if (!isOwner) {
                Toast.makeText(this, "Bạn không phải người bán món đồ này!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val db = DatabaseHelper(this).writableDatabase
            for (item in group.rawItems.take(buyQuantity)) {
                val v = ContentValues()
                v.put("isListedOnShop", 0)
                db.update("History", v, "ID=?", arrayOf(item.historyId.toString()))
            }
            Toast.makeText(this, "Đã rút $buyQuantity ${group.sampleItem.name} về túi!", Toast.LENGTH_SHORT).show()
            loadShopData()
        }

        btnCloseShopPanel.setOnClickListener { panelBuyDetail.visibility = View.GONE; currentSelectedGroup = null }
        btnFilterAll.setOnClickListener { currentFilterStar = 0; loadShopData() }
        btnFilter5.setOnClickListener { currentFilterStar = 5; loadShopData() }
        btnFilter4.setOnClickListener { currentFilterStar = 4; loadShopData() }
        btnFilter3.setOnClickListener { currentFilterStar = 3; loadShopData() }
        btnSortStarAsc.setOnClickListener { currentSortMode = "STAR_ASC"; loadShopData() }
        btnSortStarDesc.setOnClickListener { currentSortMode = "STAR_DESC"; loadShopData() }
        btnSortPriceAsc.setOnClickListener { currentSortMode = "PRICE_ASC"; loadShopData() }
        btnSortPriceDesc.setOnClickListener { currentSortMode = "PRICE_DESC"; loadShopData() }
        btnBuyPlus.setOnClickListener {
            if (buyQuantity < (currentSelectedGroup?.totalCount ?: 1)) {
                buyQuantity++; updateBuyUI()
            }
        }
        btnBuyMinus.setOnClickListener { if (buyQuantity > 1) { buyQuantity--; updateBuyUI() } }
        imgbtnback.setOnClickListener { setupMainActivity() }

        loadShopData()
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

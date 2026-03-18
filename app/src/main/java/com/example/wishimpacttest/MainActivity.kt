package com.example.wishimpacttest

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
        UserManager.loadItems(this)
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
            ItemsManager.STT++

            // Lấy thời gian hiện tại chính xác lúc vật phẩm rơi ra
            val currentTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

            // Định dạng lại chuỗi Độ hiếm (Ví dụ: "5 ★")
            val rarityString = "${item.rarity.stars} ★"

            // Gom cả 4 dữ liệu vào Object và lưu vào kho
            val historyRecord = WishHistory(
                stt = ItemsManager.STT,  // Dữ liệu cột 1
                name = item.name,                  // Dữ liệu cột 2
                rarity = item.rarity,             // Dữ liệu cột 3
                time = currentTime                 // Dữ liệu cột 4
            )
            ItemsManager.historyList.add(historyRecord)

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
    // nơi quản lý toàn bộ vật phẩm trong game
    object ItemsManager {
        var STT = 0 // Biến đếm số thứ tự đếm lượt quay Gacha
        // Không bao giờ được dùng lệnh .remove() xóa đồ khỏi đây để tránh lỗi mất dữ liệu lịch sử.
        val historyList = mutableListOf<WishHistory>() // mọi món đồ người chơi từng sở hữu.

        val totalInventoryItems: Int // Đếm tổng số lượng đồ đang có thực sự trong Túi (Chưa bán hoặc Chưa đem lên Shop)
            get() = historyList.count { !it.isSold && !it.isListedOnShop }

        val InventoryList: List<ItemsGroup> //Tự động gom nhóm đồ vật để hiển thị lên màn hình Inventory
            get() = historyList
                .filter { !it.isSold && !it.isListedOnShop } // Chỉ lấy đồ đang rảnh rỗi trong túi
                .groupBy { it.name } // Gom những món cùng tên lại với nhau
                .map { (_, items) ->
                    // Đóng gói vào ItemsGroup để hiển thị số lượng
                    ItemsGroup(items.first(), items.size, items.toMutableList())
                }
        val ShopList: List<ItemsGroup>
            get() = historyList
                .filter { it.isListedOnShop && !it.isSold }
                .groupBy { "${it.name}_${it.customPrice}" }
                .map { (_, items) ->
                    ItemsGroup(items.first(), items.size, items.toMutableList())
                }
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
    private fun showHistory(){
        setContentView(R.layout.reward_history)

        tvHistory = findViewById(R.id.tvHistory)
        imgbtnback = findViewById(R.id.imgbtnBack)

        val allData = ItemsManager.historyList.reversed()

        val rcvHistory: RecyclerView = findViewById(R.id.rcvHistory)

        val historyAdapter = HistoryAdapter(emptyList())
        rcvHistory.layoutManager = LinearLayoutManager(this)
        rcvHistory.adapter = historyAdapter

        rcvHistory.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))

        val btnPrevPage: Button = findViewById(R.id.btnDecrease)
        val btnNextPage: Button = findViewById(R.id.btnIncrease)
        val tvCurrentPage: TextView = findViewById(R.id.tvNumber)

        var currentPage = 1
        val itemsPerPage = 6 // Mỗi trang hiện 6 dòng

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


    private fun showInventory() {
        setContentView(R.layout.inventory)
        imgbtnback = findViewById(R.id.imgbtnBack)

        val btnFilterAll: TextView = findViewById(R.id.btnFilterAll)
        val btnFilter5: TextView = findViewById(R.id.btnFilter5)
        val btnFilter4: TextView = findViewById(R.id.btnFilter4)
        val btnFilter3: TextView = findViewById(R.id.btnFilter3)
        val btnSortStarDesc: TextView = findViewById(R.id.btnSortStarDesc)
        val btnSortStarAsc: TextView = findViewById(R.id.btnSortStarAsc)
        val layoutFilterSort: LinearLayout = findViewById(R.id.layoutFilterSort)


        // Cột giữa (Lưới đồ)

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
        var currentSortMode = "STAR_DESC" // Mặc định xếp sao từ cao xuống thấp
        var currentSelectedGroup: ItemsGroup? = null
        var sellQuantity = 1



// Kiểm tra toàn bộ Túi đồ của bạn có trống không (star)
        if (ItemsManager.historyList.isEmpty()) {
            layoutFilterfollowstar.visibility = View.GONE
        } else {
            layoutFilterfollowstar.visibility = View.VISIBLE
        }

// Kiểm tra toàn bộ Túi đồ của bạn có trống không (sort)
        if (ItemsManager.historyList.isEmpty()) {
            layoutFilterSort.visibility = View.GONE
        } else {
            layoutFilterSort.visibility = View.VISIBLE
        }


        fun updateQuantityUI() {
            val group = currentSelectedGroup ?: return
            tvCurrentQuantity.text = sellQuantity.toString()

            val singlePrice = PriceManager.getPrice(group.sampleItem) ?: 0
            tvTotalEarn.text = "Thu về: +${singlePrice * sellQuantity} Tiền"

            // Khóa/mở nút cộng trừ
            btnMinus.isEnabled = sellQuantity > 1
            btnPlus.isEnabled = sellQuantity < group.totalCount
        }

        fun loadGroupedInventory() {
            // 1. LẤY DỮ LIỆU ĐÚNG 1 LẦN DUY NHẤT VÀ LƯU VÀO BIẾN TẠM (Tối ưu hiệu năng cực mạnh)
            val baseInventory = ItemsManager.InventoryList
            // 2. Chuyển sang biến có thể thay đổi để đem đi lọc
            var processData = baseInventory.toList()
            // 3. KIỂM TRA SỰ TỒN TẠI CỦA SAO (Lấy từ biến tạm baseInventory)
            val has5Star = baseInventory.any { it.sampleItem.rarity.stars == 5 }
            val has4Star = baseInventory.any { it.sampleItem.rarity.stars == 4 }
            val has3Star = baseInventory.any { it.sampleItem.rarity.stars == 3 }

            btnFilter5.visibility = if (has5Star) View.VISIBLE else View.GONE
            btnFilter4.visibility = if (has4Star) View.VISIBLE else View.GONE
            btnFilter3.visibility = if (has3Star) View.VISIBLE else View.GONE

            // Reset bộ lọc nếu món cuối cùng của loại sao đó bị bán mất
            if (currentFilterStar == 5 && !has5Star) currentFilterStar = 0
            if (currentFilterStar == 4 && !has4Star) currentFilterStar = 0
            if (currentFilterStar == 3 && !has3Star) currentFilterStar = 0

            // 4. LỌC VÀ SẮP XẾP
            if (currentFilterStar != 0) {
                processData = processData.filter { it.sampleItem.rarity.stars == currentFilterStar }
            }

            processData = when (currentSortMode) {
                "STAR_ASC" -> processData.sortedBy { it.sampleItem.rarity.stars }
                "STAR_DESC" -> processData.sortedByDescending { it.sampleItem.rarity.stars }
                else -> processData
            }

            // 5. THIẾT LẬP LAYOUT MANAGER
            if (rcvInventory.layoutManager == null) {
                rcvInventory.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
            }

            // 6. XỬ LÝ HIỂN THỊ (Empty State)
            if (processData.isEmpty()) {
                tvEmptyInventory.visibility = View.VISIBLE
                rcvInventory.visibility = View.GONE
                panelDetail.visibility = View.GONE
            } else {
                tvEmptyInventory.visibility = View.GONE
                rcvInventory.visibility = View.VISIBLE

                panelDetail.visibility = View.GONE
                currentSelectedGroup = null

                // 7. GÁN ADAPTER
                val adapter = InventoryAdapter(processData) { clickedGroup ->
                    currentSelectedGroup = clickedGroup
                    sellQuantity = 1
                    panelDetail.visibility = View.VISIBLE

                    tvSelectedName.text = clickedGroup.sampleItem.name
                    tvDetailStars.text = "★".repeat(clickedGroup.sampleItem.rarity.stars)
                    tvDetailStars.setTextColor(Color.parseColor(clickedGroup.sampleItem.rarity.colorHex))

                    // Hiện giá gốc items
                    // Truyền đúng số sao của món đồ vào để lấy giá niêm yết
                    val originalPrice = getBasePrice(currentSelectedGroup!!.sampleItem.rarity.stars)
                    // Hiển thị lên màn hình
                    tvDetailPrice.text = "Giá trị gốc: $originalPrice Tiền"



                    updateQuantityUI()
                }

                rcvInventory.adapter = adapter
            }
            tvTotalItems.text = "Tổng số vật phẩm: ${ItemsManager.totalInventoryItems}"
        }

        // Nút tắt bảng chi tiết (X)
        btnClosePanel.setOnClickListener {
            panelDetail.visibility = View.GONE
            currentSelectedGroup = null
        }

        // Cụm nút Lọc sao
        btnFilterAll.setOnClickListener { currentFilterStar = 0; loadGroupedInventory() }
        btnFilter5.setOnClickListener { currentFilterStar = 5; loadGroupedInventory() }
        btnFilter4.setOnClickListener { currentFilterStar = 4; loadGroupedInventory() }
        btnFilter3.setOnClickListener { currentFilterStar = 3; loadGroupedInventory() }

        // Cụm nút Sắp xếp
        btnSortStarAsc.setOnClickListener { currentSortMode = "STAR_ASC"; loadGroupedInventory() }
        btnSortStarDesc.setOnClickListener { currentSortMode = "STAR_DESC"; loadGroupedInventory() }

        // Nút tăng/giảm số lượng bán
        btnPlus.setOnClickListener {
            if (currentSelectedGroup != null && sellQuantity < currentSelectedGroup!!.totalCount) {
                sellQuantity++
                updateQuantityUI()
            }
        }

        btnMinus.setOnClickListener {
            if (sellQuantity > 1) {
                sellQuantity--
                updateQuantityUI()
            }
        }






// NÚT BÁN LẤY TIỀN LUÔN (Giá cố định của hệ thống)
        btnSell.setOnClickListener {
            val selectedGroup = currentSelectedGroup ?: return@setOnClickListener


            // 1. Tính tiền
            val pricePerItem = PriceManager.getPrice(selectedGroup.sampleItem)
            val totalEarned = pricePerItem * sellQuantity

            // 2. Lấy đúng số lượng đồ mang đi bán
            val itemsToSell = selectedGroup.rawItems.take(sellQuantity)

            // 3. LOGIC CHUYỂN ĐỔI: Chỉ cần bật cờ "isSold"
            itemsToSell.forEach { item ->
                item.isSold = true // Đồ lập tức tàng hình khỏi cả Túi và Shop
            }
            // 4. Cộng tiền cho User
            UserManager.addWishes(this, totalEarned)

            // 5. LƯU VÀO KÉT SẮT VÀ CẬP NHẬT UI
            UserManager.saveItems(this)
            loadGroupedInventory() // Hàm vẽ lại lưới Túi đồ của sếp

            Toast.makeText(this, "Đã bán $sellQuantity món, nhận $totalEarned Tiền!", Toast.LENGTH_SHORT).show()
            panelDetail.visibility = View.GONE
        }

// NÚT ĐƯA LÊN CHỢ (Giá do người chơi nhập)
        btnPushToShop.setOnClickListener {
            val selectedGroup = currentSelectedGroup ?: return@setOnClickListener

            // 1. Kiểm tra giá người chơi nhập
            val priceStr = edtCustomPrice.text.toString()
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập giá muốn bán!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userCustomPrice = priceStr.toInt()

            // 2. Lấy đúng số lượng đồ mang lên Shop
            val itemsToPush = selectedGroup.rawItems.take(sellQuantity)

            // 3. LOGIC CHUYỂN ĐỔI: Tắt cờ Túi, Bật cờ Shop, Dán giá
            itemsToPush.forEach { item ->
                item.isListedOnShop = true // Lập tức tàng hình khỏi Túi và hiện ra ở Shop
                item.customPrice = userCustomPrice
            }

            // 4. LƯU VÀO KÉT SẮT VÀ CẬP NHẬT UI
            UserManager.saveItems(this)
            loadGroupedInventory() // Hàm vẽ lại lưới Túi đồ của sếp

            Toast.makeText(this, "Đã đưa $sellQuantity món lên Shop với giá $userCustomPrice!", Toast.LENGTH_SHORT).show()
            edtCustomPrice.text.clear()
            panelDetail.visibility = View.GONE
        }
        imgbtnback .setOnClickListener {
            setupMainActivity()
        }

        loadGroupedInventory()
    }
    //  Bảng giá và Ví tiền trung tâm

    private fun showShop() {
        setContentView(R.layout.shop)
        imgbtnback = findViewById(R.id.imgbtnBack)

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

        //  BIẾN TRẠNG THÁI
        var currentFilterStar = 0
        var currentSortMode = "STAR_DESC"
        var currentSelectedGroup: ItemsGroup? = null
        var buyQuantity = 1

        //Hiển thị Tiền và tên Customer ở góc phải
        if(UserManager.isLoggedIn(this)==false) {
            findViewById<TextView>(R.id.tvTotalWishes).text = 0.toString()
        } else {
            findViewById<TextView>(R.id.tvTotalWishes).text = UserManager.getWishes(this).toString()
        }
        //  HÀM CẬP NHẬT giao diện
        fun updateBuyUI() {
            val group = currentSelectedGroup ?: return
            tvBuyQuantity.text = buyQuantity.toString()
            val unitPrice = group.sampleItem.customPrice
            tvTotalPrice.text = "Tổng thanh toán: ${unitPrice * buyQuantity} Tiền"

            btnBuyMinus.isEnabled = buyQuantity > 1
            btnBuyPlus.isEnabled = buyQuantity < group.totalCount
        }

        // 4. HÀM TẢI LƯỚI SHOP
        fun loadShopData() {
                // KIỂM TRA TRÊN KỆ SHOP CÓ SAO NÀO
            // KIỂM TRA TRÊN KỆ SHOP CÓ SAO NÀO (Lấy từ hộp đồ đã gom nhóm)
            val has5Star = ItemsManager.ShopList.any { it.sampleItem.rarity.stars == 5 }
            val has4Star = ItemsManager.ShopList.any { it.sampleItem.rarity.stars == 4 }
            val has3Star = ItemsManager.ShopList.any { it.sampleItem.rarity.stars == 3 }

                //  ẨN / HIỆN NÚT LỌC SHOP
                btnFilter5.visibility = if (has5Star) View.VISIBLE else View.GONE
                btnFilter4.visibility = if (has4Star) View.VISIBLE else View.GONE
                btnFilter3.visibility = if (has3Star) View.VISIBLE else View.GONE

                //  RESET LỌC SHOP NẾU KẾT QUẢ RỖNG
                if (currentFilterStar == 5 && !has5Star) currentFilterStar = 0
                if (currentFilterStar == 4 && !has4Star) currentFilterStar = 0
                if (currentFilterStar == 3 && !has3Star) currentFilterStar = 0

            var processData = ItemsManager.ShopList.toList() // Lấy từ kệ hàng

            if (currentFilterStar != 0) { processData = processData.filter { it.sampleItem.rarity.stars == currentFilterStar } }

            // VÌ processData ĐÃ LÀ CÁC HỘP (InventoryGroup) ĐƯỢC GOM SẴN TỪ KHO,
            // NÊN TA BỎ QUA BƯỚC GOM NHÓM VÀ ĐI THẲNG VÀO BƯỚC SẮP XẾP!

            val groupedList = when (currentSortMode) {
                "STAR_ASC" -> processData.sortedBy { it.sampleItem.rarity.stars }
                "STAR_DESC" -> processData.sortedByDescending { it.sampleItem.rarity.stars }
                "PRICE_ASC" -> processData.sortedBy { it.sampleItem.customPrice }
                "PRICE_DESC" -> processData.sortedByDescending { it.sampleItem.customPrice }
                else -> processData
            }



            // Kiểm tra rỗng và Ép ẩn bảng mua
            if (groupedList.isEmpty()) {
                tvShopEmpty.visibility = View.VISIBLE
                rcvShopItems.visibility = View.GONE
                panelBuyDetail.visibility = View.GONE
            } else {
                tvShopEmpty.visibility = View.GONE
                rcvShopItems.visibility = View.VISIBLE

                // Xóa chọn cũ và ẨN bảng
                panelBuyDetail.visibility = View.GONE
                currentSelectedGroup = null

                val adapter = InventoryAdapter(groupedList) { clickedGroup ->
                    currentSelectedGroup = clickedGroup
                    buyQuantity = 1


                    // HIỆN BẢNG KHI CLICK
                    panelBuyDetail.visibility = View.VISIBLE

                    tvBuyName.text = clickedGroup.sampleItem.name
                    tvBuyStars.text = "★".repeat(clickedGroup.sampleItem.rarity.stars)
                    tvBuyStars.setTextColor(Color.parseColor(clickedGroup.sampleItem.rarity.colorHex))
                    tvUnitPrice.text = "Đơn giá: ${PriceManager.getPrice(clickedGroup.sampleItem) } tiền"

                    tvUnitPrice.text = "Đơn giá: ${clickedGroup.sampleItem.customPrice} tiền"
                    updateBuyUI()


                }

                rcvShopItems.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
                rcvShopItems.adapter = adapter
            }
        }

        // BẮT SỰ KIỆN NÚT
        btnCloseShopPanel.setOnClickListener {
            panelBuyDetail.visibility = View.GONE
            currentSelectedGroup = null
        }

        btnFilterAll.setOnClickListener { currentFilterStar = 0; loadShopData() }
        btnFilter5.setOnClickListener { currentFilterStar = 5; loadShopData() }
        btnFilter4.setOnClickListener { currentFilterStar = 4; loadShopData() }
        btnFilter3.setOnClickListener { currentFilterStar = 3; loadShopData() }

        btnSortStarAsc.setOnClickListener { currentSortMode = "STAR_ASC"; loadShopData() }
        btnSortStarDesc.setOnClickListener { currentSortMode = "STAR_DESC"; loadShopData() }
        btnSortPriceAsc.setOnClickListener { currentSortMode = "PRICE_ASC"; loadShopData() }
        btnSortPriceDesc.setOnClickListener { currentSortMode = "PRICE_DESC"; loadShopData() }

        btnBuyPlus.setOnClickListener {
            if (currentSelectedGroup != null && buyQuantity < currentSelectedGroup!!.totalCount) {
                buyQuantity++
                updateBuyUI()
            }
        }

        btnBuyMinus.setOnClickListener {
            if (buyQuantity > 1) {
                buyQuantity--
                updateBuyUI()
            }
        }

        btnConfirmBuy.setOnClickListener {

            val group = currentSelectedGroup ?: return@setOnClickListener

            val totalCost = group.sampleItem.customPrice * buyQuantity

            val itemsToBuy = group.rawItems.take(buyQuantity)
            val currentMoney = UserManager.getWishes(this)


            if (currentMoney < totalCost) {
                Toast.makeText(this, "Không đủ tiền! Bạn cần $totalCost tiền.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            itemsToBuy.forEach { item ->
                item.isSold = true           // Đồ đã bị bán
                item.isListedOnShop = false  // Gỡ xuống khỏi kệ
            }
            UserManager.removeWishes(this, totalCost)

            UserManager.saveItems(this)

            // Báo tin
            Toast.makeText(this, "Giao dịch thành công $buyQuantity ${group.sampleItem.name}!", Toast.LENGTH_SHORT).show()

            // Load lại màn hình Shop hệ thống sẽ tự quét lại Kho, thấy món nào isListedOnShop = false là tự cho tàng hình khỏi kệ
            loadShopData()
        }
        btnBackInventory.setOnClickListener {
            // 1. Lấy cái hộp đồ mà người chơi đang bấm chọn trên màn hình
            val group = currentSelectedGroup ?: return@setOnClickListener

            // 2. Lấy đúng số lượng đồ muốn rút về (Dùng chung biến buyQuantity cho tiện)
            val itemsToWithdraw = group.rawItems.take(buyQuantity)

            // 3. LOGIC LÕI: Tắt cờ Shop.
            // (Vì isSold vẫn là false, nên tắt cờ Shop xong kính lúp Túi Đồ sẽ tự động nhìn thấy nó lại)
            itemsToWithdraw.forEach { item ->
                item.isListedOnShop = false
            }

            // 4. LƯU LẠI VÀO KÉT SẮT (Bắt buộc để chống mất dữ liệu)
            UserManager.saveItems(this) // Nếu sếp để hàm save trong UserManager thì gọi UserManager.saveHistory(this)

            // 5. Báo tin vui
            Toast.makeText(this, "Đã rút $buyQuantity ${group.sampleItem.name} về túi!", Toast.LENGTH_SHORT).show()

            // 6. Tải lại kệ Shop (Hàm loadShopData của sếp đã có sẵn logic ép ẩn bảng và vẽ lại lưới rồi)
            loadShopData()
        }

        // Chạy lần đầu
        loadShopData()

        imgbtnback .setOnClickListener {
            setupMainActivity()
        }

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


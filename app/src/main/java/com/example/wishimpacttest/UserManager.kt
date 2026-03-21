package com.example.wishimpacttest

import android.R
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject



// Object này giúp chúng ta lưu dữ liệu người dùng vào máy (SharedPreferences)
// để khi tắt app mở lại vẫn còn tài khoản.
object UserManager {
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_TOTAL_WISHES = "totalWishes"  //Lưu tổng số lần quay theo tài khoản
    private const val KEY_HISTORY = "history"
    private fun getHistoryKey(accountId: String) = "history_$accountId"  //Lưu lịch sử quay theo tài khoản

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Hàm lưu thông tin đăng ký
    fun register(context: Context, name: String, user: String, pass: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_DISPLAY_NAME, name) //Tên hiển thị
        editor.putString(KEY_USERNAME, user) //Tên đăng nhập
        editor.putString(KEY_PASSWORD, pass) //Mật khẩu
        // Khi đăng ký thành công, tự động đánh dấu là đã đăng nhập luôn
        editor.putBoolean(KEY_IS_LOGGED_IN, true) //Tên hiển thị khi đăng nhập

        // Lưu lại tổng số lần quay
        editor.putInt(KEY_TOTAL_WISHES, 100) //Tặng free 100 Roll khi tạo acc
        editor.apply()

    }

    // Hàm kiểm tra đăng nhập
    fun login(context: Context, user: String, pass: String): Boolean {
        val prefs = getPrefs(context)
        val savedUser = prefs.getString(KEY_USERNAME, "")
        val savedPass = prefs.getString(KEY_PASSWORD, "")
        
        if (user == savedUser && pass == savedPass && user.isNotEmpty()) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply() //khi nhập dữ liệu vô thì sẽ kiểm tra xem các điều kiện xem nếu thỏa mãn thì sẽ cho phép đăng nhập vô
            return true
        }
        return false
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false) //khi nhập dữ liệu vô thì sẽ kiểm tra xem các điều kiện xem nếu không thỏa mãn thì sẽ không cho phép đăng nhập vô
    }

    //Chỉ hiện tên nếu đã đăng nhập, nếu chưa thì hiện "Customer"
    fun getDisplayName(context: Context): String {
        if (!isLoggedIn(context)) return "Customer"
        return getPrefs(context).getString(KEY_DISPLAY_NAME, "Customer") ?: "Customer"
    }

    fun updateProfile(context: Context, newName: String, newPass: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_DISPLAY_NAME, newName)
        if (newPass.isNotEmpty()) {
            editor.putString(KEY_PASSWORD, newPass)
        }
        editor.apply()
    }
    
    fun logout(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    //Các hàm về xử lý tiền tệ
    //Hàm kiểm tra tiền tệ còn lại trong tài khoản
    fun getWishes(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_WISHES,0)
    }
    //Hàm tăng tiền tệ(khi nạp,làm nv,...)
    fun addWishes(context: Context, amount: Int) {
        val currentWishes = getWishes(context)
        getPrefs(context).edit().putInt(KEY_TOTAL_WISHES, currentWishes + amount).apply()
    }
    //Hàm giảm tiền tệ(khi quay)
    fun removeWishes(context: Context, amount: Int): Boolean {
        val currentWishes = getWishes(context)
        if(currentWishes >= amount) {   //trừ tiền khi quay nếu đủ
            getPrefs(context).edit().putInt(KEY_TOTAL_WISHES, currentWishes - amount).apply()
            return true
        }
        return false    //Không đủ tiền
    }

    fun getUsername(context: Context): String {
        return getPrefs(context).getString(KEY_USERNAME, "") ?: ""
    }

    fun clearItems(context: Context, accountId: String) {
        getPrefs(context).edit()
            .remove(getHistoryKey(accountId)) // xóa "history_abc" hoặc "history_xyz"
            .apply()
        MainActivity.ItemsManager.historyList.clear()
    }

    //bảo vệ tài sản của người chơi không bị bốc hơi sau khi họ thoát ứng dụng hoặc tắt điện thoại
    fun saveItems(context: Context, accountId: String) {
        // chỉnh tiền
        val editor = getPrefs(context).edit()

        // Tạo một nơi để chứa tất cả các món đồ
        val jsonArray = JSONArray()

        // scan từng món đồ đang có trong historyList
        for (item in MainActivity.ItemsManager.historyList) {

            // với mỗi món đồ tạo một JSONObject để gói thông tin
            val jsonObject = JSONObject()

            // bỏ từng thông tin của món đồ vào JSONObject
            jsonObject.put("name", item.name)
            jsonObject.put("star", item.rarity.stars)
            jsonObject.put("time", item.time)
            jsonObject.put("customPrice", item.customPrice) // Giá tự định ở shop
            jsonObject.put("listedBy", item.listedBy) // ← nhớ ai là người push


            // lưu 2 trạng thái (quyết định vị trí của món đồ)
            jsonObject.put("isListedOnShop", item.isListedOnShop)
            jsonObject.put("isSold", item.isSold)
            jsonObject.put("listedBy", item.listedBy) // nhớ ai là người push


            jsonArray.put(jsonObject)
        }

        // Ép toàn bộ cái thông tin của món đồ thành 1 Dòng Chữ duy nhất
        val historyString = jsonArray.toString()

        editor.putString(getHistoryKey(accountId), historyString) // ✅ đổi key

        // Khóa lại và lưu thay đổi!
        editor.apply()
    }

    fun loadItems(context: Context,  accountId: String) {
        val prefs = getPrefs(context)

        // dọn sạch kho hiện tại tránh tình trạng người chơi ấn Load đồ trong túi lại bị nhân đôi lên.
        MainActivity.ItemsManager.historyList.clear()

        // đọc dữ liệu từ kho nếu rỗng thì tự động trả về mảng rỗng
        val historyString = prefs.getString(getHistoryKey(accountId), "[]")

        try {
            // biến cái chuỗi chữ dài ngoằng đó thành mảng
            val jsonArray = JSONArray(historyString)

            // Khui từng cái trong mảng ra
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                // đọc các thông số cơ bản
                val itemName = jsonObject.getString("name")        // Tên món đồ
                val itemStar = jsonObject.getInt("star")           // Số sao
                val itemTime = jsonObject.getString("time")        // Thời gian nhận
                val itemPrice = jsonObject.getInt("customPrice")   // Giá người chơi đặt

                val isListed = jsonObject.optBoolean("isListedOnShop", false)
                val isSold = jsonObject.optBoolean("isSold", false)
                val listedBy = jsonObject.optString("listedBy", "")



                val restoredItem = WishHistory(
                    stt = i + 1, // đánh lại stt
                    name = itemName,
                    rarity = Rarity.entries.first { it.stars == itemStar },
                    time = itemTime,
                    customPrice = itemPrice,
                    isListedOnShop = isListed,
                    isSold = isSold,
                    listedBy = listedBy

                )

                // Xếp món đồ vào lại Kho
                MainActivity.ItemsManager.historyList.add(restoredItem)
            }
        } catch (e: Exception) {
            // Nếu file save bị hỏng
            // App sẽ không bị văng mà chỉ in lỗi ra màn hình Log.
            e.printStackTrace()
        }
    }
}

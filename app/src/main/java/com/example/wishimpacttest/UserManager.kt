package com.example.wishimpacttest

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
    private const val KEY_HISTORY = "history"   //Lưu lịch sử quay theo tài khoản

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

    // HÀM LƯU DỮ LIỆU VÀO ĐIỆN THOẠI
    fun saveHistory(context: Context) {
        // 1. Mở "két sắt"
        val editor = getPrefs(context).edit()
        val jsonArray = JSONArray()

        // 2. CHẠY SANG HISTORY MANAGER ĐỂ LẤY DANH SÁCH ĐỒ
        for (item in MainActivity.ItemsManager.historyList) {
            val jsonObject = JSONObject()

            // Đóng gói từng thuộc tính của món đồ
            jsonObject.put("name", item.name)
            jsonObject.put("star", item.rarity.stars)
            jsonObject.put("time", item.time)
            jsonObject.put("customPrice", item.customPrice)

            // 👇 DÒNG SINH TỬ SẾP QUÊN: LƯU TRẠNG THÁI VÀO JSON 👇
            jsonObject.put("isListedOnShop", item.isListedOnShop)
            jsonObject.put("isSold", item.isSold)

            // Bỏ vào mảng
            jsonArray.put(jsonObject)
        }

        // 3. Lưu mảng danh sách đồ thành chuỗi JSON
        val historyString = jsonArray.toString()
        editor.putString(KEY_HISTORY, historyString)

        // 4. Chốt lệnh lưu
        editor.apply()
    }

    fun openHistory(context: Context) {
        val prefs = getPrefs(context)

        // 1. Dọn sạch kho cũ
        MainActivity.ItemsManager.historyList.clear()

        // 2. Lấy chuỗi JSON từ "két sắt" ra
        val historyString = prefs.getString(KEY_HISTORY, "[]")

        try {
            val jsonArray = JSONArray(historyString)

            // Vòng lặp bóc tách từng gói đồ
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val itemName = jsonObject.getString("name")
                val itemStar = jsonObject.getInt("star")
                val itemTime = jsonObject.getString("time")
                val itemPrice = jsonObject.getInt("customPrice")

                // 👇 DÒNG SINH TỬ SẾP QUÊN: ĐỌC TRẠNG THÁI TỪ JSON RA 👇
                // (Dùng optBoolean để nếu file cũ không có thì mặc định là false, không bị crash app)
                val isListed = jsonObject.optBoolean("isListedOnShop", false)
                val isSold = jsonObject.optBoolean("isSold", false)

                // 3. Lắp ráp lại thành món đồ hoàn chỉnh
                val restoredItem = WishHistory(
                    stt = i + 1,
                    name = itemName,
                    rarity = Rarity.entries.first { it.stars == itemStar },
                    time = itemTime,
                    customPrice = itemPrice,
                    // 👇 GẮN LẠI TRẠNG THÁI CHO MÓN ĐỒ 👇
                    isListedOnShop = isListed,
                    isSold = isSold
                )

                // 4. Nhét đồ ngược trở lại KHO
                MainActivity.ItemsManager.historyList.add(restoredItem)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

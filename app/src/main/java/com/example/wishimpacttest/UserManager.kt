package com.example.wishimpacttest

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

// Object này giúp chúng ta lưu dữ liệu người dùng vào máy (SharedPreferences)
// để khi tắt app mở lại vẫn còn tài khoản.
object UserManager {
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_USER_ID = "currentUserId" // Chỉ dùng SharedPreferences để lưu ID người đang dùng máy

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Hàm lưu thông tin đăng ký
    fun register(context: Context, name: String, user: String, pass: String) {
        val db = DatabaseHelper(context)
        // Gọi hàm thêm người dùng vào SQLite
        val newId = db.insertUser(name, user, pass)
        
        // Sau khi thêm thành công, lưu ID này vào máy để tự động đăng nhập
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true) //Tên hiển thị khi đăng nhập
        editor.putInt(KEY_USER_ID, newId.toInt())
        editor.apply()
    }

    // Hàm kiểm tra đăng nhập
    fun login(context: Context, user: String, pass: String): Boolean {
        val db = DatabaseHelper(context)
        val userId = db.checkLogin(user, pass) // Gọi SQLite để kiểm tra coi đã hợp lệ chưa
        
        if (userId != -1) {
            val editor = getPrefs(context).edit()
            editor.putBoolean(KEY_IS_LOGGED_IN, true).apply() //khi nhập dữ liệu vô thì sẽ kiểm tra xem các điều kiện xem nếu thỏa mãn thì sẽ cho phép đăng nhập vô
            editor.putInt(KEY_USER_ID, userId)
            editor.apply()
            return true
        }
        return false
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false) //khi nhập dữ liệu vô thì sẽ kiểm tra xem các điều kiện xem nếu không thỏa mãn thì sẽ không cho phép đăng nhập vô
    }

    // Lấy ID người dùng hiện tại để kiểm tra xem đã đăng nhập chưa
    fun getCurrentUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    // Chỉ hiện tên nếu đã đăng nhập, nếu chưa thì hiện "Customer"
    fun getDisplayName(context: Context): String {
        if (!isLoggedIn(context)) return "Customer"
        
        // Gọi SQLite để lấy tên theo ID
        val userId = getCurrentUserId(context)
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery("SELECT TenHienThi FROM User WHERE ID=?", arrayOf(userId.toString()))
        var name = "Customer"
        if (cursor.moveToFirst()) name = cursor.getString(0)
        cursor.close()
        return name
    }

    // Hàm cập nhật thông tin cá nhân (chủ yếu đổi mật khẩu)
    fun updateProfile(context: Context, newName: String, newPass: String) {
        val userId = getCurrentUserId(context)
        val db = DatabaseHelper(context).writableDatabase
        val v = android.content.ContentValues()
        v.put("TenHienThi", newName)
        if (newPass.isNotEmpty()) v.put("Password", newPass)
        
        db.update("User", v, "ID=?", arrayOf(userId.toString()))
    }
    
    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply() // Xóa hết ID và trạng thái đăng nhập
    }

    // Các hàm về xử lý tiền tệ
    // Hàm kiểm tra tiền tệ còn lại trong tài khoản
    fun getWishes(context: Context): Int {
        if (!isLoggedIn(context)) return 0
        val userId = getCurrentUserId(context)
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery("SELECT SoXu FROM User WHERE ID=?", arrayOf(userId.toString()))
        var xu = 0
        if (cursor.moveToFirst()) xu = cursor.getInt(0)
        cursor.close()
        return xu
    }

    // Hàm tăng tiền tệ (khi nạp, làm nv,...)
    fun addWishes(context: Context, amount: Int) {
        val current = getWishes(context)
        val db = DatabaseHelper(context)
        db.updateSoXu(getCurrentUserId(context), current + amount)
    }

    // Hàm giảm tiền tệ (khi quay)
    fun removeWishes(context: Context, amount: Int): Boolean {
        val current = getWishes(context)
        if(current >= amount) {   // trừ tiền khi quay nếu đủ
            val db = DatabaseHelper(context)
            db.updateSoXu(getCurrentUserId(context), current - amount)
            return true
        }
        return false    // Không đủ tiền
    }

    // bảo vệ tài sản của người chơi không bị bốc hơi sau khi họ thoát ứng dụng hoặc tắt điện thoại
    fun saveItems(context: Context) {
        // SQLite tự động lưu mỗi khi gọi người dùng thêm hay cập nhật tài khoản
    }

    fun loadItems(context: Context) {
        // Dữ liệu sẽ được load trực tiếp từ bảng History khi cần thiết
    }

    // Hàm lấy mật khẩu thật hiện tại
    fun getPassword(context: Context): String {
        val userId = getCurrentUserId(context)
        val db = DatabaseHelper(context).readableDatabase
        val cursor = db.rawQuery("SELECT Password FROM User WHERE ID=?", arrayOf(userId.toString()))
        var pass = ""
        if (cursor.moveToFirst()) pass = cursor.getString(0)
        cursor.close()
        return pass
    }
}

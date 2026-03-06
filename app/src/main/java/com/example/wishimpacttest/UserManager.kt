package com.example.wishimpacttest

import android.content.Context
import android.content.SharedPreferences

// Object này giúp chúng ta lưu dữ liệu người dùng vào máy (SharedPreferences)
// để khi tắt app mở lại vẫn còn tài khoản.
object UserManager {
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"

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
}
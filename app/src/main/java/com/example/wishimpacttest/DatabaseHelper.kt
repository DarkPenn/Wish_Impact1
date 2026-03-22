package com.example.wishimpacttest

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// DatabaseHelper giúp quản lý việc tạo file .db và các bảng dữ liệu
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "WishImpact.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tạo bảng User (Người dùng)
        val createUserTable = """
            CREATE TABLE User (
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                TenHienThi TEXT,
                TenDangNhap TEXT UNIQUE,
                Password TEXT,
                SoXu INTEGER
            )
        """.trimIndent()

        // 2. Tạo bảng VatPham (Danh mục gốc)
        val createVatPhamTable = """
            CREATE TABLE VatPham (
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                TenVatPham TEXT,
                SoSao INTEGER,
                HinhAnh TEXT
            )
        """.trimIndent()

        // 3. Tạo bảng History (Túi đồ cá nhân)
        val createHistoryTable = """
            CREATE TABLE History (
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                UserID INTEGER,
                VatPhamID INTEGER,
                ThoiGian TEXT,
                isSold INTEGER DEFAULT 0,
                isListedOnShop INTEGER DEFAULT 0,
                customPrice INTEGER DEFAULT 0,
                 listedBy TEXT DEFAULT '',
                FOREIGN KEY(UserID) REFERENCES User(ID),
                FOREIGN KEY(VatPhamID) REFERENCES VatPham(ID)
            )
        """.trimIndent()

        // 4. Tạo bảng GiaBanVP (Chợ đồ cũ/Shop)
        val createShopTable = """
            CREATE TABLE GiaBanVP (
                ID INTEGER PRIMARY KEY AUTOINCREMENT,
                UserID INTEGER,
                VatPhamID INTEGER,
                GiaOffer INTEGER,
                SoLuong INTEGER,
                FOREIGN KEY(UserID) REFERENCES User(ID),
                FOREIGN KEY(VatPhamID) REFERENCES VatPham(ID)
            )
        """.trimIndent()

        // Thực thi lệnh tạo bảng
        db.execSQL(createUserTable)
        db.execSQL(createVatPhamTable)
        db.execSQL(createHistoryTable)
        db.execSQL(createShopTable)

        // Tự động nạp danh sách vật phẩm vào bảng VatPham khi app chạy lần đầu
        insertDefaultVatPham(db)
    }

    private fun insertDefaultVatPham(db: SQLiteDatabase) {
        val items = listOf(
            Pair("Diluc", 5), Pair("Jean", 5), Pair("Keqing", 5), Pair("Mona", 5), Pair("Qiqi", 5),
            Pair("Amber", 4), Pair("Kaeya", 4), Pair("Lisa", 4), Pair("Barbara", 4), Pair("Bennett", 4),
            Pair("Kiếm Cùi", 3), Pair("Sách Cũ", 3), Pair("Gậy Gỗ", 3), Pair("Cung Tập Sự", 3)
        )
        for (item in items) {
            val v = ContentValues()
            v.put("TenVatPham", item.first)
            v.put("SoSao", item.second)
            db.insert("VatPham", null, v)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS User")
        db.execSQL("DROP TABLE IF EXISTS VatPham")
        db.execSQL("DROP TABLE IF EXISTS History")
        db.execSQL("DROP TABLE IF EXISTS GiaBanVP")
        onCreate(db)
    }

    //          !!!Cách Thêm (Insert)!!!
    // Hàm này dùng khi Đăng ký thành viên
    fun insertUser(tenHien: String, tenDN: String, pass: String): Long {
        val db = writableDatabase
        val v = ContentValues().apply {
            put("TenHienThi", tenHien)  //Bỏ tên vào cột tên hiển thị
            put("TenDangNhap", tenDN)   //Bỏ tên đăng nhập vào cột tên đăng nhập
            put("Password", pass)       //Bỏ password vào cột password
            put("SoXu", 100) // Tặng 100 xu khi mới chơi, kiểu dữ liệu mặc định khi mới vô cho người chơi 100 xu
        }
        return db.insert("User", null, v) // Đẩy dữ liệu vào bảng User
    }

    //          !!!Cách Xem dữ liệu (Query)!!!
    // Kiểm tra đăng nhập
    fun checkLogin(user: String, pass: String): Int {
        val db = readableDatabase       //Đọc dữ liệu từ database
        val cursor = db.rawQuery("SELECT ID FROM User WHERE TenDangNhap=? AND Password=?", arrayOf(user, pass))
        var id = -1
        if (cursor.moveToFirst()) id = cursor.getInt(0)   //Nếu có dữ liệu thì lấy ID ra
        cursor.close()
        return id // Trả về ID người dùng nếu đúng, ngược lại trả về -1
    }

    //          !!!Cách Sửa (Update)!!!
    // Cập nhật số xu (Khi quay hoặc bán đồ)
    fun updateSoXu(userId: Int, soXuMoi: Int) {
        val db = writableDatabase
        val v = ContentValues()
        v.put("SoXu", soXuMoi)
        db.update("User", v, "ID=?", arrayOf(userId.toString()))
    }

    //          !!!Cách Xóa (Delete)!!!
    // Xóa một món đồ khỏi shop
    fun deleteFromShop(shopId: Int) {
        val db = writableDatabase
        db.delete("GiaBanVP", "ID=?", arrayOf(shopId.toString()))
    }
}
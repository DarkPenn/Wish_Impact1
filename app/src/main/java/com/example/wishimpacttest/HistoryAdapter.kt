package com.example.wishimpacttest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        holder.tvRarity.text = "${item.rarity.stars} ★"
        // tô màu cho dòng chữ hiển thị số sao đó.
        holder.tvRarity.setTextColor(android.graphics.Color.parseColor(item.rarity.colorHex))
    }

    //Báo cáo cho hệ thống biết mảng dữ liệu này có tổng cộng bao nhiêu món
    override fun getItemCount() = list.size

    // Hàm này dùng để cập nhật lại nội dung của bảng
    fun updateData(newList: List<WishHistory>) {
        list = newList
        notifyDataSetChanged()
    }
}
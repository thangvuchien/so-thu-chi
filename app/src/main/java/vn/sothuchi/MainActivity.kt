package vn.sothuchi

import android.app.*
import android.os.Bundle
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val GREEN = 0xFF18864B.toInt()
private const val RED = 0xFFD94A45.toInt()
private const val BLUE = 0xFF276EF1.toInt()
private const val BG = 0xFFF5F7FA.toInt()
private const val DARK = 0xFF202632.toInt()
private const val MUTED = 0xFF727C8A.toInt()
private val vn = Locale("vi", "VN")
private val moneyFmt = NumberFormat.getNumberInstance(vn)
private fun money(v: Long) = moneyFmt.format(v) + " đ"
private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun dateHuman(d: String): String = try { SimpleDateFormat("dd/MM/yyyy", vn).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d)!!) } catch (_: Exception) { d }

data class Tx(val id: Long, val type: String, val amount: Long, val date: String, val category: String, val note: String)

class DB(ctx: android.content.Context) : SQLiteOpenHelper(ctx, "sothuchi.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tx(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, amount INTEGER NOT NULL, date TEXT NOT NULL, category TEXT NOT NULL, note TEXT)")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO settings(key,value) VALUES('goal','10000000')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(type:String, amount:Long, date:String, category:String, note:String) {
        val cv=ContentValues().apply { put("type",type); put("amount",amount); put("date",date); put("category",category); put("note",note) }
        writableDatabase.insert("tx",null,cv)
    }
    fun remove(id:Long) { writableDatabase.delete("tx","id=?", arrayOf(id.toString())) }
    fun all(where:String="", args:Array<String> = emptyArray()):List<Tx> {
        val result= mutableListOf<Tx>()
        val c=readableDatabase.query("tx",null,where.ifBlank { null },args.ifEmpty { null },null,null,"date DESC,id DESC")
        c.use { while(it.moveToNext()) result.add(Tx(it.getLong(0),it.getString(1),it.getLong(2),it.getString(3),it.getString(4),it.getString(5) ?: "")) }
        return result
    }
    fun setting(k:String, default:String=""):String {
        val c=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?", arrayOf(k))
        c.use { if(it.moveToFirst()) return it.getString(0) }; return default
    }
    fun set(k:String,v:String) {
        val cv=ContentValues().apply { put("key",k); put("value",v) }
        writableDatabase.insertWithOnConflict("settings",null,cv,SQLiteDatabase.CONFLICT_REPLACE)
    }
}

class MainActivity : Activity() {
    private lateinit var db: DB
    private lateinit var root: LinearLayout
    private var page = "home"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db=DB(this)
        show()
    }
    private fun show() {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,18,20,12); setBackgroundColor(Color.WHITE) }
        header.addView(text("SỔ THU CHI",18,DARK,true))
        header.addView(text(dateHuman(today()),14,MUTED,false))
        root.addView(header)
        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,14,16,24) }
        scroll.addView(body); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        when(page) {
            "home" -> home(body)
            "history" -> history(body)
            "stats" -> stats(body)
            "goal" -> goal(body)
        }
        val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(4,8,4,8) }
        listOf("home" to "Hôm nay","history" to "Lịch sử","stats" to "Thống kê","goal" to "Mục tiêu").forEach { (key,label) ->
            val b=Button(this).apply { text=label; textSize=11f; setTextColor(if(page==key) BLUE else MUTED); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { page=key; show() } }
            nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(nav)
        setContentView(root)
    }
    private fun text(s:String,size:Int,color:Int,bold:Boolean=false):TextView = TextView(this).apply {
        text=s; textSize=size.toFloat(); setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD); setPadding(0,4,0,4)
    }
    private fun card():LinearLayout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,14,16,14); setBackgroundColor(Color.WHITE) }
    private fun addCard(parent:LinearLayout, v:View, margin:Int=0) { parent.addView(v,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,margin) }) }
    private fun button(label:String,color:Int=BLUE, click:()->Unit):Button=Button(this).apply {
        text=label; setTextColor(Color.WHITE); setBackgroundColor(color); setOnClickListener { click() }
    }
    private fun sum(type:String,date:String):Long = db.all("type=? AND date=?", arrayOf(type,date)).sumOf{it.amount}
    private fun home(p:LinearLayout) {
        val date=today(); val income=sum("income",date); val expense=sum("expense",date)
        val top=card()
        top.addView(text("THU NHẬP HÔM NAY",13,MUTED,true))
        top.addView(text(money(income),28,GREEN,true))
        top.addView(text("Chi tiêu hôm nay",14,MUTED))
        top.addView(text(money(expense),22,RED,true))
        top.addView(text("Thu − chi hôm nay",14,MUTED))
        top.addView(text(money(income-expense),24,if(income-expense>=0) GREEN else RED,true))
        addCard(p,top,12)
        val goal=db.setting("goal","10000000").toLongOrNull() ?: 10000000L
        val saved=db.all().sumOf { if(it.type=="income") it.amount else -it.amount }.coerceAtLeast(0)
        val g=card()
        g.addView(text("MỤC TIÊU",13,MUTED,true))
        g.addView(text(money(goal),23,DARK,true))
        g.addView(text("Đã tích lũy: ${money(saved)}",15,GREEN))
        g.addView(text("Còn thiếu: ${money((goal-saved).coerceAtLeast(0))}",16,BLUE,true))
        val pct=if(goal>0) (saved*100/goal).coerceIn(0,100) else 0
        g.addView(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100; progress=pct; progressTintList=android.content.res.ColorStateList.valueOf(GREEN) })
        g.addView(text("$pct% hoàn thành",13,MUTED))
        addCard(p,g,12)
        val actions=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        actions.addView(button("+ THÊM THU",GREEN){ addTransaction("income") },LinearLayout.LayoutParams(0,58,1f).apply { setMargins(0,0,6,0) })
        actions.addView(button("− THÊM CHI",RED){ addTransaction("expense") },LinearLayout.LayoutParams(0,58,1f).apply { setMargins(6,0,0,0) })
        addCard(p,actions,16)
        p.addView(text("Giao dịch hôm nay",17,DARK,true))
        val list=db.all("date=?", arrayOf(date))
        if(list.isEmpty()) p.addView(text("Chưa có giao dịch nào. Bấm Thêm thu hoặc Thêm chi để bắt đầu.",14,MUTED))
        list.forEach { transactionRow(p,it,true) }
    }
    private fun addTransaction(type:String) {
        val dialog=Dialog(this)
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,18,22,12); setBackgroundColor(Color.WHITE) }
        box.addView(text(if(type=="income") "Thêm thu nhập" else "Thêm chi tiêu",20,DARK,true))
        val amount=EditText(this).apply { hint="Số tiền (VND)"; inputType=2 }
        box.addView(amount)
        val categories=if(type=="income") arrayOf("Chạy xe","Làm thêm","Tiền thưởng","Thu nhập khác") else arrayOf("Xăng","Ăn uống","Mua đồ","Điện thoại","Chi tiêu khác")
        val spinner=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,categories) }
        box.addView(spinner)
        val date=EditText(this).apply { setText(today()); hint="Ngày YYYY-MM-DD" }
        box.addView(date)
        val note=EditText(this).apply { hint="Ghi chú (không bắt buộc)" }
        box.addView(note)
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        row.addView(button("Hủy",MUTED){ dialog.dismiss() },LinearLayout.LayoutParams(0,-2,1f))
        row.addView(button("Lưu",GREEN){
            val value=amount.text.toString().replace(",","").replace(".","").trim().toLongOrNull()
            val d=date.text.toString().trim()
            if(value==null || value<=0) { amount.error="Nhập số tiền hợp lệ"; return@button }
            if(!Regex("\\d{4}-\\d{2}-\\d{2}").matches(d)) { date.error="Ngày theo dạng YYYY-MM-DD"; return@button }
            db.add(type,value,d,spinner.selectedItem.toString(),note.text.toString())
            dialog.dismiss(); show()
        },LinearLayout.LayoutParams(0,-2,1f))
        box.addView(row)
        dialog.setContentView(box); dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(),-2); dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(),-2)
    }
    private fun transactionRow(parent:LinearLayout,t:Tx,allowDelete:Boolean) {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(10,10,8,10); setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL }
        val info=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        info.addView(text("${t.category} • ${dateHuman(t.date)}",14,DARK,true))
        if(t.note.isNotBlank()) info.addView(text(t.note,12,MUTED))
        row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(text((if(t.type=="income") "+" else "−")+money(t.amount),15,if(t.type=="income") GREEN else RED,true))
        if(allowDelete) {
            val del=TextView(this).apply { text="  ×"; textSize=24f; setTextColor(RED); setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setMessage("Xóa giao dịch này?").setPositiveButton("Xóa"){_,_-> db.remove(t.id); show() }.setNegativeButton("Hủy",null).show()
            } }
            row.addView(del)
        }
        parent.addView(row,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,4) })
    }
    private fun history(p:LinearLayout) {
        p.addView(text("Lịch sử giao dịch",20,DARK,true))
        val all=db.all()
        if(all.isEmpty()) p.addView(text("Chưa có dữ liệu.",14,MUTED))
        all.groupBy{it.date}.forEach { (date,items) ->
            p.addView(text(dateHuman(date),16,DARK,true))
            p.addView(text("Thu ${money(items.filter{it.type=="income"}.sumOf{it.amount})}  |  Chi ${money(items.filter{it.type=="expense"}.sumOf{it.amount})}",13,MUTED))
            items.forEach { transactionRow(p,it,true) }
        }
    }
    private fun stats(p:LinearLayout) {
        val month=SimpleDateFormat("yyyy-MM",Locale.US).format(Date())
        val items=db.all("date LIKE ?", arrayOf("$month%"))
        val income=items.filter{it.type=="income"}.sumOf{it.amount}
        val expense=items.filter{it.type=="expense"}.sumOf{it.amount}
        p.addView(text("Thống kê tháng $month",20,DARK,true))
        val c=card()
        c.addView(text("Tổng thu",14,MUTED)); c.addView(text(money(income),24,GREEN,true))
        c.addView(text("Tổng chi",14,MUTED)); c.addView(text(money(expense),24,RED,true))
        c.addView(text("Chênh lệch",14,MUTED)); c.addView(text(money(income-expense),24,BLUE,true))
        addCard(p,c,12)
        p.addView(text("Theo nhóm chi",17,DARK,true))
        items.filter{it.type=="expense"}.groupBy{it.category}.toList().sortedByDescending{it.second.sumOf{t->t.amount}}.forEach { (cat,txs) ->
            p.addView(text("$cat: ${money(txs.sumOf{it.amount})}",15,DARK))
        }
    }
    private fun goal(p:LinearLayout) {
        val current=db.setting("goal","10000000").toLongOrNull() ?: 10000000L
        p.addView(text("Thiết lập mục tiêu",20,DARK,true))
        val edit=EditText(this).apply { hint="Mục tiêu VND"; inputType=2; setText(current.toString()) }
        p.addView(edit)
        p.addView(button("Lưu mục tiêu",GREEN){
            val v=edit.text.toString().trim().toLongOrNull()
            if(v==null || v<=0) edit.error="Mục tiêu phải lớn hơn 0"
            else { db.set("goal",v.toString()); Toast.makeText(this,"Đã lưu mục tiêu",Toast.LENGTH_SHORT).show(); show() }
        })
        p.addView(text("Tiền tích lũy được tính bằng tổng thu trừ tổng chi từ toàn bộ giao dịch đã nhập.",13,MUTED))
    }
}

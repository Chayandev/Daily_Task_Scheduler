package com.example.todolist.modules

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todolist.R
import com.example.todolist.adapters.TodoAdapter
import com.example.todolist.dataClasses.TodoData
import com.example.todolist.roomDB.DatabaseProvider
import com.example.todolist.roomDB.TodoDao
import com.example.todolist.util.SpinnerHolder
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.max


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonAdd: FloatingActionButton
    private lateinit var calendar: Calendar
    private lateinit var emptyImg: ImageView
    private lateinit var textView: TextView
    private lateinit var todoList: ArrayList<TodoData>
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var pgBar: ProgressBar
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var dayNum: TextView
    private lateinit var dayLang: TextView
    private lateinit var dayYM: TextView
    private lateinit var bottomNavigationView:BottomNavigationView
    private lateinit var date:String
    //database
    private lateinit var todoDao: TodoDao
    private var id: Long = 0

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView=findViewById(R.id.bottomBarAppNavigationView)
        bottomNavigationView.background=null

        bottomNavigationView.menu.getItem(1).setOnMenuItemClickListener {
            val intent=Intent(this, AllTaskViewActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        bottomNavigationView.menu.getItem(2).setOnMenuItemClickListener {
            val intent=Intent(this, PastTaskViewActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        pgBar = findViewById(R.id.loadingProgressBar)
        pgBar.visibility = View.VISIBLE
        emptyImg = findViewById(R.id.imageView)
        textView=findViewById(R.id.noTextView)
        bottomAppBar = findViewById(R.id.bottom_appbar_ll)
        val radius = resources.getDimension(R.dimen.corner_radius)
        val color1 = ContextCompat.getColor(this, R.color.bottomAndTopBar_bgLight)
        val color2 = ContextCompat.getColor(this, R.color.bottomAndTopBar_bgDark)
        val backgroundDrawable = MaterialShapeDrawable()
        if (this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            backgroundDrawable.setFillColor(ColorStateList.valueOf(color2))
        else
            backgroundDrawable.setFillColor(ColorStateList.valueOf(color1))
        backgroundDrawable.shapeAppearanceModel = backgroundDrawable.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, radius)
            .setTopRightCorner(CornerFamily.ROUNDED, radius)
            .build()

        bottomAppBar.background = backgroundDrawable

        dayNum = findViewById(R.id.dayNum)
        dayLang = findViewById(R.id.dayLang)
        dayYM = findViewById(R.id.dateMY)

        calendar = Calendar.getInstance()
        val simpleDateFormat = SimpleDateFormat("d, EEEE, MMMM yyyy")
        val currentDateAndTime = Date()

        val formattedDate = simpleDateFormat.format(currentDateAndTime)

        val day = SimpleDateFormat("d").format(currentDateAndTime)
        val dayOfWeek = SimpleDateFormat("EEEE").format(currentDateAndTime)
        val monthYear = SimpleDateFormat("MMMM YYYY").format(currentDateAndTime)

        dayNum.text = day.toString()
        dayLang.text = dayOfWeek.toString()
        dayYM.text = monthYear.toString()

        var format = SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
         date = format.format(Date())
        // Initialize database and DAO
        val todoDatabase = DatabaseProvider.getInstance(applicationContext)
        todoDao = todoDatabase.todoDao()

        todoList = ArrayList()

        CoroutineScope(Dispatchers.Main).launch {
            val tasksFromDB = withContext(Dispatchers.IO) {
                todoDao.getAllTodos()
            }
            val expiredTask = ArrayList<TodoData>()
            val activeTask = ArrayList<TodoData>()
            val checkedTask = ArrayList<TodoData>()
            for (task in tasksFromDB) {
                id = max(id, task.id)
                if (task.date == date) {
                    if (!SpinnerHolder.spinnerItems.contains(task.category)) {
                        SpinnerHolder.spinnerItems.add(0, task.category)
                    }
                    if (task.isExpired)
                        expiredTask.add(task)
                    else if (task.isChecked)
                        checkedTask.add(task)
                    else
                        activeTask.add(task)
                }
            }
            Log.i("taskSaved", "$tasksFromDB")
            todoList.clear()
            todoList.addAll(expiredTask)
            todoList.addAll(activeTask)
            todoList.addAll(checkedTask)
            todoAdapter.notifyDataSetChanged()

            // Show or hide the empty image view based on the task list size
            if (todoList.isEmpty()) {
                emptyImg.visibility = View.VISIBLE
                textView.visibility=View.VISIBLE

            }else {
                emptyImg.visibility = View.GONE
                textView.visibility=View.GONE

            }


            pgBar.visibility = View.GONE
        }

        todoAdapter = TodoAdapter(this, todoList, todoDao, emptyImg,textView,0,::scheduleNotification)

        recyclerView = findViewById(R.id.recycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = todoAdapter


        buttonAdd = findViewById(R.id.addTaskBtn)
//        editTextItem = findViewById(R.id.addItems)

        buttonAdd.setOnClickListener {
            calendar = Calendar.getInstance()
            val bottomDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
            val view = layoutInflater.inflate(R.layout.activity_adding_task, null)

            var et_item = view.findViewById<EditText>(R.id.et_taskName)
            var time_tv = view.findViewById<TextView>(R.id.et_time)
            var clockBtn = view.findViewById<ImageView>(R.id.ic_time)
            var cancelTime = view.findViewById<ImageView>(R.id.ic_time_cross)
            var spinner = view.findViewById<Spinner>(R.id.spinner_category)
            var et_cat = view.findViewById<EditText>(R.id.editText_category)
            var et_remove_cat = view.findViewById<EditText>(R.id.delet_category_text)
            val addCat = view.findViewById<ImageButton>(R.id.addCatButton)
            val removeCat = view.findViewById<ImageButton>(R.id.removeCat_btn)
            val addSaveBtn = view.findViewById<FloatingActionButton>(R.id.addAndSaveBtn)
            val addTaskLl = view.findViewById<CoordinatorLayout>(R.id.coordinateLL)

            updateItemViews(SpinnerHolder.spinnerItems, spinner)
            var categorySelected: String = SpinnerHolder.spinnerItems[0]
            spinner.setSelection(0)
            //spinner
            val spinnerAdapter =
                ArrayAdapter(this, R.layout.spinner_drop_down_view, SpinnerHolder.spinnerItems)
            spinner.adapter = spinnerAdapter

            addCat.setOnClickListener {
                val newCategory = et_cat.text.toString().lowercase()
                if (newCategory.isNotEmpty()) {
                    if (!SpinnerHolder.spinnerItems.contains(newCategory)) {
                        et_cat.text.clear()
                        SpinnerHolder.spinnerItems.add(0, newCategory)
                        updateItemViews(SpinnerHolder.spinnerItems, spinner)
                        categorySelected = newCategory
                    } else {
                        Snackbar.make(it, "Already Exists.", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val snackBar = Snackbar.make(
                        it, "No input Found!",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null)
                    snackBar.setActionTextColor(Color.WHITE)
                    val snackBarView = snackBar.view
                    snackBarView.setBackgroundColor(Color.RED)
                    val textView =
                        snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
                    textView.setTextColor(Color.WHITE)
                    snackBar.show()
                }
            }
            removeCat.setOnClickListener {
                val newCategory = et_remove_cat.text.toString()
                if (newCategory.isNotEmpty()) {
                    if (SpinnerHolder.spinnerItems.contains(newCategory)) {
                        if (newCategory != "work" && newCategory != "personal" && newCategory != "shopping") {
                            SpinnerHolder.spinnerItems.remove(newCategory)
                            et_remove_cat.text.clear()
                            updateItemViews(SpinnerHolder.spinnerItems, spinner)
                        } else {
                            Snackbar.make(
                                addTaskLl,
                                "you are Not Allowed to delete those default Categories",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Snackbar.make(
                            addTaskLl,
                            "Please Enter Correct Existing Category.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val snackBar = Snackbar.make(
                        addTaskLl, "No input Found!",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null)
                    snackBar.setActionTextColor(Color.WHITE)
                    val snackBarView = snackBar.view
                    snackBarView.setBackgroundColor(Color.RED)
                    val textView =
                        snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
                    textView.setTextColor(Color.WHITE)
                    snackBar.show()
                }
            }
            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View, position: Int, id: Long
                ) {
                    categorySelected = spinner.selectedItem as String
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    categorySelected = SpinnerHolder.spinnerItems[0]
                }

            }
            //end spinner


            clockBtn.setOnClickListener {
                showTimer(time_tv)
            }
            cancelTime.setOnClickListener {
                time_tv.text = ""
            }
            addSaveBtn.setOnClickListener {

                //edit task
                if (et_item.text.isNotEmpty()) {
                    id++
                    var taskName = et_item.text.toString()
                    var category = categorySelected
                    if (time_tv.text.isNotEmpty()) {
                        var time = time_tv.text.toString()
//                        var format = SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
//                        var date = format.format(Date())
                        val todo = TodoData(
                            id,
                            taskName,
                            false,
                            time,
                            date,
                            category,
                            isExpired = false,
                            isNotified = false
                        )
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                todoDao.insert(todo)
                            }
                            scheduleNotification(id,taskName,time)
                        }

                        todoList.add(todo)
                        Log.i("Add", "$todo")
                        todoAdapter.notifyItemInserted(todoList.size)
                        bottomDialog.dismiss()
                        emptyImg = findViewById(R.id.imageView)
                        if (todoList.size == 0)
                            emptyImg.visibility = View.VISIBLE
                        else
                            emptyImg.visibility = View.INVISIBLE
                    } else {
                        val snackBar = Snackbar.make(
                            addTaskLl, "Please Select Due Time!",
                            Snackbar.LENGTH_LONG
                        ).setAction("Action", null)
                        snackBar.setActionTextColor(Color.WHITE)
                        val snackBarView = snackBar.view
                        snackBarView.setBackgroundColor(Color.RED)
                        val textView =
                            snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
                        textView.setTextColor(Color.WHITE)
                        snackBar.show()
                    }

                } else {
                    val snackBar = Snackbar.make(
                        addTaskLl, "Task item Can't be Empty!",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null)
                    snackBar.setActionTextColor(Color.WHITE)
                    val snackBarView = snackBar.view
                    snackBarView.setBackgroundColor(Color.RED)
                    val textView =
                        snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
                    textView.setTextColor(Color.WHITE)
                    snackBar.show()
                }

            }
            // bottomDialog.setCancelable(false)
            bottomDialog.setContentView(view)

            bottomDialog.show()
        }

    }
    private fun scheduleNotification(taskId: Long, taskName: String, expirationTime: String) {
        val workManager = WorkManager.getInstance(applicationContext)

        // Parse the expiration time string to a Date object
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val expirationDate = format.parse(expirationTime)

        // Calculate the delay before showing the notification (5 minutes)
        val notificationDelay = 5 * 60 * 1000 // 5 minutes in milliseconds

        // Calculate the expiration time in milliseconds
        val expirationMillis = expirationDate?.time ?: return

        // Calculate the notification time by subtracting the delay from the expiration time
        val notificationTime = expirationMillis - notificationDelay

        // Build the input data
        val inputData = Data.Builder()
            .putLong("taskId", taskId)
            .putString("taskName", taskName)
            .build()

        // Create a OneTimeWorkRequest with input data and delay
        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(notificationTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()

        // Enqueue the work request with a unique tag
        workManager.enqueueUniqueWork(
            "notification_$taskId",
            ExistingWorkPolicy.REPLACE,
            notificationWorkRequest
        )
    }

    private fun showTimer(etTime: TextView) {
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        var timeTxt = ""
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                if (hour < currentHour || (hour == currentHour && minute < currentMinute)) {
                    // The selected time is before the current time
                    Toast.makeText(
                        this,
                        "Please select a time after the current time.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    timeTxt = formatTime(calendar.time)
                    etTime.text = timeTxt
                }
            },
            currentHour,
            currentMinute,
            false
        )
        timePickerDialog.show()
    }

    private fun formatTime(time: java.util.Date): String {
        val format = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(time)
    }

    private fun updateItemViews(
        spinnerItems: MutableList<String>,
        spinner: Spinner
    ) {
        val adapter = ArrayAdapter(this, R.layout.spinner_drop_down_view, spinnerItems)
        spinner.adapter = adapter

        spinner.setSelection(0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
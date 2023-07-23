package com.example.todolist.modules

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.todolist.databinding.ActivityPastTaskViewBinding
import com.example.todolist.roomDB.DatabaseProvider
import com.example.todolist.roomDB.TodoDao
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PastTaskViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPastTaskViewBinding
    private lateinit var pastTaskAdapter: TodoAdapter
    private lateinit var todoDao: TodoDao
    private lateinit var pastDataSet: ArrayList<TodoData>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPastTaskViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomBarAppNavigationView.background = null
        val menu = binding.bottomBarAppNavigationView.menu
        menu.getItem(2).isChecked=true
        //style of bottom appbar
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

        binding.bottomAppbarLl.background = backgroundDrawable

        //date
        val simpleDateFormat = SimpleDateFormat("d, EEEE, MMMM yyyy")
        val currentDateAndTime = Date()

        val formattedDate = simpleDateFormat.format(currentDateAndTime)

        val day = SimpleDateFormat("d").format(currentDateAndTime)
        val dayOfWeek = SimpleDateFormat("EEEE").format(currentDateAndTime)
        val monthYear = SimpleDateFormat("MMMM YYYY").format(currentDateAndTime)

        binding.dayNum.text = day.toString()
        binding.dayLang.text = dayOfWeek.toString()
        binding.dateMY.text = monthYear.toString()

        val todoDatabase = DatabaseProvider.getInstance(applicationContext)
        todoDao = todoDatabase.todoDao()

        binding.loadingProgressBar.visibility = View.VISIBLE
        pastDataSet = ArrayList()
        var format = SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
        val date = format.format(Date())

        CoroutineScope(Dispatchers.Main).launch {
            val tasksFromDB = withContext(Dispatchers.IO) {
                todoDao.getAllTodos()
            }
            val expiredTask = ArrayList<TodoData>()
            val checkedTask = ArrayList<TodoData>()
            for (task in tasksFromDB) {
                if (task.date != date) {
                    if (task.isExpired)
                        expiredTask.add(task)
                    else if (task.isChecked)
                        checkedTask.add(task)
                }
            }
            Log.i("taskSaved", "$tasksFromDB")
            pastDataSet.clear()
            pastDataSet.addAll(expiredTask)
            pastDataSet.addAll(checkedTask)

//            selectedTaskAdapter.notifyDataSetChanged()
            binding.loadingProgressBar.visibility = View.GONE
            if (pastDataSet.isEmpty()) {
                binding.imageViewEmpty.visibility = View.VISIBLE
                binding.noTextView.visibility = View.VISIBLE
            } else {
                binding.imageViewEmpty.visibility = View.GONE
                binding.noTextView.visibility = View.GONE
            }

        }
        pastTaskAdapter = TodoAdapter(
            this,
            pastDataSet,
            todoDao,
            binding.imageViewEmpty,
            binding.noTextView,
            1,
            ::scheduleNotification
        )
        pastTaskAdapter.setDataset(pastDataSet)

        binding.recycleView.layoutManager = LinearLayoutManager(this)
        binding.recycleView.adapter = pastTaskAdapter
        binding.bottomBarAppNavigationView.menu.getItem(0).setOnMenuItemClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        binding.bottomBarAppNavigationView.menu.getItem(1).setOnMenuItemClickListener {
            val intent = Intent(this, AllTaskViewActivity::class.java)
            startActivity(intent)
            finish()
            true
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
    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }
}

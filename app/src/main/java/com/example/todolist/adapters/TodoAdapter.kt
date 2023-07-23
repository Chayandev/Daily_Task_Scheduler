package com.example.todolist.adapters

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.example.todolist.R
import com.example.todolist.util.SpinnerHolder
import com.example.todolist.util.SpinnerHolder.spinnerItems
import com.example.todolist.roomDB.TodoDao
import com.example.todolist.dataClasses.TodoData
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


open class TodoAdapter(
    val c: Context,
    var todoList: ArrayList<TodoData>,
    private val todoDao: TodoDao,
    private val imageView: ImageView,
    private val noTextView: TextView,
    private val flag:Int,
    private val scheduleNotification: (Long, String, String) -> Unit
) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {
    private val handler = Handler()
    private val runnableMap = mutableMapOf<Int, Runnable>() // Map to hold task-specific runnables
    private var isComputingLayout = false
    internal fun setDataset(dataList: ArrayList<TodoData>) {
        todoList = dataList
    }
    inner class TodoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textItem = view.findViewById<TextView>(R.id.itemText)
        val time = view.findViewById<TextView>(R.id.Time)
        val checkBox = view.findViewById<CheckBox>(R.id.checkDone)
        val deletBtn = view.findViewById<ImageButton>(R.id.delete)
        val editBtn = view.findViewById<ImageButton>(R.id.edit_btn)
        val category = view.findViewById<TextView>(R.id.category_tag)
        val categoryTagLL = view.findViewById<LinearLayout>(R.id.category_tag_ll)
        val dateView = view.findViewById<TextView>(R.id.Date)
        val recyclerItemll=view.findViewById<CardView>(R.id.recylerItemll)
        val expiredView=view.findViewById<TextView>(R.id.expired)
        init {
            deletBtn.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        deleteItem()
                    }
                }
            }
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                val item = todoList[position]
                item.isChecked = isChecked
                CoroutineScope(Dispatchers.IO).launch {
                    todoDao.update(item)
                }
                if(!isChecked)
                    moveUncheckedItem(position, isChecked)

                updateTextStrikeThrough(position, checkBox,expiredView,textItem,recyclerItemll, isChecked)
                Handler().post {
                    if (!isComputingLayout && isChecked)
                        moveCheckedandExpiredItem(position, isChecked, item.isExpired)
                }
            }
            editBtn.setOnClickListener {
                val position = adapterPosition
                val item=todoList[position]
                if (!item.isChecked && !item.isExpired)
                    editItem(position)
                else if(!item.isChecked && item.isExpired){
                    Snackbar.make(it, "Task is expired can't edit this.", Snackbar.LENGTH_LONG).show()
                }
                else
                    Snackbar.make(it, "Task is done can't edit this.", Snackbar.LENGTH_LONG).show()
            }

        }

        //edit feature implementation

        open fun editItem(position: Int) {

            val item = todoList[position]

            val dialogBuilder = AlertDialog.Builder(c)
            val inflater = LayoutInflater.from(c)
            val dialogView = inflater.inflate(R.layout.edit_item_dialog, null)
            dialogBuilder.setView(dialogView)

            val editItemExt = dialogView.findViewById<EditText>(R.id.edit_itemTask)
            val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_btn)
            val saveBtn = dialogView.findViewById<Button>(R.id.save_btn)
            val optionalCategoryLL=dialogView.findViewById<LinearLayout>(R.id.categoryOptionalLL)
            //spinner
            val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
            val editTextCategory = dialogView.findViewById<EditText>(R.id.editText_category)
            val addCat_btn = dialogView.findViewById<ImageButton>(R.id.addButton)
            val removeCategoty_et = dialogView.findViewById<EditText>(R.id.delet_category_text)
            val removeCat_Btn = dialogView.findViewById<ImageButton>(R.id.remove_btn)
            var categorySelected: String = spinnerItems[0]
           if(flag==0) {
               optionalCategoryLL.visibility=View.VISIBLE
               val spinnerAdapter =
                   ArrayAdapter(c, R.layout.spinner_drop_down_view, SpinnerHolder.spinnerItems)
               categorySpinner.adapter = spinnerAdapter

               categorySpinner.setSelection(0)


               addCat_btn.setOnClickListener {
                   val newCategory = editTextCategory.text.toString().lowercase()
                   if (newCategory.isNotEmpty()) {
                       if (!spinnerItems.contains(newCategory)) {
                           editTextCategory.text.clear()
                           SpinnerHolder.spinnerItems.add(0, newCategory)
                           updateItemViews(categorySpinner)
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
               removeCat_Btn.setOnClickListener {
                   val newCategory = removeCategoty_et.text.toString()
                   if (newCategory.isNotEmpty()) {
                       if (spinnerItems.contains(newCategory)) {
                           if (newCategory != "work" && newCategory != "personal" && newCategory != "shopping") {
                               SpinnerHolder.spinnerItems.remove(newCategory)
                               removeCategoty_et.text.clear()
                               updateItemViews(categorySpinner)
                           } else {
                               Snackbar.make(
                                   it,
                                   "you are Not Allowed to delete those default Categories",
                                   Snackbar.LENGTH_LONG
                               ).show()
                           }
                       } else {
                           Snackbar.make(
                               it,
                               "Please Enter Correct Existing Category.",
                               Snackbar.LENGTH_LONG
                           ).show()
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
               updateItemViews(categorySpinner)
               categorySpinner.onItemSelectedListener = object :
                   AdapterView.OnItemSelectedListener {
                   override fun onItemSelected(
                       parent: AdapterView<*>,
                       view: View, position: Int, id: Long
                   ) {
                       categorySelected = categorySpinner.selectedItem as String
                   }

                   override fun onNothingSelected(parent: AdapterView<*>) {
                       //categorySelected=spinnerItems[0]
                   }

               }
               //end spinner
           }
         else{
               optionalCategoryLL.visibility=View.GONE
           }

            //clock
            val timeBtn = dialogView.findViewById<ImageView>(R.id.update_clock)
            val editTimeTxt = dialogView.findViewById<TextView>(R.id.edit_time)
            val removeTimeBtn = dialogView.findViewById<ImageView>(R.id.clock_cross)

            editTimeTxt.setText(item.time)
            timeBtn.setOnClickListener {
                showTimer(editTimeTxt)
            }
            removeTimeBtn.setOnClickListener {
                editTimeTxt.setText(item.time)
            }
            //end Clock
            editItemExt.setText(item.itemText)

            val dialog = dialogBuilder.create()
            dialog.setCanceledOnTouchOutside(false)

            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

            saveBtn.setOnClickListener {
                var newItemTask = editItemExt.text.toString()
                if(newItemTask.isEmpty())
                    newItemTask=item.itemText
//                val datePicked = editDateTxt.text.toString()
                val timePicked = editTimeTxt.text.toString()
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        updateItemTask(position, newItemTask, timePicked, categorySelected)
                    }
                }
                dialog.dismiss()
            }

            dialog.show()
        }



        private fun showTimer(editTimeTxt: TextView) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val timeTxt = editTimeTxt.text.toString()
            val selectedTime = formatTimeText(timeTxt)

            var pickedTime = ""
            val timePickerDialog = TimePickerDialog(
                c,
                { _, hour, minute ->
                    if (hour < currentHour || (hour == currentHour && minute < currentMinute)) {
                        // The selected time is before the current time
                        Toast.makeText(
                            c,
                            "Please select a time after the current time.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        pickedTime = formatTime(calendar.time)
                        editTimeTxt.setText(pickedTime)
                    }
                },
                selectedTime!!.get(Calendar.HOUR_OF_DAY),
                selectedTime!!.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }

        private fun formatDate(date: Date): String {
            val format = SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
            return format.format(date)
        }

        private fun formatTime(time: Date): String {
            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return format.format(time)
        }

        private fun formatTimeText(timeText: String): Calendar? {
            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return try {
                val date = format.parse(timeText)
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar
            } catch (e: ParseException) {
                null
            }
        }

        private fun isSame(selectedCalendar: Calendar, currentCalendar: Calendar): Boolean {
            return (selectedCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                    && selectedCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
                    && selectedCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH))
        }

        private suspend fun updateItemTask(
            position: Int,
            newTask: String,
            time: String,
            category: String
        ) {
            withContext(Dispatchers.Main) {
                val item = todoList[position]
                item.itemText = newTask
                item.category = category
                //            item.date=date
                item.time = time
                editNotificationTime(item.id,item.itemText,item.time)
                notifyItemChanged(position)
                withContext(Dispatchers.IO) {
                    todoDao.update(item)
                }

                withContext(Dispatchers.Main) {
                   // removeCatagoryView(position,categoryTagLL,category)
                    moveUncheckedItem(position, item.isChecked)
                    moveLowerItem(position, item.isChecked)
                }
            }
        }



        private suspend fun deleteItem() {
            withContext(Dispatchers.Main) {
                val deletedItem = todoList[position]
                todoDao.delete(deletedItem)
                todoList.removeAt(position)
                cancelNotification(deletedItem.id)
                notifyDataSetChanged()
                // Check if the todoList is empty
                if (todoList.isEmpty()) {
                    imageView.visibility = View.VISIBLE
                   noTextView.visibility=View.VISIBLE

                } else {
                    imageView.visibility = View.GONE
                    noTextView.visibility=View.GONE
                }
                runnableMap[deletedItem.id.toInt()]?.let { handler.removeCallbacks(it) }
                // Remove the runnable from the map
                runnableMap.remove(deletedItem.id.toInt())
                Log.i("taskSaved", "$todoList")

            }

        }


    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TodoViewHolder {
        val inflate = LayoutInflater.from(viewGroup.context)
        val view = inflate.inflate(R.layout.item_view, viewGroup, false)

        return TodoViewHolder(view)
    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.layoutManager?.isItemPrefetchEnabled = false
    }
    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: TodoViewHolder, position: Int) {
        isComputingLayout = true
        val newItem = todoList[position]
        viewHolder.textItem.text = newItem.itemText
        viewHolder.time.text = newItem.time
        viewHolder.checkBox.isChecked = newItem.isChecked
        viewHolder.category.text = newItem.category
        viewHolder.dateView.text = newItem.date

        updateItemBg(position,viewHolder.checkBox,viewHolder.expiredView,viewHolder.recyclerItemll,viewHolder.textItem,newItem.isChecked,newItem.isExpired)
        updateTextStrikeThrough(position,viewHolder.checkBox,viewHolder.expiredView,viewHolder.textItem,viewHolder.recyclerItemll, newItem.isChecked)
        //check for expire or not
//       if(!newItem.isChecked && !newItem.isExpired && !newItem.isNotified){
//           newItem.isNotified=true
//           scheduleNotification(newItem)
//       }


        Handler().post {
            moveUncheckedItem(position, newItem.isChecked)
            moveCheckedandExpiredItem(position, newItem.isChecked,newItem.isExpired)
            moveLowerItem(position,newItem.isChecked)
        }



        // Check if the todoList is empty
        if (todoList.isEmpty()) {
            imageView.visibility = View.VISIBLE
            noTextView.visibility=View.VISIBLE
        } else {
            imageView.visibility = View.GONE
            noTextView.visibility=View.GONE
        }
        isComputingLayout = false
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Cancel all runnables when the adapter is detached from the RecyclerView
        for (runnable in runnableMap.values) {
            handler.removeCallbacks(runnable)
        }
        // Clear the map
        runnableMap.clear()
    }
    private fun expireTimeCalculationAndSet(position: Int,checkBox:CheckBox,expiredView:TextView,layout:CardView,textView: TextView){
        val newItem=todoList[position]
        if(!newItem.isChecked && !newItem.isExpired) {
            val (isExpired,timeDiffrence)=getExpirationStatus(newItem.time,newItem.date)
            if(!isExpired && !newItem.isChecked){
                // Task is not expired yet, calculate the remaining time
                val remainingTime = timeDiffrence
                // Cancel any existing runnable for this task (if any)
                runnableMap[newItem.id.toInt()]?.let { handler.removeCallbacks(it) }

                // Schedule a new runnable to mark the task as expired after the remaining time
                val runnable = Runnable {
                    Log.i("IN2","BG check")
                    newItem.isExpired=true
                    CoroutineScope(Dispatchers.IO).launch {
                        todoDao.update(newItem)
                    }
                    updateItemBg(position,checkBox,expiredView,layout,textView,newItem.isChecked,newItem.isExpired)
                    // Mark the task as expired
                    // Update the UI to indicate that the task is expired
                }
                handler.postDelayed(runnable, remainingTime)
                runnableMap[newItem.id.toInt()] = runnable
            }
            else{
                Log.i("IN3","BG check")
                newItem.isExpired=true
                updateItemBg(position,checkBox,expiredView,layout,textView,newItem.isChecked,newItem.isExpired)
                CoroutineScope(Dispatchers.IO).launch {
                    todoDao.update(newItem)
                }
            }
        }
        if (newItem.isChecked) {
            newItem.isNotified=true
            runnableMap[newItem.id.toInt()]?.let { handler.removeCallbacks(it) }
            // Remove the runnable from the map
            runnableMap.remove(newItem.id.toInt())
            updateItemBg(position,checkBox,expiredView,layout,textView,newItem.isChecked,newItem.isExpired)
        }

    }
    private fun getExpirationStatus(expirationTime: String,expiredDate:String): Pair<Boolean, Long> {
        var format = android.icu.text.SimpleDateFormat("E, MMM d, yyyy", Locale.getDefault())
        var date = format.format(Date())
        if(date==expiredDate) {
            val currentTimeMillis = System.currentTimeMillis()
            val currentTime =
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentTimeMillis))

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val expirationDate = sdf.parse(expirationTime)
            val currentDate = sdf.parse(currentTime)

            val isExpired = expirationDate != null && expirationDate.before(currentDate)
            val timeDifference =
                if (expirationDate != null) expirationDate.time - currentDate.time else 0L

            return Pair(isExpired, timeDifference)
        }
        return Pair(false,0)
    }


    private fun moveUncheckedItem(position: Int, isChecked: Boolean) {
        if (!isChecked) {
            var currentPosition = position
            Handler().post {
                // positioning items on the basis of checked or unchecked
                while (currentPosition >= 1 && (todoList[currentPosition - 1].isChecked /*|| (!todoList[currentPosition - 1].isChecked && todoList[currentPosition - 1].isExpired)*/)) {

                    val currentItem = todoList[currentPosition]
                    val targetItem = todoList[currentPosition - 1]

                    todoList[currentPosition] = targetItem
                    todoList[currentPosition - 1] = currentItem

                    notifyItemMoved(currentPosition, currentPosition - 1)

                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                            todoDao.update(currentItem)
                            todoDao.update(targetItem)
                        }

                    }
                    currentPosition -= 1
                }
                //positioning items on the basis of time
                //if (currentPosition > 1)
                while (currentPosition >= 1 && (!todoList[currentPosition].isChecked )&& convertTimeToInt(
                        todoList[currentPosition].time
                    ) < convertTimeToInt(
                        todoList[currentPosition - 1].time
                    )
                ) {

                    var currentItem = todoList[currentPosition]
                    var targetItem = todoList[currentPosition - 1]

                    todoList[currentPosition] = targetItem
                    todoList[currentPosition - 1] = currentItem

                    notifyItemMoved(currentPosition, currentPosition - 1)
                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                            todoDao.updateTodos(todoList)
                        }
                    }

                    currentPosition -= 1

                }
            }

        }
        Log.i("taskSavedINUnchecked", "$todoList")

    }

    private fun moveCheckedandExpiredItem(position: Int, isChecked: Boolean,isExpired:Boolean) {
        if (isChecked) {
//            var currentPostion=position
//              while(currentPostion+1<todoList.size && !todoList[currentPostion+1].isExpired) {
//                   currentPostion++
//              }
            val item = todoList[position]
            todoList.removeAt(position)
            todoList.add(item)
            notifyItemMoved(position, todoList.size - 1)
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    todoDao.updateTodos(todoList)
                }
            }
        }

        Log.i("taskSavedInChecked", "$todoList")

    }
    private fun moveLowerItem(position: Int, isChecked: Boolean) {
        var currentPosition = position
        while (currentPosition < todoList.size - 1 && (!todoList[currentPosition + 1].isChecked /*&& !todoList[currentPosition+1].isExpired*/) && convertTimeToInt(
                todoList[currentPosition].time
            ) > convertTimeToInt(
                todoList[currentPosition + 1].time
            )
        ) {

            val currentItem = todoList[currentPosition]
            val targetItem = todoList[currentPosition + 1]
            todoList[currentPosition] = targetItem
            todoList[currentPosition + 1] = currentItem
            notifyItemMoved(currentPosition, currentPosition + 1)
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    todoDao.update(currentItem)
                    todoDao.update(targetItem)
                }
            }
            currentPosition += 1
        }
        Log.i("taskSavedINMoveLower", "$todoList")

    }

    private fun convertTimeToInt(time: String): Int {
        val (hoursString, minutesString, periods) = time.split(":", " ")
        val hours = hoursString.toInt()
        val minutes = minutesString.toInt()

        var totalTimeInMinutes = (hours * 60) + minutes
        if (periods.lowercase() == "pm") {
            if(hours!=12)
                totalTimeInMinutes += 12 * 60
        } else if (periods.lowercase() == "am") {
            if(hours==12)
                totalTimeInMinutes -= 12 * 60
        }
        return totalTimeInMinutes
    }

    private fun updateTextStrikeThrough(position: Int,checkBox: CheckBox,expiredView: TextView,textView: TextView,layout:CardView, isChecked: Boolean) {
        if (isChecked) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textView.setTextColor(Color.parseColor("#BED9D9D9"))
            layout.setBackgroundResource(R.drawable.item_card_checked_bg)
            runnableMap[todoList[position].id.toInt()]?.let { handler.removeCallbacks(it) }
            // Remove the runnable from the map
            runnableMap.remove(todoList[position].id.toInt())
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            if (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                if(!todoList[position].isExpired) {
                    textView.setTextColor(Color.WHITE)
                    layout.setBackgroundResource(R.drawable.item_card_bg_dark)
                }

            } else {
                if(!todoList[position].isExpired) {
                    textView.setTextColor(Color.BLACK)
                    layout.setBackgroundResource(R.drawable.item_card_bg_light)
                }
            }
            expireTimeCalculationAndSet(position,checkBox, expiredView,layout,textView )
        }
    }
    private fun  updateItemBg(position: Int,checkBox:CheckBox,expiredView:TextView,layout:CardView,textView: TextView,isChecked:Boolean,isExpired:Boolean){
        if(isExpired && !isChecked){
            expiredView.visibility=View.VISIBLE
            checkBox.visibility=View.GONE
            layout.setBackgroundResource(R.drawable.item_card_expired_bg)
            textView.setTextColor(Color.parseColor("#BED9D9D9"))
            runnableMap[todoList[position].id.toInt()]?.let { handler.removeCallbacks(it) }
            // Remove the runnable from the map
            runnableMap.remove(todoList[position].id.toInt())
            cancelNotification(todoList[position].id)
        }
        else {
            expiredView.visibility=View.GONE
            checkBox.visibility=View.VISIBLE
            if(!isChecked) {
                if (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    layout.setBackgroundResource(R.drawable.item_card_bg_dark)
                    textView.setTextColor(Color.WHITE)
                } else {
                    layout.setBackgroundResource(R.drawable.item_card_bg_light)
                    textView.setTextColor(Color.BLACK)
                }
            }
        }
    }
    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return todoList.size
    }

    fun getData(): List<TodoData> {
        return todoList
    }
    fun setData(newData: ArrayList<TodoData>) {
        todoList = newData
        notifyDataSetChanged()

        // Check if the todoList is empty
        if (todoList.isEmpty()) {
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
    }
    private fun cancelNotification(taskId: Long) {
        val workManager = WorkManager.getInstance(c)
        workManager.cancelUniqueWork("notification_$taskId")
    }
    private fun editNotificationTime(taskId: Long,taskName:String, newExpirationTime: String) {
        val workManager = WorkManager.getInstance(c)
        workManager.cancelUniqueWork("notification_$taskId")
        // Schedule the notification with the new expiration time
        // using the scheduleNotification() method
        scheduleNotification(taskId, taskName, newExpirationTime)
    }


    private fun updateItemViews(
        categorySpinner: Spinner
    ) {
        val adapter = ArrayAdapter(c, R.layout.spinner_drop_down_view, SpinnerHolder.spinnerItems)
        categorySpinner.adapter = adapter
        categorySpinner.setSelection(0)
    }


}









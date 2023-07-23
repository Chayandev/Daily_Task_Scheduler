package com.example.todolist.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.dataClasses.ALLTaskViewData
import com.example.todolist.R
import com.example.todolist.modules.SelectedTaskViewActivity

class AllTaskViewAdapter(val context: Context) :
    RecyclerView.Adapter<AllTaskViewAdapter.ViewHolder>() {


    var dataSet: ArrayList<ALLTaskViewData> = ArrayList()
    internal fun setDataset(dataList: ArrayList<ALLTaskViewData>) {
        dataSet = dataList
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title = view.findViewById<TextView>(R.id.title)
        var itemCardView=view.findViewById<CardView>(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.all_task_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataSet[position]

        holder.title.text = data.title
        holder.itemCardView.setOnClickListener {
           val intent= Intent(it.context,SelectedTaskViewActivity::class.java)
            intent.putExtra("title",data.title)

            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

}
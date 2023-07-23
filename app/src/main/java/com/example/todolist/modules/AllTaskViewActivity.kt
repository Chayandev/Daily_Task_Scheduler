package com.example.todolist.modules

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.todolist.modules.MainActivity
import com.example.todolist.R
import com.example.todolist.adapters.AllTaskViewAdapter
import com.example.todolist.dataClasses.ALLTaskViewData
import com.example.todolist.databinding.ActivityAllTaskViewBinding
import com.example.todolist.util.SpinnerHolder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable

class AllTaskViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllTaskViewBinding
    private var dataSet = ArrayList<ALLTaskViewData>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllTaskViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //set manu bar
        binding.bottomBarAppNavigationView.background = null
        binding.bottomBarAppNavigationView.menu.getItem(1).isChecked = true

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


        //
        binding.recycleView.layoutManager = GridLayoutManager(applicationContext, 2)
        val allTaskAdapter = AllTaskViewAdapter(applicationContext)
        binding.recycleView.adapter = allTaskAdapter
        binding.loadingProgressBar.visibility = View.VISIBLE
        for (category in SpinnerHolder.spinnerItems) {
            dataSet.add(ALLTaskViewData(category))
        }

        allTaskAdapter.setDataset(dataSet)
        binding.loadingProgressBar.visibility = View.GONE

        binding.bottomBarAppNavigationView.menu.getItem(0).setOnMenuItemClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        binding.bottomBarAppNavigationView.menu.getItem(2).setOnMenuItemClickListener {
            val intent = Intent(this,PastTaskViewActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
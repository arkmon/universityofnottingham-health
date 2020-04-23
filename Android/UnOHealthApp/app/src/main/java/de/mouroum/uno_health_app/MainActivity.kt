package de.mouroum.uno_health_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import java.lang.Thread.sleep
import kotlin.concurrent.thread

enum class AnswerType {
    Boolean , MultipleChoice
}
class MainActivity : AppCompatActivity() {

    private var adapter:MyAdapter? = null
    private var currentSurvey:Survey? = null

    private var onQuestion:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = MyAdapter(this)

        var gson = Gson()
        val basic_survey = resources.openRawResource(R.raw.basic_survey)
        .bufferedReader().use { it.readText() }

        var result = gson?.fromJson(basic_survey, Survey::class.java)
        currentSurvey = result

        openQuestion()
        nextQuestion()

    }

    val fragment = GeneralFragment.newInstance(R.layout.question_container)

    fun nextQuestion(){

        if(onQuestion >= currentSurvey?.questions?.size ?: 0){
            return
        }

        thread {
            while(fragment.createdView == null){
                sleep(50)
            }

            runOnUiThread {
                adapter?.setCurrentQuestion(currentSurvey!!.questions[onQuestion])
                fragment.view!!.findViewById<TextView>(R.id.questionTitle).text = currentSurvey!!.questions[onQuestion].question
                onQuestion += 1
                adapter?.notifyDataSetChanged()
            }
        }



    }

    fun openQuestion(){
        supportFragmentManager
            // 3
            .beginTransaction()
            // 4
            .add(R.id.purpelContainer, fragment, "questionFragment")
            // 5
            .commit()

        thread {
            while(fragment.createdView == null){
                sleep(50)
            }
            runOnUiThread {

                fragment.view!!.findViewById<ListView>(R.id.answerList).adapter = adapter

                fragment.view!!.findViewById<Button>(R.id.questionButton).setOnTouchListener { v, event ->

                        if (event.action == MotionEvent.ACTION_DOWN) {
                            v.background = this@MainActivity.getDrawable(R.drawable.round_rect_violet)
                        } else {
                            v.background = this@MainActivity.getDrawable(R.drawable.round_rect_purple)
                        }
                        return@setOnTouchListener false
                    }


            }
        }


    }

    fun closeQuestion(){
        supportFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
    }

    fun uponClick(view:View){

        nextQuestion()
    }

    private class MyAdapter(context: Context): BaseAdapter(){

        private val mContext: Context

        var type:AnswerType = AnswerType.MultipleChoice
        var question:Question? = null
        var selected:MutableSet<Int> = mutableSetOf()

        init {
            mContext = context
        }

        fun setCurrentQuestion(q:Question){
            this.question = q
            this.selected = mutableSetOf()

            when(q.type){
                "CHOICE" -> type = AnswerType.MultipleChoice
                "BOOL" -> type = AnswerType.Boolean
            }

            notifyDataSetChanged()

        }

        override fun getCount(): Int {
            when(type){
                AnswerType.MultipleChoice -> return question?.answers?.size ?: 0
                AnswerType.Boolean -> return 1
            }
        }

        override fun getItem(position: Int): Any {
            return ""
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInActivity = LayoutInflater.from(mContext)

            when(type){
                AnswerType.MultipleChoice -> {
                    val cell = layoutInActivity.inflate(R.layout.multiple_choice_question,parent,false)
                    var textview = cell.findViewById<TextView>(R.id.answerTextview)
                    val constLayOut = textview.layoutParams as ConstraintLayout.LayoutParams
                    textview.text = question!!.answers!![position].value
                    cell.setOnClickListener {

                        var textview = it.findViewById<TextView>(R.id.answerTextview)
                        val constLayOut = textview.layoutParams as ConstraintLayout.LayoutParams

                        if(question?.multiple == false && selected.isEmpty() == false){
                            selected.clear()
                            selected.add(position)
                        }
                        else{
                            selected.add(position)
                        }

                        this@MyAdapter.notifyDataSetChanged()
                    }

                    if(selected.contains(position)){
                        constLayOut.leftMargin   =  20
                    }
                    else{
                        constLayOut.leftMargin   =  0
                    }
                    textview.layoutParams = constLayOut

                    return cell
                }
                AnswerType.Boolean -> {
                    val cell = layoutInActivity.inflate(R.layout.boolean_choice_question,parent,false)
                    return cell
                }
            }


            return View(mContext)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }
}

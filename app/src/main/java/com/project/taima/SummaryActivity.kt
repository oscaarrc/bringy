package com.project.taima.com.project.taima

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.taima.HomeActivity
import com.project.taima.R
import com.project.taima.adapter.SummaryAdapter
import com.project.taima.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private lateinit var chargedText: TextView
    private lateinit var depositText: TextView
    private lateinit var transactionsList: ListView
    private lateinit var noTransactionsMessage: TextView
    private lateinit var loadMoreButton: FloatingActionButton
    private lateinit var btnDailySummary: Button
    private var selectedSummaryType: String = "Resumen diario"

    private val db = FirebaseFirestore.getInstance()
    private val transactions = mutableListOf<Transaction>()
    private var lastLoadedTransaction: DocumentSnapshot? = null
    private var totalCharged = 0.0
    private var totalDeposit = 0.0
    private lateinit var adapter: SummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        setupBackButton()
        setupDayFilterButton()

        chargedText = findViewById(R.id.chargedText)
        depositText = findViewById(R.id.depositText)
        transactionsList = findViewById(R.id.transactionsListSummary)
        noTransactionsMessage = findViewById(R.id.noTransactionsMessageSummary)
        loadMoreButton = findViewById(R.id.loadMoreButton)

        adapter = SummaryAdapter(this, transactions)
        transactionsList.adapter = adapter

        transactions.clear()
        lastLoadedTransaction = null
        totalCharged = 0.0
        totalDeposit = 0.0

        val (startDate, endDate) = getDateRange("Resumen diario")
        totalChargedAndDepositCalc(startDate, endDate) { totalCharged, totalDeposit ->
            chargedText.text = "Total cobrado: %.2f".format(totalCharged) + " €"
            depositText.text = "Total fiado: %.2f".format(totalDeposit) + " €"
        }
        loadTransactions()

        loadMoreButton.setOnClickListener {
            loadTransactions()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDayFilterButton(){
        btnDailySummary = findViewById(R.id.dailySummaryBtn)
        btnDailySummary.text = "Resumen diario"
        btnDailySummary.setOnClickListener {
            showSummaryFilterDialog()
        }
    }

    private fun showSummaryFilterDialog() {
        val summaryType = arrayOf(
            "Resumen diario", "Resumen semanal", "Resumen mensual", "Resumen anual"
        )

        AlertDialog.Builder(this)
            .setTitle("Selecciona un tipo de resumen")
            .setItems(summaryType) { _, which ->
                selectedSummaryType = summaryType[which]
                btnDailySummary.text = selectedSummaryType

                transactions.clear()
                lastLoadedTransaction = null
                totalCharged = 0.0
                totalDeposit = 0.0
                val (startDate, endDate) = getDateRange(selectedSummaryType)
                totalChargedAndDepositCalc(startDate, endDate) { totalCharged, totalDeposit ->
                    chargedText.text = "Total cobrado: %.2f".format(totalCharged) + " €"
                    depositText.text = "Total fiado: %.2f".format(totalDeposit) + " €"
                }
                loadTransactions()
            }
            .show()
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backIconSummary).setOnClickListener {
            val intent = Intent(this@SummaryActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadTransactions() {
        val (startDate, endDate) = getDateRange(selectedSummaryType)

        var query = db.collection("Transactions")
            .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
            .whereLessThan("date", Timestamp(endDate))
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)

        lastLoadedTransaction?.let {
            query = query.startAfter(it)
        }

        query.get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                loadMoreButton.visibility = View.GONE
                return@addOnSuccessListener
            }

            for (document in result) {
                val id = document.id
                val date = document.getTimestamp("date")
                val customerId = document.getString("customerId") ?: ""

                val chargedValue = (document.get("charged") as? List<Number>)?.firstOrNull()?.toDouble() ?: 0.0
                val depositValue = (document.get("deposit") as? List<Number>)?.firstOrNull()?.toDouble() ?: 0.0


                if (chargedValue > 0) {
                    transactions.add(
                        Transaction(
                            id = id,
                            date = date,
                            amount = chargedValue,
                            isIncome = true,
                            customerId = customerId
                        )
                    )
                } else if (depositValue > 0) {
                    transactions.add(
                        Transaction(
                            id = id,
                            date = date,
                            amount = depositValue,
                            isIncome = false,
                            customerId = customerId
                        )
                    )
                }
            }

            lastLoadedTransaction = result.documents.last()

            loadMoreButton.visibility =
                if (result.size() == 20) View.VISIBLE
                else View.GONE

            noTransactionsMessage.visibility = View.GONE
            transactionsList.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            noTransactionsMessage.text = "Error al cargar las transacciones"
            noTransactionsMessage.visibility = View.VISIBLE
            e.printStackTrace()
        }
    }

    private fun getDateRange(summaryType: String): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val endDate: Date
        val startDate: Date

        when (summaryType) {
            "Resumen diario" -> {
                startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                endDate = calendar.time
            }
            "Resumen semanal" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                startDate = calendar.time
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                endDate = calendar.time
            }
            "Resumen mensual" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startDate = calendar.time
                calendar.add(Calendar.MONTH, 1)
                endDate = calendar.time
            }
            "Resumen anual" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                startDate = calendar.time
                calendar.add(Calendar.YEAR, 1)
                endDate = calendar.time
            }
            else -> {
                startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                endDate = calendar.time
            }
        }

        Log.w("Dates", "Start: $startDate\nEnd: $endDate")
        return Pair(startDate, endDate)
    }

    private fun totalChargedAndDepositCalc(startDate: Date, endDate: Date, onResult: (Double, Double) -> Unit) {

        db.collection("Transactions")
            .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
            .whereLessThan("date", Timestamp(endDate))
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val chargedValue = (document.get("charged") as? List<Number>)?.firstOrNull()?.toDouble() ?: 0.0
                    val depositValue = (document.get("deposit") as? List<Number>)?.firstOrNull()?.toDouble() ?: 0.0

                    if (chargedValue > 0) {
                        totalCharged += chargedValue
                    } else if (depositValue > 0) {
                        totalDeposit += depositValue
                    }
                }

                onResult(totalCharged, totalDeposit)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onResult(0.0, 0.0)
            }
    }




}

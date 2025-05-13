package com.project.taima

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import com.project.taima.adapter.RouteAdapter
import com.project.taima.model.Customer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouteActivity : AppCompatActivity() {
    private lateinit var routeAdapter: RouteAdapter
    private var customers = mutableListOf<Customer>()
    private val db = FirebaseFirestore.getInstance()
    private val originalCustomers = mutableListOf<Customer>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)
 
        setupRecyclerView()
        fetchCustomersFromFirestore()
        setupItemTouchHelper()
        setupSearchBar()
        setupBackButton()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun setupRecyclerView() {
        val routeRecyclerView = findViewById<RecyclerView>(R.id.routeCustomerRecyclerView)
        routeAdapter = RouteAdapter(customers)
        routeRecyclerView.layoutManager = LinearLayoutManager(this)
        routeRecyclerView.adapter = routeAdapter
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition

                routeAdapter.swapItems(fromPosition, toPosition)

                val currentDay = dailyRoute()

                updatePositionsForDay(currentDay)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(findViewById(R.id.routeCustomerRecyclerView))
    }

    private fun updatePositionsForDay(currentDay: String) {
        val currentDayTitled = currentDay.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        customers.forEachIndexed { index, customer ->
            customer.position[currentDayTitled] = index

            db.collection("Customer").document(customer.id)
                .update("position", customer.position)
                .addOnSuccessListener {
                    Log.d("Firestore", "Posición actualizada para ${customer.alias}: Día $currentDay, Posición $index")
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error al actualizar posición para ${customer.alias}", exception)
                }
        }
    }



    private fun setupSearchBar() {
        val searchEditText = findViewById<EditText>(R.id.searchbar)
        searchEditText.textLocale = Locale("es", "ES")
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCustomers(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCustomers(searchText: String) {
        if (searchText.isEmpty()) {
            routeAdapter.updateData(originalCustomers)
        } else {
            val filteredCustomers = originalCustomers.filter { customer ->
                customer.alias.lowercase(Locale.getDefault()).contains(searchText.lowercase()) ||
                        customer.route.lowercase(Locale.getDefault()).contains(searchText.lowercase())
            }
            routeAdapter.updateData(filteredCustomers)
        }
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            val intent = Intent(this@RouteActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun dailyRoute(): String {
        val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
        return dayFormat.format(Date()).replaceFirstChar { it.uppercaseChar() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchCustomersFromFirestore() {
        val routeDay = dailyRoute().replaceFirstChar { it.uppercaseChar() }
        Log.d("DailyRoute", "Customer success: $routeDay")

        db.collection("Customer")
            .orderBy("position")
            .get()
            .addOnSuccessListener { result ->
                updateCustomerList(result, routeDay)
            }
            .addOnFailureListener { exception ->
                Log.w("RouteActivity", "Error al obtener documentos.", exception)
            }
    }

    private fun updateCustomerList(result: QuerySnapshot, routeDay: String) {
        customers.clear()
        originalCustomers.clear()

        for (document in result) {
            val customer = document.toObject<Customer>()
            customer.id = document.id

            if (customer.routeDay.any { it.equals(routeDay, ignoreCase = true) }) {
                if (customer.position[routeDay] == null) {
                    customer.position[routeDay] = customers.size
                }

                customers.add(customer)
                originalCustomers.add(customer)
            }
        }

        customers.sortBy { it.position[routeDay] ?: Int.MAX_VALUE }
        routeAdapter.updateData(customers.toList())
        Log.d("DailyRoute", "Total customers: ${customers.size}")
    }


}

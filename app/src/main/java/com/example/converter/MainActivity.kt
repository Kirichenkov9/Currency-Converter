package com.example.converter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private var spinnerFrom: Spinner? = null
    private var spinnerTo: Spinner? = null
    private var convertButton: Button? = null
    private var valFromView: EditText? = null
    private var resultView: TextView? = null
    private var lineResult: LinearLayout? = null

    private var valueFrom: Float? = null
    private var result: String? = null

    private var currencyFrom: String? = null
    private var currencyTo: String? = null

    private var arrayList = ArrayList<String>()
    private var arrayAdapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinnerFrom = find(R.id.list_currency_from)
        spinnerTo = find(R.id.list_currency_to)
        convertButton = find(R.id.get_result)
        valFromView = find(R.id.value_from)
        resultView = find(R.id.result_value)
        lineResult = find(R.id.line_result)

        // сохраняем значения переменных при смене ориентации экрана
        if (savedInstanceState != null) {
            currencyFrom = savedInstanceState.getString("CURRENCY_FROM", null)
            currencyTo = savedInstanceState.getString("CURRENCY_TO", null)
            valueFrom = savedInstanceState.getFloat("VALUE_FROM", 0F)
            result = savedInstanceState.getString("RESULT", null)

            resultView?.text = result
            lineResult?.visibility = View.VISIBLE

            arrayList.addAll(savedInstanceState.getStringArrayList("ARRAY_CURRENCIES"))

            arrayAdapter =
                ArrayAdapter<String>(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    arrayList
                )
            arrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerFrom?.adapter = arrayAdapter
            spinnerTo?.adapter = arrayAdapter

        } else
        // если переменная savedInstanceState == null получаем список валют через get запрос
            getCurrencies()

        // устанавливаем слушатель на спиннер для обработки выбранной валюты
        spinnerFrom?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = parent.getItemAtPosition(position)
                currencyFrom = item.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // устанавливаем слушатель на спиннер для обработки выбранной валюты
        spinnerTo?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = parent.getItemAtPosition(position)
                currencyTo = item.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // устанавливаем слушатель на кнопку конвертирования
        convertButton?.setOnClickListener {

            // прверка на валидность введеного значения суммы и наличие выбранной валюты
            val focusView: View?

            if (valFromView?.text.toString().toFloatOrNull() != null) {
                valueFrom = valFromView?.text.toString().toFloat()

                if (currencyTo.isNullOrEmpty() || currencyFrom.isNullOrEmpty())
                    toast("Проверьте подключение к интернету!")
                else
                    convert()
            } else {
                valFromView?.error = getString(R.string.empty_value)
                focusView = valFromView
                focusView?.requestFocus()
            }
        }
    }

    // функция для получения списка валют
    private fun getCurrencies(): ArrayList<String> {

        val queue = Volley.newRequestQueue(this)
        val url: String = "https://free.currconv.com/api/v7/currencies?apiKey=1ca94974e1defb8bf62c/"

        val cacheRequest =
            CacheRequest(Request.Method.POST, url, Response.Listener<NetworkResponse> { response ->

                val jsonStr = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers)))
                val jsonObj = JSONObject(jsonStr)

                Log.d("JSON:", jsonObj.toString())

                val json: JSONObject = jsonObj.getJSONObject("results")

                for (i in json.keys()) arrayList.add(i.toString())

                arrayList.sort()

                arrayAdapter =
                    ArrayAdapter<String>(
                        this@MainActivity,
                        android.R.layout.simple_spinner_item,
                        arrayList
                    )
                arrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinnerFrom?.adapter = arrayAdapter
                spinnerTo?.adapter = arrayAdapter


            },
                Response.ErrorListener { error ->
                    if (error is NetworkError || error is NoConnectionError)
                        toast("Проверьте подключение к интернету!")
                    else if (error is ServerError || error is TimeoutError)
                        toast("Попробуйте снова!")
                })

        queue.add(cacheRequest)

        return arrayList
    }

    // функиця для получения текущего курса и конвертирования валют
    private fun convert() {
        val queue = Volley.newRequestQueue(this)
        val url: String =
            """
            https://free.currconv.com/api/v7/convert?q=${currencyFrom}_$currencyTo&apiKey=1ca94974e1defb8bf62c/
            """
        val cacheRequest =
            CacheRequest(Request.Method.POST, url, Response.Listener<NetworkResponse> { response ->

                val jsonString =
                    String(response.data, charset(HttpHeaderParser.parseCharset(response.headers)))

                val json = JSONObject(jsonString).getJSONObject("results")
                    .getJSONObject("${currencyFrom}_$currencyTo")

                val res = json.get("val").toString().toFloat() * valueFrom!!

                result = valueFrom.toString() + " " + currencyFrom + " -> " + res + " " + currencyTo

                resultView?.text = result
                lineResult?.visibility = View.VISIBLE


            }, Response.ErrorListener { error ->
                if (error is NetworkError || error is NoConnectionError)
                    toast("Проверьте подключение к интернету!")
                else if (error is ServerError || error is TimeoutError)
                    toast("Попробуйте снова!")
            })

        queue.add(cacheRequest)
    }

    // сохраняем значения переменных для поддержки поворота устройства
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString("CURRENCY_FROM", currencyFrom)
        outState?.putString("CURRENCY_TO", currencyTo)
        outState?.putStringArrayList("ARRAY_CURRENCIES", arrayList)
        valueFrom?.let { outState?.putFloat("VALUE_FROM", it) }
        outState?.putString("RESULT", result)
    }
}
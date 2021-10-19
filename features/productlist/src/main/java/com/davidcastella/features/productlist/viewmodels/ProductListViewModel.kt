package com.davidcastella.features.productlist.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidcastella.domain.conversionrates.entities.CurrencyCode
import com.davidcastella.domain.core.util.toCurrencyString
import com.davidcastella.domain.transactions.interactors.GetTransactions
import com.davidcastella.features.productlist.mappers.TransactionListMapper
import com.davidcastella.features.productlist.models.ProductTransactionsUI
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductListViewModel(
    private val getTransactions: GetTransactions,
    private val transactionListMapper: TransactionListMapper
) : ViewModel() {

    companion object {
        private val DEFAULT_CURRENCY = CurrencyCode.EUR
    }

    private val _viewState: MutableLiveData<ViewState> = MutableLiveData()
    val viewState: LiveData<ViewState>
        get() = _viewState

    private lateinit var productList: List<ProductTransactionsUI>

    sealed class ViewState {
        object Loading : ViewState()
        object Empty : ViewState()
        class Success(val productNameList: List<String>) : ViewState()
        class ProductDetails(val productName: String, val amounts: List<String>, val total: String) : ViewState()
    }

    sealed class ViewEvent {
        object OnStart : ViewEvent()
        class OnProductClick(val productName: String) : ViewEvent()
    }

    fun dispatchEvent(event: ViewEvent) = when (event) {
        is ViewEvent.OnStart -> onStart()
        is ViewEvent.OnProductClick -> onProductClick(event.productName)
    }

    private fun onStart() {
        _viewState.postValue(ViewState.Loading)
        viewModelScope.launch {
            getTransactions(DEFAULT_CURRENCY)
                .collect {
                    if (it.isEmpty()) _viewState.postValue(ViewState.Empty)
                    else {
                        productList = transactionListMapper(it)
                        _viewState.postValue(ViewState.Success(productList.map { it.productSku }))
                    }
                }
        }
    }

    private fun onProductClick(productName: String) {
        val product = productList.first { it.productSku == productName }
        var total = BigDecimal.ZERO
        val amounts = product.amountList.map {
            total += it
            it.toCurrencyString(DEFAULT_CURRENCY)
        }
        _viewState.postValue(ViewState.ProductDetails(product.productSku, amounts, total.toCurrencyString(DEFAULT_CURRENCY)))
    }
}
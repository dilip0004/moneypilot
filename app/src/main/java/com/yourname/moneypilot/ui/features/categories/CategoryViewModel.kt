package com.yourname.moneypilot.ui.features.categories

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesState(
    val categories: List<CategoryEntity> = emptyList(),
    val subcategoriesMap: Map<Long, List<SubcategoryEntity>> = emptyMap()
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : BaseViewModel<CategoriesState>() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        seedAndLoad()
    }

    private fun seedAndLoad() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            categoryRepository.seedDefaults()
            
            categoryRepository.getAllCategories().collect { categories ->
                if (categories.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    val subMap = mutableMapOf<Long, List<SubcategoryEntity>>()
                    categories.forEach { category ->
                        viewModelScope.launch {
                            categoryRepository.getSubcategories(category.id).collect { subList ->
                                subMap[category.id] = subList
                                _uiState.value = ScreenState.Success(CategoriesState(categories, subMap.toMap()))
                            }
                        }
                    }
                }
            }
        }
    }

    fun addCategory(name: String, type: String, color: Int, icon: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = name,
                    type = type,
                    color = color,
                    icon = icon,
                    parentId = null,
                    budgetLimit = null
                )
            )
        }
    }

    fun addSubcategory(categoryId: Long, name: String) {
        viewModelScope.launch {
            categoryRepository.insertSubcategory(
                SubcategoryEntity(
                    categoryId = categoryId,
                    name = name
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val count = categoryRepository.getTransactionCountForCategory(category.id)
            if (count > 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot delete category used by $count transactions."))
            } else {
                categoryRepository.deleteCategory(category)
            }
        }
    }

    fun deleteSubcategory(subcategory: SubcategoryEntity) {
        viewModelScope.launch {
            val count = categoryRepository.getTransactionCountForSubcategory(subcategory.id)
            if (count > 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot delete subcategory used by $count transactions."))
            } else {
                categoryRepository.deleteSubcategory(subcategory)
            }
        }
    }
}

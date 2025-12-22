package com.yourname.moneypilot.ui.features.categories

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
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

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            categoryRepository.getAllCategories().collectLatest { list ->
                _uiState.value = ScreenState.Success(CategoriesState(categories = list))
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

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            // Principle 14: Requires reassignment logic here in a real implementation
            categoryRepository.deleteCategory(category)
        }
    }
}

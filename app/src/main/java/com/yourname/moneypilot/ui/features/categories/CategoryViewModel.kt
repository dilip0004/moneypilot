package com.yourname.moneypilot.ui.features.categories

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
            categoryRepository.deleteCategory(category)
        }
    }

    fun deleteSubcategory(subcategory: SubcategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteSubcategory(subcategory)
        }
    }
}

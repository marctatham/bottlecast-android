package com.bottlecast.app.feature.penname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.isValidPenName
import com.bottlecast.app.common.profile.domain.limitPenName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

/** The pen-name sheet (spec §3.2, G13): type one, or have one suggested, never from the Google name. */
@HiltViewModel
class PenNameViewModel @Inject constructor(
    private val profileRepository: IProfileRepository,
    private val random: Random,
) : ViewModel() {

    private val _state: MutableStateFlow<PenNameUiState> = MutableStateFlow(PenNameUiState.Editing())
    val state: StateFlow<PenNameUiState> = _state.asStateFlow()

    // the last suggestion, so a save can say whether it was typed or suggested
    private var suggested: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[savePenName] Couldn't save the pen name")
        _state.value = PenNameUiState.Editing(name = editing()?.name.orEmpty(), showError = false)
    }

    init {
        // the pen name there is now, to edit
        viewModelScope.launch(exceptionHandler) {
            val current = profileRepository.profile.firstOrNull()?.penName ?: return@launch
            _state.update { if (it is PenNameUiState.Editing && it.name.isEmpty()) it.copy(name = current) else it }
        }
    }

    fun onNameChange(name: String) {
        val editing = editing() ?: return
        _state.value = editing.copy(name = limitPenName(name), showError = false)
    }

    /** "Surprise me": one of [suggestions], never the name already there. */
    fun onSurpriseMe(suggestions: List<String>) {
        val editing = editing() ?: return
        val choices = suggestions.filter { it != editing.name }.ifEmpty { return }
        val pick = choices[random.nextInt(choices.size)]
        suggested = pick
        _state.value = editing.copy(name = pick, showError = false)
    }

    fun onSave() {
        val editing = editing() ?: return
        val name = editing.name.trim()
        if (!isValidPenName(name)) {
            _state.value = editing.copy(showError = true)
            return
        }
        viewModelScope.launch(exceptionHandler) {
            profileRepository.savePenName(name)
            _state.value = PenNameUiState.Saved(name = name, suggested = name == suggested)
        }
    }

    private fun editing(): PenNameUiState.Editing? = _state.value as? PenNameUiState.Editing
}

sealed class PenNameUiState {

    data class Editing(
        val name: String = "",
        /** Save was tapped with a name too short: the helper says why, in error. */
        val showError: Boolean = false,
    ) : PenNameUiState()

    /** Saved; the sheet closes. [suggested] when it was "Surprise me"'s. */
    data class Saved(val name: String, val suggested: Boolean) : PenNameUiState()
}

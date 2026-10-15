package com.bottlecast.app.feature.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.R
import com.bottlecast.app.common.threading.DispatcherIO
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Open-source licences (spec §3.17): the fonts and icons bundled in the app, whose licence texts ship
 * with it, and every library, from the list the AboutLibraries plugin writes at build time.
 */
@HiltViewModel
class LicencesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state: MutableStateFlow<LicencesUiState> = MutableStateFlow(LicencesUiState.Loading)
    val state: StateFlow<LicencesUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[licences] Couldn't read the licences")
        _state.value = LicencesUiState.Showing(libraries = emptyList())
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            val libraries = withContext(dispatcher) {
                Libs.Builder().withContext(context).build().libraries.map { library ->
                    LicensedLibrary(
                        id = library.uniqueId,
                        name = library.name,
                        version = library.artifactVersion,
                        licences = library.licenses.map { it.name }.sorted(),
                        url = library.licenses.firstNotNullOfOrNull { it.url } ?: library.website,
                    )
                }.sortedBy { it.name.lowercase() }
            }
            _state.value = LicencesUiState.Showing(libraries = libraries)
        }
    }

    /** Reads a bundled licence's text, to show in full. */
    fun onOpenBundled(asset: BundledAsset) {
        viewModelScope.launch(exceptionHandler) {
            val text = withContext(dispatcher) { context.assets.open(asset.licenceFile).bufferedReader().use { it.readText() } }
            _state.update { (it as? LicencesUiState.Showing)?.copy(openText = OpenLicence(asset, text)) ?: it }
        }
    }

    fun onCloseBundled() {
        _state.update { (it as? LicencesUiState.Showing)?.copy(openText = null) ?: it }
    }
}

sealed class LicencesUiState {
    data object Loading : LicencesUiState()

    data class Showing(
        val libraries: List<LicensedLibrary>,
        val bundled: List<BundledAsset> = BundledAsset.entries,
        /** A bundled licence's text, open in a sheet. */
        val openText: OpenLicence? = null,
    ) : LicencesUiState()
}

data class LicensedLibrary(
    val id: String,
    val name: String,
    val version: String?,
    val licences: List<String>,
    /** The licence's page, or the library's, to open in a Custom Tab. */
    val url: String?,
)

data class OpenLicence(val asset: BundledAsset, val text: String)

/** The fonts and icons bundled in the app (licenses/README.md), each with its licence text in the app's assets. */
enum class BundledAsset(@param:StringRes val title: Int, @param:StringRes val licence: Int, val licenceFile: String) {
    RobotoFlex(R.string.licences_roboto_flex, R.string.licences_ofl, "RobotoFlex-OFL.txt"),
    RobotoMono(R.string.licences_roboto_mono, R.string.licences_ofl, "RobotoMono-OFL.txt"),
    RobotoSlab(R.string.licences_roboto_slab, R.string.licences_apache, "RobotoSlab-LICENSE.txt"),
    MaterialSymbols(R.string.licences_material_symbols, R.string.licences_apache, "MaterialSymbols-LICENSE.txt"),
}
